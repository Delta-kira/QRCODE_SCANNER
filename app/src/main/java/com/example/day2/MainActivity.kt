package com.example.day2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.day2.ui.theme.Day2Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Day2Theme {
                var code by remember {
                    mutableStateOf("")
                }
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val cameraProviderFuture = remember {
                    ProcessCameraProvider.getInstance(context)
                }
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { granted ->
                        hasPermission = granted
                    }
                )
                LaunchedEffect(key1 = true) {
                    launcher.launch(Manifest.permission.CAMERA)
                }

                Column(
                    modifier = Modifier.fillMaxSize() // Occupy entire screen
                ) {
                    if (hasPermission) {
                        AndroidView(factory = { context ->
                            val previewView = PreviewView(context)
                            val preview = androidx.camera.core.Preview.Builder().build()
                            val selector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(context),
                                QrCodeAnalyzer { result ->
                                    code = result
                                }
                            )
                            try {
                                cameraProviderFuture.get().bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            previewView
                        }, modifier = Modifier.weight(1f)
                        )
                        ClickableText(context = context, text = code)
                    }
                }
            }
        }
    }
}



@Composable
fun ClickableText(context: Context, text: String) {
    Column {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clickable {
                    if (isUrl(text)) {
                        // Open the link in a browser when clicked
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(text))
                        ContextCompat.startActivity(context, intent, null)
                    }
                    // Otherwise, do nothing
                }
        )
        if (isUrl(text)) {
            Text(
                text = "Click to open",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 32.dp)
            )
        }
    }
}

// Function to check if the given text is a URL
private fun isUrl(text: String): Boolean {
    return try {
        val uri = Uri.parse(text)
        uri.scheme != null
    } catch (e: Exception) {
        false
    }
}