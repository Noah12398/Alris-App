package com.example.alris.user

import android.util.Size
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.alris.Constants
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import kotlinx.serialization.Serializable

@Serializable
data class ImagePayload(
    val filename: String,
    val base64: String
)

@Serializable
data class UploadPayload(
    val latitude: Double,
    val longitude: Double,
    val images: List<ImagePayload>,
    val userId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPhotoCameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var locationText by remember { mutableStateOf("") }
    var photoFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isCapturing by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<UploadStatus>(UploadStatus.Idle) }
    var hasLocation by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<Location?>(null) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var captureSuccess by remember { mutableStateOf(false) }

    fun fetchLocation() {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fused.lastLocation.addOnSuccessListener {
            location = it
            hasLocation = it != null
            locationText = if (it != null) "${it.latitude.format(4)}, ${it.longitude.format(4)}" else "No location"
        }
    }

    LaunchedEffect(Unit) {
        if (hasPermissions(context)) {
            startCamera(previewView, lifecycleOwner) { imageCapture = it }
            while (true) {
                fetchLocation()
                delay(5000)
            }
        }
    }

    LaunchedEffect(captureSuccess) {
        if (captureSuccess) {
            delay(1000)
            captureSuccess = false
        }
    }

    fun takePhoto() {
        if (isCapturing) return
        isCapturing = true
        val capture = imageCapture ?: return

        val photoFile = File(context.getExternalFilesDir(null), "IMG_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(outputOptions, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                isCapturing = false
                uploadStatus = UploadStatus.Error("Capture failed")
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                photoFiles = photoFiles + photoFile
                isCapturing = false
                captureSuccess = true
            }
        })
    }
    fun getUserDocumentId(onResult: (String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null)

        FirebaseFirestore.getInstance()
            .collection("users")
            .whereEqualTo("uid", uid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.documents[0].id
                    onResult(docId)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
    fun uploadAllPhotos() {
        val currentLocation = location ?: return

        getUserDocumentId { userId ->
            if (userId == null) {
                uploadStatus = UploadStatus.Error("User document not found")
                return@getUserDocumentId
            }

            coroutineScope.launch(Dispatchers.IO) {
                uploadStatus = UploadStatus.Uploading

                var allSuccess = true
                uploadMultipleImagesAsJson(
                    files = photoFiles,
                    latitude = currentLocation.latitude,
                    longitude = currentLocation.longitude,
                    userId = userId  // ✅ Firestore document ID
                ) { success ->
                    if (!success) allSuccess = false
                }

                uploadStatus = if (allSuccess) UploadStatus.Success else UploadStatus.Error("One or more uploads failed")
            }
        }
    }


    fun deletePhoto(index: Int) {
        photoFiles = photoFiles.toMutableList().also { it.removeAt(index) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Overlay for better readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // Top Status Bar
        TopAppBar(
            title = {
                Text(
                    "Photo Capture",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            actions = {
                IconButton(onClick = { isFlashEnabled = !isFlashEnabled }) {
                    Icon(
                        if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            }
        )

        // Success Animation Overlay
        AnimatedVisibility(
            visible = captureSuccess,
            enter = scaleIn(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(),
            exit = scaleOut(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color.Green,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        // Status Information
        StatusBar(
            hasLocation = hasLocation,
            locationText = locationText,
            uploadStatus = uploadStatus,
            photoCount = photoFiles.size,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        )

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo Gallery
            AnimatedVisibility(
                visible = photoFiles.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) + fadeOut()
            ) {
                PhotoGallery(
                    photoFiles = photoFiles,
                    onDeletePhoto = ::deletePhoto,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upload Button
                AnimatedVisibility(
                    visible = photoFiles.isNotEmpty(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = { uploadAllPhotos() },
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(12.dp, CircleShape),
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ) {
                        when (uploadStatus) {
                            is UploadStatus.Uploading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = "Upload",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Main Capture Button
                CaptureButton(
                    isCapturing = isCapturing,
                    onCaptureClick = ::takePhoto,
                    modifier = Modifier
                )

                // Gallery Counter
                AnimatedVisibility(
                    visible = photoFiles.isNotEmpty(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(8.dp, CircleShape),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${photoFiles.size}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBar(
    hasLocation: Boolean,
    locationText: String,
    uploadStatus: UploadStatus,
    photoCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Location Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = if (hasLocation) Color(0xFF4CAF50) else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasLocation) locationText else "Getting location...",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Upload Status
            UploadStatusIndicator(uploadStatus)
        }
    }
}

@Composable
fun UploadStatusIndicator(status: UploadStatus) {
    AnimatedContent(
        targetState = status,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        }
    ) { currentStatus ->
        when (currentStatus) {
            is UploadStatus.Idle -> {
                // Nothing to show
            }
            is UploadStatus.Uploading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Uploading photos...",
                        color = Color(0xFF2196F3),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            is UploadStatus.Success -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload complete!",
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            is UploadStatus.Error -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentStatus.message,
                        color = Color(0xFFFF5722),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CaptureButton(
    isCapturing: Boolean,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isCapturing) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulse ring
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(if (isCapturing) pulseScale else 1f)
                .background(
                    Color.White.copy(alpha = 0.3f),
                    CircleShape
                )
        )

        // Main button
        FloatingActionButton(
            onClick = onCaptureClick,
            modifier = Modifier
                .size(64.dp)
                .shadow(20.dp, CircleShape),
            containerColor = Color.White,
            contentColor = Color.Black,
            shape = CircleShape
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = Color.Black
                )
            } else {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = "Capture",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun PhotoGallery(
    photoFiles: List<File>,
    onDeletePhoto: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Captured Photos (${photoFiles.size})",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(photoFiles) { index, file ->
                    PhotoThumbnail(
                        file = file,
                        onDelete = { onDeletePhoto(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoThumbnail(
    file: File,
    onDelete: () -> Unit
) {
    var showDeleteIcon by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDeleteIcon = !showDeleteIcon }
    ) {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(file)
                .crossfade(true)
                .build()
        )

        Image(
            painter = painter,
            contentDescription = "Captured Photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Delete overlay
        AnimatedVisibility(
            visible = showDeleteIcon,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    2.dp,
                    Color.White.copy(alpha = 0.8f),
                    RoundedCornerShape(12.dp)
                )
        )
    }
}

// Helper function for formatting coordinates
fun Double.format(digits: Int) = "%.${digits}f".format(this)

sealed class UploadStatus {
    object Idle : UploadStatus()
    object Uploading : UploadStatus()
    object Success : UploadStatus()
    data class Error(val message: String) : UploadStatus()
}

fun startCamera(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onCaptureReady: (ImageCapture) -> Unit
) {
    val context = previewView.context
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val capture = ImageCapture.Builder()
            .setTargetResolution(Size(1280, 720))
            .build()
        onCaptureReady(capture)

        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
    }, ContextCompat.getMainExecutor(context))
}

fun uploadMultipleImagesAsJson(
    files: List<File>,
    latitude: Double,
    longitude: Double,
    userId: String, // <-- Add this parameter
    onComplete: (Boolean) -> Unit
) {
    val client = OkHttpClient()

    val images = files.mapNotNull { file ->
        if (file.exists()) {
            try {
                val base64 = android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.DEFAULT)
                ImagePayload(filename = file.name, base64 = base64)
            } catch (e: IOException) {
                Log.e("UPLOAD", "Failed to read file: ${file.name}", e)
                null
            }
        } else {
            Log.e("UPLOAD", "File not found: ${file.absolutePath}")
            null
        }
    }

    val payload = UploadPayload(
        latitude = latitude,
        longitude = longitude,
        images = images,
        userId = userId // <-- Include it in payload
    )

    val json = Json.encodeToString(UploadPayload.serializer(), payload)
    val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)

    val request = Request.Builder()
        .url("${Constants.BASE_URL}/upload/multi")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("UPLOAD", "Upload failed: ${e.message}")
            onComplete(false)
        }

        override fun onResponse(call: Call, response: Response) {
            Log.d("UPLOAD", "Upload response: ${response.body?.string()}")
            onComplete(response.isSuccessful)
        }
    })
}



fun hasPermissions(context: Context): Boolean {
    return listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}