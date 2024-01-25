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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException

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
                    KameraApp(controller, cameraExecutor, context)
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
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

@Composable
fun KameraApp(
    cameraController: LifecycleCameraController,
    cameraExecutor: Executor,
    context: Context
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        var imageUri by remember { mutableStateOf<Bitmap?>(null) }
        val bitmapShare: Bitmap
        val imageBitmap: ImageBitmap
        if (imageUri != null) {
            bitmapShare = imageUri!!
            imageBitmap = imageUri!!.asImageBitmap()
            val newBit=CaptureComponentAsBitmap {
                Box() {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "",
                    )
                    LocationScreen()
                }
            }

            val newImageSize=newBit!!.asImageBitmap()
            Image(bitmap = newImageSize, contentDescription = "AA")
            Column(verticalArrangement = Arrangement.Bottom) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FloatingActionButton(
                        onClick = {
                            imageUri = null
                        }
                    ) {
                        Icon(Icons.Filled.Delete, "DeleteButton")
                    }
                    FloatingActionButton(
                        onClick = {

                            ShareUtils.shareImageToOthers(context, "Test", newBit)
                        }
                    ) {
                        Icon(Icons.Filled.Share, "ShareButton")
                    }
                }
            }


        } else {
            val imageCapture = remember {
                ImageCapture.Builder().build()
            }
            var kameraYon: CameraSelector
            kameraYon = CameraSelector.DEFAULT_BACK_CAMERA
            CameraPreview(imageCapture = imageCapture, cameraSelector = kameraYon)
            Column(verticalArrangement = Arrangement.Bottom) {
                Text(text = "$imageUri")
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = {
                    }
                    ) {
                        Icon(Icons.Filled.FlashOff, contentDescription = "Rotate Camera")
                    }
                    CameraCapture(
                        cameraExecutor = cameraExecutor,
                        imageCapture = imageCapture,
                        context = context
                    ) {
                        imageUri = it
                    }
                    IconButton(onClick = {
                        if (kameraYon == (CameraSelector.DEFAULT_BACK_CAMERA)) {
                            kameraYon = CameraSelector.DEFAULT_FRONT_CAMERA
                            Log.e("A", kameraYon.toString())
                        } else {
                            kameraYon = CameraSelector.DEFAULT_BACK_CAMERA
                            Log.e("A", kameraYon.toString())
                        }
                    }
                    ) {
                        Icon(Icons.Filled.Cameraswitch, contentDescription = "Rotate Camera")
                    }
                }
            }
        }
    }
}
@Composable
fun CaptureComponentAsBitmap(component: @Composable () -> Unit): Bitmap? {
    val context = LocalContext.current
    val rootView = (context as? ComponentActivity)?.window?.decorView?.findViewById<View>(android.R.id.content)
    rootView?.let { view ->
        val width = view.width
        val height = view.height

        val bitmap = remember {
            createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

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

fun saveImageLocally(context: Context, image: ImageBitmap): Uri {
    val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File(imagesDir, "temp_image.png")

    try {
        val stream = FileOutputStream(file)
        image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

fun shareImage(context: Context, imageBitmap: ImageBitmap) {
    val cachePath = File.createTempFile("temp_image", null)
    try {
        val stream = FileOutputStream(cachePath)
        imageBitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    val imageUri = FileProvider.getUriForFile(
        context,
        context.applicationContext.packageName + ".provider",
        cachePath
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
}

@Composable
fun Share(bitmap: Bitmap, context: Context) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, bitmap)
    }
    val shareIntent = Intent.createChooser(sendIntent, null)


    Button(onClick = {
        startActivity(context, shareIntent, null)
    }) {
        Icon(imageVector = Icons.Default.Share, contentDescription = null)
        Text("Share", modifier = Modifier.padding(start = 8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PhotoShareSmtTheme {
    }
}