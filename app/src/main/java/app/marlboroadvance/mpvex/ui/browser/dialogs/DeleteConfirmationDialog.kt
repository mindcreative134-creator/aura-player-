package app.marlboroadvance.mpvex.ui.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeleteConfirmationDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  itemType: String,
  itemCount: Int,
  itemNames: List<String> = emptyList(),
) {
  if (!isOpen) return

  val itemText = if (itemCount == 1) itemType else "${itemType}s"

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Delete $itemCount $itemText?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = AuraTokens.ObsidianSurface,
            ),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, AuraTokens.AuraSunset.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "This action cannot be undone. The selected item${if (itemCount == 1) "" else "s"} will be permanently deleted.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AuraTokens.AuraSunset,
            modifier = Modifier.padding(16.dp),
          )
        }

        if (itemNames.isNotEmpty()) {
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor = AuraTheme.cardBackgroundElevated,
              ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AuraTheme.cardBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
            ) {
              val scrollState = rememberScrollState()

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 200.dp)
                  .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                itemNames.forEachIndexed { index, name ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                  ) {
                    Text(
                      text = "${index + 1}. ",
                      style = MaterialTheme.typography.bodyMedium,
                      color = AuraTokens.AuraCyan,
                    )
                    Text(
                      text = name,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.weight(1f),
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm()
          onDismiss()
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = AuraTokens.AuraSunset,
            contentColor = androidx.compose.ui.graphics.Color.White,
          ),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text(
          text = "Delete",
          fontWeight = FontWeight.Bold,
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(16.dp),
      ) {
        Text("Cancel", fontWeight = FontWeight.Medium)
      }
    },
    containerColor = AuraTheme.cardBackground,
    tonalElevation = 0.dp,
    shape = RoundedCornerShape(24.dp),
  )
}
