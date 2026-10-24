package app.marlboroadvance.mpvex.domain.vault

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

@Serializable
data class VaultItem(
  val id: String,
  val fileName: String,
  val originalPath: String,
  val sizeBytes: Long,
  val dateAdded: Long,
  val vaultFilePath: String,
)

object VaultManager {
  private const val TAG = "VaultManager"
  private const val PREFS_NAME = "apna_vault_prefs"
  private const val KEY_PIN_HASH = "vault_pin_hash"
  private const val VAULT_FOLDER_NAME = ".vault"
  private const val INDEX_FILE_NAME = "vault_index.json"

  private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

  private fun getPrefs(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun isPinSet(context: Context): Boolean {
    return !getPrefs(context).getString(KEY_PIN_HASH, null).isNullOrBlank()
  }

  fun setPin(context: Context, pin: String) {
    val hash = hashPin(pin)
    getPrefs(context).edit().putString(KEY_PIN_HASH, hash).apply()
  }

  fun verifyPin(context: Context, pin: String): Boolean {
    val savedHash = getPrefs(context).getString(KEY_PIN_HASH, null) ?: return false
    return savedHash == hashPin(pin)
  }

  private fun hashPin(pin: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
  }

  fun getVaultDir(context: Context): File {
    val dir = File(context.filesDir, VAULT_FOLDER_NAME)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    val noMedia = File(dir, ".nomedia")
    if (!noMedia.exists()) {
      try {
        noMedia.createNewFile()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to create .nomedia in vault", e)
      }
    }
    return dir
  }

  private fun getIndexFile(context: Context): File {
    return File(getVaultDir(context), INDEX_FILE_NAME)
  }

  @Synchronized
  fun getVaultItems(context: Context): List<VaultItem> {
    val file = getIndexFile(context)
    if (!file.exists()) return emptyList()
    return try {
      val content = file.readText()
      json.decodeFromString<List<VaultItem>>(content)
    } catch (e: Exception) {
      Log.e(TAG, "Error reading vault index", e)
      emptyList()
    }
  }

  @Synchronized
  private fun saveVaultItems(context: Context, items: List<VaultItem>) {
    val file = getIndexFile(context)
    try {
      val content = json.encodeToString(items)
      file.writeText(content)
    } catch (e: Exception) {
      Log.e(TAG, "Error saving vault index", e)
    }
  }

  suspend fun addToVault(context: Context, sourceFile: File): Result<VaultItem> =
    withContext(Dispatchers.IO) {
      try {
        if (!sourceFile.exists()) {
          return@withContext Result.failure(IllegalArgumentException("Source file does not exist"))
        }

        val vaultDir = getVaultDir(context)
        val id = UUID.randomUUID().toString()
        val extension = sourceFile.extension
        val targetName = if (extension.isNotBlank()) "$id.$extension" else id
        val targetFile = File(vaultDir, targetName)

        // Copy file stream
        FileInputStream(sourceFile).use { input ->
          FileOutputStream(targetFile).use { output ->
            input.copyTo(output)
          }
        }

        val item = VaultItem(
          id = id,
          fileName = sourceFile.name,
          originalPath = sourceFile.absolutePath,
          sizeBytes = sourceFile.length(),
          dateAdded = System.currentTimeMillis(),
          vaultFilePath = targetFile.absolutePath,
        )

        val currentItems = getVaultItems(context).toMutableList()
        currentItems.add(0, item)
        saveVaultItems(context, currentItems)

        // Attempt to remove original file from storage and update MediaStore
        val originalDeleted = sourceFile.delete()
        if (originalDeleted) {
          MediaScannerConnection.scanFile(
            context,
            arrayOf(sourceFile.absolutePath),
            null,
            null,
          )
        }

        Result.success(item)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to move file to vault", e)
        Result.failure(e)
      }
    }

  suspend fun addToVault(context: Context, uri: Uri, displayName: String? = null): Result<VaultItem> =
    withContext(Dispatchers.IO) {
      try {
        val vaultDir = getVaultDir(context)
        val id = UUID.randomUUID().toString()
        val originalName = displayName ?: (uri.lastPathSegment ?: "video_$id.mp4")
        val extension = originalName.substringAfterLast('.', "mp4")
        val targetName = "$id.$extension"
        val targetFile = File(vaultDir, targetName)

        var sizeBytes = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(targetFile).use { output ->
            sizeBytes = input.copyTo(output)
          }
        } ?: return@withContext Result.failure(IllegalStateException("Cannot open input stream for $uri"))

        val item = VaultItem(
          id = id,
          fileName = originalName,
          originalPath = uri.path ?: "",
          sizeBytes = sizeBytes,
          dateAdded = System.currentTimeMillis(),
          vaultFilePath = targetFile.absolutePath,
        )

        val currentItems = getVaultItems(context).toMutableList()
        currentItems.add(0, item)
        saveVaultItems(context, currentItems)

        try {
          context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
          Log.w(TAG, "Could not delete original content uri directly: ${e.message}")
        }

        Result.success(item)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to move uri to vault", e)
        Result.failure(e)
      }
    }

  suspend fun restoreFromVault(context: Context, itemId: String): Result<File> =
    withContext(Dispatchers.IO) {
      try {
        val currentItems = getVaultItems(context).toMutableList()
        val item = currentItems.find { it.id == itemId }
          ?: return@withContext Result.failure(NoSuchElementException("Item not found in vault"))

        val vaultFile = File(item.vaultFilePath)
        if (!vaultFile.exists()) {
          return@withContext Result.failure(IllegalStateException("Vault file missing from storage"))
        }

        // Determine destination path
        var destFile = File(item.originalPath)
        val destDir = destFile.parentFile
        if (destDir == null || !destDir.exists() || !destDir.canWrite()) {
          val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
          val restoredDir = File(moviesDir, "Restored")
          if (!restoredDir.exists()) restoredDir.mkdirs()
          destFile = File(restoredDir, item.fileName)
        }

        // Copy out of vault
        FileInputStream(vaultFile).use { input ->
          FileOutputStream(destFile).use { output ->
            input.copyTo(output)
          }
        }

        // Remove from vault
        vaultFile.delete()
        currentItems.removeAll { it.id == itemId }
        saveVaultItems(context, currentItems)

        // Trigger MediaScanner so restored video appears in gallery / browser immediately
        MediaScannerConnection.scanFile(
          context,
          arrayOf(destFile.absolutePath),
          null,
          null,
        )

        Result.success(destFile)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to restore file from vault", e)
        Result.failure(e)
      }
    }

  suspend fun deleteFromVault(context: Context, itemId: String): Boolean =
    withContext(Dispatchers.IO) {
      try {
        val currentItems = getVaultItems(context).toMutableList()
        val item = currentItems.find { it.id == itemId } ?: return@withContext false

        val vaultFile = File(item.vaultFilePath)
        if (vaultFile.exists()) {
          vaultFile.delete()
        }

        currentItems.removeAll { it.id == itemId }
        saveVaultItems(context, currentItems)
        true
      } catch (e: Exception) {
        Log.e(TAG, "Failed to delete item from vault", e)
        false
      }
    }
}
