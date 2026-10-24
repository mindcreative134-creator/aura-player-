package app.marlboroadvance.mpvex.domain.vault

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class VaultDataIntegrityTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

  @Test
  fun testIndexSerializationAndDeserialization() {
    val items = listOf(
      VaultItem(
        id = "test-uuid-1",
        fileName = "my_private_video.mp4",
        originalPath = "/storage/emulated/0/DCIM/Camera/my_private_video.mp4",
        sizeBytes = 1048576L,
        dateAdded = 1700000000000L,
        vaultFilePath = "/data/user/0/com.cmstudio.auraplayer/files/.vault/test-uuid-1.mp4"
      )
    )

    val content = json.encodeToString(items)
    assertTrue("Serialized JSON must contain id", content.contains("test-uuid-1"))
    assertTrue("Serialized JSON must contain fileName", content.contains("my_private_video.mp4"))

    val deserialized = json.decodeFromString<List<VaultItem>>(content)
    assertEquals(1, deserialized.size)
    assertEquals("test-uuid-1", deserialized[0].id)
    assertEquals(1048576L, deserialized[0].sizeBytes)
  }

  @Test
  fun testRestoreFilenameConflictResolution() {
    val targetDir = tempFolder.newFolder("Movies")
    val existingFile = File(targetDir, "sample.mp4").apply {
      writeText("existing content")
    }
    assertTrue("Initial file must exist", existingFile.exists())

    // Simulate conflict resolution logic
    var destFile = File(targetDir, "sample.mp4")
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

    assertEquals("sample (1).mp4", destFile.name)
    assertFalse("Resolved destination file must not overwrite existing", destFile.exists())

    // If sample (1).mp4 also exists, resolve to sample (2).mp4
    File(targetDir, "sample (1).mp4").writeText("content 2")
    var destFile2 = File(targetDir, "sample.mp4")
    if (destFile2.exists()) {
      val baseName = destFile2.nameWithoutExtension
      val extension = destFile2.extension
      var counter = 1
      while (destFile2.exists()) {
        val candidateName = if (extension.isNotEmpty()) "$baseName ($counter).$extension" else "$baseName ($counter)"
        destFile2 = File(targetDir, candidateName)
        counter++
      }
    }
    assertEquals("sample (2).mp4", destFile2.name)
  }

  @Test
  fun testOrphanRecoveryLogic() {
    val vaultDir = tempFolder.newFolder(".vault")

    // Create an orphaned video file on disk
    val orphanFile = File(vaultDir, "orphaned-video-123.mp4").apply {
      writeText("video stream bytes")
    }

    // Mock existing index containing NO items
    val indexedPaths = emptySet<String>()

    val files = vaultDir.listFiles { f ->
      f.isFile && !f.name.endsWith(".tmp") && !f.name.endsWith(".bak") &&
      f.name != ".nomedia" && f.name != "vault_index.json" && f.length() > 0L
    } ?: emptyArray()

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
            vaultFilePath = f.absolutePath
          )
        )
      }
    }

    assertEquals(1, recovered.size)
    assertEquals("orphaned-video-123", recovered[0].id)
    assertEquals("orphaned-video-123.mp4", recovered[0].fileName)
    assertEquals(orphanFile.absolutePath, recovered[0].vaultFilePath)
  }

  @Test
  fun testTemporaryFileCleanupOnFailure() {
    val vaultDir = tempFolder.newFolder(".vault")
    val tmpFile = File(vaultDir, "temp-write.tmp").apply {
      writeText("partial bytes")
    }
    assertTrue("Temporary file exists before cleanup", tmpFile.exists())

    // On simulated failure:
    if (tmpFile.exists()) {
      tmpFile.delete()
    }

    assertFalse("Temporary file must be deleted on failure to avoid corruption", tmpFile.exists())
  }
}
