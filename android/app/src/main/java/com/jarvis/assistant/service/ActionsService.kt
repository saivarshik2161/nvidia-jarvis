package com.jarvis.assistant.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ActionResult {
    data class Success(val message: String) : ActionResult
    data class Failure(val message: String) : ActionResult
    data class ClarificationNeeded(val question: String) : ActionResult
}

class ActionsService(private val context: Context) {

    suspend fun executeAction(
        action: String,
        target: String?,
        params: com.jarvis.assistant.data.InterpretResponseParams
    ): ActionResult = withContext(Dispatchers.IO) {
        return@withContext when (action) {
            "open_app" -> openApp(params.app_name)
            "call_contact" -> callContact(params.contact_name)
            "send_message" -> sendMessage(params.contact_name, params.body)
            "set_alarm" -> setAlarm(params.time)
            "remember" -> ActionResult.Success(context.getString(R.string.memory_remembered))
            "forget_memory" -> ActionResult.Success(context.getString(R.string.memory_forgotten))
            "clarify" -> ActionResult.ClarificationNeeded(params.clarifying_question ?: "Could you clarify?")
            else -> ActionResult.Failure("Unknown action")
        }
    }

    private fun openApp(appName: String?): ActionResult {
        appName?.let { name ->
            val packageManager = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val activities = packageManager.queryIntentActivities(intent, 0)
            
            for (activity in activities) {
                val label = activity.loadLabel(packageManager).toString()
                if (label.equals(name, ignoreCase = true) || 
                    label.contains(name, ignoreCase = true) ||
                    name.contains(label, ignoreCase = true)) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(activity.activityInfo.packageName)
                    launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    launchIntent?.let { context.startActivity(it) }
                    return ActionResult.Success("Opening $label.")
                }
            }
        }
        return ActionResult.Failure(context.getString(R.string.error_no_matching_app))
    }

    private fun callContact(contactName: String?): ActionResult {
        contactName?.let { name ->
            val phoneNumber = findContactPhoneNumber(name)
            phoneNumber?.let { number ->
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$number")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return ActionResult.Success(context.getString(R.string.call_prepared, name))
            }
        }
        return ActionResult.Failure(context.getString(R.string.error_no_matching_contact))
    }

    private fun findContactPhoneNumber(name: String): String? {
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")
        
        contentResolver.query(uri, null, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return cursor.getString(numberIndex)
            }
        }
        return null
    }

    private fun sendMessage(contactName: String?, body: String?): ActionResult {
        contactName?.let { name ->
            body?.let { message ->
                val phoneNumber = findContactPhoneNumber(name)
                phoneNumber?.let { number ->
                    try {
                        SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
                        return ActionResult.Success(context.getString(R.string.message_sent, name))
                    } catch (e: Exception) {
                        Log.e("JARVIS", "SMS send failed", e)
                        return ActionResult.Failure(context.getString(R.string.error_sms_failed))
                    }
                }
            }
        }
        return ActionResult.ClarificationNeeded("Who should I send the message to and what should it say?")
    }

    private fun setAlarm(time: String?): ActionResult {
        time?.let { timeStr ->
            val parsedTime = parseTime(timeStr)
            parsedTime?.let { (hour, minute) ->
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return ActionResult.Success(context.getString(R.string.alarm_set, formatTime(hour, minute)))
                }
            }
        }
        return ActionResult.ClarificationNeeded("What time should I set the alarm for?")
    }

    private fun parseTime(timeStr: String): Pair<Int, Int>? {
        val cleaned = timeStr.trim().lowercase().replace(" ", "")
        
        // 24-hour format: HH:MM or HHMM
        val regex24 = "^(?:([01]?\\d|2[0-3])):?([0-5]\\d)$".toRegex()
        val match24 = regex24.matchEntire(cleaned)
        if (match24 != null) {
            return Pair(match24.groupValues[1].toInt(), match24.groupValues[2].toInt())
        }

        // 12-hour format: h:mm AM/PM or h AM/PM
        val regex12 = "^(?:(\\d{1,2})):?(\\d{2})?(am|pm)$".toRegex()
        val match12 = regex12.matchEntire(cleaned)
        if (match12 != null) {
            var hour = match12.groupValues[1].toInt()
            val minute = match12.groupValues[2]?.toInt() ?: 0
            val ampm = match12.groupValues[3]
            
            when {
                ampm == "pm" && hour != 12 -> hour += 12
                ampm == "am" && hour == 12 -> hour = 0
            }
            
            if (hour in 0..23 && minute in 0..59) {
                return Pair(hour, minute)
            }
        }

        return null
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val ampm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", displayHour, minute, ampm)
    }
}