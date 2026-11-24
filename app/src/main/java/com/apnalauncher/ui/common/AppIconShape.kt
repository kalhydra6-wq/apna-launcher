package com.apnalauncher.ui.common

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape

/** Icon masking shapes used by the settings screen. */
fun iconShapeFor(name: String): Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    when (name) {
        "circle" -> addOval(Rect(0f, 0f, w, h))
        "square" -> addRect(Rect(0f, 0f, w, h))
        "rounded" -> addRoundRect(
            androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, w * 0.22f, w * 0.22f),
        )
        "teardrop" -> {
            moveTo(w / 2, 0f)
            cubicTo(w, 0f, w, h * 0.35f, w, h * 0.62f)
            cubicTo(w, h * 0.85f, w * 0.78f, h, w * 0.5f, h)
            cubicTo(w * 0.22f, h, 0f, h * 0.85f, 0f, h * 0.62f)
            cubicTo(0f, h * 0.35f, 0f, 0f, w / 2, 0f)
            close()
        }
        else -> { // squircle (smooth rounded square)
            val r = size.minDimension * 0.28f
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            quadraticTo(w, 0f, w, r)
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            lineTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            close()
        }
    }
}
