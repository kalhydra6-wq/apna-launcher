package com.apnalauncher.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Hosts a real AppWidget inside the Compose grid. */
@Composable
fun WidgetView(
    widgetId: Int,
    host: AppWidgetHost,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    AndroidView(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        factory = { context ->
            val manager = AppWidgetManager.getInstance(context)
            val info = manager.getAppWidgetInfo(widgetId)
            val container = FrameLayout(context)
            if (info != null) {
                val view = host.createView(context, widgetId, info)
                if (view != null) {
                    view.setAppWidget(widgetId, info)
                    container.addView(view)
                }
            }
            container
        },
        update = { container ->
            if (container.childCount == 0) {
                val manager = AppWidgetManager.getInstance(container.context)
                val info = manager.getAppWidgetInfo(widgetId)
                if (info != null) {
                    host.createView(container.context, widgetId, info)?.let { view ->
                        view.setAppWidget(widgetId, info)
                        container.addView(view)
                    }
                }
            }
        },
    )
}
