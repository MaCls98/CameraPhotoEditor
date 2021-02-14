package com.mcelis.cameraphotoeditor.ui.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.ImageReader.newInstance
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mcelis.cameraphotoeditor.R
import com.mcelis.cameraphotoeditor.ui.dialogs.DialogEditPhoto
import com.mcelis.cameraphotoeditor.ui.views.AutoFitTextureView
import com.mcelis.cameraphotoeditor.utils.CompareSizesByArea
import kotlinx.android.synthetic.main.activity_photo.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.max


class PhotoActivity : AppCompatActivity() {

    private var mAutoFocusSupported: Boolean = true
    private var textureView: AutoFitTextureView? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var imageReader: ImageReader? = null
    private lateinit var file: File
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest
    private var state = STATE_PREVIEW
    private val cameraOpenCloseLock = Semaphore(1)
    private var isFlashSupported = false
    private var isTorchOn: Boolean = false
    private var sensorOrientation = 0
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private val CAMERA_FRONT = "1"
    private val CAMERA_BACK = "0"
    private var cameraId: String = CAMERA_BACK
    private lateinit var previewSize: Size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)
        file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "/test.jpeg")
        textureView = texture_view_camera
        textureView!!.surfaceTextureListener = surfaceTextureListener
        btn_take_photo.setOnClickListener {
            if (mAutoFocusSupported){
                lockFocus()
            }else{
                captureStillPicture()
            }
        }
        btn_rotate_camera.setOnClickListener {
            switchCamera()
        }
        btn_switch_flash.setOnClickListener {
            switchFlash()
        }
        fixImageStretching()
    }

    private fun switchFlash() {
        if (cameraId == CAMERA_BACK){
            if (isFlashSupported){
                isTorchOn = if (isTorchOn){
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                    captureSession?.setRepeatingRequest(previewRequestBuilder.build(), captureCallback, backgroundHandler)
                    false
                }else{
                    previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                    captureSession?.setRepeatingRequest(previewRequestBuilder.build(), captureCallback, backgroundHandler)
                    false
                }
            }
        }
    }

    private fun switchCamera(){
        if (cameraId == CAMERA_FRONT){
            cameraId = CAMERA_BACK
            closeCamera()
            reopenCamera()
        }else if(cameraId == CAMERA_BACK){
            cameraId = CAMERA_FRONT
            closeCamera()
            reopenCamera()
        }
    }

    private fun reopenCamera(){
        if(textureView!!.isAvailable){
            openCamera(textureView!!.width, textureView!!.height)
        }else{
            textureView!!.surfaceTextureListener = surfaceTextureListener
        }
    }

    @Suppress("DEPRECATION")
    private fun fixImageStretching() {
        val cameraAspectRatio = 0.75.toFloat()
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        var finalWidth = screenWidth
        var finalHeight = screenHeight
        var widthDifference = 0
        var heightDifference = 0
        val screenAspectRatio = screenWidth.toFloat() / screenHeight
        if (screenAspectRatio > cameraAspectRatio) {
            finalHeight = (screenWidth / cameraAspectRatio).toInt()
            heightDifference = finalHeight - screenHeight
        } else {
            finalWidth = (screenHeight * cameraAspectRatio).toInt()
            widthDifference = finalWidth - screenWidth
        }
        val lp = texture_view_camera.layoutParams as RelativeLayout.LayoutParams
        lp.width = finalWidth
        lp.height = finalHeight
        lp.leftMargin = -(widthDifference / 2)
        lp.topMargin = -(heightDifference / 2)
        texture_view_camera.layoutParams = lp
    }

    private val onImageAvailableListener = OnImageAvailableListener {
        Handler(Looper.getMainLooper()).postDelayed({
            saveImage(imageReader!!.acquireNextImage(), file)
        }, 500)
    }

    private fun saveImage(
            image: Image,
            file: File,
    ){
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(file).apply {
                write(bytes)
            }
        } catch (e: IOException) {
            Log.e("PHOTO", e.toString())
        } finally {
            image.close()
            output?.let {
                try {
                    it.close()
                    launchEditPhotoDialog(file.absolutePath)
                } catch (e: IOException) {
                    Log.e("PHOTO", e.toString())
                }
            }
        }
    }

    private fun launchEditPhotoDialog(absolutePath: String) {
        val dialogEditPhoto = DialogEditPhoto()
        val bundle = Bundle()
        bundle.putString("photo", absolutePath)
        dialogEditPhoto.arguments = bundle
        dialogEditPhoto.show(supportFragmentManager, dialogEditPhoto.tag)
    }

    private val surfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cam: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = cam
            createCameraPreviewSession()
        }

        override fun onDisconnected(cam: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(cam: CameraDevice, error: Int) {
            onDisconnected(cam)
            finish()
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        private fun process(result: CaptureResult) {
            when (state) {
                STATE_PREVIEW -> Unit // Do nothing when the camera preview is working normally.
                STATE_WAITING_LOCK -> capturePicture(result)
                STATE_WAITING_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        state = STATE_WAITING_NON_PRECAPTURE
                    }
                }
                STATE_WAITING_NON_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
            }
        }

        private fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureStillPicture()
            } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                // CONTROL_AE_STATE can be null on some devices
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    state = STATE_PICTURE_TAKEN
                    captureStillPicture()
                } else {
                    runPreCaptureSequence()
                }
            }
        }

        override fun onCaptureProgressed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                partialResult: CaptureResult,
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult,
        ) {
            process(result)
        }

    }

    @Suppress("DEPRECATION")
    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraId)

            val afAvailableModes = characteristics[CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES]
            mAutoFocusSupported = !(afAvailableModes!!.isEmpty() || (afAvailableModes.size == 1
                    && afAvailableModes[0] == CameraMetadata.CONTROL_AF_MODE_OFF))

            val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val largest = Collections.max(
                    listOf(*map!!.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea())
            imageReader = newInstance(width, height,
                    ImageFormat.JPEG, /*maxImages*/ 2).apply {
                setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }

            val displayRotation = windowManager.defaultDisplay.rotation

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            val swappedDimensions = areDimensionsSwapped(displayRotation)

            val displaySize = Point()
            windowManager.defaultDisplay.getSize(displaySize)
            val rotatedPreviewWidth = if (swappedDimensions) height else width
            val rotatedPreviewHeight = if (swappedDimensions) width else height
            var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
            var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewWidth, rotatedPreviewHeight,
                    maxPreviewWidth, maxPreviewHeight,
                    largest)

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView!!.setAspectRatio(previewSize.width, previewSize.height, maxPreviewWidth, maxPreviewHeight, previewSize)
            } else {
                textureView!!.setAspectRatio(previewSize.height, previewSize.width, maxPreviewWidth, maxPreviewHeight, previewSize)
            }

            isFlashSupported =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: NullPointerException) {

        }

    }

    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    private fun openCamera(width: Int, height: Int) {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            return
        }
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

    }

    @Suppress("DEPRECATION")
    private fun createCameraPreviewSession() {
        try {
            val texture = textureView!!.surfaceTexture

            texture?.setDefaultBufferSize(previewSize.width, previewSize.height)

            val surface = Surface(texture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(listOf(surface, imageReader?.surface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            if (cameraDevice == null) return

                            captureSession = cameraCaptureSession
                            try {
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                //setAutoFlash(previewRequestBuilder)

                                previewRequest = previewRequestBuilder.build()
                                captureSession?.setRepeatingRequest(previewRequest,
                                        captureCallback, backgroundHandler)
                            } catch (e: CameraAccessException) {
                                Log.e(TAG, e.toString())
                            }

                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {

                        }
                    }, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    @Suppress("DEPRECATION")
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = max(
                    viewHeight.toFloat() / previewSize.height,
                    viewWidth.toFloat() / previewSize.width)
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView!!.setTransform(matrix)
    }

    private fun lockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START)
            val rect = Rect(0, 0, textureView!!.width, textureView!!.height)
            previewRequestBuilder.set(
                    CaptureRequest.SCALER_CROP_REGION,
                    rect
            )
            state = STATE_WAITING_LOCK
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun runPreCaptureSequence() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            state = STATE_WAITING_PRECAPTURE
            val rect = Rect(0, 0, textureView!!.width, textureView!!.height)
            previewRequestBuilder.set(
                    CaptureRequest.SCALER_CROP_REGION,
                    rect
            )
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    @Suppress("DEPRECATION")
    private fun captureStillPicture() {
        try {
            val rotation = windowManager.defaultDisplay.rotation
            val captureBuilder = cameraDevice?.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                addTarget(imageReader?.surface!!)

                set(CaptureRequest.JPEG_ORIENTATION,
                        (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360)

                set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }?.also {
            //setAutoFlash(it)
            }

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {

                override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult,
                ) {
                    Log.d(TAG, file.toString())
                    unlockFocus()
                }
            }

            captureSession?.apply {
                stopRepeating()
                abortCaptures()
                capture(captureBuilder?.build()!!, captureCallback, null)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun unlockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            //setAutoFlash(previewRequestBuilder)
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler)
            state = STATE_PREVIEW
            captureSession?.setRepeatingRequest(previewRequest, captureCallback,
                    backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        if (isFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (textureView!!.isAvailable) {
            openCamera(textureView!!.width, textureView!!.height)
        } else {
            textureView!!.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper!!)
    }

    companion object {
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        private const val TAG = "Camera2BasicFragment"
        private const val STATE_PREVIEW = 0
        private const val STATE_WAITING_LOCK = 1
        private const val STATE_WAITING_PRECAPTURE = 2
        private const val STATE_WAITING_NON_PRECAPTURE = 3
        private const val STATE_PICTURE_TAKEN = 4
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080

        private fun chooseOptimalSize(
                choices: Array<Size>,
                textureViewWidth: Int,
                textureViewHeight: Int,
                maxWidth: Int,
                maxHeight: Int,
                aspectRatio: Size,
        ): Size {
            val bigEnough = ArrayList<Size>()
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                        option.height == option.width * h / w) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            return when {
                bigEnough.size > 0 -> {
                    Collections.min(bigEnough, CompareSizesByArea())
                }
                notBigEnough.size > 0 -> {
                    Collections.max(notBigEnough, CompareSizesByArea())
                }
                else -> {
                    Log.e(TAG, "Couldn't find any suitable preview size")
                    choices[0]
                }
            }
        }
    }
}