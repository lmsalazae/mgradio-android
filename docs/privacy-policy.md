# Política de Privacidad de mgRadio

**Última actualización:** 20 de agosto de 2026

En **mgRadio**, respetamos y protegemos la privacidad de nuestros usuarios. Esta Política de Privacidad describe cómo se recopila, utiliza y protege la información cuando utilizas nuestra aplicación móvil **mgRadio** para Android.

---

## 1. Información que recopilamos

mgRadio está diseñada para ofrecer una experiencia de escucha de radio en directo sencilla, rápida y sin recopilar datos personales innecesarios.

### a. Información recopilada automáticamente
Para garantizar la estabilidad técnica, el rendimiento y mejorar la calidad del servicio, la aplicación utiliza **Google Firebase Analytics**:
- **Datos técnicos del dispositivo:** Modelo de dispositivo, versión del sistema operativo Android, idioma del sistema y resolución de pantalla.
- **Métricas anónimas de uso:** Eventos de interacción dentro de la app (por ejemplo, apertura de la app, reproducción de estaciones y navegación entre pestañas).
- **Diagnóstico y rendimiento:** Informes anónimos de errores o fallos en la reproducción.

*Ninguno de estos datos se asocia con tu identidad personal, nombre, dirección de correo electrónico ni número de teléfono.*

### b. Datos almacenados localmente en tu dispositivo
- **Estaciones Favoritas y Caché:** Las emisoras marcadas como favoritas y la copia local del catálogo se guardan exclusivamente en el almacenamiento interno de tu dispositivo (Base de datos SQLite/Room). Esta información no se comparte ni se transfiere a servidores externos.

---

## 2. Permisos del Dispositivo y Finalidad

La aplicación solicita únicamente los permisos estrictamente necesarios para su funcionamiento:

1. **Acceso a Internet (`android.permission.INTERNET`):**  
   Permite descargar el catálogo actualizado de emisoras y reproducir el flujo de audio (streaming) en directo.

2. **Estado de la Red (`android.permission.ACCESS_NETWORK_STATE`):**  
   Permite detectar si el dispositivo cuenta con conexión a internet activa (Wi-Fi o datos móviles) para informar al usuario oportunamente.

3. **Servicio en Primer Plano y Reproducción Multimedia (`FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_MEDIA_PLAYBACK`):**  
   Permite que la transmisión de radio continúe reproduciéndose en segundo plano cuando sales de la app o bloqueas la pantalla.

4. **Notificaciones (`android.permission.POST_NOTIFICATIONS`):**  
   Utilizado en dispositivos con Android 13 o superior exclusivamente para mostrar los controles de reproducción (Play/Pausa, nombre de la emisora) en la barra de estado y en la pantalla de bloqueo.

---

## 3. Servicios de Terceros

Nuestra aplicación utiliza servicios de terceros que pueden recopilar información conforme a sus propias políticas de privacidad:

- **Google Firebase (Firestore y Analytics):** Utilizado para sincronizar el catálogo de emisoras y obtener métricas anónimas de rendimiento.  
  Puedes consultar la política de privacidad de Google en: [https://policies.google.com/privacy](https://policies.google.com/privacy).

- **Servidores de Radiodifusión:** Al sintonizar una estación, tu dispositivo se conecta directamente al enlace de transmisión público proporcionado por la radiodifusora correspondiente.

---

## 4. Privacidad de Menores

mgRadio no recopila conscientemente información de identificación personal de niños menores de 13 años. Si consideras que de alguna manera se ha recopilado dicha información, contáctanos de inmediato para proceder a su eliminación.

---

## 5. Seguridad de los Datos

Nos comprometemos a proteger la seguridad de tu información. Toda la comunicación con los servicios de catálogo se realiza a través de conexiones seguras y cifradas (HTTPS/SSL).

---

## 6. Cambios a esta Política de Privacidad

Podemos actualizar nuestra Política de Privacidad periódicamente. Cualquier modificación será publicada en este mismo documento con la fecha de última actualización.

---

## 7. Contacto

Si tienes alguna pregunta o sugerencia sobre nuestra Política de Privacidad, puedes ponerte en contacto con nosotros a través del correo electrónico:

**Correo de soporte:** `soporte@mgradio.com` *(o el correo que tengas asociado a tu cuenta de desarrollador de Google Play)*
