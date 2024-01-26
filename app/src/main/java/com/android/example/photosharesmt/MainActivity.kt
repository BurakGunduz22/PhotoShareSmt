package com.android.example.photosharesmt

import DraggableCard
import LocationScreen
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView.ScaleType
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.android.example.photosharesmt.ui.theme.PhotoShareSmtTheme
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import java.util.Date
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.log
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: Executor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, CAMERAX_PERMISSIONS, 0)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            PhotoShareSmtTheme {
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                    }
                }
                val context = LocalContext.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KameraApp(controller, cameraExecutor, context, hasRequiredPermissions())
                }
            }
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}

@Composable
fun CameraCapture(
    onPhotoCaptured: (Bitmap) -> Unit = { },
    imageCapture: ImageCapture,
    cameraExecutor: Executor,
    context: Context,
    capturedImage: (Bitmap) -> Unit
) {

    FloatingActionButton(
        modifier = Modifier
            .size(60.dp),
        onClick = {
            captureImage(imageCapture, cameraExecutor, context) { Bitmap ->
                capturedImage(Bitmap)
            }
        }
    ) {
        Icon(Icons.Filled.CameraAlt, "Take a Photo", modifier = Modifier.size(40.dp))
    }

}

fun Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyy_MM_dd_HH:mm:ss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    return File.createTempFile(
        imageFileName,
        ".jpg",
        externalCacheDir
    )
}

fun captureImage(
    imageCapture: ImageCapture,
    cameraExecutor: Executor,
    context: Context,
    onImageCaptured: (Bitmap) -> Unit
) {
    /*val file = context.createImageFile()
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(outputFileOptions,cameraExecutor,object:OnImageCapturedCallback{
        override fun onCaptureSuccess(image: ImageProxy) {
            super.onCaptureSuccess(image)
        }

        override fun onError(exception: ImageCaptureException) {
            super.onError(exception)
        }
    })*/
    imageCapture.takePicture(cameraExecutor, object : OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            super.onCaptureSuccess(image)

            val matrix = Matrix().apply {
                postRotate(image.imageInfo.rotationDegrees.toFloat())
            }
            val rotatedBitmap = Bitmap.createBitmap(
                image.toBitmap(),
                0,
                0,
                image.width,
                image.height,
                matrix,
                true
            )
            onImageCaptured(rotatedBitmap)
        }

        override fun onError(exception: ImageCaptureException) {
            super.onError(exception)
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KameraApp(
    cameraController: LifecycleCameraController,
    cameraExecutor: Executor,
    context: Context,
    hasRequiredPermission: Boolean
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        var imageUri by remember { mutableStateOf<Bitmap?>(null) }
        val bitmapShare: Bitmap
        val imageBitmap: ImageBitmap
        if (imageUri != null) {
            bitmapShare = imageUri!!
            imageBitmap = imageUri!!.asImageBitmap()
            var width by remember { mutableStateOf(0) }
            var height by remember { mutableStateOf(0) }
            var buttonEnabled by remember { mutableStateOf(true) }
            val coroutineScope = rememberCoroutineScope()
            var isTextFieldVisible by remember { mutableStateOf(false) }
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusRequester = remember { FocusRequester() }
            var textFieldValue by remember { mutableStateOf("") }
            var cardValue by remember { mutableStateOf("") }
            var ifCardCreated by remember { mutableStateOf(false) }
            var location by remember {
                mutableStateOf<String>("")
            }
            fun Int.toDp(): Dp {
                return this.dp
            }
            BackHandler(enabled = true) {
                imageUri = null
            }
            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .onGloballyPositioned { coordinates ->
                    width = coordinates.size.width.toDp().value.toInt()
                    height = coordinates.size.height.toDp().value.toInt()
                }) {
                if(!ifCardCreated){
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clickable { isTextFieldVisible = true }) {

                    }
                }
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                location = LocationScreen(hasRequiredPermission)
                if (ifCardCreated) {
                    DraggableCard(cardValue){
                        isTextFieldVisible=it
                    }
                }
            }
            Column(verticalArrangement = Arrangement.Bottom) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (buttonEnabled) {
                        FloatingActionButton(
                            onClick = {
                                imageUri = null
                            }
                        ) {
                            Icon(Icons.Filled.Delete, "DeleteButton")
                        }
                        FloatingActionButton(
                            onClick = {
                                buttonEnabled = false
                                coroutineScope.launch {
                                    buttonEnabled =
                                        callFunctionAndWait(context, width, height, location)
                                }

                            }
                        ) {
                            Icon(Icons.Filled.Share, "ShareButton")
                        }
                    }
                }
            }
            if (isTextFieldVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    TextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                cardValue = textFieldValue
                                ifCardCreated = true
                                isTextFieldVisible = false
                                textFieldValue = "" // Clear text field after submission
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )
                    BackHandler(enabled = true) {
                        isTextFieldVisible = false
                        textFieldValue = ""
                    }
                }
            }

        } else {
            val imageCapture = remember {
                ImageCapture.Builder().setJpegQuality(100)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
            }
            var kameraYon: CameraSelector
            kameraYon = CameraSelector.DEFAULT_BACK_CAMERA
            CameraPreview(imageCapture = imageCapture, cameraSelector = kameraYon)
            Column(verticalArrangement = Arrangement.Bottom) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CameraCapture(
                        cameraExecutor = cameraExecutor,
                        imageCapture = imageCapture,
                        context = context
                    ) {
                        imageUri = it
                    }
                }
            }
        }
    }
}

@Composable
fun DraggableCard(lokasyon: String,isTextFieldVisible:(Boolean)->Unit) {
    var offset by remember { mutableStateOf(IntOffset.Zero) }

    Box(
        modifier = Modifier.padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .offset { offset }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        var newOffset = offset.toOffset()
                        newOffset += dragAmount
                        offset = newOffset.round()
                        change.consumePositionChange()
                    }
                }
                .clickable { isTextFieldVisible(true) },

            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
        ) {
            Text(text = lokasyon)
        }
    }
}

suspend fun callFunctionAndWait(
    context: Context,
    width: Int,
    height: Int,
    location: String
): Boolean {
    // Wait for some time
    delay(100) // 3 seconds
    val newBit = CaptureComponentAsBitmap(width, height, context) {
    }
    val newImageSize = newBit!!.asImageBitmap()
    ShareUtils.shareImageToOthers(context, location, newBit)
    return true
}

fun CaptureComponentAsBitmap(
    width: Int,
    height: Int,
    context: Context,
    component: @Composable () -> Unit
): Bitmap? {
    val rootView =
        (context as? ComponentActivity)?.window?.decorView?.findViewById<View>(android.R.id.content)
    rootView?.let { view ->
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }
    return null
}

@Composable
fun CameraPreview(
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector,
    imageCapture: ImageCapture
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    AndroidView(factory = { context ->
        val previewView = PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            this.scaleType = scaleType
        }
        val previewUseCase = androidx.camera.core.Preview.Builder().build()
        previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
        coroutineScope.launch {
            val cameraProvider = context.cameraProvider()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                imageCapture
            )
        }
        previewView
    })
}

suspend fun Context.cameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val listenableFuture = ProcessCameraProvider.getInstance(this)
    listenableFuture.addListener({
        continuation.resume(listenableFuture.get())
    }, ContextCompat.getMainExecutor(this))
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PhotoShareSmtTheme {
    }
}