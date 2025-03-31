package com.example.btlkotlin.ui.image_recognize

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.btlkotlin.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.android.material.button.MaterialButton
import androidx.camera.core.ImageAnalysis



class ImageRecognitionActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var qrScanLine: View
    private lateinit var btnScanning: MaterialButton
    private lateinit var topMenuLayout: View
    private lateinit var bottomMenuLayout: View
    private var qrScanMode: Boolean = false
    private var scanLineAnimator: ObjectAnimator? = null

    private val CAMERA_PERMISSION_CODE = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_image_recognition)

        // Initialize views
        viewFinder = findViewById(R.id.viewFinder)
        qrScanLine = findViewById(R.id.qrScanLine)
        topMenuLayout = findViewById(R.id.topMenuLayout)
        bottomMenuLayout = findViewById(R.id.bottomMenuLayout)

        // Request camera permissions
        checkCameraPermissionAndOpenCamera()

        // Set up QR mode button
        val btnQR = findViewById<CardView>(R.id.btnQR)
        btnQR.setOnClickListener {
            toggleQRMode()
        }

        btnScanning = findViewById(R.id.btnScanning)

        btnScanning.setOnClickListener {
            toggleQRMode() // Switch back to camera mode
        }
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Show rationale if needed (if user has denied before)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                showPermissionRationaleDialog()
            } else {
                // Request permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
        } else {
            // Permission already granted, open camera
            startCamera()
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Needed")
            .setMessage("This app needs camera access to take pictures. Please grant this permission to continue.")
            .setPositiveButton("Grant") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showPermissionDeniedMessage()
            }
            .create()
            .show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind previous use cases
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun showPermissionDeniedMessage() {
        Toast.makeText(
            this,
            "Camera permission is required to use this feature",
            Toast.LENGTH_LONG
        ).show()

        // Ask if they want to go to settings to enable it
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("The camera permission is required for this feature. Would you like to enable it in settings?")
            .setPositiveButton("Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open camera
                startCamera()
            } else {
                // Permission denied
                showPermissionDeniedMessage()
            }
        }
    }

    private fun toggleQRMode() {
        qrScanMode = !qrScanMode

        if (qrScanMode) {
            // Hide UI elements
            topMenuLayout.visibility = View.GONE
            bottomMenuLayout.visibility = View.GONE

            // Show QR scan line and start animation
            btnScanning.visibility = View.VISIBLE
            qrScanLine.visibility = View.VISIBLE
            startScanLineAnimation()

            // Configure camera for QR scanning
            startQRScanner()

            Toast.makeText(this, "QR scanning mode enabled", Toast.LENGTH_SHORT).show()
        } else {
            // Show UI elements
            topMenuLayout.visibility = View.VISIBLE
            bottomMenuLayout.visibility = View.VISIBLE

            // Hide QR scan line and stop animation
            btnScanning.visibility = View.GONE
            qrScanLine.visibility = View.INVISIBLE
            stopScanLineAnimation()


            Toast.makeText(this, "Normal camera mode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startScanLineAnimation() {
        scanLineAnimator?.cancel()

        scanLineAnimator = ObjectAnimator.ofFloat(
            qrScanLine,
            "translationY",
            0f,
            viewFinder.height.toFloat()
        ).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopScanLineAnimation() {
        scanLineAnimator?.cancel()
        scanLineAnimator = null
    }


    private fun startQRScanner() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // QR code analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrCode ->
                        // Handle QR code result here
                        runOnUiThread {
                            Toast.makeText(this, "QR Code: $qrCode", Toast.LENGTH_LONG).show()
                            // Optionally return to normal mode after scan
                            // toggleQRMode()
                        }
                    })
                }

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind previous use cases
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "QR scanner initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
