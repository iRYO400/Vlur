package com.sadvakassov.vlur.internal

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import com.sadvakassov.vlur.exceptions.VlurRuntimeException

internal class VulkanImageProcessor(context: Context, isDebugEnabled: Boolean) : ImageProcessor {

    private var mVulkanProcessor = initVulkanProcessor(isDebugEnabled, context.assets)

    init {
        if (mVulkanProcessor == 0L) {
            throw VlurRuntimeException("Failed to initialize Vulkan processor")
        }
    }

    private val outputImages = hashMapOf<Int, Bitmap>()

    // Native methods
    // Initialize the image processor backed by Vulkan.
    // Return a non-zero handle on success, and 0L if failed.
    private external fun initVulkanProcessor(enableDebug: Boolean, assetManager: AssetManager): Long

    // Set the input image from bitmap and allocate output images backed by AHardwareBuffers.
    // Return true on success, and false if failed.
    private external fun configureInputAndOutput(processor: Long, inputBitmap: Bitmap, id: Int): Boolean

    // Get the HardwareBuffer of the target output. This method must be invoked after
    // configureInputAndOutput, and index must be within [0, numberOfOutputImages).
    // Return null if failed.
    private external fun getOutputHardwareBuffer(processor: Long, id: Int): HardwareBuffer?

    // Apply the blur filter in Vulkan and write the results to the indexed output image.
    private external fun blur(processor: Long, radius: Float, id: Int): Boolean

    // Frees up any underlying native resources. After calling this method, the Vulkan processor
    // must not be used in any way.
    private external fun destroyVulkanProcessor(processor: Long)

    override fun configureInputAndOutput(inputImage: Bitmap, id: Int) {
        val success = configureInputAndOutput(mVulkanProcessor, inputImage, id)
        if (!success) throw VlurRuntimeException("Failed to configureInputOutput")

        val buffer = getOutputHardwareBuffer(mVulkanProcessor, id)
            ?: throw VlurRuntimeException("Failed to getOutputHardwareBuffer at index $id")
        val outputImage = Bitmap.wrapHardwareBuffer(buffer, null)
            ?: throw VlurRuntimeException("Failed to wrapHardwareBuffer at index $id")
        buffer.close()

        outputImages[id] = outputImage
    }

    override fun blur(radius: Float, id: Int): Bitmap? {
        val success = blur(mVulkanProcessor, radius, id)
        if (!success) throw VlurRuntimeException("Failed to blur")
        return outputImages[id]
    }

    override fun cleanup() {
        if (mVulkanProcessor != 0L) {
            destroyVulkanProcessor(mVulkanProcessor)
            mVulkanProcessor = 0L
        }
    }
}
