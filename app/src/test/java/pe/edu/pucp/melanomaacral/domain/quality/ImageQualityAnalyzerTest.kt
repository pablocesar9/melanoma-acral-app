package pe.edu.pucp.melanomaacral.domain.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.pucp.melanomaacral.domain.model.LightingProblem

// RF-03: la revisión determinista distingue imágenes nítidas/borrosas, oscuras/quemadas y pequeñas.
class ImageQualityAnalyzerTest {

    private val w = 128
    private val h = 128

    // Tablero de ajedrez de 4 px con dos niveles de gris: mucho borde = nítida.
    private fun checker(low: Int = 60, high: Int = 190) =
        IntArray(w * h) { i -> if (((i % w) / 4 + (i / w) / 4) % 2 == 0) low else high }

    private fun flat(v: Int) = IntArray(w * h) { v }

    // Degradado suave: casi sin borde (equivale a una imagen muy borrosa).
    private fun gradient() = IntArray(w * h) { i -> 80 + (i % w) / 2 }

    @Test
    fun imagenConBordes_esNitida() {
        val r = ImageQualityAnalyzer.analyze(checker(), w, h, originalWidth = 1000, originalHeight = 1000)
        assertTrue(r.focusOk)
        assertTrue(r.allOk)
        assertTrue(r.isHeuristic)
        assertFalse(r.isSimulated)
    }

    @Test
    fun degradadoSuave_esBorroso() {
        val r = ImageQualityAnalyzer.analyze(gradient(), w, h, 1000, 1000)
        assertFalse(r.focusOk)
        assertFalse(r.allOk)
    }

    @Test
    fun imagenNegra_estaMuyOscura() {
        val r = ImageQualityAnalyzer.analyze(flat(5), w, h, 1000, 1000)
        assertFalse(r.lightingOk)
        assertEquals(LightingProblem.TOO_DARK, r.lightingProblem)
    }

    @Test
    fun imagenBlanca_estaSobreexpuesta() {
        val r = ImageQualityAnalyzer.analyze(flat(252), w, h, 1000, 1000)
        assertEquals(LightingProblem.TOO_BRIGHT, r.lightingProblem)
    }

    @Test
    fun campoUniforme_noTieneEncuadre() {
        val r = ImageQualityAnalyzer.analyze(flat(120), w, h, 1000, 1000)
        assertFalse(r.framingOk)
    }

    @Test
    fun resolucionBaja_seRechaza() {
        val r = ImageQualityAnalyzer.analyze(checker(), w, h, originalWidth = 640, originalHeight = 300)
        assertFalse(r.resolutionOk)
        assertFalse(r.allOk)
    }

    @Test
    fun resolucionEnElLimite_seAcepta() {
        val r = ImageQualityAnalyzer.analyze(checker(), w, h, originalWidth = 800, originalHeight = ImageQualityAnalyzer.MIN_SHORT_SIDE_PX)
        assertTrue(r.resolutionOk)
    }

    @Test
    fun varianzaDelLaplaciano_esCeroEnUnaImagenPlana() {
        assertEquals(0f, ImageQualityAnalyzer.laplacianVariance(flat(100), w, h), 0.0001f)
    }

    @Test
    fun imagenMuyPequena_noFalla() {
        assertEquals(0f, ImageQualityAnalyzer.laplacianVariance(IntArray(4), 2, 2), 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun dimensionesInconsistentes_sonUnError() {
        ImageQualityAnalyzer.analyze(IntArray(10), 4, 4)
    }

    @Test
    fun metricasQuedanEnElInforme() {
        val r = ImageQualityAnalyzer.analyze(checker(60, 190), w, h, 1000, 1000)
        assertEquals(125f, r.meanLuma!!, 1f)
        assertTrue(r.sharpness!! > ImageQualityAnalyzer.MIN_SHARPNESS)
    }

    // Foto con poca luz de una cámara que subió la exposición: brillo ~60 (antes pasaba con el mínimo de 50).
    @Test
    fun brilloMedioDe60_esPocaLuz() {
        val dim = IntArray(w * h) { i -> if (((i % w) / 4 + (i / w) / 4) % 2 == 0) 40 else 80 }
        val r = ImageQualityAnalyzer.analyze(dim, w, h, 1000, 1000)
        assertEquals(LightingProblem.TOO_DARK, r.lightingProblem)
        assertFalse(r.lightingOk)
    }

    // Ruido de cámara (sigma 4) sobre un gris medio: la varianza del laplaciano es enorme (parece nítida) y no hay
    // bordes reales. Desde la calibración del 2026-11-17 (umbrales en el percentil 2 de 600 dermatoscopias), la nitidez
    // tras suavizar (~8) ya no lo separa de una dermatoscopia lisa real (mediana 3,5 en BALD): la imagen la rechaza el
    // encuadre, porque el centro no tiene contenido (desviación ~4 < 8).
    @Test
    fun ruidoSinBordes_loRechazaElEncuadre() {
        val rnd = java.util.Random(1)
        val noisy = IntArray(w * h) { (128 + rnd.nextGaussian() * 4).toInt().coerceIn(0, 255) }
        val r = ImageQualityAnalyzer.analyze(noisy, w, h, 1000, 1000)
        assertTrue(r.sharpness!! > ImageQualityAnalyzer.MIN_SHARPNESS)
        assertFalse(r.framingOk)
        assertFalse(r.allOk)
    }

    @Test
    fun fotoDeContexto_noExigeEncuadreCentrado() {
        val r = ImageQualityAnalyzer.analyze(flat(120), w, h, 1000, 1000, checkFraming = false)
        assertTrue(r.framingOk)
    }
}
