package app.marlboroadvance.mpvex.domain.download

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ApnaDownloadManager private constructor(private val context: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val activeJobs = ConcurrentHashMap<String, Job>()
  private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

  private val stateFile: File
    get() = File(context.filesDir, "apna_downloads.json")

  private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
  val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

  init {
    loadPersistedTasks()
  }

  private fun loadPersistedTasks() {
    scope.launch {
      runCatching {
        if (stateFile.exists()) {
          val content = stateFile.readText()
          val loaded = json.decodeFromString<List<DownloadTask>>(content)
          _tasks.value = loaded.map {
            if (it.status == DownloadStatus.DOWNLOADING) {
              it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L)
            } else {
              it
            }
          }
        }
      }
    }
  }

  private fun persistTasks() {
    runCatching {
      val content = json.encodeToString(_tasks.value)
      stateFile.writeText(content)
    }
  }

  private fun getDownloadDirectory(): File {
    val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
    val appDir = File(moviesDir, "ApnaPlayer")
    if (!appDir.exists()) {
      appDir.mkdirs()
    }
    return if (appDir.canWrite()) appDir else context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
  }

  fun enqueueDownload(url: String, customName: String? = null): String {
    val cleanUrl = url.trim()
    val id = UUID.randomUUID().toString()
    val fileName = resolveFileName(cleanUrl, customName)
    val destFile = File(getDownloadDirectory(), fileName)

    val task = DownloadTask(
      id = id,
      url = cleanUrl,
      fileName = fileName,
      filePath = destFile.absolutePath,
      status = DownloadStatus.PENDING
    )

    _tasks.value = listOf(task) + _tasks.value
    persistTasks()
    startTask(task)
    return id
  }

  fun pauseDownload(id: String) {
    activeJobs[id]?.cancel()
    activeJobs.remove(id)
    updateTask(id) { it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L) }
  }

  fun resumeDownload(id: String) {
    val task = _tasks.value.find { it.id == id } ?: return
    if (task.status == DownloadStatus.DOWNLOADING) return
    startTask(task)
  }

  fun cancelDownload(id: String, deleteFile: Boolean = true) {
    activeJobs[id]?.cancel()
    activeJobs.remove(id)
    val task = _tasks.value.find { it.id == id }
    if (deleteFile && task != null) {
      runCatching { File(task.filePath).delete() }
    }
    _tasks.value = _tasks.value.filterNot { it.id == id }
    persistTasks()
  }

  fun retryDownload(id: String) {
    val task = _tasks.value.find { it.id == id } ?: return
    startTask(task)
  }

  fun clearCompleted() {
    _tasks.value = _tasks.value.filterNot { it.status == DownloadStatus.COMPLETED }
    persistTasks()
  }

  private fun startTask(task: DownloadTask) {
    activeJobs[task.id]?.cancel()
    val job = scope.launch {
      executeDownload(task)
    }
    activeJobs[task.id] = job
  }

  private suspend fun executeDownload(task: DownloadTask) = withContext(Dispatchers.IO) {
    updateTask(task.id) { it.copy(status = DownloadStatus.DOWNLOADING, errorMessage = null) }

    val targetFile = File(task.filePath)
    val existingBytes = if (targetFile.exists()) targetFile.length() else 0L

    var downloadedBytes = existingBytes
    var totalBytes = task.totalBytes

    try {
      val requestBuilder = Request.Builder().url(task.url)
      if (existingBytes > 0L) {
        requestBuilder.addHeader("Range", "bytes=$existingBytes-")
      }

      val response = okHttpClient.newCall(requestBuilder.build()).execute()
      if (!response.isSuccessful && response.code != 206) {
        throw RuntimeException("HTTP Server returned code: ${response.code} ${response.message}")
      }

      val body = response.body ?: throw RuntimeException("Response body is empty")
      val contentLength = body.contentLength()

      if (response.code == 206) {
        if (contentLength > 0L) totalBytes = existingBytes + contentLength
      } else {
        downloadedBytes = 0L
        if (contentLength > 0L) totalBytes = contentLength
      }

      updateTask(task.id) {
        it.copy(totalBytes = totalBytes, downloadedBytes = downloadedBytes)
      }

      val outputStream = if (response.code == 206 && targetFile.exists()) {
        FileOutputStream(targetFile, true)
      } else {
        FileOutputStream(targetFile, false)
      }

      val inputStream: InputStream = body.byteStream()
      val buffer = ByteArray(32 * 1024)
      var bytesRead: Int

      var lastTime = System.currentTimeMillis()
      var bytesSinceLastTime = 0L

      outputStream.use { out ->
        inputStream.use { input ->
          while (input.read(buffer).also { bytesRead = it } != -1) {
            out.write(buffer, 0, bytesRead)
            downloadedBytes += bytesRead
            bytesSinceLastTime += bytesRead

            val now = System.currentTimeMillis()
            val timeDiff = now - lastTime
            if (timeDiff >= 1000L) {
              val speed = (bytesSinceLastTime * 1000L) / timeDiff
              updateTask(task.id) {
                it.copy(
                  downloadedBytes = downloadedBytes,
                  speedBytesPerSec = speed,
                  totalBytes = totalBytes
                )
              }
              lastTime = now
              bytesSinceLastTime = 0L
            }
          }
        }
      }

      updateTask(task.id) {
        it.copy(
          downloadedBytes = downloadedBytes,
          status = DownloadStatus.COMPLETED,
          speedBytesPerSec = 0L
        )
      }
      activeJobs.remove(task.id)

      // Notify Android MediaScanner so new video shows up in Apna Player immediately!
      MediaScannerConnection.scanFile(
        context.applicationContext,
        arrayOf(targetFile.absolutePath),
        null
      ) { _, _ -> }

    } catch (e: Exception) {
      if (e is kotlinx.coroutines.CancellationException) {
        updateTask(task.id) { it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L) }
      } else {
        updateTask(task.id) {
          it.copy(
            status = DownloadStatus.FAILED,
            errorMessage = e.localizedMessage ?: "Download failed",
            speedBytesPerSec = 0L
          )
        }
      }
      activeJobs.remove(task.id)
    } finally {
      persistTasks()
    }
  }

  private fun updateTask(id: String, transform: (DownloadTask) -> DownloadTask) {
    _tasks.value = _tasks.value.map {
      if (it.id == id) transform(it) else it
    }
  }

  private fun resolveFileName(url: String, customName: String?): String {
    if (!customName.isNullOrBlank()) {
      var name = customName.trim()
      if (!name.contains(".")) name += ".mp4"
      return name
    }
    val rawName = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
    val decoded = runCatching { android.net.Uri.decode(rawName) }.getOrDefault(rawName)
    if (decoded.isNotBlank() && decoded.contains('.')) {
      return decoded
    }
    return "video_${System.currentTimeMillis()}.mp4"
  }

  companion object {
    @Volatile
    private var instance: ApnaDownloadManager? = null

    fun getInstance(context: Context): ApnaDownloadManager {
      return instance ?: synchronized(this) {
        instance ?: ApnaDownloadManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
