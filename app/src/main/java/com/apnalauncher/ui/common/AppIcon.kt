package com.apnalauncher.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apnalauncher.ApnaLauncherApp
import com.apnalauncher.data.AppInfo
import com.apnalauncher.data.LauncherSettings
import com.apnalauncher.ui.theme.LocalLauncherSettings
import androidx.compose.ui.platform.LocalContext

/** App icon with custom shape, optional label and notification badge. */
@Composable
fun AppIconView(
    app: AppInfo,
    badgeCount: Int = 0,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier,
    iconSize: Dp? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val settings = LocalLauncherSettings.current
    val context = LocalContext.current
    val repo = (context.applicationContext as ApnaLauncherApp).repo
    val custom by repo.customs.collectAsState()
    val override = custom[app.key]
    val iconSource = if (override?.iconPackage != null && override.iconRes != null) {
        repo.appByKey(override.iconPackage + "/" + override.iconRes) ?: app
    } else app
    val bitmap = repo.icons.get(iconSource, settings)
    val label = override?.label?.takeIf { it.isNotBlank() } ?: app.label
    val size = iconSize ?: (48.dp * settings.iconSize)

    Column(
        modifier = modifier
            .width((size.value + 16).dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(
                bitmap = bitmap,
                contentDescription = label,
                modifier = Modifier.size(size).clip(iconShapeFor(settings.iconShape)),
            )
            if (settings.badgesEnabled && badgeCount > 0) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ) {
                        Text(
                            if (badgeCount > 9) "9+" else badgeCount.toString(),
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }
        if (showLabel && settings.showLabels) {
            Text(
                text = label,
                fontSize = (11 * settings.labelSize).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
