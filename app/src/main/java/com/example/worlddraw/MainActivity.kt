package com.example.worlddraw

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class MainActivity : Activity() {
    private lateinit var map: MapView
    private val drawing = DrawOverlay()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "WorldDrawApp/1.0 (138uncia@gmail.com)"

        map = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 2.0
            isVerticalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(85.0, -85.0, 0)
            controller.setZoom(3.0)
            controller.setCenter(GeoPoint(25.0, 10.0))
            overlays.add(drawing)
        }

        val modeBtn = Button(this).apply {
            text = "Pan"
            setOnClickListener {
                drawing.drawMode = !drawing.drawMode
                text = if (drawing.drawMode) "Draw" else "Pan"
            }
        }
        val undoBtn = Button(this).apply { text = "Undo"; setOnClickListener { drawing.undo(); map.invalidate() } }
        val clearBtn = Button(this).apply { text = "Clear"; setOnClickListener { drawing.clear(); map.invalidate() } }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply { setColor(Color.argb(230, 255, 255, 255)); cornerRadius = dp(28).toFloat() }
            addView(modeBtn)
            listOf(Color.RED, Color.BLUE, Color.rgb(0, 150, 60), Color.BLACK).forEach { col ->
                addView(View(context).apply {
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(col) }
                    setOnClickListener {
                        drawing.color = col
                        drawing.drawMode = true
                        modeBtn.text = "Draw"
                    }
                }, LinearLayout.LayoutParams(dp(32), dp(32)).apply { setMargins(dp(6), 0, dp(6), 0) })
            }
            addView(undoBtn)
            addView(clearBtn)
        }

        val root = FrameLayout(this)
        root.addView(map, FrameLayout.LayoutParams(-1, -1))
        root.addView(bar, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(32)
        })
        setContentView(root)
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
}
