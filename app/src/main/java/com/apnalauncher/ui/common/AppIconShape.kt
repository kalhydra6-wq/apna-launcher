package com.apnalauncher.ui.common

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.LayoutDirection

/** Icon masking shapes used by the settings screen. */
fun iconShapeFor(name: String): Shape = when (name) {
    "circle" -> GenericShape { size, _ ->
        addOutline(androidx.compose.ui.graphics.Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        })
    }
    "square" -> GenericShape { size, _ ->
        addRect(0f, 0f, size.width, size.height)
    }
    "rounded" -> GenericShape { size, _ ->
        addRoundRect(0f, 0f, size.width, size.height, radius = size.minDimension * 0.22f)
    }
    "teardrop" -> GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        moveTo(w / 2, 0f)
        cubicTo(w, 0f, w, h * 0.35f, w, h * 0.62f)
        cubicTo(w, h * 0.85f, w * 0.78f, h, w * 0.5f, h)
        cubicTo(w * 0.22f, h, 0f, h * 0.85f, 0f, h * 0.62f)
        cubicTo(0f, h * 0.35f, 0f, 0f, w / 2, 0f)
        close()
    }
    else -> GenericShape { size, _ -> // squircle (smooth rounded square)
        val w = size.width
        val h = size.height
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
