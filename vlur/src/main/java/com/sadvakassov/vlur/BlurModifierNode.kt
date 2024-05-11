package com.sadvakassov.vlur

import androidx.annotation.FloatRange
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadvakassov.vlur.internal.PREVIEW_TEXT
import com.sadvakassov.vlur.internal.TAG
import com.sadvakassov.vlur.internal.createBitmapFromPicture
import com.sadvakassov.vlur.internal.gradientColors
import timber.log.Timber
import kotlin.random.Random

private const val NO_ID = -1

@Stable
fun Modifier.vlur(
    vulkanState: VlurState,
    @FloatRange(from = 0.0, to = 25.0) blurRadius: Float,
    id: Int = NO_ID, // for testing purposes
) = this then graphicsLayer { clip = true } then BlurElement(
    vulkanState = vulkanState,
    blurRadius = blurRadius,
    id = id,
)

private data class BlurElement(
    private val vulkanState: VlurState,
    private var blurRadius: Float,
    private val id: Int,
) : ModifierNodeElement<BlurModifier>() {
    private var internalId: Int =
        if (id == NO_ID) Random.nextInt(100) else id // TODO find better way

    override fun create(): BlurModifier {
        Timber.tag(TAG).d("BlurElement: create at $internalId")
        return BlurModifier(
            vlurState = vulkanState,
            id = internalId,
            blurRadius = blurRadius,
        )
    }

    override fun update(node: BlurModifier) {
        Timber.tag(TAG).d("BlurElement: update at $internalId")
        node.blurRadius = blurRadius
    }
}

private class BlurModifier(
    private val vlurState: VlurState,
    private val id: Int,
    var blurRadius: Float,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private var isPrepared = false

    init {
        Timber.tag(TAG).d("BlurModifier: init called at $id")
    }

    override fun ContentDrawScope.draw() {
        val canvasHardwareAccelerated = drawContext.canvas.nativeCanvas.isHardwareAccelerated

        if (isComposePreview() || !canvasHardwareAccelerated) {
            drawPreview()
            return
        }

        Timber.tag(TAG).d("BlurModifier: draw called with radius $blurRadius at $id")

        prepare()

        vlurState.blur(blurRadius, id)?.let {
            drawImage(
                image = it.asImageBitmap(),
                topLeft = Offset(0f, 0f),
            )
        }
    }

    private fun ContentDrawScope.prepare() {
        if (!isPrepared) {
            isPrepared = true
            val picture = vlurState.takePicture(size) { canvas ->
                draw(this, layoutDirection, Canvas(canvas), size) {
                    this@prepare.drawContent()
                }
            }

            val inputBitmap = createBitmapFromPicture(picture)
            vlurState.prepare(inputBitmap, id)

            Timber.tag(TAG).d("BlurModifier: prepare called at $id")
        }
    }

    private fun isComposePreview() = currentValueOf(LocalInspectionMode)

    private fun ContentDrawScope.drawPreview() {
        drawContent()

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0C68B9), Color(0xFF3D8CD1), Color(0xFF0C68B9)),
                start = Offset(0.dp.toPx(), 0.dp.toPx()),
                end = Offset(30.dp.toPx(), 30.dp.toPx()),
                tileMode = TileMode.Repeated
            ),
            alpha = .65f,
        )

        val textMeasurer = TextMeasurer(
            defaultFontFamilyResolver = vlurState.previewData.fontFamilyResolver,
            defaultDensity = vlurState.previewData.density,
            defaultLayoutDirection = vlurState.previewData.layoutDirection
        )

        val result = textMeasurer.measure(
            text = buildString {
                appendLine()
                append(PREVIEW_TEXT)
                appendLine()
            },
            style = TextStyle(
                brush = Brush.horizontalGradient(gradientColors),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
        )

        drawText(
            result,
            topLeft = Offset(
                (size.width / 2) - (result.size.width / 2),
                (size.height / 2) - (result.size.height / 2)
            )
        )
    }

    override fun onReset() {}
    override fun onAttach() {}
    override fun onDetach() {}
}
