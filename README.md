# Music

Aplicación Android nativa para organizar y reproducir música y vídeos autorizados.

## Estado inicial
Esta primera base utiliza Kotlin y Jetpack Compose. La experiencia está pensada para funcionar **local-first**: la interfaz y la biblioteca local no dependen de Render.

## Decisiones del producto
- Tema claro y oscuro, seleccionables desde la aplicación.
- Uso como invitado; la cuenta será opcional.
- Biblioteca mixta: vista general y apartados de artistas, álbumes, canciones y vídeos.
- Supabase será el servicio de sincronización y datos compartidos.
- Render será opcional y se reservará para tareas especiales que realmente necesiten servidor.

## Requisitos
- Android Studio compatible con Android Gradle Plugin 8.7.3.
- JDK 17.
- Android SDK 35.

## Configuración
Abre el proyecto en Android Studio y sincroniza Gradle. No se incluyen claves ni secretos de Supabase en el repositorio. La configuración de Supabase se añadirá mediante propiedades locales seguras cuando se integre el módulo de datos.

## Restricciones de contenido
La reproducción en segundo plano y las descargas se implementarán para archivos que el usuario posea o tenga permiso de utilizar. Los vídeos de YouTube se reproducirán mediante el reproductor oficial, sin extracción ni descarga.

## Push notifications
Signed-in users will be able to opt into Android push notifications. Supabase stores notification events and triggers an Edge Function; Firebase Cloud Messaging (FCM) delivers them even when Music is closed. Guest mode stays local-only. Setup and security details: [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md).
