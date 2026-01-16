package com.bowlingclub.fee.ui.screens.ocr

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.exifinterface.media.ExifInterface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun OcrCameraScreen(
    isProcessing: Boolean,
    onImageCaptured: (Bitmap) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }

    // 갤러리에서 이미지 선택 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = loadBitmapFromUri(context, it)
                bitmap?.let { bmp -> onImageCaptured(bmp) }
            } catch (e: Exception) {
                Log.e("OcrCamera", "갤러리 이미지 로드 실패", e)
            }
        }
    }

    // 간단한 권한 체크 (accompanist 없이)
    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    if (!hasCameraPermission) {
        CameraPermissionRequest(
            onPermissionGranted = { hasCameraPermission = true },
            onNavigateBack = onNavigateBack,
            onGalleryClick = { galleryLauncher.launch("image/*") }
        )
    } else {
        CameraPreviewContent(
            context = context,
            lifecycleOwner = lifecycleOwner,
            isProcessing = isProcessing,
            onImageCaptured = onImageCaptured,
            onNavigateBack = onNavigateBack,
            onGalleryClick = { galleryLauncher.launch("image/*") }
        )
    }
}

@Composable
private fun CameraPermissionRequest(
    onPermissionGranted: () -> Unit,
    onNavigateBack: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "카메라 권한이 필요합니다",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "점수표를 스캔하려면 카메라 접근 권한을 허용해주세요.\n또는 갤러리에서 사진을 선택할 수 있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("취소")
            }

            Button(
                onClick = onGalleryClick,
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("갤러리")
            }

            Button(
                onClick = { launcher.launch(Manifest.permission.CAMERA) }
            ) {
                Text("권한 허용")
            }
        }
    }
}

@Composable
private fun CameraPreviewContent(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    isProcessing: Boolean,
    onImageCaptured: (Bitmap) -> Unit,
    onNavigateBack: () -> Unit,
    onGalleryClick: () -> Unit
) {
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // 카메라 에러 표시
    if (cameraError != null) {
        CameraErrorContent(
            errorMessage = cameraError!!,
            onRetry = { cameraError = null },
            onNavigateBack = onNavigateBack
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .setTargetRotation(previewView.display.rotation)
                        .build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val imageCaptureBuilder = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(previewView.display.rotation)

                    imageCapture = imageCaptureBuilder.build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("OcrCamera", "카메라 바인딩 실패", e)
                        cameraError = "카메라를 시작할 수 없습니다: ${e.localizedMessage}"
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay UI
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "점수표 스캔",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    IconButton(
                        onClick = { isFlashOn = !isFlashOn }
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (isFlashOn) "플래시 끄기" else "플래시 켜기",
                            tint = Color.White
                        )
                    }
                }
            }

            // 전체 화면 촬영 안내
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (!isProcessing) {
                    Surface(
                        modifier = Modifier.padding(16.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "점수표가 화면에 잘 보이도록 촬영하세요",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "글자가 선명하게 보일수록 인식률이 높아집니다",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Bottom Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "점수표 인식 중...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        // 버튼 영역 (갤러리 + 촬영)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 갤러리 버튼
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = onGalleryClick,
                                    modifier = Modifier.size(56.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "갤러리",
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "갤러리",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // 촬영 버튼
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = {
                                        imageCapture?.let { capture ->
                                            capture.flashMode = if (isFlashOn) {
                                                ImageCapture.FLASH_MODE_ON
                                            } else {
                                                ImageCapture.FLASH_MODE_OFF
                                            }

                                            captureImage(
                                                imageCapture = capture,
                                                executor = cameraExecutor,
                                                onImageCaptured = onImageCaptured
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(72.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "촬영",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "촬영",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // 균형을 위한 빈 공간
                            Spacer(modifier = Modifier.size(56.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "촬영하거나 갤러리에서 점수표를 선택하세요",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun captureImage(
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onImageCaptured: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val bitmap = imageProxyToBitmap(imageProxy)
                imageProxy.close()
                bitmap?.let { onImageCaptured(it) }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("OcrCamera", "이미지 캡처 실패", exception)
            }
        }
    )
}

@Composable
private fun CameraErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "카메라 오류",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("취소")
            }

            Button(onClick = onRetry) {
                Text("다시 시도")
            }
        }
    }
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    val buffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: return null

    // 회전 보정 - CameraX는 기기 방향에 따라 rotationDegrees를 제공
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    Log.d("OcrCamera", "캡처된 이미지: ${bitmap.width}x${bitmap.height}, 회전: ${rotationDegrees}도")

    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        Log.d("OcrCamera", "회전 후 이미지: ${rotatedBitmap.width}x${rotatedBitmap.height}")
        bitmap.recycle() // 원본 메모리 해제
        rotatedBitmap
    } else {
        bitmap
    }
}

/**
 * 갤러리에서 이미지를 로드하고 EXIF 회전 정보를 적용
 * 또한 메모리 효율을 위해 큰 이미지는 리사이즈
 */
private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    try {
        // 1. 이미지 크기 확인 (디코딩하지 않고)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        val imageWidth = options.outWidth
        val imageHeight = options.outHeight
        Log.d("OcrCamera", "갤러리 이미지 원본 크기: ${imageWidth}x${imageHeight}")

        // 2. 샘플 사이즈 계산 (최대 2048px 기준)
        val maxDimension = 2048
        var sampleSize = 1
        while (imageWidth / sampleSize > maxDimension || imageHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        // 3. 실제 이미지 로드 (샘플 사이즈 적용)
        val loadOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        var bitmap: Bitmap? = null
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            bitmap = BitmapFactory.decodeStream(inputStream, null, loadOptions)
        }

        if (bitmap == null) {
            Log.e("OcrCamera", "비트맵 디코딩 실패")
            return null
        }

        Log.d("OcrCamera", "로드된 이미지 크기: ${bitmap!!.width}x${bitmap!!.height}, sampleSize: $sampleSize")

        // 4. EXIF 회전 정보 읽기 및 적용
        val rotation = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            try {
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } catch (e: Exception) {
                Log.w("OcrCamera", "EXIF 정보 읽기 실패", e)
                0f
            }
        } ?: 0f

        Log.d("OcrCamera", "EXIF 회전 정보: ${rotation}도")

        // 5. 회전 적용
        return if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
            Log.d("OcrCamera", "회전 후 이미지: ${rotatedBitmap.width}x${rotatedBitmap.height}")
            if (rotatedBitmap != bitmap) {
                bitmap!!.recycle()
            }
            rotatedBitmap
        } else {
            bitmap
        }
    } catch (e: Exception) {
        Log.e("OcrCamera", "이미지 로드 중 오류", e)
        return null
    }
}
