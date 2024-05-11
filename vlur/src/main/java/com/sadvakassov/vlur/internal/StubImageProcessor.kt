package com.sadvakassov.vlur.internal

import android.graphics.Bitmap

class StubImageProcessor : ImageProcessor {

    override fun configureInputAndOutput(inputImage: Bitmap, id: Int) {
        /* no-op */
    }

    override fun blur(radius: Float, id: Int): Bitmap? {
        return null
    }

    override fun cleanup() {
        /* no-op */
    }
}
