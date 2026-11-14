# Compatibility and backend audit — 10 November 2026

## Target platform
- Minimum Android: API 26 (Android 8.0).
- Compile SDK: 37 (Android 37 preview channel; used only for compilation, while targetSdk remains 35 and minSdk remains 26).
- Target SDK: 35 (kept unchanged until runtime-permission and foreground-service behavior is tested on physical devices).
- Java toolchain: 17.
- Gradle: 9.3.1.
- Android Gradle Plugin: 9.1.1.
- Kotlin: 2.4.21.
- KSP: 2.3.13.

## Updated application dependencies
- Compose BOM: 2026.06.00 (includes current Compose libraries that require compileSdk 37 and AGP 9.1.1).
- AndroidX Core KTX: 1.19.1.
- Activity Compose: 1.13.0.
- Lifecycle: 2.11.0.
- Browser: 1.10.0.
- Navigation Compose: 2.10.2.
- Room: 2.8.5.
- DataStore Preferences: 1.2.1.
- Media3 ExoPlayer/Session: 1.11.1.
- Supabase Kotlin BOM: 3.9.0.
- Ktor Android engine: 3.4.3.
- Coil: 3.6.3 with Ktor 3 network integration.

## Issues found and corrected
1. App declared minSdk 23 although the Supabase Kotlin SDK requires API 26. Updated to API 26 / Android 8.0.
2. App pinned old Lifecycle 2.8.7 and Browser 1.8.0 using resolutionStrategy.force, overriding newer dependencies. Removed the forced downgrade.
3. Media3, Compose, Room-adjacent and other AndroidX dependencies were behind current stable releases. Updated to a mutually compatible stable set.
4. Coil was upgraded to 3.x; migrated both Compose image import and the custom ImageLoader imports, and added the Ktor network fetcher.
5. Media scanner required both audio and video permissions before scanning anything. It now proceeds with whichever Android 13+ media permission was granted and uses MediaStore query arguments for five-item paging on API 26+.
6. Playback commands issued while MediaController was connecting could be dropped. They are now queued until the controller is ready.
7. Supabase catalog summary downloaded all IDs and could undercount at the PostgREST row cap. It now requests exact counts.
8. Catalog synchronization previously fetched each table in one request and could silently stop at the server row cap. It now pages in 500-row ranges.
9. UI hid Supabase Auth, catalog, and notification errors. It now surfaces a shortened underlying error message for diagnosis.
10. Remote Supabase migration history was missing the repository's seventh Music migration. Applied `music_playlist_notifications` and verified both notification triggers.
11. RLS was enabled on all ten Music tables, but the `anon` database role still had unnecessary table-level privileges on private Music tables. Revoked those privileges and verified they are false; authenticated access remains granted and RLS-scoped.

## Backend verification
- Supabase project URL matches the app BuildConfig.
- App key matches the project's active publishable key (not a secret/service-role key).
- All ten Music tables have RLS enabled.
- Public catalog tables are readable by anon/authenticated under read-only RLS policies.
- Anonymous role has no table-level SELECT privileges on Music profiles, playlists, playlist members/items, device tokens, or notifications.
- Authenticated role retains playlist SELECT privilege; row access remains constrained by RLS.
- Migration `music_playlist_notifications` is recorded remotely.
- Invitation and track-added notification triggers exist on the intended tables.
- Catalog data tables currently contain zero rows: artists, albums, tracks and videos. A successful connection therefore still yields an empty catalog until approved metadata is loaded.
- Supabase security advisor warning remains: leaked-password protection is disabled. Enable it in Supabase Auth password security settings.
- Performance advisor reports 16 unused indexes across the shared project (including Interaccion and newly created Music tables). They are not being removed: usage is not yet representative, and dropping shared-project indexes could harm Interaccion.

## Verification still pending
- No Gradle build or test was run during the current static review, in line with the instruction to pause test compilations. The repository workflow is manual-only (`workflow_dispatch`), not triggered by pushes to `main`.
- Physical-device tests remain necessary for Android 8, Android 13+ partial media permissions, file URI persistence, background playback, email confirmation and session restoration.
- FCM push delivery remains unconfigured: the Edge Function is deployed, but Firebase Android registration, service-account secret, webhook secret and Database Webhook still need to be supplied/configured.
- The catalog remains empty until data is inserted by a trusted catalog-management process.
