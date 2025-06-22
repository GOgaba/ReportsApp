package android.bignerdranch.reportsapp.map.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Bitmap.Config
import androidx.core.graphics.createBitmap

fun createMarkerBitmap(
    innerSize: Int = 128,  // Видимая часть
    padding: Int = 32     // Невидимая область для тапов
): Bitmap {
    val totalSize = innerSize + padding * 2
    val bitmap = createBitmap(totalSize, totalSize)
    val canvas = Canvas(bitmap)

    // Рисуем основную иконку в центре
    val paint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
    }
    canvas.drawCircle(
        totalSize / 2f,
        totalSize / 2f,
        innerSize / 2f,
        paint
    )

    return bitmap
}