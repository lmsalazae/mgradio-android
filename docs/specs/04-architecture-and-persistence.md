# SPEC-04: Arquitectura Técnica y Persistencia — mgRadio

**Versión:** 2.0  
**Estado:** Aprobada  

---

## 1. Configuración Gradle y Dependencias

- **Módulo único:** `:app`
- **`applicationId` / `namespace`:** `com.mgradio.app`
- **`compileSdk`:** `36`, **`minSdk`:** `26`, **`targetSdk`:** `35`
- **Plugins:** Android Application (`9.x`), Kotlin Compose (`2.x`), KSP, Hilt, Google Services (`google-services.json`)
- **Dependencias principales:**
  - Jetpack Compose Material 3 + `material-icons-extended`
  - Hilt (`hilt-android`, `hilt-navigation-compose`)
  - Room (`room-runtime`, `room-ktx`, `room-compiler`)
  - `androidx.datastore:datastore-preferences`
  - `firebase-firestore-ktx` (**sin** `firebase-analytics`)
  - AndroidX Media3 (`media3-exoplayer`, `media3-exoplayer-hls`, `media3-session`)
  - Coil (`coil-compose`) para logos de emisoras
  - Testing: `junit`, `kotlinx-coroutines-test`
- **Firma Release:** Compatible con `mgradio-keystore.jks` mediante propiedades en `local.properties` / variables de entorno (`MGRADIO_KEYSTORE_PATH`, `MGRADIO_KEYSTORE_PASS`, etc.), nunca contraseñas en texto plano dentro del repositorio.

---

## 2. Base de Datos Room (`RadioDatabase`, v2 con migración segura)

### Tabla `stations` (`StationEntity`)
- `id: String` (PK)
- `nombre: String`, `frecuencia: String`, `banda: String`, `grupo: String`
- `streamUrl: String`, `streamType: String`, `logoUrl: String`
- `categoria: String`, `pais: String`, `ciudad: String`
- `tags: String` (lista separada por comas para búsqueda)
- `activo: Boolean`, `orden: Int`, `isFavorite: Boolean`, `lastChecked: Long`

### Tabla `recent_plays` (`RecentPlayEntity`)
- `stationId: String` (PK)
- `playedAt: Long` (timestamp epoch ms)

### Regla de Sincronización No Destructiva (`StationRepositoryImpl`)
1. Obtiene snapshot actual de `stations` desde Firestore.
2. Lee los IDs marcados con `isFavorite == true` en Room.
3. En transacción `@Transaction`:
   - Upsert de las estaciones remotas preservando `isFavorite`.
   - Elimina estaciones obsoletas que ya no están en Firestore **excepto** si el usuario las marcó como favoritas (siempre conserva favoritos).
- **Esquema Firestore (`stations`):** Los documentos remotos usan nombres `snake_case` (`stream_url`, `stream_type`, `logo_url`, `last_checked`), además de `id`, `nombre`, `frecuencia`, `banda`, `grupo`, `categoria`, `pais`, `ciudad`, `tags`, `activo` y `orden`.

---

## 3. DataStore Preferences (`UserPreferences`)
- `themeMode: ThemeMode` (`SYSTEM`, `LIGHT`, `AMOLED_DARK` default)
- `useDynamicColor: Boolean` (`false` por defecto)
- `selectedCountry: String` (`"Todos"`)
- `selectedCity: String` (`"Todas"`)
- `lastStationId: String?` (`null`)
- `recordingTreeUri: String?` (`null` = `Music/mgRadio` por `MediaStore`; si tiene valor, URI SAF `OpenDocumentTree`)
- `recordingFolderLabel: String?` (`null` = nombre legible de la carpeta elegida)
- `sleepTimerPresetsMinutes: List<Int>` (`[15, 30, 45, 60]` por defecto)

---

## 4. Arquitectura Multimedia (`media/`)
- **`RadioMediaService : MediaLibraryService`:**
  - `ExoPlayer` con `DefaultHttpDataSource.Factory` (`Icy-MetaData: 1`, User-Agent VLC, cross-protocol redirects).
  - Escucha `Player.Listener.onMediaMetadataChanged` para extraer título/artista ICY y actualizar los metadatos del `MediaSession`.
  - Reconexión automática en `onPlayerError` (hasta 3 intentos con backoff 1s/2s/4s cuando es error de red/IO).
  - Expone árbol navegable para **Android Auto** (`ROOT` -> `FAVORITES`, `RECENTS`, `ALL`).
- **`StreamRecorder`:** Graba el stream HTTP/ICY directamente a `MediaStore.Audio.Media` (`Music/mgRadio/`) en `Dispatchers.IO`.
- **`AudioEqualizerController`:** Envuelve `android.media.audiofx.Equalizer` sobre el `audioSessionId` del reproductor.
