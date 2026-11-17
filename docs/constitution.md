# Constitución del Proyecto — mgRadio

Este documento define los principios rectores inmutables de **mgRadio**.

---

## 1. Identidad y Propósito
**mgRadio** es un reproductor nativo de radio en vivo para Android (móviles, Android Auto y Android TV) centrado en la estabilidad de reproducción, baja latencia, privacidad total y disponibilidad **Offline-First** del catálogo.

## 2. Principios Rectores

### P1. Privacidad Total (Sin Telemetría)
- La app **no** incluye `firebase-analytics`, crash trackers de terceros ni recopilación de identificadores de dispositivo.
- La conexión a **Firebase Firestore** se usa exclusivamente en modo lectura para sincronizar el catálogo curado de emisoras.

### P2. Offline-First e Integridad de Datos de Usuario
- El catálogo se almacena localmente en **Room** para arranque instantáneo y filtrado offline.
- La sincronización remota nunca debe destruir `isFavorite` ni el historial de reproducción (`recent_plays`), y debe limpiar estaciones eliminadas en Firestore sin afectar favoritos locales.
- Prohibido `fallbackToDestructiveMigration` sin migración explícita.

### P3. Resiliencia de Audio y Media3 Nativo
- La reproducción vive en un `MediaLibraryService` de **AndroidX Media3** con foco de audio, manejo de desconexión (`becomingNoisy`), WakeLock de red y compatibilidad con **Android Auto** y controles multimedia del sistema.
- Ante cortes de red en un stream activo, el servicio intenta **reconexión automática** antes de reportar error o marcar la emisora como Offline.

### P4. Identidad y Firma Consistente
- `applicationId = "com.mgradio.app"`, compatible con el keystore de publicación existente (`mgradio-keystore`).
- Nunca almacenar contraseñas en texto plano dentro del repositorio git.

### P5. Cero Funcionalidades Prometidas sin Implementación Real
- Toda capacidad declarada en las especificaciones (metadatos ICY, volumen in-app, sleep timer, historial de recientes, grabación a `Music/mgRadio`, ecualizador, búsqueda por nombre/frecuencia/tags, Android Auto, temas Oscuro AMOLED / Claro / Material You) debe estar implementada y conectada de extremo a extremo.
