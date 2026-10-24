package app.marlboroadvance.mpvex.ui.browser.states

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.presentation.components.ApnaLoadingAnimation

@Composable
fun LoadingState(
  icon: ImageVector? = null,
  title: String = "Scanning videos...",
  message: String = "Loading your media library",
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(bottom = 60.dp),
    contentAlignment = Alignment.Center
  ) {
    ApnaLoadingAnimation(
      size = 88.dp,
      title = title,
      subtitle = message
    )
  }
}
