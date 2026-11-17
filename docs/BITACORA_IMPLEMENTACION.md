# Bitácora de Cambios e Implementaciones — mgRadio

Registro cronológico de especificaciones, decisiones técnicas, cambios aplicados y pruebas ejecutadas sobre **mgRadio**.

> **Regla obligatoria:** Toda nueva funcionalidad o ajuste debe seguir el flujo **Spec-First** (actualizar especificaciones y reglas antes de codificar) y finalizar registrando aquí los cambios realizados y los resultados de pruebas.

---

## [2026-11-17] Creación de mgRadio, Respaldo de `radio-android` y Documentación Inicial (Spec-First)

### Contexto y Respaldo
- Se respaldó el proyecto original `/home/mick/Projects/radio-android` en `/home/mick/Projects/_archive/radio-android-backup-20261117.tar.gz` (142 MB) y se archivó en `/home/mick/Projects/_archive/radio-android`.
- Se creó la carpeta limpia `/home/mick/Projects/mgRadio`.

### Decisiones de Producto Aprobadas
- **Catálogo:** Solo Firebase Firestore (curado, sin URLs personalizadas) con sincronización no destructiva en Room.
- **Audio y Funcionalidades:** Metadatos ICY (*Now Playing*), reconexión automática ante cortes de red, Sleep Timer, historial de recientes, persistencia de última emisora y filtros en DataStore, búsqueda por nombre/frecuencia/categoría/tags, control de volumen en reproductor completo, grabación de stream a `Music/mgRadio`, ecualizador integrado, **Android Auto** (`MediaLibraryService`) y **Android TV** (D-Pad).
- **UI y Privacidad:** Pantalla única con pestañas + `BottomPlayerBar` + `FullScreenPlayerSheet`, temas Oscuro AMOLED + Claro + Material You, **sin Firebase Analytics** (cero telemetría).
- **Identidad:** `applicationId = "com.mgradio.app"`, compatible con el keystore existente sin contraseñas en texto plano en el repositorio.

### Documentación Generada
- `AGENTS.md` y `GEMINI.md`: reglas de gobernanza **Spec-First**, registro obligatorio en bitácora, arquitectura y comandos.
- `docs/constitution.md`: principios rectores de privacidad, Offline-First, resiliencia Media3 e integridad de datos.
- `docs/specs/01-functional-specs.md` (`RF-01` a `RF-13`).
- `docs/specs/02-expected-behaviors.md`.
- `docs/specs/03-ui-design-system.md`.
- `docs/specs/04-architecture-and-persistence.md`.
- `docs/PLAN_DESARROLLO.md`.
- `docs/BITACORA_IMPLEMENTACION.md`.

---

## [2026-11-17] Ejecución Completa del Plan por Fases (Fases 0–5) y Despliegue en Móvil

### Cambios Implementados
- **Fase 0 (Base y Temas):** Configurado Gradle (`com.mgradio.app`, `minSdk = 26`, `targetSdk = 35`, `compileSdk = 36`, Hilt `2.57.1`, Room `2.7.0`, Media3 `1.4.1`, DataStore `1.1.2`, sin `firebase-analytics`), `AndroidManifest.xml` (`MediaLibraryService`, `automotive_app_desc.xml`, `LEANBACK_LAUNCHER`), recursos/iconos/banner TV y temas en `presentation/theme/` (`Oscuro AMOLED`, `Claro`, `Sistema` y `Material You`).
- **Fase 1 (Dominio):** `Models.kt`, `StationUtils.kt` (búsqueda normalizada NFD por nombre, frecuencia, categoría y tags + detección unificada de MIME types) y casos de uso en `domain/usecase/`.
- **Fase 2 (Datos y Persistencia):** `RadioDatabase` v2 con `MIGRATION_1_2` (`tags` + `recent_plays`), `StationDao` con `syncFromRemote` transaccional (preserva favoritos), `FirestoreRemoteDataSource`, `StationRepositoryImpl` y `DataStorePreferencesRepository`.
- **Fase 3 (Motor Multimedia):**
  - `RadioMediaService`: `MediaLibraryService` (Android Auto: Favoritas, Recientes, Todas), `ExoPlayer` con `Icy-MetaData: 1`, `C.WAKE_MODE_NETWORK`, `DefaultLoadControl`, lectura de metadatos ICY en vivo y reconexión automática exponencial (hasta 3 intentos).
  - `StreamRecorder`: grabación directa de streams MP3/AAC a `MediaStore` (`Music/mgRadio/`) removiendo bloques ICY intercalados.
  - `AudioEqualizerController`: control de bandas y presets sobre `android.media.audiofx.Equalizer`.
  - `RadioPlayerManager`: `MediaController`, volumen `STREAM_MUSIC`, `SleepTimer`, grabación y ecualizador.
- **Fase 4 (UI Compose y TV):** `MainViewModel`, `MainScreen` (4 pestañas: `Todas`, `Favoritas`, `Recientes`, `Offline`; filtros persistentes; bordes de foco D-Pad), `BottomPlayerBar`, `FullScreenPlayerSheet` (Now Playing ICY, volumen, grabación, ecualizador, sleep timer), `EqualizerSheet`, `SleepTimerDialog`, `SettingsSheet` y `MainActivity`.
- **Fase 5 (Pruebas):** `StationUtilsTest` y `StationMapperAndRepoTest`.

### Verificación
- `./gradlew testDebugUnitTest installDebug`: **`BUILD SUCCESSFUL`** (tests unitarios e instalación en `2511FPC34G`).
- `MainActivity` lanzada en el dispositivo sin errores en `logcat`.

---

## [2026-11-17] Corrección de Mapeo `snake_case` en `FirestoreRemoteDataSource`

### Documentación Actualizada (Spec-First)
- `docs/specs/04-architecture-and-persistence.md`: documentado el esquema `snake_case` real de la colección `stations` en Firestore (`stream_url`, `stream_type`, `logo_url`, `last_checked`).

### Causa y Cambio Implementado
- La app cargaba con 0 emisoras porque `FirestoreRemoteDataSource` leía `streamUrl` en lugar de `stream_url`, descartando todos los documentos al obtener `null`.
- `app/src/main/java/com/mgradio/app/data/remote/FirestoreRemoteDataSource.kt`: actualizado para leer `stream_url`, `stream_type`, `logo_url` y `last_checked` (con fallback a camelCase).

### Verificación
- `./gradlew testDebugUnitTest installDebug` y ejecución en dispositivo físico `2511FPC34G`.

---

## [2026-11-17] Corrección de `navigationBarsPadding` en `BottomPlayerBar` (Edge-to-Edge)

### Documentación Actualizada (Spec-First)
- `docs/specs/03-ui-design-system.md`: especificado el uso de `navigationBarsPadding()` en `BottomPlayerBar` para evitar solapamiento con la barra de navegación virtual del sistema.

### Cambio Implementado
- `app/src/main/java/com/mgradio/app/presentation/player/PlayerSheets.kt` (`BottomPlayerBar`): agregado `.navigationBarsPadding()` al `Row` interno de la barra inferior de reproducción.

### Verificación
- `./gradlew testDebugUnitTest installDebug` y ejecución en dispositivo físico `2511FPC34G`.

---

## [2026-11-17] Configuración de Ruta de Grabaciones y Tiempos de Sleep Timer en Ajustes

### Documentación Actualizada (Spec-First)
- `docs/specs/01-functional-specs.md` (`RF-07`, `RF-08`): añadida configuración de tiempos de apagado y carpeta de grabaciones SAF en `SettingsSheet`.
- `docs/specs/04-architecture-and-persistence.md`: añadidos `recordingTreeUri`, `recordingFolderLabel` y `sleepTimerPresetsMinutes` en `UserPreferences` (`DataStore`).

### Cambios Implementados
- `domain/model/Models.kt` y `data/local/DataStorePreferencesRepository.kt`: persistencia de carpeta SAF de grabaciones y lista de tiempos para Sleep Timer.
- `media/StreamRecorder.kt` y `media/RadioPlayerManager.kt`: soporte de escritura en carpeta SAF (`DocumentsContract.createDocument`) cuando existe `recordingTreeUri`, con fallback a `MediaStore` (`Music/mgRadio`).
- `presentation/main/MainViewModel.kt`, `presentation/settings/SettingsSheet.kt`, `presentation/player/PlayerSheets.kt` y `presentation/main/MainScreen.kt`: UI de selector de carpeta (`OpenDocumentTree`), restablecimiento, chips y tiempo personalizado de Sleep Timer.

### Verificación
- `./gradlew testDebugUnitTest installDebug` y ejecución en dispositivo físico `2511FPC34G`.

---

## [2026-11-17] Ajuste Dinámico de Contadores de Pestañas Según Filtros Activos

### Documentación Actualizada (Spec-First)
- `docs/specs/01-functional-specs.md` (`RF-02`): especificado que los contadores de las pestañas (`Todas`, `Favoritas`, `Recientes`, `Offline`) deben reflejar los filtros activos de país, ciudad, categoría y búsqueda.

### Cambio Implementado
- `app/src/main/java/com/mgradio/app/presentation/main/MainViewModel.kt`: calculados `filteredAll`, `filteredFavs`, `filteredRecents` y `filteredOffline` aplicando los filtros activos (país, ciudad, categoría y texto) antes de poblar `allCount`, `favoritesCount`, `recentsCount` y `offlineCount`, además de filtrar `availableCategories` según el país y ciudad seleccionados.

### Verificación
- `./gradlew testDebugUnitTest installDebug` y ejecución en dispositivo físico `2511FPC34G`.

---

## [2026-11-17] Leyenda Explicativa en Pestaña `Offline`

### Documentación Actualizada (Spec-First)
- `docs/specs/03-ui-design-system.md`: documentada la leyenda informativa visible al seleccionar la pestaña `Offline`.

### Cambio Implementado
- `app/src/main/java/com/mgradio/app/presentation/main/MainScreen.kt`: agregado banner informativo cuando `state.selectedTab == StationTab.OFFLINE` explicando el propósito de la categoría y cómo usar el botón de recarga (`↻`) para validar cambios de estado online/offline.

### Verificación
- `./gradlew testDebugUnitTest installDebug` y ejecución en dispositivo físico `2511FPC34G`.

---

## [2026-11-17] Soporte Android TV: Prevención de Apagado (Respetando Sleep Timer) y Navegación por Control Remoto

### Documentación Actualizada (Spec-First)
- `docs/specs/01-functional-specs.md` (`RF-11`): especificado `FLAG_KEEP_SCREEN_ON` durante reproducción activa (limpiado al pausar, detener o finalizar Sleep Timer) y soporte de teclas multimedia del control remoto.
- `docs/specs/03-ui-design-system.md`: especificados bordes de foco D-Pad (`tvFocusBorder`) en botones del reproductor, Sleep Timer y escape `DirectionDown` en el buscador.

### Cambios Implementados
- `app/src/main/java/com/mgradio/app/MainActivity.kt`: activación/desactivación dinámica de `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` según `PlaybackStatus` (`PLAYING`/`BUFFERING`/`RECONNECTING` -> activo; `IDLE`/`PAUSED`/`ERROR` -> liberado para que el Sleep Timer permita apagar/suspender el TV).
- `app/src/main/java/com/mgradio/app/presentation/theme/TvFocusModifier.kt`: modificador reutilizable `tvFocusBorder` para indicar claramente el foco D-Pad.
- `app/src/main/java/com/mgradio/app/presentation/main/MainScreen.kt`: soporte global de teclas de control remoto (`MediaPlayPause`, `MediaPlay`, `MediaPause`, `MediaStop`) y escape `DirectionDown` en el buscador.
- `app/src/main/java/com/mgradio/app/presentation/player/PlayerSheets.kt`: aplicado `tvFocusBorder` a `ToolActionButton` y opciones de `SleepTimerDialog`.

### Verificación
- `./gradlew testDebugUnitTest installDebug` y ejecución en dispositivo físico `2511FPC34G`.

---

## [2026-11-17] Paridad de Publicación Google Play (`targetSdk = 36`, Keystore en `_secrets` y ProGuard R8)

### Documentación Actualizada (Spec-First)
- `docs/specs/04-architecture-and-persistence.md`: actualizado `targetSdk` a `36` para mantener paridad con la publicación `v1.0.6` en Google Play.

### Cambios Implementados
- `app/build.gradle.kts`: `targetSdk` ajustado a `36` (`versionCode = 8`, `versionName = "2.0.0"`).
- `app/proguard-rules.pro`: actualizadas reglas R8 para `com.mgradio.app.data.local.**` y `com.mgradio.app.data.remote.**`.
- Keystore resguardado en `/home/mick/Projects/_secrets/mgradio-keystore.jks` y configurado en `local.properties` (excluido de Git).

### Verificación
- `./gradlew testDebugUnitTest bundleRelease` y verificación de firma del `.aab`.
