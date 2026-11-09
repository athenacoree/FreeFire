# Music architecture

## Product decisions
- Android-native application written in Kotlin.
- Jetpack Compose UI.
- Light and dark themes, selected by the user.
- Guest mode is available; account creation is optional.
- Mixed library: a unified view plus dedicated artist, album, song, video and playlist sections.

## Runtime boundaries
The Android app must remain useful when every remote service is unavailable.

### On-device
- UI and navigation.
- Local library metadata and user preferences.
- Playback of media the user owns or is authorized to play.
- Cache management and playback queue.
- Background playback for authorized media through Android Media3.

### Supabase
- Optional account authentication.
- Shared catalog metadata, profiles and collaborative playlists.
- Version metadata for incremental synchronization.
- Storage only for permitted assets and files.

### Render
- Not required at startup or during normal navigation.
- No permanent data, media files, playback sessions or essential APIs.
- Reserved for optional, exceptional server-side tasks only.
- The app must degrade gracefully if Render is asleep or unavailable.

## Synchronization principles
1. Read local data first.
2. Sync only when useful and connectivity permits.
3. Compare per-record versions or checksums.
4. Fetch only changed metadata/assets.
5. Never delete a user's library when a remote service is unavailable.
6. Keep cache eviction separate from user-owned downloads.

## Security
- Never commit Supabase secrets, service-role keys, signing keys or local.properties.
- Android clients may only use a Supabase publishable key with correctly configured RLS.
- Never put a service-role or secret key in the APK.
- Guest-only local data must not silently be uploaded to a shared account.

## YouTube
Use the official YouTube player/embed for YouTube URLs. Do not extract audio, download/cache YouTube audiovisual streams, or provide background playback through the YouTube player. Native downloads and background playback are reserved for media with appropriate rights and direct authorized sources.


## Push notifications
For signed-in users, Android push delivery uses Firebase Cloud Messaging (FCM) with Supabase Database Webhooks and Edge Functions. Supabase persists notification events; FCM delivers the OS notification while the app process is closed. Realtime is not used as a substitute for OS push. Guests remain local-only. Android 13+ notification permission is requested at runtime. See [PUSH_NOTIFICATIONS.md](PUSH_NOTIFICATIONS.md).

