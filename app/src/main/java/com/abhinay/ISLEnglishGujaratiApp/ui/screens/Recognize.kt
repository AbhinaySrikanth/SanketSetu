package com.abhinay.ISLEnglishGujaratiApp.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinay.ISLEnglishGujaratiApp.data.SignCatalog
import com.abhinay.ISLEnglishGujaratiApp.ml.EngineStatus
import com.abhinay.ISLEnglishGujaratiApp.ml.Recognition
import com.abhinay.ISLEnglishGujaratiApp.ml.RecognizeViewModel
import com.abhinay.ISLEnglishGujaratiApp.ui.*
import java.util.concurrent.Executors

@Composable
fun RecognizeScreen(app: AppViewModel, onBack: () -> Unit, onSign: (String) -> Unit) {
    val vm: RecognizeViewModel = viewModel()
    val context = LocalContext.current
    val status by vm.status.collectAsStateWithLifecycle()
    var started by rememberSaveable { mutableStateOf(false) }
    var practice by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.onSolved = { app.recordDigit(it) } }
    DisposableEffect(Unit) { onDispose { vm.onSolved = null } }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> granted = ok; denied = !ok }
    val numbers = { onSign("zero") }

    Page("Number sign check", onBack) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            when {
                status == EngineStatus.Unavailable ->
                    Info("Not available in this build", "The recognition model files are not bundled with this version of the app, so the camera check is turned off. You can still learn the number signs.", numbers)
                status == EngineStatus.Failed ->
                    Info("Recognition stopped", "The recognition model could not run on this device.", numbers) { Button(onClick = vm::retry) { Text("Try again") } }
                !started || (!granted && !denied) ->
                    Intro(
                        onStart = { p -> practice = p; if (p) vm.startGame() else vm.stopGame(); started = true; if (!granted) launcher.launch(Manifest.permission.CAMERA) },
                        onBrowse = numbers,
                    )
                !granted ->
                    Info("Camera permission is off", "The number sign check needs the camera. Images stay on your device. You can allow the camera or learn the number signs without it.", numbers) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                            }) { Text("App settings") }
                        }
                    }
                else -> LiveCheck(vm, onSign, numbers, onEndPractice = { vm.stopGame(); practice = false; started = false })
            }
        }
    }
}

@Composable
private fun Info(title: String, body: String, onBrowse: () -> Unit, extra: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyLarge)
        extra?.invoke()
        OutlinedButton(onClick = onBrowse) { Text("Browse number signs") }
    }
}

@Composable
private fun Intro(onStart: (practice: Boolean) -> Unit, onBrowse: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Check a number sign", style = MaterialTheme.typography.titleLarge)
        Text("Hold one hand in front of the camera and make a digit sign from 0 to 9. The app tells you which digit it sees, or says when it is not sure.")
        SectionTitle("What to know")
        Text("• Only the digits 0 to 9 are supported. Other signs and words are not recognised.")
        Text("• Use one hand, in good light, with your hand fully in view.")
        Text("• This is experimental and can be wrong. It is not a translator.")
        Text("• The camera is used only while this screen is open. Images are processed on your device and never stored or sent anywhere.")
        Button(onClick = { onStart(false) }, modifier = Modifier.fillMaxWidth()) { Text("Start camera") }
        OutlinedButton(onClick = { onStart(true) }, modifier = Modifier.fillMaxWidth()) { Text("Practice mode: sign the digit shown") }
        OutlinedButton(onClick = onBrowse, modifier = Modifier.fillMaxWidth()) { Text("Browse number signs instead") }
    }
}

@Composable
private fun LiveCheck(vm: RecognizeViewModel, onSign: (String) -> Unit, onBrowse: () -> Unit, onEndPractice: () -> Unit) {
    val game by vm.game.collectAsStateWithLifecycle()
    var hint by remember { mutableStateOf(false) }
    var correct by remember { mutableStateOf(false) }
    LaunchedEffect(game?.score) { if ((game?.score ?: 0) > 0) { correct = true; kotlinx.coroutines.delay(1500); correct = false } }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val result by vm.result.collectAsStateWithLifecycle()
    var cameraError by remember { mutableStateOf<String?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(owner) {
        val analyzer = vm.analyzer()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                if (analyzer != null) analysis.setAnalyzer(executor, analyzer)
                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                provider.unbindAll()
                provider.bindToLifecycle(owner, selector, preview, analysis)
            } catch (e: Exception) {
                cameraError = "The camera could not be started."
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { future.get().unbindAll() }
            executor.shutdown()
        }
    }

    val message = when (val r = result) {
        Recognition.Starting -> "Looking for a hand…"
        Recognition.NoHand -> "No hand detected. Hold one hand in view, in good light."
        Recognition.MultipleHands -> "More than one hand in view. Show one hand."
        Recognition.NotConfident -> "Not sure. Hold the sign steady. It may not be a digit from 0 to 9."
        is Recognition.Match -> "Looks like ${r.digit}"
    }
    val match = (result as? Recognition.Match)?.let { m -> SignCatalog.all.firstOrNull { it.digit == m.digit } }

    if (cameraError != null) {
        Info("Camera unavailable", cameraError!!, onBrowse)
        return
    }
    Column(Modifier.fillMaxSize()) {
        AndroidView({ previewView }, Modifier.fillMaxWidth().weight(1f))
        val g = game
        if (g != null) Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Show the sign for ${g.target}", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Text(
                when {
                    correct -> "Correct! Next digit."
                    g.seen != null -> "That looks like ${g.seen}. Keep trying."
                    else -> message
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text("Score: ${g.score}", style = MaterialTheme.typography.labelLarge)
            if (hint) SignCatalog.all.firstOrNull { it.digit == g.target }?.let { SignMedia(it, false, Modifier.fillMaxWidth().height(140.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { hint = !hint }) { Text(if (hint) "Hide hint" else "Show hint") }
                OutlinedButton(onClick = vm::skip) { Text("Skip") }
                Button(onClick = onEndPractice) { Text("Finish") }
            }
            Text("Experimental. Digits 0 to 9 only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (match != null) Button(onClick = { onSign(match.id) }) { Text("View sign") }
                OutlinedButton(onClick = vm::retry) { Text("Try again") }
            }
            Text("Experimental. Digits 0 to 9 only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
