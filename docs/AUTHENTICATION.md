# Music authentication

## Current account flow

- Guest mode remains available and does not create cloud library rows.
- Users can register and sign in with email and password.
- Supabase Auth handles password hashing, verification settings, access tokens, and refresh tokens.
- The Android app uses Supabase Kotlin 3.9.0 (Android API 26+) and loads a stored session with automatic token refresh.
- Signing out clears the Supabase Auth session on this device.
- A Music-only profile row is created only when the account is registered by Music (the signup includes an app marker). The shared Interaccion project users are not automatically copied into Music.

## Email confirmation

Hosted Supabase projects commonly require email confirmation by default. After registration, the app tells the user to check their email if confirmation is required. The user can then return to the app and sign in.

The project owner can review this under Authentication > Providers > Email in the Supabase Dashboard. Supabase's default email sender has testing limits; wider registration may require configuring a trusted SMTP provider. Do not disable email confirmation merely to make the UI appear to work.

## Security

- The Android app uses only the publishable key.
- Passwords are sent directly to Supabase Auth over HTTPS; they are not written to the Music database or local app preferences.
- Row Level Security remains responsible for access to Music user data.
- The profile trigger writes only to `music_profiles`; it does not alter Interaccion's `profiles` table.
- Never put a Supabase secret or service-role key in the Android app or repository.
- Supabase's security advisor currently reports leaked-password protection disabled; enable it in Authentication password-security settings.

## Current limitations

- Email/password authentication is implemented; social sign-in and password reset UI are not yet implemented.
- The guest library is still local-only.
- Cloud synchronization of the full personal library and password-reset UI remain future work; collaborative cloud playlists are already implemented.
- GitHub Actions runs `:app:assembleDebug` on pushes to `main` and uploads a signed debug APK artifact with a seven-day retention period.
- The dependency-upgrade build is currently being verified; a physical-device sign-in/session test is still required.
