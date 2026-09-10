package com.universallive.app.streaming.preview

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Text

object AndroidPreviewBridge {
    private var latest by mutableStateOf<Bitmap?>(null)
    @Volatile var requested: Boolean = false
        private set
    val frame: Bitmap? get() = latest

    fun publish(bitmap: Bitmap) {
        Handler(Looper.getMainLooper()).post {
            val previous = latest
            latest = bitmap
            if (previous !== bitmap && previous?.isRecycled == false) previous.recycle()
        }
    }

    internal fun setRequested(value: Boolean) { requested = value }
}

@Composable
actual fun LiveOutputPreview(modifier: Modifier) {
    DisposableEffect(Unit) {
        AndroidPreviewBridge.setRequested(true)
        onDispose { AndroidPreviewBridge.setRequested(false) }
    }
    val bitmap = AndroidPreviewBridge.frame
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (bitmap != null && !bitmap.isRecycled) {
            Image(bitmap.asImageBitmap(), contentDescription = "Live composited output", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            Text("Waiting for composited preview…", color = Color(0xFF8D929A))
        }
    }
}
