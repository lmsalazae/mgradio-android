# Plan de Desarrollo por Fases — mgRadio

Este plan sigue estrictamente [docs/constitution.md](constitution.md) y las especificaciones `SPEC-01` a `SPEC-04`.

---

## Fase 0: Base del Proyecto, Gradle, Recursos, Manifest y Temas
- Configurar `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `.gitignore`.
- Copiar `google-services.json`, `network_security_config.xml`, `ic_launcher_*` y `tv_banner.png` desde `_archive/radio-android`.
- Agregar `res/xml/automotive_app_desc.xml` para Android Auto y actualizar `AndroidManifest.xml` (`MediaLibraryService`, `LEANBACK_LAUNCHER`, permisos media/notificaciones).
- Implementar `presentation/theme/Color.kt`, `Theme.kt` (`SYSTEM`, `LIGHT`, `AMOLED_DARK` + `DynamicColor`), `Type.kt`.
- **Verificación:** `./gradlew assembleDebug` exitoso.

## Fase 1: Dominio, Modelos, Filtros y Casos de Uso (100% Testeable en JVM)
- Modelos en `domain/model/`: `Station`, `PlayerUiState`, `PlaybackStatus`, `EqualizerState`, `SleepTimerState`, `UserPreferences`, `ThemeMode`.
- `domain/util/StationUtils.kt`: normalización NFD, búsqueda por nombre/frecuencia/categoría/tags con prioridad por prefijo, detección de MIME type (`PLS`, `M3U`, `HLS`, `MP3`, `AAC`).
- Interfaces `StationRepository`, `PreferencesRepository` y casos de uso en `domain/usecase/`.
- **Verificación:** Tests unitarios JVM de filtrado, búsqueda y MIME types.

## Fase 2: Capa de Datos (Room v2, Firestore No Destructivo, DataStore y Hilt)
- `data/local/`: `StationEntity`, `RecentPlayEntity`, `StationDao`, `RadioDatabase` (migración segura sin pérdida de favoritos), `DataStorePreferencesRepository`.
- `data/remote/`: `StationDto`, `StationMapper` (incluyendo `tags`), `FirestoreRemoteDataSource`.
- `data/repository/StationRepositoryImpl.kt`: sincronización transaccional no destructiva.
- `di/AppModule.kt` y `RadioApplication.kt`.
- **Verificación:** Tests unitarios de `StationMapper` y `StationRepositoryImpl`.

## Fase 3: Motor Multimedia Media3, ICY, Reconexión, Grabación, Ecualizador y Android Auto
- `media/RadioMediaService.kt`: `MediaLibraryService` con `ExoPlayer`, `Icy-MetaData: 1`, extracción de `MediaMetadata` ICY, reconexión automática (3 intentos backoff) y árbol navegable Android Auto (`Favoritas`, `Recientes`, `Todas`).
- `media/AudioEqualizerController.kt`: wrapper seguro sobre `android.media.audiofx.Equalizer`.
- `media/StreamRecorder.kt`: grabador HTTP/ICY a `MediaStore` (`Music/mgRadio/`).
- `media/RadioPlayerManager.kt`: `MediaController` + `AudioManager` volumen + `SleepTimer` + integración con `StreamRecorder` y `AudioEqualizerController`.
- **Verificación:** Compilación limpia y pruebas unitarias de utilidades de audio/estado.

## Fase 4: Interfaz Compose, D-Pad TV, FullScreenPlayer y Ajustes
- `presentation/main/MainViewModel.kt`: integra catálogo, búsqueda, país/ciudad/categoría, pestañas (`Todas`, `Favoritas`, `Recientes`, `Offline`), reproducción, favoritos, volumen, sleep timer, grabación, ecualizador y preferencias DataStore.
- `presentation/main/MainScreen.kt` + componentes (`StationItemCard`, `StationSearchBar`, `FilterDropdowns`) con soporte D-Pad TV.
- `presentation/player/`: `BottomPlayerBar.kt`, `FullScreenPlayerSheet.kt` (volumen, sleep timer, grabación, ecualizador, ICY Now Playing), `EqualizerSheet.kt`, `SleepTimerDialog.kt`.
- `presentation/settings/SettingsSheet.kt` (Temas AMOLED/Claro/Sistema + Material You).
- `MainActivity.kt`.

## Fase 5: Pruebas Integrales, Bitácora y Despliegue en Móvil
- Ejecutar `./gradlew testDebugUnitTest` (100% OK).
- Instalar en dispositivo físico (`./gradlew installDebug`), lanzar `MainActivity`, revisar `logcat` sin errores.
- Registrar todo en `docs/BITACORA_IMPLEMENTACION.md`.
