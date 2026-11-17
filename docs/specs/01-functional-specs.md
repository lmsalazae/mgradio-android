# SPEC-01: Especificaciones Funcionales — mgRadio

**Versión:** 2.0  
**Estado:** Aprobada  

---

## 1. Requisitos Funcionales (RF)

### RF-01: Catálogo Offline-First y Sincronización Firestore
- El catálogo se lee desde **Room** de forma reactiva (`Flow`).
- Al iniciar (si la caché está vacía o según política de actualización) y al pulsar el botón **Actualizar**, se sincroniza la colección `stations` desde **Firebase Firestore**.
- La sincronización preserva `isFavorite` del usuario, mantiene los `tags` de Firestore y actualiza o elimina estaciones que ya no existan en el catálogo remoto (respetando la integridad de favoritos/historial).
- Si falla la sincronización por red, se muestra un aviso no bloqueante y se continúa operando con el catálogo local.

### RF-02: Búsqueda y Filtrado Multidimensional
- **Búsqueda normalizada sin acentos (`Normalizer.NFD`)** sobre:
  - Nombre de la emisora (`nombre`)
  - Frecuencia (`frecuencia`, ej. `"97.7"`, `"104.1"`)
  - Categoría y etiquetas (`categoria`, `tags`)
- Priorización de coincidencias que inician por el prefijo buscado.
- Filtros combinables por **País**, **Ciudad**, **Categoría** y pestaña (**Todas**, **Favoritas**, **Recientes**, **Offline**). Los contadores numéricos de cada pestaña reflejan en tiempo real los filtros activos de país, ciudad, categoría y búsqueda.

### RF-03: Reproducción Media3 (`ExoPlayer` + `MediaLibraryService`)
- Reproducción continua en segundo plano con notificación multimedia del sistema y controles en pantalla de bloqueo.
- Soporte para streams Icecast/Shoutcast (HTTP/HTTPS cleartext permitido vía `network_security_config`), HLS (`.m3u8`) y playlists `.pls` / `.m3u` (mediante resolución o soporte de `ExoPlayer`).
- Manejo automático de **Audio Focus** y desconexión de audífonos/Bluetooth (`setHandleAudioBecomingNoisy(true)`).
- **Metadatos ICY en vivo ("Now Playing"):** Envía header `Icy-MetaData: 1`, escucha `Player.Listener.onMediaMetadataChanged` y expone el título/artista actual en `BottomPlayerBar`, `FullScreenPlayerSheet` y notificación del sistema.

### RF-04: Reconexión Automática Resiliente
- Si un stream activo sufre error de red/buffering (`PlaybackException` de tipo IO/timeout), el servicio reintenta automáticamente hasta **3 veces** con backoff exponencial (1s, 2s, 4s) antes de detenerse.
- Solo tras agotar los reintentos (o ante error permanente 404/codec no soportado), notifica el error en español y marca la emisora como Offline localmente.

### RF-05: Favoritos, Historial de Recientes y Persistencia de Estado (`DataStore`)
- Alternar favorito (`isFavorite`) desde cualquier tarjeta, mini-player o reproductor completo.
- **Historial de Recientes:** Registra cada emisora reproducida (`recent_plays` con `playedAt` timestamp), ordenado del más reciente al más antiguo, visible en la pestaña **Recientes** y en Android Auto.
- **DataStore Preferences:** Persiste último país seleccionado, última ciudad seleccionada, última emisora reproducida (para reanudar o mostrar en mini-player al abrir la app) y preferencias de tema (`themeMode`, `useDynamicColor`).

### RF-06: Control de Volumen en Reproductor Completo
- `FullScreenPlayerSheet` incluye un control deslizante (`Slider`) conectado a `AudioManager` (`STREAM_MUSIC`) con botones de silencio rápido y sincronización con botones físicos de volumen del dispositivo.

### RF-07: Temporizador de Apagado (Sleep Timer)
- Selector rápido en `FullScreenPlayerSheet` con los tiempos configurados por el usuario (por defecto `15, 30, 45, 60 min`, o `Cancelar`).
- Desde **Ajustes** (`SettingsSheet`), el usuario puede activar/desactivar opciones predefinidas (`5, 10, 15, 20, 30, 45, 60, 90, 120 min`) o agregar tiempos personalizados en minutos (mínimo 1 tiempo activo).
- Muestra la cuenta regresiva activa en tiempo real; al llegar a `0`, detiene la reproducción suavemente y notifica al usuario.

### RF-08: Grabación de Stream (`StreamRecorder`)
- Botón **Grabar / Detener** en `FullScreenPlayerSheet`.
- Abre una conexión HTTP independiente al `streamUrl` de la emisora activa (sin interrumpir la reproducción ni causar cortes), guarda el audio con nombre `<Emisora>_<YYYYMMDD_HHmmss>.mp3` (o `.aac` según `Content-Type`), mostrando duración/tamaño en vivo.
- **Ruta configurable en Ajustes:** Por defecto guarda en `Music/mgRadio/` vía `MediaStore`; desde `SettingsSheet` el usuario puede elegir cualquier carpeta mediante SAF (`OpenDocumentTree` con permisos persistentes) o restablecer la ruta predeterminada.

### RF-09: Ecualizador Integrado (`AudioEqualizerController`)
- Conectado al `audioSessionId` real de `ExoPlayer` mediante `android.media.audiofx.Equalizer`.
- Accesible desde `FullScreenPlayerSheet` (`EcualizadorSheet`): interruptor Activar/Desactivar, selector de presets del sistema (Normal, Clásica, Dance, Pop, Rock, Jazz, etc.) y sliders individuales por banda de frecuencia.

### RF-10: Soporte Android Auto (`MediaLibraryService`)
- `RadioMediaService` hereda de `MediaLibraryService` e implementa `onGetLibraryRoot`, `onGetChildren` y `onSearch` exponiendo las categorías navegables: **Favoritas**, **Recientes** y **Todas** (agrupadas o filtradas).
- Declaración de `com.google.android.gms.car.application` (`automotive_app_desc.xml`) en `AndroidManifest.xml`.

### RF-11: Soporte Android TV
- Mantiene `LEANBACK_LAUNCHER`, `tv_banner` y navegación completa por **D-Pad / Control remoto** (`onPreviewKeyEvent`, teclas multimedia `MediaPlayPause`/`MediaPlay`/`MediaPause`/`MediaStop`, escape desde buscador con `DirectionDown` e indicadores de foco claros) en pantalla principal, pestañas, reproductor completo, diálogo Sleep Timer y ajustes.
- **Prevención de apagado / suspensión del TV respetando Sleep Timer:** Mientras exista reproducción activa (`PLAYING`, `BUFFERING`, `RECONNECTING`), la ventana activa `FLAG_KEEP_SCREEN_ON` (sumado al `WAKE_MODE_NETWORK` de Media3). Al pausar, detener o finalizar el **Sleep Timer**, el flag se limpia automáticamente para permitir que el TV se apague o suspenda con normalidad.

### RF-12: Temas Visuales (Oscuro AMOLED, Claro y Material You)
- Soporte para `ThemeMode`: **Sistema**, **Claro**, **Oscuro AMOLED** (`#0E1217` / `#000000` fondo puro) y **Material You** (`useDynamicColor` en Android 12+), configurable desde el diálogo/hoja de **Ajustes**.

### RF-13: Cero Telemetría
- Sin `firebase-analytics` ni recolección de analíticas o identificadores de dispositivo.
