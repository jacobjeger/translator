package com.megalife.translator.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.megalife.translator.R
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : BaseActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvFlashStatus: TextView
    private lateinit var tvInstruction: TextView
    private lateinit var capturingOverlay: LinearLayout

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var isFlashOn = false
    private var isCapturing = false
    private lateinit var cameraExecutor: ExecutorService

    private var sourceLang = "en"
    private var targetLang = "he"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        sourceLang = intent.getStringExtra("source_lang") ?: "en"
        targetLang = intent.getStringExtra("target_lang") ?: "he"

        previewView = findViewById(R.id.previewView)
        tvFlashStatus = findViewById(R.id.tvFlashStatus)
        tvInstruction = findViewById(R.id.tvInstruction)
        capturingOverlay = findViewById(R.id.capturingOverlay)

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera failed to start", Toast.LENGTH_SHORT).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        if (isCapturing) return
        isCapturing = true
        capturingOverlay.visibility = View.VISIBLE

        val imageCapture = imageCapture ?: run {
            capturingOverlay.visibility = View.GONE
            isCapturing = false
            return
        }

        // Trigger autofocus first
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(
            previewView.width / 2f,
            previewView.height / 2f
        )
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        camera?.cameraControl?.startFocusAndMetering(action)?.addListener({
            // After focus, take the picture
            val photoFile = File(cacheDir, "ocr_capture_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        isCapturing = false
                        val intent = Intent(this@CameraActivity, ImageTranslationActivity::class.java)
                        intent.putExtra("image_path", photoFile.absolutePath)
                        intent.putExtra("source_lang", sourceLang)
                        intent.putExtra("target_lang", targetLang)
                        startActivityWithFade(intent)
                        finish()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isCapturing = false
                        capturingOverlay.visibility = View.GONE
                        Toast.makeText(
                            this@CameraActivity,
                            "Capture failed: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        camera?.cameraControl?.enableTorch(isFlashOn)
        tvFlashStatus.text = if (isFlashOn) {
            getString(R.string.camera_flash_on)
        } else {
            getString(R.string.camera_flash_off)
        }
    }

    override fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                capturePhoto()
                return true
            }
            KeyEvent.KEYCODE_STAR -> {
                toggleFlash()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
