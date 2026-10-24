package app.marlboroadvance.mpvex.domain.vault

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.AtomicFile
import android.util.Log
import app.marlboroadvance.mpvex.utils.security.SecureKeyStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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

sealed class PinVerificationResult {
  object Success : PinVerificationResult()
  data class Incorrect(val remainingAttempts: Int) : PinVerificationResult()
  data class LockedOut(val lockoutSecondsRemaining: Long) : PinVerificationResult()
}

sealed class VaultAddResult {
  data class Moved(val item: VaultItem) : VaultAddResult()
  data class CopiedOriginalRemains(val item: VaultItem, val reason: String) : VaultAddResult()
  data class RequiresDeleteConfirmation(val item: VaultItem, val intentSender: IntentSender) : VaultAddResult()
  data class Failed(val message: String) : VaultAddResult()
}

object VaultManager {
  private const val TAG = "VaultManager"
  private const val PREFS_NAME = "apna_vault_prefs"
  private const val KEY_PIN_HASH = "vault_pin_hash"
  private const val KEY_FAILED_ATTEMPTS = "vault_pin_failed_attempts"
  private const val KEY_LOCKOUT_UNTIL = "vault_pin_lockout_until"

  private const val VAULT_FOLDER_NAME = ".vault"
  private const val INDEX_FILE_NAME = "vault_index.json"

  private const val MAX_ATTEMPTS_TIER_1 = 5
  private const val LOCKOUT_TIER_1_MS = 30_000L // 30 seconds
  private const val MAX_ATTEMPTS_TIER_2 = 10
  private const val LOCKOUT_TIER_2_MS = 300_000L // 5 minutes

  private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

  private fun getPrefs(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  // -------------------------------------------------------------
  // PIN Management & Rate Limiting
  // -------------------------------------------------------------

  fun isPinSet(context: Context): Boolean {
    return !getPrefs(context).getString(KEY_PIN_HASH, null).isNullOrBlank()
  }

  fun setPin(context: Context, pin: String) {
    val saltedHash = SecureKeyStoreHelper.hashPinPbkdf2(pin)
    getPrefs(context).edit()
      .putString(KEY_PIN_HASH, saltedHash)
      .putInt(KEY_FAILED_ATTEMPTS, 0)
      .putLong(KEY_LOCKOUT_UNTIL, 0L)
      .apply()
  }

  fun getLockoutRemainingSeconds(context: Context): Long {
    val lockoutUntil = getPrefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)
    val now = System.currentTimeMillis()
    return if (lockoutUntil > now) (lockoutUntil - now + 999) / 1000 else 0L
  }

  fun verifyPin(context: Context, pin: String): Boolean {
    return verifyPinWithRateLimit(context, pin) is PinVerificationResult.Success
  }

  fun verifyPinWithRateLimit(context: Context, pin: String): PinVerificationResult {
    val prefs = getPrefs(context)
    val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return PinVerificationResult.Incorrect(0)

    val remainingLockout = getLockoutRemainingSeconds(context)
    if (remainingLockout > 0) {
      return PinVerificationResult.LockedOut(remainingLockout)
    }

    val isValid = SecureKeyStoreHelper.verifyPin(pin, savedHash)
    if (isValid) {
      // Automatic migration: If hash was stored in legacy SHA-256 format, upgrade it to PBKDF2 now
      if (SecureKeyStoreHelper.isLegacySha256(savedHash)) {
        val upgradedHash = SecureKeyStoreHelper.hashPinPbkdf2(pin)
        prefs.edit().putString(KEY_PIN_HASH, upgradedHash).apply()
        Log.i(TAG, "Successfully migrated legacy vault PIN hash to PBKDF2")
      }

      // Reset lockout & failed attempts
      prefs.edit()
        .putInt(KEY_FAILED_ATTEMPTS, 0)
        .putLong(KEY_LOCKOUT_UNTIL, 0L)
        .apply()

      return PinVerificationResult.Success
    } else {
      // Failed attempt
      val currentFailed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
      var lockoutDuration = 0L

      if (currentFailed >= MAX_ATTEMPTS_TIER_2) {
        lockoutDuration = LOCKOUT_TIER_2_MS
      } else if (currentFailed >= MAX_ATTEMPTS_TIER_1) {
        lockoutDuration = LOCKOUT_TIER_1_MS
      }

      val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, currentFailed)
      if (lockoutDuration > 0) {
        val lockoutUntil = System.currentTimeMillis() + lockoutDuration
        editor.putLong(KEY_LOCKOUT_UNTIL, lockoutUntil).apply()
        return PinVerificationResult.LockedOut((lockoutDuration + 999) / 1000)
      } else {
        editor.apply()
        val remaining = MAX_ATTEMPTS_TIER_1 - currentFailed
        return PinVerificationResult.Incorrect(maxOf(1, remaining))
      }
    }
  }

  // -------------------------------------------------------------
  // Vault Storage & Index Handling
  // -------------------------------------------------------------

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
    val atomicFile = AtomicFile(file)
    var items: List<VaultItem> = emptyList()

    if (file.exists() || File(file.parentFile, "${file.name}.bak").exists()) {
      try {
        val bytes = atomicFile.readFully()
        val content = String(bytes, Charsets.UTF_8)
        items = json.decodeFromString<List<VaultItem>>(content)
      } catch (e: Exception) {
        Log.e(TAG, "Error reading vault index", e)
      }
    }

    // Orphan Recovery: Scan vault folder for valid media files not present in index
    val vaultDir = getVaultDir(context)
    val files = vaultDir.listFiles { f ->
      f.isFile && !f.name.endsWith(".tmp") && !f.name.endsWith(".bak") &&
      f.name != ".nomedia" && f.name != INDEX_FILE_NAME && f.length() > 0L
    } ?: emptyArray()

    val indexedPaths = items.map { it.vaultFilePath }.toSet()
    val recovered = mutableListOf<VaultItem>()
    for (f in files) {
      if (f.absolutePath !in indexedPaths) {
        recovered.add(
          VaultItem(
            id = f.nameWithoutExtension,
            fileName = f.name,
            originalPath = "",
            sizeBytes = f.length(),
            dateAdded = f.lastModified(),
            vaultFilePath = f.absolutePath,
          )
        )
      }
    }

    if (recovered.isNotEmpty()) {
      Log.i(TAG, "Recovered ${recovered.size} orphaned media file(s) into vault index")
      val merged = items + recovered
      saveVaultItems(context, merged)
      return merged
    }

    return items
  }

  @Synchronized
  fun saveVaultItems(context: Context, items: List<VaultItem>): Boolean {
    val file = getIndexFile(context)
    val atomicFile = AtomicFile(file)
    var stream: FileOutputStream? = null
    return try {
      val content = json.encodeToString(items)
      val bytes = content.toByteArray(Charsets.UTF_8)
      stream = atomicFile.startWrite()
      stream.write(bytes)
      stream.flush()
      atomicFile.finishWrite(stream)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Error saving vault index atomically", e)
      if (stream != null) {
        atomicFile.failWrite(stream)
      }
      false
    }
  }

  // -------------------------------------------------------------
  // Add to Vault (Safe Atomic Copy & Verified Deletion)
  // -------------------------------------------------------------

  suspend fun addToVault(context: Context, sourceFile: File): VaultAddResult =
    withContext(Dispatchers.IO) {
      try {
        if (!sourceFile.exists()) {
          return@withContext VaultAddResult.Failed("Source file does not exist")
        }

        // Prevent duplicate items
        val currentItems = getVaultItems(context).toMutableList()
        val existing = currentItems.find { it.originalPath == sourceFile.absolutePath }
        if (existing != null && File(existing.vaultFilePath).exists()) {
          return@withContext VaultAddResult.CopiedOriginalRemains(
            existing,
            "File already exists in vault."
          )
        }

        val vaultDir = getVaultDir(context)
        val id = UUID.randomUUID().toString()
        val extension = sourceFile.extension
        val targetName = if (extension.isNotBlank()) "$id.$extension" else id
        val targetFile = File(vaultDir, targetName)
        val tmpFile = File(vaultDir, "$targetName.tmp")

        // 1. Copy stream safely to temporary file with flush & sync
        var bytesCopied = 0L
        FileInputStream(sourceFile).use { input ->
          FileOutputStream(tmpFile).use { output ->
            bytesCopied = input.copyTo(output)
            output.flush()
            output.fd.sync()
          }
        }

        // 2. Verify bytes written matches source
        if (bytesCopied != sourceFile.length() || !tmpFile.exists() || tmpFile.length() == 0L) {
          if (tmpFile.exists()) tmpFile.delete()
          return@withContext VaultAddResult.Failed("Integrity check failed: incomplete file write to vault")
        }

        // 3. Atomic rename .tmp to targetFile
        if (!tmpFile.renameTo(targetFile)) {
          if (tmpFile.exists()) tmpFile.delete()
          return@withContext VaultAddResult.Failed("Failed to finalize vault file")
        }

        val item = VaultItem(
          id = id,
          fileName = sourceFile.name,
          originalPath = sourceFile.absolutePath,
          sizeBytes = targetFile.length(),
          dateAdded = System.currentTimeMillis(),
          vaultFilePath = targetFile.absolutePath,
        )

        // 4. Update index safely - MUST BE VERIFIED BEFORE DELETING ORIGINAL
        currentItems.add(0, item)
        val indexSaved = saveVaultItems(context, currentItems)
        if (!indexSaved) {
          Log.e(TAG, "Index save failed; aborting vault addition and preserving original file")
          targetFile.delete()
          return@withContext VaultAddResult.Failed("Failed to durably commit vault index. Original file is preserved.")
        }

        // 5. Explicitly verify deletion of original ONLY AFTER index is saved
        val originalDeleted = try {
          sourceFile.delete()
        } catch (e: Exception) {
          Log.w(TAG, "Exception deleting original file: ${e.message}")
          false
        }

        if (originalDeleted) {
          MediaScannerConnection.scanFile(
            context,
            arrayOf(sourceFile.absolutePath),
            null,
            null,
          )
          VaultAddResult.Moved(item)
        } else {
          VaultAddResult.CopiedOriginalRemains(
            item,
            "Copied to Vault safely. However, Android storage permissions prevented deleting the original file automatically. Please delete the original video from your gallery so it remains private."
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to move file to vault", e)
        VaultAddResult.Failed(e.message ?: "Unknown error moving file to vault")
      }
    }

  suspend fun addToVault(context: Context, uri: Uri, displayName: String? = null): VaultAddResult =
    withContext(Dispatchers.IO) {
      try {
        val vaultDir = getVaultDir(context)
        val id = UUID.randomUUID().toString()
        val originalName = displayName ?: (uri.lastPathSegment ?: "video_$id.mp4")
        val extension = originalName.substringAfterLast('.', "mp4")
        val targetName = "$id.$extension"
        val targetFile = File(vaultDir, targetName)
        val tmpFile = File(vaultDir, "$targetName.tmp")

        // 1. Copy content stream safely to temporary file
        var sizeBytes = 0L
        val inputStream = context.contentResolver.openInputStream(uri)
          ?: return@withContext VaultAddResult.Failed("Cannot open input stream for $uri")

        inputStream.use { input ->
          FileOutputStream(tmpFile).use { output ->
            sizeBytes = input.copyTo(output)
            output.flush()
            output.fd.sync()
          }
        }

        if (sizeBytes <= 0L || !tmpFile.exists() || tmpFile.length() == 0L) {
          if (tmpFile.exists()) tmpFile.delete()
          return@withContext VaultAddResult.Failed("Integrity check failed: 0 bytes read from URI")
        }

        // 2. Atomic rename
        if (!tmpFile.renameTo(targetFile)) {
          if (tmpFile.exists()) tmpFile.delete()
          return@withContext VaultAddResult.Failed("Failed to finalize vault file")
        }

        val item = VaultItem(
          id = id,
          fileName = originalName,
          originalPath = uri.toString(),
          sizeBytes = targetFile.length(),
          dateAdded = System.currentTimeMillis(),
          vaultFilePath = targetFile.absolutePath,
        )

        // 3. Update index safely - MUST BE VERIFIED BEFORE DELETING ORIGINAL
        val currentItems = getVaultItems(context).toMutableList()
        currentItems.add(0, item)
        val indexSaved = saveVaultItems(context, currentItems)
        if (!indexSaved) {
          Log.e(TAG, "Index save failed; aborting vault addition and preserving original URI")
          targetFile.delete()
          return@withContext VaultAddResult.Failed("Failed to durably commit vault index. Original file is preserved.")
        }

        // 4. Attempt deletion of original URI with explicit outcome checking ONLY AFTER index is saved
        var deleted = false
        try {
          if (DocumentsContract.isDocumentUri(context, uri)) {
            deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
          } else {
            val rows = context.contentResolver.delete(uri, null, null)
            deleted = rows > 0
          }
        } catch (recoverable: RecoverableSecurityException) {
          Log.i(TAG, "MediaStore requires user confirmation to delete: $uri")
          return@withContext VaultAddResult.RequiresDeleteConfirmation(
            item,
            recoverable.userAction.actionIntent.intentSender
          )
        } catch (e: Exception) {
          Log.w(TAG, "ContentResolver.delete failed: ${e.message}")
          deleted = false
        }

        if (deleted) {
          VaultAddResult.Moved(item)
        } else {
          VaultAddResult.CopiedOriginalRemains(
            item,
            "Copied to Vault safely. However, Android storage permissions prevented deleting the original file automatically. Please delete the original video from your gallery so it remains private."
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to move uri to vault", e)
        VaultAddResult.Failed(e.message ?: "Unknown error")
      }
    }

  // -------------------------------------------------------------
  // Restore & Delete from Vault
  // -------------------------------------------------------------

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

        // Determine destination directory
        val origFile = if (item.originalPath.startsWith("/")) File(item.originalPath) else null
        val destDir = origFile?.parentFile
        val targetDir = if (destDir != null && destDir.exists() && destDir.canWrite()) {
          destDir
        } else {
          val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
          val restoredDir = File(moviesDir, "Restored")
          if (!restoredDir.exists()) restoredDir.mkdirs()
          restoredDir
        }

        // Conflict handling: Generate a unique filename if destination already exists
        var destFile = File(targetDir, item.fileName)
        if (destFile.exists()) {
          val baseName = destFile.nameWithoutExtension
          val extension = destFile.extension
          var counter = 1
          while (destFile.exists()) {
            val candidateName = if (extension.isNotEmpty()) "$baseName ($counter).$extension" else "$baseName ($counter)"
            destFile = File(targetDir, candidateName)
            counter++
          }
        }

        // Copy out of vault via temporary file
        val destTmp = File(destFile.parentFile, "${destFile.name}.tmp")
        var bytesCopied = 0L
        FileInputStream(vaultFile).use { input ->
          FileOutputStream(destTmp).use { output ->
            bytesCopied = input.copyTo(output)
            output.flush()
            output.fd.sync()
          }
        }

        if (bytesCopied != vaultFile.length() || !destTmp.exists() || destTmp.length() == 0L) {
          if (destTmp.exists()) destTmp.delete()
          return@withContext Result.failure(IllegalStateException("Failed to verify restored file integrity"))
        }

        if (!destTmp.renameTo(destFile)) {
          if (destTmp.exists()) destTmp.delete()
          return@withContext Result.failure(IllegalStateException("Failed to move restored file to destination"))
        }

        // Update index BEFORE removing the vault copy
        currentItems.removeAll { it.id == itemId }
        val indexSaved = saveVaultItems(context, currentItems)
        if (!indexSaved) {
          Log.w(TAG, "Index update failed during restore; vault copy preserved to prevent data loss")
        } else {
          // Remove from vault only after destination and index are verified
          vaultFile.delete()
        }

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

        currentItems.removeAll { it.id == itemId }
        val indexSaved = saveVaultItems(context, currentItems)
        if (indexSaved) {
          if (vaultFile.exists()) {
            vaultFile.delete()
          }
          true
        } else {
          Log.e(TAG, "Failed to update vault index during deletion")
          false
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to delete item from vault", e)
        false
      }
    }
}
