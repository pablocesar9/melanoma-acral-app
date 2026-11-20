package pe.edu.pucp.melanomaacral.ui.screens.obtainimage

import androidx.camera.core.Camera
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.ui.components.LesionImageView
import pe.edu.pucp.melanomaacral.ui.components.SwitchRow
import java.io.File

// Vista de cámara con CameraX (RF-01, RF-02).
// - Pide el permiso de cámara la primera vez.
// - Muestra una guía circular para centrar la lesión.
// - Si hay una captura previa (control), la superpone semitransparente para repetir
//   el encuadre y la orientación (RF-13).
// - Linterna (luz continua del flash) para iluminar la lesión, si el teléfono la tiene.
// La foto se escribe en `outputFile` (almacenamiento interno de la app); nunca en la galería.
@Composable
fun CameraCapture(
    outputFile: () -> File,
    overlay: LesionImage?,
    onCaptured: (File) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var askedOnce by remember { mutableStateOf(false) }
    // RF-13: la superposición ayuda a repetir el encuadre, pero se puede apagar (por ejemplo, si la foto anterior salió mal).
    var showOverlay by rememberSaveable { mutableStateOf(true) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        askedOnce = true
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (!granted) {
        Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (askedOnce) "Sin permiso de cámara no se puede capturar. Puedes darlo en los ajustes del teléfono o elegir una imagen de la galería."
                else "Se necesita el permiso de cámara."
            )
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) {
                Text("Dar permiso")
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    val hasTorch = camera?.cameraInfo?.hasFlashUnit() == true
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    // La cámara se ata al ciclo de vida de la Activity, que sigue viva al salir de esta pantalla: sin esto, la cámara
    // (y la linterna) quedaban encendidas al pasar a la revisión de la foto. Al salir, se apaga la linterna y se libera.
    DisposableEffect(Unit) {
        onDispose {
            runCatching { camera?.cameraControl?.enableTorch(false) }
            runCatching { provider?.unbindAll() }
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f), contentAlignment = Alignment.Center) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        try {
                            val p = providerFuture.get().also { provider = it }
                            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                            p.unbindAll()
                            camera = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                        } catch (e: Exception) {
                            error = "No se pudo abrir la cámara: ${e.message}"
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
            if (overlay != null && showOverlay) {
                LesionImageView(overlay, label = "Captura previa", alpha = 0.35f, modifier = Modifier.fillMaxSize())
            }
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            if (overlay != null) {
                // El interruptor va sobre la vista previa: así no empuja el botón «Tomar foto» fuera de la pantalla.
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp).fillMaxWidth(0.92f)
                ) {
                    SwitchRow(
                        label = "Superponer la captura anterior",
                        checked = showOverlay,
                        onCheckedChange = { showOverlay = it },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !busy,
                modifier = Modifier.weight(1f),
                onClick = {
                    busy = true
                    val file = outputFile()
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        options,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                busy = false
                                onCaptured(file)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                busy = false
                                error = "No se pudo tomar la foto: ${exception.message}"
                            }
                        }
                    )
                }
            ) { Text(if (busy) "Guardando…" else "Tomar foto") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (hasTorch) {
            OutlinedButton(
                onClick = {
                    torchOn = !torchOn
                    camera?.cameraControl?.enableTorch(torchOn)
                },
                modifier = Modifier.fillMaxWidth().semantics { stateDescription = if (torchOn) "Encendida" else "Apagada" }
            ) {
                Icon(
                    if (torchOn) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(if (torchOn) "Apagar linterna" else "Encender linterna")
            }
        }
    }
}
