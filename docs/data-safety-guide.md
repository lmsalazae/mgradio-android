# Guía para el Formulario de Seguridad de los Datos (Data Safety) en Google Play Console

Esta guía contiene las respuestas exactas que debes seleccionar en el formulario de **Seguridad de los Datos** en Google Play Console para la aplicación **mgRadio**.

---

## Paso 1: Información General (Overview)

1. **¿Tu aplicación recopila o comparte alguno de los tipos de datos de usuario obligatorios?**
   -  **Sí** *(Debido al uso de Firebase Analytics)*.

2. **¿Todos los datos de usuario que recopila tu aplicación se cifran en tránsito?**
   -  **Sí** *(Todo el tráfico con Firebase se realiza mediante HTTPS con cifrado TLS)*.

3. **¿Proporcionas a los usuarios una forma de solicitar que se eliminen sus datos?**
   -  **No** *(No se recopilan datos personales identificables ni cuentas de usuario; los datos son métricas anónimas)*.

---

## Paso 2: Selección de Tipos de Datos (Data Types)

En la lista de categorías, marca únicamente las siguientes:

### 1. Actividad en la aplicación (App activity)
-  **Interacciones en la aplicación (App interactions):** Marcar casilla.
  *(Apertura de la app, pantallas vistas, estaciones reproducidas)*.

### 2. Información y rendimiento de la aplicación (App info and performance)
-  **Registros de fallos (Crash logs):** Marcar casilla.
-  **Diagnósticos (Diagnostics):** Marcar casilla.
  *(Informes de rendimiento de la red y fallos de streaming anónimos)*.

### 3. Identificadores de dispositivo o de otro tipo (Device or other IDs)
-  **Identificadores de dispositivo o de otro tipo:** Marcar casilla.
  *(ID anónimo de instalación generado por Firebase Analytics)*.

> [!NOTE]
> **Todas las demás categorías deben quedar en "NO" o desmarcadas:**
> - Ubicación (Location): **No** *(Los filtros de país/ciudad son selecciones manuales, la app NO solicita GPS)*.
> - Información personal (Personal info): **No** *(No se pide nombre, email, teléfono)*.
> - Archivos de audio (Audio files): **No** *(La app no graba con micrófono ni accede a la música local del usuario)*.
> - Fotos, Videos, Contactos, Archivos: **No**.

---

## Paso 3: Uso y Tratamiento de los Datos Seleccionados

Para cada una de las 3 categorías marcadas anteriormente, responde las preguntas de detalle de la siguiente manera:

### A. Interacciones en la aplicación (App interactions)
- **¿Se recopilan, se comparten o ambas cosas?** -> **Recopilados (Collected)**.
- **¿El procesamiento es efímero?** -> **No**.
- **¿Los datos son necesarios para usar la aplicación o los usuarios pueden elegir?** -> **Datos obligatorios (no se pueden desactivar)**.
- **¿Con qué fines se recopilan estos datos?** -> Marcar: **Estadísticas de la aplicación (Analytics)**.

### B. Registros de fallos y Diagnósticos (Crash logs & Diagnostics)
- **¿Se recopilan, se comparten o ambas cosas?** -> **Recopilados (Collected)**.
- **¿El procesamiento es efímero?** -> **No**.
- **¿Los datos son necesarios para usar la aplicación o los usuarios pueden elegir?** -> **Datos obligatorios**.
- **¿Con qué fines se recopilan estos datos?** -> Marcar: **Estadísticas de la aplicación (Analytics)** y **Gestión de la aplicación (Developer communications / App management)**.

### C. Identificadores de dispositivo (Device or other IDs)
- **¿Se recopilan, se comparten o ambas cosas?** -> **Recopilados (Collected)**.
- **¿El procesamiento es efímero?** -> **No**.
- **¿Los datos son necesarios para usar la aplicación o los usuarios pueden elegir?** -> **Datos obligatorios**.
- **¿Con qué fines se recopilan estos datos?** -> Marcar: **Estadísticas de la aplicación (Analytics)**.

---

## Resumen del Resultado en Google Play
Una vez completado, Google Play mostrará en la ficha de tu app:
-  *Los datos se cifran en tránsito.*
-  *No se recopilan datos personales ni de ubicación.*
-  *Se recopilan datos anónimos de rendimiento y uso para mejorar la aplicación.*
