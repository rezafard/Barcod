package com.example.ui

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isScanning: Boolean,
    torchEnabled: Boolean,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    // Store a reference to the active Camera to toggle physical torch dynamically
    val activeCamera = remember { mutableStateOf<Camera?>(null) }

    // Update physical torch when torchEnabled state changes
    LaunchedEffect(torchEnabled) {
        activeCamera.value?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Configure Preview Use Case
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                // Configure ImageAnalysis Use Case for ML Kit barcode processing
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                // Debounce scanning interval locally
                var lastScannedTime = 0L

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    val currentTime = System.currentTimeMillis()

                    // Only process frame if scanning is enabled globally and local cooldown (>1.5s) has passed
                    if (mediaImage != null && isScanning && (currentTime - lastScannedTime > 1500L)) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val foundBarcode = barcodes.firstOrNull()?.rawValue
                                if (foundBarcode != null && foundBarcode.isNotBlank()) {
                                    lastScannedTime = System.currentTimeMillis()
                                    // Run callback on UI thread
                                    previewView.post {
                                        onBarcodeScanned(foundBarcode)
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("CameraPreview", "ML Kit barcode scan failure", e)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind camera to life cycle
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )

                    activeCamera.value = camera
                    // Set initial torch state
                    camera.cameraControl.enableTorch(torchEnabled)

                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Failed to bind camera use cases", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}
