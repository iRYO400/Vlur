package com.sadvakassov.vlur.internal

import android.graphics.Bitmap

internal interface ImageProcessor {

    // Set the input image from bitmap and allocate the memories for output images. The user may
    // invoke this method multiple times to switch between different input images.
    fun configureInputAndOutput(inputImage: Bitmap, id: Int)

    // Apply gaussian blur to the input image. The radius must be within the range of [1.0, 25.0].
    fun blur(radius: Float, id: Int): Bitmap?

    // Frees up any underlying native resources. After calling this method, this image processor
    // can not be used in any way.
    fun cleanup()
}
