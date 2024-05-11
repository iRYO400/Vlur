package com.sadvakassov.vlur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import androidx.compose.ui.geometry.Size
import com.sadvakassov.vlur.internal.TAG
import com.sadvakassov.vlur.internal.ImageProcessor
import com.sadvakassov.vlur.internal.PreviewData
import com.sadvakassov.vlur.internal.StubImageProcessor
import com.sadvakassov.vlur.internal.VulkanImageProcessor
import com.sadvakassov.vlur.internal.record
import timber.log.Timber

class VlurState(
    context: Context,
    isDebugEnabled: Boolean,
    val previewData: PreviewData
) {

    private val vulkanImageProcessor: ImageProcessor =
        if (previewData.isInspectionMode) StubImageProcessor()
        else VulkanImageProcessor(context, isDebugEnabled)

    private val picture = Picture()

    fun prepare(bitmap: Bitmap, id: Int) {
        Timber.tag(TAG).d("VlurState: prepare called at $id")
        vulkanImageProcessor.configureInputAndOutput(inputImage = bitmap, id)
    }

    fun blur(radius: Float, id: Int): Bitmap? {
        return vulkanImageProcessor.blur(radius, id)
    }

    fun clear() {
        Timber.tag(TAG).d("VlurState: clear called")
        vulkanImageProcessor.cleanup()
    }

    fun takePicture(size: Size, block: (Canvas) -> Unit): Picture {
        picture.record(size) { canvas ->
            block(canvas)
        }
        return picture
    }
}
