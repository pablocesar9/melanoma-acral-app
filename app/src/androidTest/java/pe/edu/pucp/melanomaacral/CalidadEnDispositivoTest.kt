package pe.edu.pucp.melanomaacral

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.edu.pucp.melanomaacral.data.files.AndroidImageStorage
import pe.edu.pucp.melanomaacral.data.files.HeuristicImageQualityRepository
import pe.edu.pucp.melanomaacral.domain.model.ImageKind
import pe.edu.pucp.melanomaacral.domain.quality.ImageQualityAnalyzer
import java.io.File

// Calibración de RF-03 con fotos del teléfono. Cada foto sigue el mismo camino que una foto de la cámara en la app:
// AndroidImageStorage la normaliza (orientación, lado mayor 2048, JPEG 92) y HeuristicImageQualityRepository la reduce
// a 512 px y calcula el brillo; ImageQualityAnalyzer.measure da las medidas sin umbrales.
//
// Preparación: copiar las fotos a la caché de la app, en calidad/<tanda>/<tipo>/*.jpg (adb no puede escribir en
// Android/data desde Android 11):
//   tar -cf - tanda1 tanda2 | adb exec-in run-as pe.edu.pucp.melanomaacral sh -c 'mkdir -p cache/calidad && tar -xf - -C cache/calidad'
// Resultado: cache/calidad/medidas.csv (adb exec-out run-as pe.edu.pucp.melanomaacral cat cache/calidad/medidas.csv),
// y el número de fotos medidas en la salida de la instrumentación.
// Sin la carpeta, la prueba se salta. Las copias normalizadas se borran al terminar.
@RunWith(AndroidJUnit4::class)
class CalidadEnDispositivoTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun medirFotosDeCalibracion() = runBlocking {
        val dir = File(context.cacheDir, "calidad")
        assumeTrue(dir.isDirectory)
        val storage = AndroidImageStorage(context)
        val quality = HeuristicImageQualityRepository()
        val fotos = dir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg") }
            .sortedBy { it.path }.toList()
        val csv = StringBuilder(
            "tanda,tipo,archivo,ancho,alto,nitidez,nitidez_suavizada,brillo_medio,fraccion_oscura,fraccion_clara,desv_centro,clinica_ok,clinica_foco,clinica_luz,dermo_ok,dermo_foco,dermo_luz,dermo_encuadre\n"
        )
        for (foto in fotos) {
            val rel = foto.relativeTo(dir).path.split('/')
            val raw = storage.newCameraFile()
            foto.copyTo(raw, overwrite = true)
            val image = storage.registerCameraFile(raw)
            try {
                val l = quality.luma(File(image.uri))
                val m = ImageQualityAnalyzer.measure(l.luma, l.width, l.height)
                // Decisión real de la app para cada clase de imagen.
                val clin = quality.check(image, ImageKind.CONTEXT)
                val derm = quality.check(image, ImageKind.DERMOSCOPIC)
                csv.append(
                    listOf(
                        rel.getOrElse(0) { "" }, rel.getOrElse(1) { "" }, foto.name, l.originalWidth, l.originalHeight,
                        m.sharpness, m.sharpnessDenoised, m.meanLuma, m.darkFraction, m.brightFraction, m.centerStdDev,
                        clin.allOk, clin.focusOk, clin.lightingProblem, derm.allOk, derm.focusOk, derm.lightingProblem,
                        derm.framingOk
                    ).joinToString(",")
                ).append('\n')
            } finally {
                storage.delete(image)
            }
        }
        File(dir, "medidas.csv").writeText(csv.toString())
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0, android.os.Bundle().apply { putString("calidad", "${fotos.size} fotos medidas") }
        )
    }
}
