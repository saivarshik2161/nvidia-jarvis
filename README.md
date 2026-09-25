# JARVIS - Private Android Assistant

A privacy-focused, on-device voice assistant for Android with natural language understanding powered by NVIDIA's Nemotron models.

## Features

- **Natural Language Understanding** - No predefined commands needed
- **Tap-to-Speak Interface** - Simple, visible button for voice input
- **Privacy-First Design** - Audio never stored, only text sent to backend
- **Approved Capabilities**:
  - Open installed apps by name
  - Call contacts (opens dialer with number pre-filled)
  - Send SMS messages automatically
  - Set exact-time alarms
  - Remember/forget explicit memories
- **Male TTS Voice Default** - Prefers male Android TTS voice when available
- **Futuristic Dark UI** - Black interface with green/cyan/blue accent lighting
- **Offline-Ready Architecture** - Wake-word system can be added later

## Architecture

```
┌─────────────┐     HTTPS      ┌─────────────┐     HTTPS      ┌──────────────────┐
│   Android   │ ─────────────▶ │   Backend   │ ─────────────▶ │   NVIDIA API     │
│   App       │   JSON + Auth  │  (FastAPI)  │   OpenAI-compat│  (Nemotron)      │
│             │ ◀───────────── │             │ ◀───────────── │                  │
└─────────────┘   JSON         └─────────────┘   JSON         └──────────────────┘
      │                              │
      │                              ▼
      │                      ┌─────────────┐
      │                      │  Database   │
      │                      │ (SQLite/PG) │
      │                      └─────────────┘
      ▼
┌─────────────┐
│  Android    │
│  Actions    │
│  (Local)    │
└─────────────┘
```

## Quick Start

### Prerequisites

- Android Studio Ladybug (2024.2.1) or later
- Android SDK 34
- JDK 17
- Python 3.11+ (for backend)
- Docker & Docker Compose (optional)

### Backend Setup

1. Navigate to backend directory:
```bash
cd backend
```

2. Create environment file:
```bash
cp .env.example .env
# Edit .env with your NVIDIA API key
```

3. Install dependencies:
```bash
pip install -r requirements.txt
```

4. Initialize database:
```bash
python ../scripts/init_db.py
```

5. Run backend:
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Android App Setup

1. Open `android/` in Android Studio

2. Configure `local.properties` with your SDK path and JARVIS configuration:
```
sdk.dir=/path/to/android/sdk
JARVIS_CLIENT_TOKEN=your-secure-token
BACKEND_BASE_URL=http://YOUR_IP:8000/
```
Note: `local.properties` is git-ignored and should never be committed. The `JARVIS_CLIENT_TOKEN` must match the token configured in the backend's `.env` file.

3. Build and run on device/emulator

### Docker Compose (Recommended)

```bash
# From project root
docker-compose up -d --build

# Backend available at http://localhost:8000
# PostgreSQL available at localhost:5432 (with --profile postgres)
```

## Configuration

### Backend Environment Variables (backend/.env)

| Variable | Description | Default |
|----------|-------------|---------|
| `NVIDIA_API_BASE_URL` | NVIDIA API endpoint (direct NVIDIA hosted API) | `https://integrate.api.nvidia.com/v1` |
| `NVIDIA_API_KEY` | **Required** - Your NVIDIA API key (never in Android code) | - |
| `NVIDIA_MODEL` | Model to use | `nvidia/nemotron-3.5-lightning-30b-a3b` |
| `JARVIS_CLIENT_TOKEN` | Auth token for Android↔Backend | `dev-token-change-in-production` |
| `DATABASE_URL` | Database connection string | `sqlite:///./jarvis.db` |
| `REQUEST_TIMEOUT` | Request timeout in seconds | `10.0` |
| `MAX_RETRIES` | Max retry attempts for NVIDIA API | `2` |

### Android Local Configuration (android/local.properties)

| Property | Description | Default |
|----------|-------------|---------|
| `sdk.dir` | Android SDK path | (required) |
| `JARVIS_CLIENT_TOKEN` | Client token for backend auth | `dev-token-change-in-production` |
| `BACKEND_BASE_URL` | Backend API base URL | `http://10.0.2.2:8000/` |

The Android app reads these values from `local.properties` at build time and exposes them via generated `BuildConfig` fields. The `local.properties` file is git-ignored.

### NVIDIA API Setup

The development backend uses the **direct NVIDIA hosted API** (not Nebius Token Factory).

1. Get an API key from [NVIDIA NGC](https://www.nvidia.com/en-us/gpu-cloud/)
2. Set `NVIDIA_API_KEY` in `backend/.env` (this file is git-ignored)
3. The API base URL is `https://integrate.api.nvidia.com/v1`
4. The model ID is configurable via `NVIDIA_MODEL` - check [NVIDIA's model catalog](https://integrate.api.nvidia.com/v1/models) for available models

**Important**: The NVIDIA API key must **never** be placed in Android code. It is only used by the backend.

## Natural Language Examples

| User Says | JARVIS Action |
|-----------|---------------|
| "Open Spotify" | Launch Spotify app |
| "Call Mom" | Opens dialer with Mom's number |
| "Send Mom a message saying I'll be late" | Sends SMS to Mom |
| "Set an alarm for 7 AM" | Creates alarm for 07:00 |
| "Wake me at 18:30" | Creates alarm for 18:30 |
| "Remember that my mother is called Mom" | Stores memory in backend |
| "Forget that my mother is called Mom" | Deletes specific memory |
| "Clear all my memories" | Deletes all memories for device |

## Project Structure

```
jarvis/
├── android/                 # Android app (Kotlin, Jetpack Compose)
│   └── app/
│       ├── src/main/java/com/jarvis/assistant/
│       │   ├── data/        # API models, Repository
│       │   ├── di/          # Hilt dependency injection
│       │   ├── service/     # Speech, TTS, Actions
│       │   ├── ui/          # Compose screens & ViewModels
│       │   └── util/        # Utilities
│       └── build.gradle.kts
├── backend/                 # FastAPI backend
│   ├── app/
│   │   ├── api/             # REST endpoints
│   │   ├── core/            # Config, auth
│   │   ├── db/              # Database setup
│   │   ├── models/          # SQLAlchemy models
│   │   ├── schemas/         # Pydantic schemas
│   │   └── services/        # NVIDIA, Memory services
│   ├── tests/               # Pytest tests
│   ├── requirements.txt
│   └── Dockerfile
├── database/                # Alembic migrations
│   ├── migrations/
│   └── alembic.ini
├── docs/                    # Documentation
├── scripts/                 # Utility scripts
├── reports/                 # Test reports
├── docker-compose.yml
├── README.md
├── LICENSE
├── SECURITY.md
└── .gitignore
```

## Testing

### Backend Tests

```bash
cd backend
pytest -v --cov=app --cov-report=term-missing
```

### Android Tests

```bash
cd android
./gradlew test
```

### Lint

```bash
cd android
./gradlew lint
```

## Building APK

```bash
cd android
./gradlew assembleRelease

# Output: android/app/build/outputs/apk/release/app-release.apk
```

## Installation on Nothing Phone 3a

1. Enable Developer Options: Settings → About Phone → Build Number (tap 7 times)
2. Enable USB Debugging: Settings → System → Developer Options → USB Debugging
3. Connect via USB
4. Install APK:
```bash
adb install android/app/build/outputs/apk/release/app-release.apk
```
5. Grant permissions when prompted:
   - Microphone (for voice input)
   - Contacts (for calling/messaging)
   - SMS (for sending messages)

## Privacy Design

- **No audio storage** - Speech recognition happens on-device via Android SpeechRecognizer
- **Text-only transmission** - Only final transcript sent to backend
- **Memory isolation** - Memories tied to device_id, never shared
- **No conversation logging** - Only explicit memories stored
- **Local TTS** - Text-to-speech runs entirely on device

## Security Design

- **Token-based auth** - `JARVIS_CLIENT_TOKEN` validates Android→Backend requests
- **API key isolation** - NVIDIA API key never leaves backend; **never in Android code**
- **Minimal permissions** - Only RECORD_AUDIO, READ_CONTACTS, SEND_SMS requested
- **No CALL_PHONE** - Uses ACTION_DIAL (user confirms call)
- **HTTPS required** - Production must use TLS
- **Input validation** - All API inputs validated via Pydantic

## Limitations (v1)

- No wake-word detection (architecture supports future addition)
- SQLite default (use PostgreSQL for production)
- Shared development token (replace with per-device auth for production)
- Exact-time alarms only (no recurring)
- Requires app to be open and phone unlocked
- No end-to-end encryption for stored memories

## Future Wake-Word Integration

The architecture is designed for wake-word addition:
1. Add foreground service with `FOREGROUND_SERVICE_MICROPHONE`
2. Integrate Porcupine/Picovoice or custom model
3. On wake-word → trigger same `onTapToSpeak()` flow
4. No changes needed to backend or action execution

## Deployment

### Production Backend

1. Use PostgreSQL:
```bash
DATABASE_URL=postgresql+asyncpg://user:pass@host/db docker-compose --profile postgres up -d
```

2. Set strong tokens:
```bash
JARVIS_CLIENT_TOKEN=$(openssl rand -hex 32)
```

3. Enable HTTPS with reverse proxy (nginx/Caddy)

4. Configure rate limiting and monitoring

### Android Release

1. Generate signing key:
```bash
keytool -genkey -v -keystore jarvis-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias jarvis
```

2. Configure signing in `build.gradle.kts`

3. Build signed bundle:
```bash
./gradlew bundleRelease
```

## License

MIT License - see [LICENSE](LICENSE)

## Security

See [SECURITY.md](SECURITY.md) for vulnerability reporting and security design details.