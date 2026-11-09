# Supabase integration

Music connects to the existing Supabase project used by Interaccion. It does not create a second project or modify Interaccion's existing tables.

## Android client

- Project URL and the **publishable** key are configured in `app/build.gradle.kts` as BuildConfig values.
- The publishable key is designed for public clients. It is not an admin credential.
- Never add a Supabase secret key or `service_role` key to the Android app, Gradle files, GitHub, or an APK.
- The app uses the Supabase Kotlin SDK and PostgREST to read the public music catalog.
- Android internet permission is declared in the manifest.

## First integration milestone

The Profile and settings screen has a connection check. It requests only row IDs from:
- `artists`
- `albums`
- `tracks`
- `videos`

The catalog tables have RLS policies for public read access. User-specific tables such as `music_profiles`, `playlists`, `playlist_members`, `playlist_items`, `push_devices`, and `notifications` remain protected by authenticated-user policies.

Guest mode remains local-only. This first milestone does not create accounts, upload media, or sync personal playlists yet.

## Playlist helper security

Playlist authorization helper functions live in the non-public `private` schema. RLS policies call these helpers, but they are not exposed as public PostgREST RPC endpoints.

## Next implementation steps

1. Add Auth sign-up/sign-in and persistent session handling.
2. Add Room as the local source of truth and sync catalog changes incrementally.
3. Add shared playlist operations behind authenticated RLS.
4. Add Storage only for authorized artwork/audio assets.
5. Add FCM push notifications after account and device-token flows are ready.
