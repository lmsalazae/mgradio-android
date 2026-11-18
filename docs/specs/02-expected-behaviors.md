# SPEC-02: Comportamientos Esperados — mgRadio

**Versión:** 2.0  
**Estado:** Aprobada  

---

## 1. Arranque y Sincronización
1. Al abrir la app:
   - Se cargan instantáneamente las emisoras desde Room y las preferencias desde DataStore (último país/ciudad/última emisora).
   - Si existe última emisora reproducida, se precarga en el estado del mini-player (en estado `IDLE`/pausado, sin auto-reproducir salvo que el usuario pulse Play).
   - Se lanza una sincronización en segundo plano con Firestore; si no hay red, no bloquea la UI ni borra la caché local.
2. Si el usuario pulsa **Actualizar** (`Refresh`), muestra indicador de progreso y actualiza la lista desde Firestore preservando `isFavorite` y respetando el estado de red.

## 2. Reproducción, Metadatos ICY y Reconexión
1. Al tocar una emisora:
   - Cambia inmediatamente a estado `BUFFERING`, registra la entrada en `recent_plays` y guarda `lastStationId` en `DataStore`.
   - `RadioMediaService` inicia `ExoPlayer` con `Icy-MetaData: 1`.
   - Cuando el stream emite metadatos ICY (`title` / `artist`), se actualiza `nowPlayingTitle` en `PlayerUiState` y en la notificación multimedia.
2. Si ocurre un fallo de red durante `PLAYING` o `BUFFERING`:
   - `RadioMediaService` muestra estado `RECONNECTING (intento 1/3..3/3)` y reintenta tras 1s, 2s y 4s.
   - Si uno de los reintentos conecta, vuelve a `PLAYING` automáticamente.
   - Si fallan los 3 intentos, pasa a `ERROR`, muestra el mensaje en español y marca `activo = false` localmente en Room.

## 3. Grabación de Stream (`StreamRecorder`)
1. Solo está habilitada cuando hay una emisora en reproducción/selección con URL HTTP/HTTPS directa (si es HLS `.m3u8`, informa al usuario que la grabación directa aplica a streams Icecast/Shoutcast MP3/AAC).
2. Durante la grabación:
   - Se muestra un indicador rojo pulsante `REC 00:12` en `FullScreenPlayerSheet` y `BottomPlayerBar`.
   - Si la app pasa a segundo plano o se cambia de emisora, la grabación se detiene de forma limpia cerrando el archivo en `MediaStore` para que nunca quede corrupto.

## 4. Ecualizador y Volumen
1. El ecualizador se inicializa con el `audioSessionId` cuando `ExoPlayer` está listo (`audioSessionId != C.AUDIO_SESSION_ID_UNSET`).
2. Si el dispositivo no soporta `Equalizer` hardware/software (o aún no hay audio activo), la UI muestra un mensaje claro en lugar de fallar.
3. El slider de volumen de `FullScreenPlayerSheet` refleja el volumen real del stream multimedia (`AudioManager.STREAM_MUSIC`) y actualiza al moverse.

## 5. Sleep Timer
1. Al activar un temporizador (ej. 30 min), inicia una corrutina de cuenta regresiva visible (`29:59`).
2. Al llegar a `00:00`, detiene la reproducción (`stop()`), cancela el timer y muestra un aviso *"Temporizador finalizado: reproducción detenida"*.
3. Si el usuario detiene manualmente el reproductor o cancela el timer, la cuenta regresiva se limpia.
