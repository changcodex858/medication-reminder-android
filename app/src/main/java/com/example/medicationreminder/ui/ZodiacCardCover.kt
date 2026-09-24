package com.example.medicationreminder.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.medicationreminder.R
import com.example.medicationreminder.domain.Zodiac
import kotlin.math.roundToInt

@Composable
fun ZodiacPortrait(zodiac: Zodiac, modifier: Modifier = Modifier) {
    val atlas = ImageBitmap.imageResource(R.drawable.zodiac_atlas)
    Canvas(modifier.semantics { contentDescription = "${zodiac.label}生肖立体卡面" }) {
        val cell = IntSize(atlas.width / 4, atlas.height / 3)
        drawImage(atlas,
            srcOffset = IntOffset(zodiac.ordinal % 4 * cell.width, zodiac.ordinal / 4 * cell.height),
            srcSize = cell,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        )
    }
}

@Composable
fun ZodiacCardCover(zodiac: Zodiac, onSelected: (Zodiac) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val tilt by animateFloatAsState(if (pressed) -4f else 0f, label = "cardTilt")
    val colors = when (zodiac.ordinal % 4) {
        0 -> listOf(Color(0xFFD7ECFB), Color(0xFF9EC5E5))
        1 -> listOf(Color(0xFFE9E0F7), Color(0xFFC1B1DE))
        2 -> listOf(Color(0xFFFFEBD1), Color(0xFFEBC596))
        else -> listOf(Color(0xFFE1F1E7), Color(0xFFACD3C3))
    }
    Box(Modifier.fillMaxWidth().height(204.dp)
        .graphicsLayer { rotationX = tilt; cameraDistance = 16 * density }
        .clipToBounds()
        .background(Brush.linearGradient(colors))
        .clickable(interactionSource = interactions, indication = null) { choosing = true }) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.25f), size.width * 0.42f, Offset(size.width * 0.82f, size.height * 0.28f))
            drawCircle(Color.White.copy(alpha = 0.2f), size.width * 0.28f, Offset(size.width * 0.95f, size.height))
            drawLine(Color.White.copy(alpha = 0.75f), Offset.Zero, Offset(size.width, 0f), 3.dp.toPx())
        }
        Column(Modifier.align(Alignment.CenterStart).padding(22.dp).fillMaxWidth(0.45f)) {
            Text("十二生肖", style = MaterialTheme.typography.labelMedium, color = Color(0xFF375572))
            Spacer(Modifier.height(8.dp))
            Text("${zodiac.label} · 陪伴", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF17354C))
            Spacer(Modifier.height(16.dp))
            Surface(color = Color.White.copy(alpha = 0.7f), shape = CircleShape) {
                Text("更换卡面 ›", Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelMedium, color = Color(0xFF294D6D))
            }
        }
        ZodiacPortrait(zodiac, Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(182.dp))
    }
    if (choosing) AlertDialog(
        onDismissRequest = { choosing = false },
        title = { Text("选择你的生肖伙伴") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("每个用药计划都可以拥有自己的卡面", style = MaterialTheme.typography.bodySmall)
                Zodiac.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { candidate ->
                            Column(Modifier.weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                .border(if (candidate == zodiac) 2.dp else 0.dp,
                                    if (candidate == zodiac) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable { onSelected(candidate); choosing = false }
                                .padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                ZodiacPortrait(candidate, Modifier.fillMaxWidth().aspectRatio(1f))
                                Text(if (candidate == zodiac) "${candidate.label} ✓" else candidate.label,
                                    style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { choosing = false }) { Text("完成") } },
    )
}
