package com.mgradio.app.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.mgradio.app.domain.model.RecordingState
import com.mgradio.app.domain.model.Station
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Graba streams Icecast/Shoutcast HTTP/HTTPS directamente en Music/mgRadio
 * usando MediaStore, filtrando bloques de metadatos ICY si el servidor los intercala.
 */
@Singleton
class StreamRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    fun startRecording(
        station: Station,
        customTreeUri: String? = null,
        onError: (String) -> Unit
    ) {
        if (_state.value.isRecording) {
            stopRecording()
        }
        val url = station.streamUrl
        if (url.contains(".m3u8", ignoreCase = true) ||
            station.streamType.contains("hls", ignoreCase = true)
        ) {
            onError("La grabación directa solo admite streams MP3/AAC (no HLS .m3u8).")
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeName = station.nombre.replace(Regex("[^A-Za-z0-9_-]"), "_").take(32)
        val startMs = System.currentTimeMillis()

        _state.value = RecordingState(
            isRecording = true,
            stationName = station.nombre,
            elapsedSeconds = 0L,
            bytesWritten = 0L
        )

        timerJob = scope.launch {
            while (isActive && _state.value.isRecording) {
                delay(1000L)
                _state.value = _state.value.copy(
                    elapsedSeconds = (System.currentTimeMillis() - startMs) / 1000L
                )
            }
        }

        recordingJob = scope.launch {
            var connection: HttpURLConnection? = null
            var outputUri: Uri? = null
            var outputStream: OutputStream? = null
            try {
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    setRequestProperty("User-Agent", "VLC/3.0.20 LibVLC/3.0.20")
                    setRequestProperty("Icy-MetaData", "1")
                    instanceFollowRedirects = true
                }
                connection.connect()

                val contentType = connection.contentType?.lowercase() ?: ""
                val ext = if (contentType.contains("aac")) "aac" else "mp3"
                val mime = if (ext == "aac") "audio/aac" else "audio/mpeg"
                val fileName = "${safeName}_$timestamp.$ext"
                val metaInt = connection.getHeaderFieldInt("icy-metaint", 0)

                val createdAndIsMediaStore = if (!customTreeUri.isNullOrBlank()) {
                    val safUri = createSafDocument(customTreeUri, fileName, mime)
                    if (safUri != null) {
                        safUri to false
                    } else {
                        (createMediaStoreEntry(fileName, mime)
                            ?: throw IllegalStateException("No se pudo crear el archivo en la carpeta seleccionada.")) to true
                    }
                } else {
                    (createMediaStoreEntry(fileName, mime)
                        ?: throw IllegalStateException("No se pudo crear el archivo en Music/mgRadio.")) to true
                }
                val created = createdAndIsMediaStore.first
                val isMediaStore = createdAndIsMediaStore.second
                outputUri = created
                outputStream = context.contentResolver.openOutputStream(created)
                    ?: throw IllegalStateException("No se pudo abrir el flujo de escritura.")

                connection.inputStream.use { input ->
                    copyAudioStrippingIcy(input, outputStream, metaInt) { totalBytes ->
                        _state.value = _state.value.copy(bytesWritten = totalBytes)
                    }
                }
                if (isMediaStore) finalizeMediaStoreEntry(created)
                _state.value = RecordingState(isRecording = false, lastSavedFileName = fileName)
            } catch (e: Exception) {
                if (customTreeUri.isNullOrBlank()) {
                    outputUri?.let { runCatching { finalizeMediaStoreEntry(it) } }
                }
                val cancelled = recordingJob?.isCancelled == true
                _state.value = RecordingState(isRecording = false)
                if (!cancelled) {
                    onError("Error al grabar: ${e.message ?: "fallo de conexión"}")
                }
            } finally {
                runCatching { outputStream?.close() }
                runCatching { connection?.disconnect() }
                timerJob?.cancel()
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        timerJob?.cancel()
        _state.value = _state.value.copy(isRecording = false)
    }

    private fun copyAudioStrippingIcy(
        input: InputStream,
        output: OutputStream,
        metaInt: Int,
        onBytes: (Long) -> Unit
    ) {
        val buffer = ByteArray(8192)
        var totalWritten = 0L
        if (metaInt <= 0) {
            while (recordingJob?.isActive == true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                totalWritten += read
                onBytes(totalWritten)
            }
            output.flush()
            return
        }

        var remainingAudio = metaInt
        while (recordingJob?.isActive == true) {
            val toRead = minOf(buffer.size, remainingAudio)
            val read = input.read(buffer, 0, toRead)
            if (read <= 0) break
            output.write(buffer, 0, read)
            totalWritten += read
            remainingAudio -= read
            onBytes(totalWritten)

            if (remainingAudio == 0) {
                val metaLenByte = input.read()
                if (metaLenByte < 0) break
                val metaLen = metaLenByte * 16
                if (metaLen > 0) {
                    var toSkip = metaLen
                    while (toSkip > 0) {
                        val skipped = input.skip(toSkip.toLong())
                        if (skipped <= 0) {
                            if (input.read() < 0) break
                            toSkip--
                        } else {
                            toSkip -= skipped.toInt()
                        }
                    }
                }
                remainingAudio = metaInt
            }
        }
        output.flush()
    }

    private fun createSafDocument(treeUriString: String, fileName: String, mimeType: String): Uri? {
        return runCatching {
            val treeUri = Uri.parse(treeUriString)
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            DocumentsContract.createDocument(context.contentResolver, parentDocUri, mimeType, fileName)
        }.getOrNull()
    }

    private fun createMediaStoreEntry(fileName: String, mimeType: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/mgRadio")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        return context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun finalizeMediaStoreEntry(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, values, null, null)
        }
    }
}
