package com.example.btlkotlin.ui.object_recognize

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.example.btlkotlin.MainActivity
import com.example.btlkotlin.R
import java.io.IOException
import java.io.InputStream
import com.google.android.filament.utils.ModelViewer
import java.nio.ByteBuffer
import java.nio.channels.Channels

class ObjectRecognitionActivity : AppCompatActivity() {
    private lateinit var joystickInner: ImageView
    private lateinit var joystickLayout: RelativeLayout
    private var centerX = 0f
    private var centerY = 0f

    private lateinit var surfaceView: SurfaceView
    private lateinit var modelViewer: ModelViewer

    override fun onCreate(savedInstanceState: Bundle?) {            // Action to perform when loading screen
        super.onCreate(savedInstanceState)

        val surfaceView = SurfaceView(this)

        supportActionBar?.hide()
        setContentView(R.layout.activity_object_recognition)

        joystickInner = findViewById(R.id.joystick_inner)
        //joystickLayout = findViewById(R.id.joystick_layout)
        val zoomSlider = findViewById<SeekBar>(R.id.zoom_slider)

        val tvMainScreen = findViewById<TextView>(R.id.tvMainScreen)
        tvMainScreen.setOnClickListener {                   // Go to home screen
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        /* 3D objects code below*/


        modelViewer = ModelViewer(surfaceView)
        loadGlbModel("train.glb") // Your .glb file in assets
    }

    private fun loadGlbModel(filename: String) {
        try {
            val inputStream: InputStream = assets.open(filename)
            val byteBuffer = inputStreamToByteBuffer(inputStream)

            modelViewer.loadModelGlb(byteBuffer) // Now passing ByteBuffer
            modelViewer.transformToUnitCube()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun inputStreamToByteBuffer(inputStream: InputStream): ByteBuffer {
        val output = ByteBuffer.allocateDirect(inputStream.available())
        val channel = Channels.newChannel(inputStream)
        channel.read(output)
        output.flip() // Important: Reset buffer position to 0
        return output
    }
}