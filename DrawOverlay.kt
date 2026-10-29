package com.example.worlddraw

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class Stroke(val color: Int, val width: Float) {
    val points = mutableListOf<GeoPoint>()
}

/** Strokes are stored as lat/lon, so they stay glued to the map when you pan or zoom. */
class DrawOverlay : Overlay() {
    val strokes = mutableListOf<Stroke>()
    var drawMode = false
    var color = Color.RED
    var strokeWidth = 8f

    private var current: Stroke? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val tmp = Point()

    override fun draw(c: Canvas, map: MapView, shadow: Boolean) {
        if (shadow) return
        val proj = map.projection
        for (s in strokes) {
            if (s.points.isEmpty()) continue
            paint.color = s.color
            paint.strokeWidth = s.width
            if (s.points.size == 1) {
                proj.toPixels(s.points[0], tmp)
                paint.style = Paint.Style.FILL
                c.drawCircle(tmp.x.toFloat(), tmp.y.toFloat(), s.width / 2, paint)
                paint.style = Paint.Style.STROKE
                continue
            }
            val path = Path()
            s.points.forEachIndexed { i, gp ->
                proj.toPixels(gp, tmp)
                if (i == 0) path.moveTo(tmp.x.toFloat(), tmp.y.toFloat())
                else path.lineTo(tmp.x.toFloat(), tmp.y.toFloat())
            }
            c.drawPath(path, paint)
        }
    }

    override fun onTouchEvent(e: MotionEvent, map: MapView): Boolean {
        if (!drawMode || e.pointerCount > 1) { current = null; return false } // two fingers = pinch zoom
        fun geo() = map.projection.fromPixels(e.x.toInt(), e.y.toInt()).let { GeoPoint(it.latitude, it.longitude) }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                current = Stroke(color, strokeWidth).also { it.points.add(geo()); strokes.add(it) }
            }
            MotionEvent.ACTION_MOVE -> current?.points?.add(geo())
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> current = null
        }
        map.invalidate()
        return true
    }

    fun undo() { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }
    fun clear() = strokes.clear()
}
