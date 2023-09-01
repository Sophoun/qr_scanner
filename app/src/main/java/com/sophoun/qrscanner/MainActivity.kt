package com.sophoun.qrscanner

import android.Manifest
import android.content.ContentValues.TAG
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.sophoun.qrscanner.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()


    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {

                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun startCamera() {
        var cameraController = LifecycleCameraController(baseContext)
        val previewView: PreviewView = binding.activityMainSvCameraView

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val barcodeScanner = BarcodeScanning.getClient(options)

        cameraController.setZoomRatio(2f)
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            MlKitAnalyzer(
                listOf(barcodeScanner),
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(this)
            ) { result: MlKitAnalyzer.Result? ->
                val barcodeResults = result?.getValue(barcodeScanner)
                Log.d(TAG, "startCamera: barcode: ${barcodeResults?.firstOrNull()?.boundingBox}")
                val l = IntArray(2)
                binding.qrFrame.getLocationInWindow(l)
                val x = l[0]
                val y = l[1] - window.statusBarHeight()
                val w =  l[0] + binding.qrFrame.width
                val h = l[1] + binding.qrFrame.height - window.statusBarHeight()
                val availableRect = Rect(x, y, w, h)
                Log.d(TAG, "startCamera: position: $availableRect")

                val barcode = barcodeResults?.firstOrNull()?.boundingBox
                binding.root.overlay.clear()
                binding.root.overlay.add(QrCodeDrawer(availableRect,barcodeResults?.firstOrNull()?.rawValue ?: "empty" ))
                if(barcode != null) {
                    binding.root.overlay.add(QrCodeDrawer(barcode,barcodeResults?.firstOrNull()?.rawValue ?: "empty" ))
                    val result = availableRect.isContains(barcode)
                    Log.d(TAG, "startCamera: contains: $result")
                }
            }
        )

        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermissions()
    }
}


fun Window.statusBarHeight(): Int {
    val rect = Rect()
    decorView.getWindowVisibleDisplayFrame(rect)
    return rect.top
}


fun Rect.isContains(rect: Rect) : Boolean {
    if(rect.left > left && rect.top > top && rect.right < right && rect.bottom < bottom)
        return true
    return false
}
