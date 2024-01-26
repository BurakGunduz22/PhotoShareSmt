@file:OptIn(ExperimentalPermissionsApi::class)

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset

@SuppressLint("MissingPermission")
@Composable
fun LocationScreen(hasRequiredPermissions: Boolean) :String{
    val activity = LocalContext.current as ComponentActivity
    val locationPermissionState by remember {
        mutableStateOf(hasRequiredPermissions)
    }

    var location by remember { mutableStateOf("Location: Unknown") }

    LaunchedEffect(locationPermissionState) {
        if (locationPermissionState.equals(true)) {
            // Permission granted, fetch location
            location = fetchLocation(activity)
        } else {
            location = "Permission Denied"
        }
    }

    DraggableCard(location)

    DisposableEffect(key1 = locationPermissionState) {
        onDispose {
            // Clean up if necessary
        }
    }
    return location
}
@Composable
fun DraggableCard(lokasyon:String) {
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
                },
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

    @SuppressLint("MissingPermission")
private suspend fun fetchLocation(context: Context): String {
    return suspendCancellableCoroutine { continuation ->
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val geocoder = Geocoder(context)
                try {
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null) {
                        if (addresses.isNotEmpty()) {
                            continuation.resume(addresses[0].getAddressLine(0))
                        } else {
                            continuation.resume("Location: Unknown") {}
                        }
                    }
                } catch (e: IOException) {
                    continuation.resumeWithException(e)
                }
            } else {
                continuation.resume("Location: Unknown") {}
            }
        }.addOnFailureListener { e ->
            continuation.resumeWithException(e)
        }
    }
}
