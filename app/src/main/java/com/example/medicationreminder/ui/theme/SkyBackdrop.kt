package com.example.medicationreminder.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

@Composable
fun SkyBackdrop(dark: Boolean, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.verticalGradient(if (dark) listOf(
                Color(0xFF071426), Color(0xFF172A50), Color(0xFF25304B),
            ) else listOf(Color(0xFF80BDEC), Color(0xFFD3ECFF), Color(0xFFF0F7FC))))
            if (dark) {
                val random = Random(72)
                repeat(150) {
                    val at = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
                    val radius = (0.6f + random.nextFloat() * 1.2f) * density
                    drawCircle(Color.White.copy(alpha = 0.3f + random.nextFloat() * 0.65f), radius, at)
                    if (it % 17 == 0) {
                        drawLine(Color(0xFFB9D9FF).copy(alpha = 0.45f), at - Offset(radius * 3, 0f), at + Offset(radius * 3, 0f), density)
                        drawLine(Color(0xFFB9D9FF).copy(alpha = 0.45f), at - Offset(0f, radius * 3), at + Offset(0f, radius * 3), density)
                    }
                }
                drawCircle(Brush.radialGradient(listOf(Color(0x4456A3D6), Color.Transparent),
                    Offset(size.width * 0.85f, size.height * 0.18f), size.width * 0.3f), size.width * 0.3f,
                    Offset(size.width * 0.85f, size.height * 0.18f))
                drawCircle(Color(0xFFF1E8D4), size.width * 0.047f, Offset(size.width * 0.85f, size.height * 0.15f))
            } else {
                drawCircle(Brush.radialGradient(listOf(Color(0xAAFFF7D6), Color.Transparent),
                    Offset(size.width * 0.86f, size.height * 0.1f), size.width * 0.25f), size.width * 0.25f,
                    Offset(size.width * 0.86f, size.height * 0.1f))
                cloud(0.05f, 0.13f, 0.55f)
                cloud(0.62f, 0.30f, 0.65f)
                cloud(-0.10f, 0.61f, 0.8f)
                cloud(0.67f, 0.85f, 0.55f)
            }
        }
        content()
    }
}

private fun DrawScope.cloud(x: Float, y: Float, scale: Float) {
    val w = size.width * scale
    val at = Offset(size.width * x, size.height * y)
    val white = Color.White.copy(alpha = 0.65f)
    drawOval(white, at, Size(w, w * 0.22f))
    drawCircle(white, w * 0.15f, at + Offset(w * 0.36f, 0f))
    drawCircle(white, w * 0.12f, at + Offset(w * 0.60f, w * 0.01f))
}
