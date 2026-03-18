package com.megalife.translator.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.megalife.translator.R
import java.io.File

class CameraActivity : BaseActivity() {

    companion object {
        const val EXTRA_SOURCE_LANG = "source_lang"
        const val EXTRA_TARGET_LANG = "target_lang"
    }

    private lateinit var previewView: PreviewView
    private lateinit var tvFlashToggle: TextView
    private lateinit var tvInstruction: TextView

    private var imageCapture: ImageCapture? = null
    private var isFlashOn = false
    private var isCapturing = false
    private var cameraProvider: ProcessCameraProvider? = null

    private var sourceLang: String? = null
    private var targetLang: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        sourceLang = intent.getStringExtra(EXTRA_SOURCE_LANG)
        targetLang = intent.getStringExtra(EXTRA_TARGET_LANG)

        previewView = findViewById(R.id.previewView)
        tvFlashToggle = findViewById(R.id.tvFlashToggle)
        tvInstruction = findViewById(R.id.tvInstruction)

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "Camera failed to start", Toast.LENGTH_SHORT).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        if (isCapturing) return
        isCapturing = true

        val imageCapture = imageCapture ?: return

        val photoFile = File(cacheDir, "ocr_capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        tvInstruction.text = "Capturing…"

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    // Navigate to Image Translation screen
                    val intent = Intent(this@CameraActivity, ImageTranslationActivity::class.java).apply {
                        putExtra(ImageTranslationActivity.EXTRA_IMAGE_PATH, photoFile.absolutePath)
                        putExtra(ImageTranslationActivity.EXTRA_SOURCE_LANG, sourceLang)
                        putExtra(ImageTranslationActivity.EXTRA_TARGET_LANG, targetLang)
                    }
                    startActivityWithFade(intent)
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    tvInstruction.text = getString(R.string.camera_instruction)
                    Toast.makeText(this@CameraActivity, "Capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        tvFlashToggle.text = if (isFlashOn) getString(R.string.camera_flash_on) else getString(R.string.camera_flash_off)
        imageCapture?.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    override fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> {
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
        cameraProvider?.unbindAll()
    }
}
