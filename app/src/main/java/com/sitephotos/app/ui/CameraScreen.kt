package com.sitephotos.app.ui

import android.graphics.Bitmap
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sitephotos.app.Building
import com.sitephotos.app.PhotoStore
import com.sitephotos.app.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CameraScreen(
    project: Project,
    building: Building,
    onBack: () -> Unit,
    onReview: () -> Unit,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FIT_CENTER } }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .build()
    }

    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var count by remember { mutableIntStateOf(PhotoStore.list(ctx, project, building).size) }
    var takenNow by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var lastThumb by remember { mutableStateOf<Bitmap?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }
    var showFlash by remember { mutableStateOf(false) }

    // Bind camera
    LaunchedEffect(Unit) {
        try {
            val provider = ProcessCameraProvider.getInstance(ctx).await()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    DisposableEffect(Unit) {
        // Correct photo orientation when the phone is turned sideways (screen is locked to portrait).
        val listener = object : OrientationEventListener(ctx) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                imageCapture.targetRotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
        listener.enable()
        onDispose {
            listener.disable()
            runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
        }
    }

    // White flash feedback on capture
    LaunchedEffect(flashTick) {
        if (flashTick > 0) {
            showFlash = true
            delay(120)
            showFlash = false
        }
    }
    val flashAlpha by animateFloatAsState(if (showFlash) 0.7f else 0f, tween(120), label = "flash")

    fun takePhoto() {
        if (busy) return
        busy = true
        val file = PhotoStore.newPhotoFile(ctx, project, building)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        flashTick++
        imageCapture.takePicture(options, ContextCompat.getMainExecutor(ctx),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    busy = false
                    count++
                    takenNow++
                    // load thumbnail off the main thread
                    scope.launch {
                        lastThumb = withContext(Dispatchers.IO) { PhotoStore.loadThumbnail(file, 200) }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    busy = false
                    file.delete()
                    Toast.makeText(ctx, "Photo failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .alpha(flashAlpha)
                .background(Color.White)
        )

        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("‹ Done", color = Color.White, fontSize = 16.sp) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Project ${project.code}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(building.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            TextButton(onClick = {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                    else -> ImageCapture.FLASH_MODE_AUTO
                }
                imageCapture.flashMode = flashMode
            }) {
                Text(
                    "⚡ " + when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> "Auto"
                        ImageCapture.FLASH_MODE_ON -> "On"
                        else -> "Off"
                    },
                    color = Color.White,
                )
            }
        }

        // Bottom controls
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .navigationBarsPadding()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "$takenNow taken now  •  $count waiting for Sync (not saved in Gallery)",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.size(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Last photo thumbnail -> review
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                        .clickable(enabled = count > 0) { onReview() },
                    contentAlignment = Alignment.Center,
                ) {
                    val t = lastThumb
                    if (t != null) {
                        Image(t.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text("$count", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Shutter
                Box(
                    Modifier
                        .size(78.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(if (busy) Color.Gray else Color.White)
                        .clickable(enabled = !busy) { takePhoto() }
                )

                Spacer(Modifier.size(56.dp))
            }
        }
    }
}
