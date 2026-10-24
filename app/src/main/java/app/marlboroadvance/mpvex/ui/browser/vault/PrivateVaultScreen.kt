package app.marlboroadvance.mpvex.ui.browser.vault

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.domain.vault.VaultItem
import app.marlboroadvance.mpvex.domain.vault.VaultManager
import app.marlboroadvance.mpvex.domain.vault.VaultAddResult
import app.marlboroadvance.mpvex.domain.vault.PinVerificationResult
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
object PrivateVaultScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val scope = rememberCoroutineScope()

    var isUnlocked by remember { mutableStateOf(false) }
    var isSettingUpNewPin by remember { mutableStateOf(!VaultManager.isPinSet(context)) }
    var pinSetupStep by remember { mutableStateOf(1) } // 1 = enter new, 2 = confirm
    var firstEnteredPin by remember { mutableStateOf("") }
    var currentPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val vaultItems = remember { mutableStateListOf<VaultItem>() }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshItems() {
      vaultItems.clear()
      vaultItems.addAll(VaultManager.getVaultItems(context))
    }

    LaunchedEffect(isUnlocked) {
      if (isUnlocked) {
        refreshItems()
      }
    }

    var showRemainingOriginalsDialog by remember { mutableStateOf(false) }
    var remainingOriginalsCount by remember { mutableStateOf(0) }

    // Video picker launcher to add videos to vault
    val videoPickerLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
      if (uris.isNotEmpty()) {
        isLoading = true
        scope.launch(Dispatchers.IO) {
          var movedCount = 0
          var copiedCount = 0
          uris.forEach { uri ->
            try {
              var displayName: String? = null
              context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                  val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                  if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex)
                  }
                }
              }
              when (val result = VaultManager.addToVault(context, uri, displayName)) {
                is VaultAddResult.Moved -> movedCount++
                is VaultAddResult.CopiedOriginalRemains -> copiedCount++
                is VaultAddResult.RequiresDeleteConfirmation -> copiedCount++
                is VaultAddResult.Failed -> {
                  android.util.Log.w("PrivateVault", "Failed to add to vault: ${result.message}")
                }
              }
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }
          withContext(Dispatchers.Main) {
            isLoading = false
            refreshItems()
            if (copiedCount > 0) {
              remainingOriginalsCount = copiedCount
              showRemainingOriginalsDialog = true
            } else if (movedCount > 0) {
              Toast.makeText(
                context,
                "Moved $movedCount video(s) to Private Vault. Originals deleted from gallery.",
                Toast.LENGTH_LONG,
              ).show()
            }
          }
        }
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = "Private Vault",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
              )
              if (isUnlocked) {
                Text(
                  text = "${vaultItems.size} hidden videos (PIN protected)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          },
          navigationIcon = {
            IconButton(onClick = { backstack.removeLastOrNull() }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
          actions = {
            if (isUnlocked) {
              IconButton(onClick = {
                isUnlocked = false
                currentPinInput = ""
                Toast.makeText(context, "Vault Locked", Toast.LENGTH_SHORT).show()
              }) {
                Icon(Icons.Default.Lock, contentDescription = "Lock Vault", tint = MaterialTheme.colorScheme.primary)
              }
            }
          },
        )
      },
      floatingActionButton = {
        if (isUnlocked) {
          FloatingActionButton(
            onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ) {
            Icon(Icons.Default.Add, contentDescription = "Hide Videos")
          }
        }
      },
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
      ) {
        if (!isUnlocked) {
          // PIN Entry / Setup Screen
          VaultPinKeypad(
            isSettingUp = isSettingUpNewPin,
            setupStep = pinSetupStep,
            enteredLength = currentPinInput.length,
            errorMessage = errorMessage,
            onDigitClick = { digit ->
              errorMessage = null
              if (currentPinInput.length < 4) {
                val updated = currentPinInput + digit
                currentPinInput = updated
                if (updated.length == 4) {
                  if (isSettingUpNewPin) {
                    if (pinSetupStep == 1) {
                      firstEnteredPin = updated
                      currentPinInput = ""
                      pinSetupStep = 2
                    } else {
                      if (updated == firstEnteredPin) {
                        VaultManager.setPin(context, updated)
                        isSettingUpNewPin = false
                        isUnlocked = true
                        Toast.makeText(context, "Vault PIN created successfully!", Toast.LENGTH_SHORT).show()
                      } else {
                        errorMessage = "PINs do not match. Try again."
                        currentPinInput = ""
                        pinSetupStep = 1
                      }
                    }
                  } else {
                    when (val res = VaultManager.verifyPinWithRateLimit(context, updated)) {
                      is PinVerificationResult.Success -> {
                        isUnlocked = true
                        currentPinInput = ""
                        errorMessage = null
                      }
                      is PinVerificationResult.Incorrect -> {
                        errorMessage = "Incorrect PIN. ${res.remainingAttempts} attempt(s) remaining."
                        currentPinInput = ""
                      }
                      is PinVerificationResult.LockedOut -> {
                        errorMessage = "Too many failed attempts. Try again in ${res.lockoutSecondsRemaining}s."
                        currentPinInput = ""
                      }
                    }
                  }
                }
              }
            },
            onBackspaceClick = {
              if (currentPinInput.isNotEmpty()) {
                currentPinInput = currentPinInput.dropLast(1)
                errorMessage = null
              }
            },
          )
        } else {
          // Unlocked Vault Content
          if (vaultItems.isEmpty()) {
            VaultEmptyView(onAddClick = { videoPickerLauncher.launch(arrayOf("video/*")) })
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(vaultItems, key = { it.id }) { item ->
                VaultVideoCard(
                  item = item,
                  onPlay = {
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                      action = Intent.ACTION_VIEW
                      setDataAndType(Uri.fromFile(File(item.vaultFilePath)), "video/*")
                      putExtra("internal_launch", true)
                    }
                    context.startActivity(intent)
                  },
                  onRestore = {
                    scope.launch(Dispatchers.IO) {
                      val result = VaultManager.restoreFromVault(context, item.id)
                      withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                          refreshItems()
                          Toast.makeText(context, "Restored to gallery: ${item.fileName}", Toast.LENGTH_SHORT).show()
                        } else {
                          Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
                        }
                      }
                    }
                  },
                  onDelete = {
                    scope.launch(Dispatchers.IO) {
                      val success = VaultManager.deleteFromVault(context, item.id)
                      withContext(Dispatchers.Main) {
                        if (success) {
                          refreshItems()
                          Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                        }
                      }
                    }
                  },
                )
              }
            }
          }
        }
      }
    }

    if (showRemainingOriginalsDialog) {
      AlertDialog(
        onDismissRequest = { showRemainingOriginalsDialog = false },
        title = { Text("Original File Remains in Gallery") },
        text = {
          Text(
            "$remainingOriginalsCount video(s) were safely copied into your Private Vault. However, Android storage permissions prevented deleting the original file(s) automatically.\n\nTo ensure complete privacy, please delete the original video(s) manually from your device gallery."
          )
        },
        confirmButton = {
          Button(onClick = { showRemainingOriginalsDialog = false }) {
            Text("Understood")
          }
        },
      )
    }
  }

  @Composable
  private fun VaultPinKeypad(
    isSettingUp: Boolean,
    setupStep: Int,
    enteredLength: Int,
    errorMessage: String?,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(72.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(36.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = if (isSettingUp) {
          if (setupStep == 1) "Create Vault PIN" else "Confirm Your PIN"
        } else {
          "Enter Vault PIN"
        },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = if (isSettingUp) {
          if (setupStep == 1) "Choose a 4-digit security code" else "Re-enter the code to confirm"
        } else {
          "Keep your private videos hidden & secure"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(24.dp))

      // 4 Dot indicators
      Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        for (i in 0 until 4) {
          val isFilled = i < enteredLength
          Box(
            modifier = Modifier
              .size(16.dp)
              .clip(CircleShape)
              .background(
                if (isFilled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
              ),
          )
        }
      }

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = errorMessage,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
        )
      }

      Spacer(modifier = Modifier.height(36.dp))

      // Numeric Keypad Grid (1-9, empty/backspace, 0)
      val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL"),
      )

      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        digits.forEach { row ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            row.forEach { item ->
              when (item) {
                "" -> Spacer(modifier = Modifier.size(68.dp))
                "DEL" -> {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                      .size(68.dp)
                      .clickable { onBackspaceClick() },
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        Icons.Default.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                      )
                    }
                  }
                }
                else -> {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                      .size(68.dp)
                      .clickable { onDigitClick(item) },
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = item,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  @Composable
  private fun VaultEmptyView(onAddClick: () -> Unit) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(88.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.LockOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Your Vault is Empty",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Hide personal or sensitive videos here.\nHidden videos will disappear from your gallery and phone storage.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = onAddClick,
        shape = RoundedCornerShape(12.dp),
      ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Hide Videos Now")
      }
    }
  }

  @Composable
  private fun VaultVideoCard(
    item: VaultItem,
    onPlay: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
  ) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onPlay() },
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      ),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(48.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.PlayCircleOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(28.dp),
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.fileName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(3.dp))
          val formattedSize = remember(item.sizeBytes) {
            val mb = item.sizeBytes.toDouble() / (1024 * 1024)
            String.format(Locale.US, "%.1f MB", mb)
          }
          val dateStr = remember(item.dateAdded) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.dateAdded))
          }
          Text(
            text = "$formattedSize • Hidden $dateStr",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Box {
          IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
          }
          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
          ) {
            DropdownMenuItem(
              text = { Text("Restore to Gallery") },
              leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
              onClick = {
                showMenu = false
                onRestore()
              },
            )
            DropdownMenuItem(
              text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
              leadingIcon = {
                Icon(
                  Icons.Default.Delete,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
              },
              onClick = {
                showMenu = false
                showDeleteConfirm = true
              },
            )
          }
        }
      }
    }

    if (showDeleteConfirm) {
      AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text("Delete permanently?") },
        text = { Text("This will erase '${item.fileName}' from your device. This cannot be undone.") },
        confirmButton = {
          TextButton(onClick = {
            showDeleteConfirm = false
            onDelete()
          }) {
            Text("Delete", color = MaterialTheme.colorScheme.error)
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteConfirm = false }) {
            Text("Cancel")
          }
        },
      )
    }
  }
}
