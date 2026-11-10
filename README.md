# Music

Aplicación Android nativa para organizar y reproducir música y vídeos autorizados.

## Plataforma y herramientas
- Android 8.0 o superior (API 26+).
- Kotlin 2.4.21, Jetpack Compose y Material 3.
- Android Gradle Plugin 8.13.2, Gradle 8.13 y JDK 17.
- Android SDK de compilación 37 (canal preview, solo para compilar); target SDK 35. La versión mínima de instalación sigue siendo Android 8.0 (API 26).
- Room para la biblioteca local y DataStore para preferencias.
- Media3 para reproducción nativa de archivos locales/autorizados.
- Supabase Kotlin 3.9.0 para Auth y catálogo compartido.
- Coil 3.6.3 para portadas, con caché local limitada.

## Decisiones del producto
- Tema claro y oscuro, seleccionables desde la aplicación.
- Uso como invitado; la cuenta es opcional.
- Biblioteca local-first: la aplicación sigue funcionando sin conexión.
- Supabase es el servicio de autenticación, catálogo y listas colaborativas.
- Music reutiliza el proyecto Supabase compartido con Interaccion, manteniendo las tablas y políticas de Music separadas.
- Render no es necesario para iniciar la aplicación ni para sus funciones esenciales.

## Configuración
Abre el repositorio en Android Studio compatible con AGP 9.1.1, con JDK 17 y Android SDK 36, y sincroniza Gradle.

La URL de Supabase y la clave **publishable** están definidas como valores públicos de configuración Android. Una clave publishable está diseñada para clientes móviles y no es una credencial administrativa. Nunca agregues una clave Supabase secret o `service_role` al APK, al repositorio o a GitHub.

## Construcción de APK
El workflow de GitHub Actions compila ` :app:assembleDebug ` al actualizar `main` y publica un APK de prueba firmado en el artefacto `Music-test-debug-APK`. No genera un AAB de producción.

## Estado del catálogo
Las tablas de artistas, álbumes, canciones y vídeos ya existen y tienen RLS/políticas de lectura pública. En este momento están vacías; la app podrá conectarse correctamente, pero no mostrará contenido hasta que se carguen metadatos aprobados en el catálogo.

## Restricciones de contenido
La reproducción en segundo plano y las descargas se limitan a archivos que el usuario posea o tenga permiso de utilizar. Los vídeos de YouTube se abren mediante el reproductor oficial; no se extrae, descarga ni reproduce su audio en segundo plano.

## Documentación
- [Arquitectura](docs/ARCHITECTURE.md)
- [Integración Supabase](docs/SUPABASE_INTEGRATION.md)
- [Autenticación](docs/AUTHENTICATION.md)
- [Almacenamiento local](docs/LOCAL_STORAGE.md)
- [Notificaciones push](docs/PUSH_NOTIFICATIONS.md)
- [Auditoría de compatibilidad](docs/COMPATIBILITY_AUDIT.md)

## Notificaciones push
La bandeja dentro de la app está conectada a Supabase para usuarios registrados. La entrega push mediante FCM aún requiere terminar la configuración Firebase y el webhook; no se considera activa hasta completar esas pruebas.
