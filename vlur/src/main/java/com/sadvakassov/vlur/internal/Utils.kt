package com.sadvakassov.vlur.internal

import android.graphics.Bitmap
import android.graphics.Picture
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

internal fun createBitmapFromPicture(picture: Picture): Bitmap {
    val bitmap = Bitmap.createBitmap(
        picture.width,
        picture.height,
        Bitmap.Config.ARGB_8888,
        false
    )

    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawPicture(picture)
    return bitmap
}

internal inline fun Picture.record(size: Size, block: (android.graphics.Canvas) -> Unit) {
    try {
        beginRecording(size.width.toInt(), size.height.toInt()).apply(block)
    } finally {
        endRecording()
    }
}

internal val gradientColors = listOf(
    Color(0xFFFF685D),
    Color(0xFFFF64F0),
//    Color(0xFF5155FF),
    Color(0xFF54EDFF),
    Color(0xFF5BFF7B),
    Color(0xFFFDFF59),
    Color(0xFFFFCA55),
)
