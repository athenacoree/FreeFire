# Push notifications for Music

## Can notifications arrive while the app is closed?
Yes. Supabase can be the event/data backend, while Firebase Cloud Messaging (FCM) delivers Android push notifications. Supabase Realtime alone is not a reliable closed-app notification mechanism: Android may suspend the process, so the app cannot depend on an active socket.

## Delivery architecture
1. An authenticated Android installation obtains an FCM registration token.
2. The app stores/refreshes that token in `public.push_devices` for the signed-in user. Guest mode does not register a user push token.
3. A trusted event inserts a row into `public.notifications`.
4. A Supabase Database Webhook invokes a Supabase Edge Function.
5. The Edge Function reads that user's active FCM device tokens and sends the push through the FCM HTTP v1 API.
6. Android displays the notification; tapping it opens the relevant Music screen/deep link.

Supabase's official guide documents this pattern using Database Webhooks, Edge Functions and FCM: https://supabase.com/docs/guides/functions/examples/push-notifications

## Credentials and security
- Create a Firebase project only for Firebase Cloud Messaging; Music does not need Firebase Auth, Firestore or Realtime Database.
- Register the Android application in Firebase using the final Android application ID.
- Keep the Firebase service-account private key only in Supabase Edge Function secrets. Never commit it to GitHub or package it in the APK.
- The Android app contains only its normal Firebase client configuration and FCM SDK setup; never include a service-account key or Supabase service-role key.
- Enable RLS. A user may manage only their own device tokens and read only their own notifications. Clients cannot insert arbitrary notification rows; trusted server-side operations do that.
- Rotate/delete stale tokens and remove tokens on sign-out where appropriate.

## Notification categories
- Playlist collaboration: someone adds/removes a track or invites the user.
- Social/account events: invitation or account security event.
- Optional library updates: only if the user enables them.
- No push spam for every playback event. Notification preferences and rate limits are required.

## Guest mode and permissions
- Guest users remain local-first and do not need push notifications.
- Account users can opt in to push notifications.
- Android 13+ requires runtime notification permission (`POST_NOTIFICATIONS`). If denied, Music still works; in-app notification history can remain available after sign-in.
- Provide per-category controls and a global pause switch.

## Reliability and fallback
- Push delivery is best-effort, not guaranteed real-time delivery. FCM may delay, collapse or drop messages under device/network restrictions.
- Persist notification events in Supabase so the app can fetch unread notifications on next launch, even if a push was missed.
- Do not use Render. Supabase Edge Functions handle the delivery trigger.
- Keep payloads small and avoid sensitive content on lock-screen notifications; send an event ID/deep link and fetch details after app opens.

## Setup still required
- Music currently reuses the existing `interaccion` Supabase project to stay within the free-project limit; Music data remains in separate `music_*` tables and its own policies.
- Create a Firebase project and register `com.athenacoree.music` for FCM.
- Add the FCM Android SDK and notification service to the app.
- Store the FCM service-account credential as a Supabase secret.
- Deploy the Edge Function and configure a Database Webhook.
- Test foreground, background, force-stopped, denied-permission and token-refresh cases on a real Android device.


## Current integration boundary

The signed-in in-app inbox is implemented against the recipient-only `notifications` RLS policy. FCM delivery is not activated yet: the project still needs a Firebase Android app registration and service-account credentials stored as Supabase Edge Function secrets. Do not place Firebase service-account material in the Android app or GitHub repository. Until those credentials and the webhook are configured, push delivery cannot be claimed as working.


## Deployed Edge Function

The function is deployed at:

`https://wlbnulfixkfmvcifvpum.supabase.co/functions/v1/music-push-dispatch`

It has JWT verification disabled only because it is designed for a server-to-server database webhook. It rejects requests unless the `x-push-webhook-secret` header matches the server-side `PUSH_WEBHOOK_SECRET`. It reads device tokens using the service-role key available only in the Edge Function runtime and uses a Firebase service-account JSON secret to call FCM HTTP v1.

Before it can send anything, configure these Edge Function secrets in Supabase:
- `PUSH_WEBHOOK_SECRET`: a long random secret shared only with the database webhook.
- `FIREBASE_SERVICE_ACCOUNT_JSON`: the Firebase service-account JSON for the registered Firebase project.

Then create a Database Webhook on inserts into `public.notifications`, targeting this function and sending the webhook secret header. Do not put either secret in the Android app, GitHub, or client-side configuration. Firebase Android app registration and the `google-services.json` file are still required for Android devices to obtain FCM tokens.
