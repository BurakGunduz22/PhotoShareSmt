package com.android.example.photosharesmt

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview2(
    controller: LifecycleCameraController,
    modifier: Modifier=Modifier,
){
    val lifecycleowner = LocalLifecycleOwner.current
    AndroidView(
        factory = {
            PreviewView(it).apply { this.controller=controller
                controller.bindToLifecycle(lifecycleowner) }
                  },
        modifier=modifier
        )

}