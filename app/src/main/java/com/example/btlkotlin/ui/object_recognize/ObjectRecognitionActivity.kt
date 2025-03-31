package com.example.btlkotlin.ui.object_recognize

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.btlkotlin.MainActivity
import com.example.btlkotlin.R
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.core.HitResult

class ObjectRecognitionActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private var modelNode: TransformableNode? = null
    private var modelRenderable: ModelRenderable? = null
    private lateinit var joystickInner: ImageView
    private lateinit var joystickLayout: RelativeLayout
    private var centerX = 0f
    private var centerY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_object_recognition)

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        joystickInner = findViewById(R.id.joystick_inner)
        //joystickLayout = findViewById(R.id.joystick_layout)
        val zoomSlider = findViewById<SeekBar>(R.id.zoom_slider)

        val tvMainScreen = findViewById<TextView>(R.id.tvMainScreen)
        tvMainScreen.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        load3DModel()

        // Set up a tap listener for placing the 3D model
        arFragment?.setOnTapArPlaneListener { hitResult, _, _ ->
            place3DModel(hitResult) // Call place3DModel to place the object on the AR plane
        }

        /*joystickLayout.post {
            centerX = joystickInner.x
            centerY = joystickInner.y
        }

        // Joystick to move the object in the AR world
        joystickInner.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - centerX
                    val dy = event.y - centerY
                    val distance = sqrt(dx * dx + dy * dy)
                    val angle = atan2(dy, dx)

                    if (distance < 50) { // Limit movement radius
                        joystickInner.x = centerX + dx
                        joystickInner.y = centerY + dy
                        moveObject(cos(angle) * 0.1f, sin(angle) * 0.1f)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    joystickInner.x = centerX
                    joystickInner.y = centerY
                }
            }
            true
        }*/

        // Zoom Object with SeekBar
        zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Adjust the model scale based on SeekBar progress
                val scaleFactor = 1 + (progress / 100f)  // Scale range from 1.0 to 2.0 (or more)
                modelNode?.let {
                    it.localScale = it.localScale.scaled(scaleFactor)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }



    private fun load3DModel() {
        ModelRenderable.builder()
            .setSource(this, Uri.parse("file:///android_asset/train.glb"))  // MUST GLB
            .build()
            .thenAccept { renderable -> modelRenderable = renderable }
            .exceptionally {
                it.printStackTrace()
                null
            }
    }

    // This function is responsible for placing the 3D model on the AR plane
    private fun place3DModel(hitResult: HitResult) {
        if (modelRenderable == null) return

        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment?.arSceneView?.scene)

        modelNode = TransformableNode(arFragment?.transformationSystem)
        modelNode?.renderable = modelRenderable
        modelNode?.setParent(anchorNode)
        modelNode?.select()
    }
    /*
    // This function moves the object based on joystick input
    private fun moveObject(dx: Float, dz: Float) {
        modelNode?.let {
            val position = it.localPosition
            position.x += dx
            position.z += dz
            it.localPosition = position
        }
    }*/

    }

