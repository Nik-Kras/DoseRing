package com.nikkras.dosering.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp

/** In-app twin of the widget's [com.nikkras.dosering.widget.RingRenderer]. */
@Composable
fun MedRing(
    color: Color,
    progress: Float,
    label: String,
    ready: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val side = this.size.minDimension
            val stroke = side * 0.095f
            val inset = stroke / 2f + side * 0.02f
            val arcSize = Size(side - inset * 2, side - inset * 2)
            val topLeft = Offset(inset, inset)
            drawCircle(color.copy(alpha = if (ready) 0.20f else 0.08f), radius = arcSize.minDimension / 2f - stroke / 2f)
            drawArc(color.copy(alpha = 0.25f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val sweep = if (ready) 360f else 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(color, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        val fontSize = with(LocalDensity.current) { (size * if (label.length > 1) 0.27f else 0.34f).toSp() }
        Text(label, color = color, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}
