package com.example.btlkotlin.ui.object_recognize

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.view.*
import android.view.MotionEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.btlkotlin.MainActivity
import com.example.btlkotlin.R
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2
import com.google.android.filament.Fence
import com.google.android.filament.utils.*
import android.widget.Toast
import java.nio.ByteBuffer
import java.io.*
import kotlinx.coroutines.*
import java.net.URI
import java.util.zip.ZipInputStream
import java.nio.charset.StandardCharsets
import java.nio.Buffer


class ObjectRecognitionActivity : AppCompatActivity() {
    companion object {
        // Load the library for the utility layer, which in turn loads gltfio and the Filament core.
        init { Utils.init() }
        private const val TAG = "gltf-viewer"
    }

    private lateinit var joystickInner: ImageView
    private lateinit var joystickLayout: ConstraintLayout
    private var centerX = 0f
    private var centerY = 0f

    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private val frameScheduler = FrameCallback()
    private lateinit var modelViewer: ModelViewer
    private lateinit var titlebarHint: TextView
    private var remoteServer: RemoteServer? = null
    private var statusToast: Toast? = null
    private var statusText: String? = null
    private var latestDownload: String? = null
    private val automation = AutomationEngine()
    private var loadStartTime = 0L
    private var loadStartFence: Fence? = null
    private val viewerContent = AutomationEngine.ViewerContent()

    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button

    //@SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_object_recognition)

        joystickInner = findViewById(R.id.joystick_inner)
        joystickLayout = findViewById(R.id.joystick_layout)
        val zoomSlider = findViewById<SeekBar>(R.id.zoom_slider)

        val tvMainScreen = findViewById<TextView>(R.id.tvMainScreen)
        tvMainScreen.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btn1 = findViewById<Button>(R.id.btn1)
        btn2 = findViewById<Button>(R.id.btn2)
        btn3 = findViewById<Button>(R.id.btn3)

        /* Add button to load model here */

        btn1.setOnClickListener {
            modelViewer.destroyModel()
            createRenderables("Iphone seceond version finished.glb")
        }

        btn2.setOnClickListener {
            modelViewer.destroyModel()
            createRenderables("Iphone_8.glb")
        }

        btn3.setOnClickListener {
            modelViewer.destroyModel()
            createRenderables("train.glb")
        }


        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        titlebarHint = findViewById(R.id.user_hint)
        surfaceView = findViewById(R.id.main_sv)
        choreographer = Choreographer.getInstance()
        modelViewer = ModelViewer(surfaceView)
        viewerContent.view = modelViewer.view
        viewerContent.sunlight = modelViewer.light
        viewerContent.lightManager = modelViewer.engine.lightManager
        viewerContent.scene = modelViewer.scene
        viewerContent.renderer = modelViewer.renderer

        // Create a default object here
        createRenderables("Iphone seceond version finished.glb")


        createIndirectLight()
        setStatusText("Please wait for the system to load a new model.")
        val view = modelViewer.view


        remoteServer = RemoteServer(8082)

        joystickLayout.post {
            centerX = joystickInner.x
            centerY = joystickInner.y
        }

        // Override performClick in the activity
        joystickInner.setOnClickListener {
            // Add any click logic if necessary
        }

        joystickInner.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - centerX
                    val dy = event.y - centerY
                    val distance = sqrt(dx * dx + dy * dy)
                    val angle = atan2(dy, dx)

                    if (distance < 50) {
                        joystickInner.x = centerX + dx
                        joystickInner.y = centerY + dy

                        // Move the camera (translate along X and Z axes based on joystick input)
                        val camera = modelViewer.camera
                        val movementSpeed = 0.1  // Speed of camera movement

                        // Calculate movement for the camera along the X and Z axes
                        val cameraMoveX = cos(angle) * movementSpeed
                        val cameraMoveZ = sin(angle) * movementSpeed

                        // Apply the translation to the camera's position
                        val cameraPosition = camera.setShift(cameraMoveX, cameraMoveZ)

                        //modelViewer. .updateCamera() // Ensure the camera's new position is applied
                    }
                }
            }
            true
        }

        surfaceView.setOnTouchListener { _, event ->
            modelViewer.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                surfaceView.performClick()
            }
            true
        }

        // Override performClick
        surfaceView.setOnClickListener {
            // Add any click logic if needed
        }

        /*      Code for zoom slider    */

        zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val zoomFactor = 1 + ((100 - progress.toDouble()) / 100)

                // Calculate the aspect ratio based on the surface view size
                val surfaceViewWidth = surfaceView.width
                val surfaceViewHeight = surfaceView.height
                val aspectRatio:Double = surfaceViewWidth.toDouble() / surfaceViewHeight.toDouble()

                // Adjust the camera's field of view (FOV) for zoom effect
                modelViewer.camera?.let { camera ->
                    val fov = 45 / zoomFactor  // Adjust the FOV based on the zoom factor (less FOV means zoomed in)
                    camera.setLensProjection(fov, aspectRatio, 0.1, 100.toDouble())  // Near and far plane distances can be adjusted
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


    }

    private fun createRenderables(file: String) {        // Only accept GLB file
        val buffer = assets.open("models/" + file).use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        }
        modelViewer.loadModelGltfAsync (buffer) { uri -> readCompressedAsset ("models/$uri") }
        updateRootTransform()
    }

    private fun createIndirectLight() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene
        val ibl = "venetian_crossroads_2k"
        readCompressedAsset("envs/$ibl/${ibl}_ibl.ktx").let {
            scene.indirectLight = KTX1Loader.createIndirectLight (engine, it)
            scene.indirectLight!!.intensity = 30_000.0f
            viewerContent.indirectLight = modelViewer.scene.indirectLight
        }

        readCompressedAsset("envs/$ibl/${ibl}_skybox.ktx").let {
            scene.skybox = KTX1Loader.createSkybox (engine, it)
        }
    }

    private fun readCompressedAsset (assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray (input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun clearStatusText() {
        statusToast?.let {
            it.cancel()
            statusText = null
        }
    }
    private fun setStatusText(text: String) {
        runOnUiThread {
            if (statusToast == null || statusText != text) {
                statusText = text
                statusToast = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
                statusToast!!.show()
            }
        }
    }

    private suspend fun loadGlb(message: RemoteServer. ReceivedMessage) {
        withContext(Dispatchers.Main) {
            modelViewer.destroyModel()
            modelViewer.loadModelGlb(message.buffer)
            updateRootTransform()
            loadStartTime = System.nanoTime()
            loadStartFence = modelViewer.engine.createFence()
        }
    }

    private suspend fun loadZip (message: RemoteServer. ReceivedMessage) {
        // To alleviate memory pressure, remove the old model before deflating the zip.
        withContext(Dispatchers.Main) {
            modelViewer.destroyModel()
        }

        // Large zip files should first be written to a file to prevent OOM.
        // It is also crucial that we null out the message "buffer" field. Y
        val (zipStream, zipFile) = withContext(Dispatchers.IO) {
            val file = File.createTempFile("incoming", "zip", cacheDir)
            val raf = RandomAccessFile(file, "rw")
            raf.channel.write(message.buffer)
            message.buffer = null
            raf.seek(0)
            Pair(FileInputStream(file), file)
        }

        // Deflate each resource using the 10 dispatcher, one by one.
        var gltfPath: String? = null
        var outOfMemory: String? = null
        val pathToBufferMapping = withContext(Dispatchers.IO) {
                val deflater = ZipInputStream(zipStream)
                val mapping = HashMap<String, Buffer>()
                while (true) {
                    val entry = deflater.nextEntry ?: break
                    if (entry.isDirectory) continue

                    // This isn't strictly required, but as an optimization
                    // we ignore common junk that often pollutes ZIP files.
                    if (entry.name.startsWith("__MACOSX")) continue
                    if (entry.name.startsWith(".DS_Store")) continue

                    val uri = entry.name
                    val byteArray: ByteArray? = try {
                        deflater.readBytes()
                    }
                    catch (e: OutOfMemoryError) {
                        outOfMemory = uri
                        break
                    }
                    //Log.i("Deflated ${byteArray!!.size) bytes from $uri")
                    val buffer = ByteBuffer.wrap (byteArray)
                    mapping[uri] = buffer
                    if (uri.endsWith(".gltf") || uri.endsWith(".glb")) {
                        gltfPath = uri
                    }
                }
                mapping
            }

        zipFile.delete()
        if (gltfPath == null) {
            setStatusText("Could not find .gltf or glb in the zip.")
            return
        }
        if (outOfMemory != null) {
            setStatusText("Out of memory while deflating $outOfMemory")
            return
        }

        val gltfBuffer = pathToBufferMapping [gltfPath]!!

// In a zip file, the gltf file might be in the same folder as resources, or in a different
// folder. It is crucial to test against both of these cases. In any case, the resource
// paths are all specified relative to the location of the gltf file.
        var prefix = URI(gltfPath!!).resolve(".")
        withContext(Dispatchers.Main) {
            if (gltfPath!!.endsWith(".glb")) {
                modelViewer.loadModelGlb(gltfBuffer)
            }
            else {
                modelViewer.loadModelGltf(gltfBuffer) { uri->
                    val path = prefix.resolve(uri).toString()
                    if (!pathToBufferMapping.contains(path)) {
                        //Log.e("Could not find '$uri' in zip using prefix '$prefix' and base path '${gltfPath!!}'")
                        setStatusText("Zip is missing $path")
                    }
                    pathToBufferMapping[path]
                }
            }
            updateRootTransform()
            loadStartTime = System.nanoTime()
            loadStartFence = modelViewer.engine.createFence()
        }
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameScheduler)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameScheduler)
        remoteServer?.close()
    }

    fun LoadModelData (message: RemoteServer. ReceivedMessage) {
        //Log.i(TAG, msg: "Downloaded model ${message.label) (${message.buffer.capacity()) bytes)")
        clearStatusText()
        titlebarHint.text = message.label
        CoroutineScope(Dispatchers.IO).launch {
            when {
                message.label.endsWith( ".zip") -> loadZip(message)
                else -> loadGlb (message)
            }
        }
    }
    fun loadSettings (message: RemoteServer.ReceivedMessage) {
        val json = StandardCharsets.UTF_8.decode(message.buffer).toString()
        viewerContent.assetLights = modelViewer.asset?.lightEntities
        automation.applySettings(modelViewer.engine, json, viewerContent)
        modelViewer.view.colorGrading = automation.getColorGrading (modelViewer.engine)
        modelViewer.cameraFocalLength = automation.viewerOptions.cameraFocalLength
        modelViewer.cameraNear = automation.viewerOptions.cameraNear
        modelViewer.cameraFar = automation.viewerOptions.cameraFar
        updateRootTransform()
    }
    private fun updateRootTransform() {
        if (automation.viewerOptions.autoScaleEnabled) {
            modelViewer.transformToUnitCube()
        } else {
            modelViewer.clearRootTransform()
        }

    }
    inner class FrameCallback: Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame (frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)
            loadStartFence?.let {
                if (it.wait(Fence.Mode.FLUSH, 0) == Fence.FenceStatus.CONDITION_SATISFIED) {
                    val end = System.nanoTime()
                    val total = (end - loadStartTime) / 1_000_000
                    //Log.i("The Filament backend took $total ms to load the model geometry.")
                    modelViewer.engine.destroyFence (it)
                    loadStartFence = null
                }
            }

            modelViewer.animator?.apply {
                if (animationCount > 0) {
                    val elapsedTimeSeconds = (frameTimeNanos - startTime).toDouble() / 1_000_000_000
                    applyAnimation(0, elapsedTimeSeconds.toFloat())
                }
                updateBoneMatrices()
            }

            modelViewer.render(frameTimeNanos)

            // Check if a new download is in progress. If so, let the user know with toast.
            val currentDownload = remoteServer?.peekIncomingLabel()
            if (RemoteServer.isBinary(currentDownload) && currentDownload != latestDownload) {
                latestDownload = currentDownload
                //Log.i("Downloading $currentDownload")
                setStatusText("Downloading $currentDownload")
            }

            // Check if a new message has been fully received from the client.
            val message = remoteServer?.acquireReceivedMessage()
            if (message != null) {
                if (message.label == latestDownload) {
                    latestDownload = null
                }
                if (RemoteServer.isJson(message.label)) {
                    loadSettings(message)
                }
            }
        }
    }
}