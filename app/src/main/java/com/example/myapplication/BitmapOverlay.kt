package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object BitmapOverlay {

    fun drawElements(original: Bitmap, nodes: Map<Int, AccessibilityNodeInfo>): Bitmap {
        // Create a mutable copy to draw on
        val mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // Paint for the Box (Red Outline)
        val boxPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        // Paint for the ID Tag (Green Background)
        val bgPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
        }

        // Paint for the Text (Black ID)
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            style = Paint.Style.FILL
            isFakeBoldText = true
        }

        // Iterate and Draw
        for ((id, node) in nodes) {
            val rect = Rect()
            node.getBoundsInScreen(rect)

            // 1. Draw Box
            canvas.drawRect(rect, boxPaint)

            // 2. Draw ID Tag
            val label = id.toString()
            // Calculate label background size
            val bounds = Rect()
            textPaint.getTextBounds(label, 0, label.length, bounds)
            val padding = 10

            // Draw green background for text at top-left of the element
            canvas.drawRect(
                rect.left.toFloat(),
                rect.top.toFloat(),
                rect.left.toFloat() + bounds.width() + padding * 2,
                rect.top.toFloat() + bounds.height() + padding * 2,
                bgPaint
            )

            // 3. Draw ID Text
            canvas.drawText(
                label,
                rect.left.toFloat() + padding,
                rect.top.toFloat() + bounds.height() + padding,
                textPaint
            )
        }

        return mutableBitmap
    }
}