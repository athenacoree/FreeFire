# Supabase integration

Music uses the existing shared Supabase project used by Interaccion. It does not create a second project and does not modify Interaccion's existing tables.

## Android client

- Project URL and the **publishable** key are configured as Android BuildConfig values.
- The publishable key is designed for public clients. It is not an admin credential.
- Never add a Supabase secret key or `service_role` key to the Android app, Gradle files, GitHub, or an APK.
- The Android client uses Supabase Kotlin SDK 3.9.0, PostgREST, Auth, Storage, and Ktor Android engine 3.4.3.
- Supabase Kotlin requires Android API 26, so the app's `minSdk` is 26 (Android 8.0).
- Android internet permission is declared in the manifest.

## Database schema and isolation

Music tables are isolated by name in the shared project:
- Public catalog: `artists`, `albums`, `tracks`, `videos`.
- User data: `music_profiles`, `playlists`, `playlist_members`, `playlist_items`.
- Notifications: `push_devices`, `notifications`.

All Music tables have Row Level Security enabled. Public catalog tables permit read-only access to `anon` and `authenticated`; user data is restricted to authenticated users by ownership or playlist membership. Playlist authorization helpers live in the non-exposed `private` schema.

## Current catalog status

The schema and public-read policies are present. The latest direct count is **50 artists, 22 videos, 0 albums and 0 tracks**. Artists and videos can therefore appear after synchronization, while album and track sections remain empty until approved metadata is loaded through the trusted catalog-management process.

The app's connection check reads row IDs from the four public catalog tables. Catalog sync stores fetched metadata in Room for offline browsing. The current catalog sync is manual from the Explore tab.

## Migrations

The seven Music migration files are stored in `supabase/migrations`. The remote database had the first six Music migrations recorded; the missing `music_playlist_notifications` migration has now been applied to the shared project and verified in the remote migration history.

## Security notes

- Do not expose secret/service-role keys in the Android client.
- RLS is enabled for every Music table in the exposed `public` schema.
- The database security advisor currently warns that leaked-password protection is disabled. Enable it in Supabase Auth password security settings.
- The Supabase project is shared with Interaccion; do not drop, rename, or overwrite unrelated tables or policies.
