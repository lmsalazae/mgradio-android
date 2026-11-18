# SPEC-03: Diseño Esperado (UI / UX) — mgRadio

**Versión:** 2.0  
**Estado:** Aprobada  

---

## 1. Paleta de Colores y Temas

- **Oscuro AMOLED (Default):**
  - `Background`: `#0E1217` (o `#0A0D12` AMOLED profundo)
  - `Surface`: `#161B22`
  - `SurfaceVariant` (Cards): `#1E2633`
  - `Primary`: `#58A6FF` (Cyan eléctrico)
  - `Secondary`: `#39D353` (Teal/Verde señal en vivo)
  - `Favorite`: `#FF4081` (Rosa favorito)
  - `RecordRed`: `#FF5252` (Indicador de grabación)
  - `OnBackground`: `#F0F6FC`
  - `OnSurfaceVariant`: `#8B949E`
- **Claro:**
  - `Background`: `#F6F8FA`
  - `Surface`: `#FFFFFF`
  - `SurfaceVariant`: `#EAEFF5`
  - `Primary`: `#0969DA`
  - `Secondary`: `#1A7F37`
  - `OnBackground`: `#1F2328`
- **Material You (`useDynamicColor`):** Disponible en Android 12+ (`dynamicDarkColorScheme` / `dynamicLightColorScheme`).

---

## 2. Wireframe de Pantalla Principal (`MainScreen`)

```text
┌──────────────────────────────────────────────────────────┐
│ TopAppBar:  mgRadio              [↻ Actualizar] [⚙ Ajustes]│
├──────────────────────────────────────────────────────────┤
│ [ 🔍 Buscar por nombre, frecuencia o género...       ✕ ] │
├──────────────────────────────────────────────────────────┤
│ [ País: Todos ▾ ]          [ Ciudad: Todas ▾ ]           │
├──────────────────────────────────────────────────────────┤
│ [ Todas (42) ] [ Favoritas (8) ] [ Recientes ] [ Offline ]│
├──────────────────────────────────────────────────────────┤
│ Chips categoría: (Todas) (Música) (Noticias) (Deportes)  │
├──────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────┐ │
│ │ [Logo]  Estación 97.7 FM                    [♥] [▶]  │ │
│ │         Ciudad · Pop/Rock · EN VIVO                  │ │
│ └──────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ [Logo]  Radio Noticias 104.1 AM             [♡] [▶]  │ │
│ │         Ciudad · Noticias                            │ │
│ └──────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────┤
│ BottomPlayerBar (si hay emisora seleccionada):           │
│ [Logo] Estación 97.7 FM · ♪ Artista - Canción   [REC] [⏸]│
└──────────────────────────────────────────────────────────┘
```

---

## 3. Wireframe de Reproductor Completo (`FullScreenPlayerSheet`)

- ModalBottomSheet expandido:
  - Respetar `navigationBarsPadding()` en `BottomPlayerBar` (y hojas modales) para evitar que los botones de navegación virtuales del sistema queden sobre los controles.
  - Logo grande redondeado + estado (`EN VIVO`, `CONECTANDO...`, `RECONECTANDO (1/3)...`, `PAUSADO`) + badge `REC 00:24` si está grabando.
  - Nombre de la emisora, frecuencia + banda (`97.7 FM`), ubicación (`Ciudad, País`).
  - **Banner Now Playing (ICY):** `♪ Artista - Título de canción` (si el stream lo emite).
  - Controles principales: `[ ♥ Favorito ]` `[ ▶ / ⏸ Play/Pause ]` `[ ⏹ Stop ]`.
  - **Fila de herramientas:** `[ 🎙 Grabar ]` `[ 🎚 Ecualizador ]` `[ ⏱ Sleep Timer ]` `[ 📋 Copiar URL ]`.
  - **Control de Volumen:** Icono mute/volumen + `Slider` sincronizado con `STREAM_MUSIC`.
  - Tarjeta de detalles técnicos (Grupo, Categoría, Tags, Tipo de stream, Última verificación).

---

## 4. Soporte D-Pad (Android TV) y Leyenda en Pestaña Offline
- Todas las tarjetas, pestañas, chips, herramientas del reproductor completo (`ToolActionButton`), opciones de `SleepTimerDialog` y botones mantienen bordes visibles cuando reciben foco (`tvFocusBorder` con borde `2.dp` en color `Primary`).
- El buscador permite salir hacia abajo con `DirectionDown` para no atrapar el foco del control remoto.
- Al seleccionar la pestaña **Offline**, se muestra una leyenda informativa explicando que agrupa emisoras detectadas sin respuesta o fuera de línea, e indicando que el botón de recarga (`↻`) permite volver a validar qué emisoras pasaron de offline a online o viceversa.
