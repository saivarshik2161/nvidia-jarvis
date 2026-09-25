# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x     | :white_check_mark: |

## Reporting a Vulnerability

Please report security vulnerabilities by opening an issue with the "security" label or by emailing security@jarvis.example.com.

## Security Design

### Authentication
- Android app authenticates to backend using `JARVIS_CLIENT_TOKEN` (Bearer token)
- Backend validates token on all protected endpoints
- Invalid tokens return HTTP 401
- NVIDIA API key never exposed to Android client

### Data Protection
- No audio stored by JARVIS
- Only recognized text sent to backend
- Memories stored in backend database, not on device
- User/device isolation enforced at database level
- No logging of sensitive data (phone numbers, SMS bodies, memories, transcripts)

### Permissions
- Minimum required permissions requested at runtime
- RECORD_AUDIO for speech input
- READ_CONTACTS for contact actions
- SEND_SMS for automatic SMS (requested on first use)
- No CALL_PHONE permission (uses ACTION_DIAL)

### Network Security
- All backend communication over HTTPS in production
- Certificate pinning recommended for production
- Short request timeouts
- No infinite retries

### Secrets Management
- NVIDIA_API_KEY stored in backend environment only
- JARVIS_CLIENT_TOKEN configurable via environment
- DATABASE_URL configurable for PostgreSQL
- No secrets committed to repository
- Use .env files for local development (in .gitignore)

### Production Hardening
- Replace shared JARVIS_CLIENT_TOKEN with per-device authentication
- Use strong random tokens (32+ bytes)
- Enable HTTPS with valid certificates
- Configure rate limiting
- Enable audit logging for memory operations
- Regular security updates for dependencies

## Known Limitations (v1)
- Wake-word detection not implemented (architecture supports future addition)
- Shared development token (upgrade for production)
- SQLite default (use PostgreSQL for production)
- No end-to-end encryption for memory data at rest