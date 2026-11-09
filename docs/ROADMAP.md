# Music implementation roadmap

- [x] Create Android/Kotlin/Compose project foundation.
- [x] Add initial navigation shell: Home, Library, Videos, Playlists, Profile.
- [x] Add selectable light/dark visual modes.
- [x] Present guest mode as the initial account experience.
- [x] Persist appearance and data-saver preferences with DataStore.
- [x] Add local Room database for tracks, favorites and playlists.
- [x] Add remote catalog models and version-aware local Room mapping.
- [x] Implement version-aware catalog synchronization for artists, albums, tracks and videos.
- [x] Integrate Media3 native playback for user-selected local audio files.
- [x] Add Coil image loading with a 48 MB disk cache and data-saver image suppression.
- [x] Add optional Supabase email/password authentication with guest mode preserved.
- [x] Load and refresh Supabase Auth sessions on Android.
- [x] Create Music-only account profiles without touching Interaccion profiles.
- [x] Add a read-only Supabase catalog connection check.
- [x] Add shared playlist creation, owner invitations, editor roles and track insertion with RLS.
- [x] Add Android share intents and handoff to the official YouTube app/player.
- [x] Persist data-saver preference and suppress catalog artwork requests while enabled.
- [ ] Test offline behavior, local file playback and remote-service failure behavior.
- [ ] Confirm the Android compile workflow passes after the Kotlin/AndroidX compatibility fixes.
- [ ] Test registration, email confirmation, sign-in, session restoration and sign-out on a real Android device.

- [x] Reuse the existing Interaccion Supabase project without changing its existing tables.
- [x] Apply Music schema and security migrations `0001` through `0004`.
- [ ] Create Firebase Android app registration and add its configuration file (no Firebase Auth or Firestore required).
- [ ] Add FCM token lifecycle, notification channels and Android 13+ permission handling.
- [x] Deploy the secured Supabase push Edge Function.\n- [ ] Configure Firebase service-account and webhook secrets, then connect the Database Webhook.
- [x] Add signed-in in-app notification inbox.
- [ ] Add FCM device-token registration, notification channels and deep-link routing after Firebase app configuration is supplied.
