package com.smarttranslator.lectures.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SplitterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onDrag: ((dyPx: Float) -> Unit)? = null
    private var lastY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastY = event.rawY; return true }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.rawY - lastY
                lastY = event.rawY
                onDrag?.invoke(dy)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}