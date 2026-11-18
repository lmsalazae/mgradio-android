# AGENTS.md — mgRadio

> Fuente normativa para agentes que desarrollen o mantengan **mgRadio**.  
> Principios rectores: [docs/constitution.md](docs/constitution.md) · Plan de desarrollo: [docs/PLAN_DESARROLLO.md](docs/PLAN_DESARROLLO.md)

---

## 1. Contexto Rápido

Aplicación nativa Android de streaming de radio en vivo (**Offline-First** con caché local en Room y sincronización desde **Firebase Firestore**), construida con **Kotlin**, **Jetpack Compose Material 3**, **Hilt**, **Room**, **DataStore** y **AndroidX Media3 (`ExoPlayer` + `MediaLibraryService`)**.

- `applicationId` / `namespace`: `com.mgradio.app`
- `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`
- Soporta móviles, **Android Auto** (`MediaLibraryService`) y **Android TV** (`LEANBACK_LAUNCHER` + navegación D-Pad).
- **Cero telemetría:** No incluye `firebase-analytics` ni rastreadores.

---

## 2. Reglas Obligatorias de Gobernanza (Spec-First + Bitácora)

1. **Lectura previa obligatoria:** Antes de planificar o modificar código, revisa:
   - [docs/constitution.md](docs/constitution.md)
   - [docs/specs/01-functional-specs.md](docs/specs/01-functional-specs.md)
   - [docs/specs/02-expected-behaviors.md](docs/specs/02-expected-behaviors.md)
   - [docs/specs/03-ui-design-system.md](docs/specs/03-ui-design-system.md)
   - [docs/specs/04-architecture-and-persistence.md](docs/specs/04-architecture-and-persistence.md)
2. **Flujo obligatorio Spec-First:** Siempre que se vaya a implementar o ajustar una funcionalidad, **primero** se deben generar o actualizar los documentos de especificación (`docs/specs/`), reglas (`AGENTS.md`, `docs/constitution.md`) y el plan (`docs/PLAN_DESARROLLO.md`) antes de escribir código.
3. **Registro obligatorio en Bitácora:** Después de aplicar cualquier cambio y ejecutar las pruebas correspondientes, es obligatorio insertar una entrada en [`docs/BITACORA_IMPLEMENTACION.md`](docs/BITACORA_IMPLEMENTACION.md) detallando qué se cambió, archivos tocados y resultados de pruebas para mantener la memoria histórica del proyecto.
4. **Cero pérdida de datos del usuario:** Prohibido usar `fallbackToDestructiveMigration` sin migración explícita en Room; los favoritos, historial de recientes y preferencias del usuario deben sobrevivir a actualizaciones y sincronizaciones con Firestore.
5. **Idioma de UI:** Toda la interfaz de usuario, notificaciones multimedia, mensajes de error y ajustes deben estar en **español**.

---

## 3. Comandos Operativos

```bash
# Pruebas unitarias JVM (siempre desde /home/mick/Projects/mgRadio)
./gradlew testDebugUnitTest

# Pruebas E2E instrumentadas (Compose UI) en dispositivo/TV conectado
./gradlew connectedDebugAndroidTest

# Pruebas E2E con captura de screenshots en Pictures/mgRadioShots
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.captureScreenshots=true

# Compilar APK Debug
./gradlew assembleDebug

# Instalar en dispositivo físico conectado por ADB y lanzar la app
./gradlew installDebug
adb shell am start -n com.mgradio.app/com.mgradio.app.MainActivity

# Verificar errores en runtime
adb logcat -d -t 50 *:E | grep -i "mgradio\|AndroidRuntime\|FATAL" || true
```

---

## 4. Estructura de Paquetes (`com.mgradio.app`)

```text
app/src/main/java/com/mgradio/app/
├── RadioApplication.kt                # @HiltAndroidApp
├── MainActivity.kt                    # @AndroidEntryPoint, Edge-to-Edge, temas, permisos
├── di/AppModule.kt                    # Room, Firestore, DataStore, Repositories
├── domain/
│   ├── model/                         # Station, PlaybackState, EqualizerState, SleepTimerState, ThemeMode, UserPreferences
│   ├── repository/                    # StationRepository, PreferencesRepository, PlayerController
│   └── usecase/                       # GetStations, RefreshStations, ToggleFavorite, SearchFilter, etc.
├── data/
│   ├── local/                         # RadioDatabase, StationDao, StationEntity, RecentPlayEntity, DataStorePreferences
│   ├── remote/                        # FirestoreRemoteDataSource, StationDto, StationMapper
│   └── repository/                    # StationRepositoryImpl (sync no destructivo)
├── media/
│   ├── RadioMediaService.kt           # MediaLibraryService (ExoPlayer + Android Auto + ICY + Reconexión)
│   ├── RadioPlayerManager.kt          # MediaController + Equalizer + SleepTimer + Volume + StreamRecorder
│   ├── StreamRecorder.kt              # Grabación de stream HTTP/ICY a MediaStore (Music/mgRadio)
│   └── AudioEqualizerController.kt    # Control de bandas android.media.audiofx.Equalizer
└── presentation/
    ├── theme/                         # Color.kt, Theme.kt (AMOLED Dark + Light + Material You), Type.kt
    ├── main/                          # MainViewModel, MainScreen, StationList, FilterBars
    ├── player/                        # BottomPlayerBar, FullScreenPlayerSheet, EqualizerSheet, SleepTimerDialog
    └── settings/                      # SettingsSheet (Tema, Material You, Reconexión)
```

---

## 5. *Definition of Done* (Verificación Obligatoria)

Antes de dar por terminada cualquier fase o tarea:
1. `./gradlew testDebugUnitTest` pasa al 100%.
2. `./gradlew assembleDebug` / `installDebug` compila e instala sin errores.
3. Si hay dispositivo/TV conectado, ejecutar `./gradlew connectedDebugAndroidTest` (`MgRadioE2ETest`; con `-Pandroid.testInstrumentationRunnerArguments.captureScreenshots=true` para exportar capturas vía `MediaStore`) y validar arranque limpio en `logcat` y flujos clave (catálogo, filtros, reproducción, metadatos ICY, favoritos, volumen, sleep timer, grabación, ecualizador, temas).
4. Actualizar [`docs/BITACORA_IMPLEMENTACION.md`](docs/BITACORA_IMPLEMENTACION.md) con los cambios y pruebas ejecutadas.
