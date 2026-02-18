package com.example.alris.user

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.alris.data.TokenManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.alris.data.ApiClient
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun compressImageToMaxSize(file: File, maxSizeMB: Int = 4): File {
    Log.d("IMAGE_COMPRESS", "Starting compression for file: ${file.name}, size: ${file.length()} bytes")

    val maxBytes = maxSizeMB * 1024 * 1024
    val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)

    if (originalBitmap == null) {
        Log.e("IMAGE_COMPRESS", "Failed to decode bitmap from ${file.absolutePath}")
        return file
    }

    var compressQuality = 100
    var streamLength: Int

    do {
        val outputStream = ByteArrayOutputStream()
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, outputStream)
        val byteArray = outputStream.toByteArray()
        streamLength = byteArray.size
        Log.d("IMAGE_COMPRESS", "Quality: $compressQuality, Size: $streamLength bytes")
        compressQuality -= 5
    } while (streamLength > maxBytes && compressQuality > 5)

    val compressedFile = File(file.parent, "COMP_${file.name}")
    val fos = compressedFile.outputStream()
    originalBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, fos)
    fos.flush()
    fos.close()

    Log.d("IMAGE_COMPRESS", "Compression complete. Final size: ${compressedFile.length()} bytes")
    return compressedFile
}

// --- Upload Status Sealed Class ---
sealed class UploadStatus {
    object Idle : UploadStatus()
    object Uploading : UploadStatus()
    object Success : UploadStatus()
    data class Error(val message: String) : UploadStatus()
}

// --- Main Camera Screen ---
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
    var photoFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isCapturing by remember { mutableStateOf(false) }
    var captureSuccess by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<UploadStatus>(UploadStatus.Idle) }
    var location by remember { mutableStateOf<Location?>(null) }
    var hasPermissions by remember { mutableStateOf(false) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    val tokenManager = remember { TokenManager(context) }
    val token by tokenManager.accessTokenFlow.collectAsState(initial = null)

    // --- Permissions Launcher ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        Log.d("PERMISSIONS", "Permissions granted: $granted")
        hasPermissions = granted
        if (granted) startCamera(previewView, lifecycleOwner) { imageCapture = it }
    }

    // --- Request Permissions ---
    LaunchedEffect(Unit) {
        val allGranted = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

        Log.d("PERMISSIONS", "Initial permission check: $allGranted")

        if (!allGranted) permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        )
        else {
            hasPermissions = true
            startCamera(previewView, lifecycleOwner) { imageCapture = it }
        }
    }

    // --- Fetch Location Every 3s ---
    LaunchedEffect(hasPermissions) {
        if (!hasPermissions) return@LaunchedEffect
        Log.d("LOCATION", "Starting location updates")
        val fused = LocationServices.getFusedLocationProviderClient(context)
        while (true) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fused.lastLocation.addOnSuccessListener { loc ->
                        location = loc
                        Log.d("LOCATION", "Location updated: ${loc?.latitude}, ${loc?.longitude}")
                    }.addOnFailureListener { e ->
                        Log.e("LOCATION", "Failed to get location", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("LOCATION", "Error reading location", e)
            }
            delay(3000)
        }
    }

    // --- Reset Capture Success Animation ---
    LaunchedEffect(captureSuccess) {
        if (captureSuccess) {
            delay(1000)
            captureSuccess = false
        }
    }

    // --- Capture Photo ---
    fun takePhoto() {
        if (isCapturing) return
        val capture = imageCapture ?: return
        Log.d("CAMERA", "Taking photo...")
        isCapturing = true
        val photoFile = File(context.getExternalFilesDir(null), "IMG_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CAMERA", "Photo capture failed", exc)
                    isCapturing = false
                    uploadStatus = UploadStatus.Error("Capture failed: ${exc.message}")
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CAMERA", "Photo saved: ${photoFile.absolutePath}")
                    photoFiles = photoFiles + photoFile
                    isCapturing = false
                    captureSuccess = true
                }
            })
    }

    fun uploadPhotos(userDescription: String) {
        val loc = location
        Log.d("UPLOAD", "Upload initiated")
        Log.d("UPLOAD", "Photos: ${photoFiles.size}")
        Log.d("UPLOAD", "Location: $loc")
        Log.d("UPLOAD", "Token available: ${!token.isNullOrEmpty()}")

        if (photoFiles.isEmpty()) {
            uploadStatus = UploadStatus.Error("No photos")
            return
        }
        if (loc == null) {
            uploadStatus = UploadStatus.Error("Location unavailable")
            return
        }
        if (token.isNullOrEmpty()) {
            uploadStatus = UploadStatus.Error("Login required")
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            uploadStatus = UploadStatus.Uploading

            try {
                // Compress files
                val compressedFiles = photoFiles.map { compressImageToMaxSize(it, 4) }
                Log.d("UPLOAD", "Files compressed successfully")

                // Call fixed upload function (removed token parameter)
                val success = uploadPhotosWithRetrofit(
                    context = context,
                    files = compressedFiles,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    description = if (userDescription.isBlank()) "User report from ALRIS mobile app" else userDescription
                )

                uploadStatus = if (success) {
                    Log.d("UPLOAD", "Upload successful")
                    // Clear photos and clean up compressed files
                    val filesToDelete = photoFiles.toList()
                    photoFiles = emptyList()

                    // Delete original and compressed files
                    filesToDelete.forEach { it.delete() }
                    compressedFiles.forEach {
                        if (it.name.startsWith("COMP_")) it.delete()
                    }

                    UploadStatus.Success
                } else {
                    Log.e("UPLOAD", "Upload failed")
                    UploadStatus.Error("Upload failed")
                }

            } catch (e: Exception) {
                Log.e("UPLOAD", "Upload exception", e)
                uploadStatus = UploadStatus.Error("Upload error: ${e.message}")
            }
        }
    }

    // --- UI ---
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.6f)))
        ))

        // Top app bar
        TopAppBar(
            title = { Text("Photo Capture", color = Color.White, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                IconButton(onClick = { isFlashEnabled = !isFlashEnabled }) {
                    Icon(if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Flash", tint = Color.White)
                }
            }
        )

        // Debug info overlay (remove in production)
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(
                Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp)
            ).padding(8.dp)
        ) {
            Text("Debug Info:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Token: ${if (token.isNullOrEmpty()) "None" else "Available"}", color = Color.White, fontSize = 8.sp)
            Text("Location: ${location?.let { "${it.latitude.toString().take(6)}, ${it.longitude.toString().take(6)}" } ?: "None"}", color = Color.White, fontSize = 8.sp)
            Text("Photos: ${photoFiles.size}", color = Color.White, fontSize = 8.sp)
            when (uploadStatus) {
                is UploadStatus.Idle -> Text("Status: Idle", color = Color.White, fontSize = 8.sp)
                is UploadStatus.Uploading -> Text("Status: Uploading", color = Color.Yellow, fontSize = 8.sp)
                is UploadStatus.Success -> Text("Status: Success", color = Color.Green, fontSize = 8.sp)
                is UploadStatus.Error -> Text("Status: ${(uploadStatus as UploadStatus.Error).message}", color = Color.Red, fontSize = 8.sp)
            }
        }

        // Capture success overlay
        AnimatedVisibility(
            visible = captureSuccess,
            enter = scaleIn(tween(300)) + fadeIn(),
            exit = scaleOut(tween(300)) + fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.Green, modifier = Modifier.size(80.dp))
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = photoFiles.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                PhotoGallery(photoFiles = photoFiles, onDeletePhoto = { idx -> photoFiles = photoFiles.toMutableList().also { it.removeAt(idx) } })
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = photoFiles.isNotEmpty(),
                    enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = { showUploadDialog = true },
                        modifier = Modifier.size(56.dp).shadow(12.dp, CircleShape),
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ) {
                        when (uploadStatus) {
                            is UploadStatus.Uploading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                            else -> Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(24.dp))
                        }
                    }
                }

                CaptureButton(isCapturing = isCapturing, onCaptureClick = ::takePhoto)

                AnimatedVisibility(visible = photoFiles.isNotEmpty(),
                    enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()
                ) {
                    Card(modifier = Modifier.size(56.dp).shadow(8.dp, CircleShape), shape = CircleShape, colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("${photoFiles.size}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    if (showUploadDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showUploadDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE8F5E9), CircleShape)
                            .padding(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Add Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Text(
                        text = "Help authorities understand the issue better.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("e.g. Broken streetlight near the park entrance...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = Color(0xFF4CAF50)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showUploadDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("Cancel", color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                showUploadDialog = false
                                uploadPhotos(description)
                                description = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Upload Report")
                        }
                    }
                }
            }
        }
    }
}

// --- Enhanced Upload Function with Better Error Handling ---
suspend fun uploadPhotosWithRetrofit(
    context: Context,
    files: List<File>,
    latitude: Double,
    longitude: Double,
    description: String?
): Boolean {
    return try {
        Log.d("UPLOAD_REQUEST", "=== UPLOAD REQUEST START ===")
        Log.d("UPLOAD_REQUEST", "Base URL: https://alris-node.vercel.app/")
        Log.d("UPLOAD_REQUEST", "Endpoint: reports")
        Log.d("UPLOAD_REQUEST", "Files count: ${files.size}")

        // Validate files exist
        files.forEach { file ->
            if (!file.exists()) {
                Log.e("UPLOAD_REQUEST", "File does not exist: ${file.absolutePath}")
                return false
            }
        }

        val fileParts = files.mapIndexed { index, file ->
            Log.d("UPLOAD_REQUEST", "File[$index]: ${file.name}, ${file.length()} bytes")
            val reqBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            // IMPORTANT: Use "files" as field name - this is what your multer config expects
            MultipartBody.Part.createFormData("files", file.name, reqBody)
        }

        val latitudeBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val longitudeBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val descriptionBody = description?.toRequestBody("text/plain".toMediaTypeOrNull())

        Log.d("UPLOAD_REQUEST", "Request parameters:")
        Log.d("UPLOAD_REQUEST", "- Files: ${files.size} parts")
        Log.d("UPLOAD_REQUEST", "- Latitude: $latitude")
        Log.d("UPLOAD_REQUEST", "- Longitude: $longitude")
        Log.d("UPLOAD_REQUEST", "- Description: $description")
        Log.d("UPLOAD_REQUEST", "- Auth: Handled by AuthInterceptor")

        val api = ApiClient.createUserApi(context)

        // AuthInterceptor will automatically add "Authorization: Bearer <token>"
        val response = api.uploadReport(
            files = fileParts,
            latitude = latitudeBody,
            longitude = longitudeBody,
            description = descriptionBody
        )

        Log.d("UPLOAD_REQUEST", "=== RESPONSE RECEIVED ===")
        Log.d("UPLOAD_REQUEST", "Status: ${response.code()} ${response.message()}")
        Log.d("UPLOAD_REQUEST", "Headers: ${response.headers()}")

        if (response.isSuccessful) {
            val body = response.body()
            val uploadData = body?.data
            Log.d("UPLOAD_REQUEST", "✅ SUCCESS!")
            Log.d("UPLOAD_REQUEST", "Report ID: ${uploadData?.report?.id}")
            Log.d("UPLOAD_REQUEST", "User ID: ${uploadData?.report?.user_id}")
            Log.d("UPLOAD_REQUEST", "Uploads count: ${uploadData?.uploads?.size}")

            uploadData?.uploads?.forEach { upload ->
                Log.d("UPLOAD_REQUEST", "  Upload: ${upload.filename}")
            }

            return true
        } else {
            Log.e("UPLOAD_REQUEST", "❌ FAILED!")
            val errorBody = response.errorBody()?.string()
            Log.e("UPLOAD_REQUEST", "Error: $errorBody")

            when (response.code()) {
                401 -> Log.e("UPLOAD_REQUEST", "Unauthorized - token issue")
                413 -> Log.e("UPLOAD_REQUEST", "Payload too large")
                422 -> Log.e("UPLOAD_REQUEST", "Validation failed")
                500 -> Log.e("UPLOAD_REQUEST", "Server error")
            }

            return false
        }

    } catch (e: ConnectException) {
        Log.e("UPLOAD_REQUEST", "Connection failed", e)
        false
    } catch (e: SocketTimeoutException) {
        Log.e("UPLOAD_REQUEST", "Timeout", e)
        false
    } catch (e: UnknownHostException) {
        Log.e("UPLOAD_REQUEST", "DNS resolution failed", e)
        false
    } catch (e: Exception) {
        Log.e("UPLOAD_REQUEST", "Unexpected error", e)
        false
    }
}

// --- CameraX Setup ---
fun startCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner, onCaptureReady: (ImageCapture) -> Unit) {
    val context = previewView.context
    Log.d("CAMERA", "Starting camera setup")
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val capture = ImageCapture.Builder().build()
            onCaptureReady(capture)
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            Log.d("CAMERA", "Camera setup complete")
        } catch (e: Exception) {
            Log.e("CAMERA", "Camera setup failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

// --- Capture Button ---
@Composable
fun CaptureButton(isCapturing: Boolean, onCaptureClick: () -> Unit) {
    val scale by animateFloatAsState(if (isCapturing) 0.85f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    val pulseTransition = rememberInfiniteTransition()
    val pulseScale by pulseTransition.animateFloat(1f, 1.1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse))

    Box(modifier = Modifier.size(80.dp).scale(scale), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(80.dp).scale(if (isCapturing) pulseScale else 1f).background(Color.White.copy(alpha = 0.3f), CircleShape))
        FloatingActionButton(onClick = onCaptureClick, modifier = Modifier.size(64.dp).shadow(20.dp, CircleShape), containerColor = Color.White, contentColor = Color.Black, shape = CircleShape) {
            if (isCapturing) CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = Color.Black)
            else Icon(Icons.Default.PhotoCamera, contentDescription = "Capture", modifier = Modifier.size(28.dp))
        }
    }
}

// --- Photo Gallery ---
@Composable
fun PhotoGallery(photoFiles: List<File>, onDeletePhoto: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).shadow(12.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Captured Photos (${photoFiles.size})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(photoFiles) { index, file -> PhotoThumbnail(file) { onDeletePhoto(index) } }
            }
        }
    }
}

// --- Photo Thumbnail ---
@Composable
fun PhotoThumbnail(file: File, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).clickable { showDelete = !showDelete }) {
        val painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(file).crossfade(true).build())
        Image(painter = painter, contentDescription = "Photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        AnimatedVisibility(visible = showDelete, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { onDelete() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Box(modifier = Modifier.fillMaxSize().border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp)))
    }
}