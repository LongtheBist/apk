package com.example.worlddraw

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class MainActivity : Activity() {
    private lateinit var map: MapView
    private val drawing = DrawOverlay()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "WorldDrawApp/1.0"

        map = MapView(this).apply {
            setTileSource(XYTileSource(
                "CartoVoyager", 0, 19, 256, ".png",
                arrayOf(
                    "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                    "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                    "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
                ),
                "© OpenStreetMap contributors © CARTO"
            ))
            setMultiTouchControls(true)
            minZoomLevel = 2.0
            isVerticalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(85.0, -85.0, 0)
            controller.setZoom(3.0)
            controller.setCenter(GeoPoint(25.0, 10.0))
            overlays.add(drawing)
        }

        fun btn(label: String, onClick: (Button) -> Unit) = Button(this).apply {
            text = label
            minWidth = 0
            minimumWidth = 0
            setOnClickListener { onClick(this) }
        }

        val modeBtn = btn("Pan") { b ->
            drawing.drawMode = !drawing.drawMode
            b.text = if (drawing.drawMode) "Draw" else "Pan"
        }

        val colorRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            listOf(Color.RED, Color.BLUE, Color.rgb(0, 150, 60), Color.BLACK).forEach { col ->
                addView(View(context).apply {
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(col) }
                    setOnClickListener {
                        drawing.color = col
                        drawing.drawMode = true
                        modeBtn.text = "Draw"
                    }
                }, LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(dp(8), dp(4), dp(8), dp(4)) })
            }
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(modeBtn)
            addView(btn("Undo") { drawing.undo(); map.invalidate() })
            addView(btn("Clear") { drawing.clear(); map.invalidate() })
            addView(btn("Save") { saveImage() })
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.argb(230, 255, 255, 255))
                cornerRadius = dp(24).toFloat()
            }
            addView(colorRow)
            addView(buttonRow)
        }

        val root = FrameLayout(this)
        root.addView(map, FrameLayout.LayoutParams(-1, -1))
        root.addView(bar, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(32)
        })
        setContentView(root)
    }

    /** Saves the current map view plus drawings as a PNG in Pictures/WorldDraw. */
    private fun saveImage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            return
        }
        try {
            val bmp = Bitmap.createBitmap(map.width, map.height, Bitmap.Config.ARGB_8888)
            map.draw(Canvas(bmp))

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "worlddraw_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WorldDraw")
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("could not create file")
            contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(this, "Saved to Pictures/WorldDraw", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not save: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) saveImage()
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
}
