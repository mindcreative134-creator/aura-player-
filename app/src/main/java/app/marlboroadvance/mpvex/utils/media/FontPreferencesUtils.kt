package app.marlboroadvance.mpvex.utils.media

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.yubyf.truetypeparser.TTFFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** Copies font files from the selected directory to the app's internal storage. */
@SuppressLint("UseKtx")
fun copyFontsFromDirectory(
  context: Context,
  uriString: String,
) {
  runCatching {
    val destinationDir = File(context.filesDir, "fonts")
    if (!destinationDir.exists()) {
      destinationDir.mkdirs()
    }

    val uri = Uri.parse(uriString)
    if (uri.scheme == "content") {
      val treeDoc = DocumentFile.fromTreeUri(context, uri)
        ?: DocumentFile.fromSingleUri(context, uri)
      if (treeDoc != null && treeDoc.exists() && treeDoc.isDirectory) {
        treeDoc.listFiles().forEach { docFile ->
          if (docFile.isFile) {
            val fileName = docFile.name ?: return@forEach
            if (fileName.lowercase(Locale.ROOT).matches(".*\\.[ot]tf$".toRegex())) {
              context.contentResolver.openInputStream(docFile.uri)?.use { inputStream ->
                val outputFile = File(destinationDir, fileName)
                outputFile.outputStream().use { outputStream ->
                  inputStream.copyTo(outputStream)
                }
              }
            }
          }
        }
      }
    } else {
      // Direct file path or file:// URI
      val path = if (uri.scheme == "file") uri.path ?: uriString else uriString
      val sourceFolder = File(path)
      if (sourceFolder.exists() && sourceFolder.isDirectory) {
        sourceFolder.listFiles()?.forEach { file ->
          if (file.isFile && file.name.lowercase(Locale.ROOT).matches(".*\\.[ot]tf$".toRegex())) {
            val outputFile = File(destinationDir, file.name)
            file.inputStream().use { inputStream ->
              outputFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
              }
            }
          }
        }
      }
    }
  }.onFailure { e ->
    Log.e("SubtitlesPreferences", "Error copying fonts", e)
  }
}

// getSimplifiedPathFromUri is defined in AdvancedPreferencesScreen.kt within this package.

/** Represents a custom font discovered in the app's internal fonts directory. */
data class CustomFontEntry(
  val familyName: String,
  val file: File,
)

/**
 * Loads custom fonts (family name + file) from the app's internal fonts directory for previews.
 * Returns one entry per family name (first match kept if duplicates exist).
 */
suspend fun loadCustomFontEntries(context: Context): List<CustomFontEntry> =
  withContext(Dispatchers.IO) {
    val fontsDir = File(context.filesDir, "fonts")
    if (!fontsDir.exists()) return@withContext emptyList()

    val fontFiles =
      fontsDir
        .listFiles()
        ?.filter { it.isFile && it.name.lowercase(Locale.ROOT).matches(".*\\.[ot]tf$".toRegex()) }
        .orEmpty()

    val entries = mutableListOf<CustomFontEntry>()
    val seenFamilies = mutableSetOf<String>()

    for (fontFile in fontFiles) {
      val familyName =
        runCatching {
          fontFile.inputStream().use { input ->
            TTFFile
              .open(input)
              .families
              .values
              .firstOrNull()
          }
        }.getOrNull()

      if (!familyName.isNullOrBlank() && seenFamilies.add(familyName)) {
        entries += CustomFontEntry(familyName, fontFile)
      }
    }

    entries.sortedBy { it.familyName }
  }
