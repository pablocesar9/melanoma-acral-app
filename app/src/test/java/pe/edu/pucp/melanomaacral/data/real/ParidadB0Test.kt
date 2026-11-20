package pe.edu.pucp.melanomaacral.data.real

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO
import kotlin.math.abs

// Paridad del preprocesado y del posprocesado en Kotlin con los de la tesis (cache_lib.py y explicar_final.py),
// con el paquete de paridad que generó la exportación en la VM (27 imágenes elegidas por regla fija).
// Los datos están FUERA del repositorio: Tesis/exportacion-movil/salida/paridad (o -Dparidad.dir=...).
// Si no están, la prueba se omite.
class ParidadB0Test {
    private val dir = File(System.getProperty("paridad.dir") ?: "../../../exportacion-movil/salida/paridad")
    private val casos: List<File> get() = dir.listFiles { f -> f.isDirectory }?.sortedBy { it.name }.orEmpty()

    private fun valores(d: File) = Json.parseToJsonElement(File(d, "valores.json").readText()) as JsonObject

    private fun caja(v: JsonObject) = v.getValue("caja_x0y0x1y1").jsonArray.map { it.jsonPrimitive.int }
        .let { PreprocesadoB0.Caja(it[0], it[1], it[2], it[3]) }

    private fun png(f: File): PreprocesadoB0.Rgb {
        val im = ImageIO.read(f)
        val px = im.getRGB(0, 0, im.width, im.height, null, 0, im.width)
        return PreprocesadoB0.Rgb.desdeArgb(im.width, im.height, px)
    }

    // .npy float32 little-endian (formato de numpy, cabecera v1)
    private fun npy(f: File): FloatArray {
        val b = f.readBytes()
        val lh = (b[8].toInt() and 0xFF) or ((b[9].toInt() and 0xFF) shl 8)
        val cab = String(b, 10, lh)
        require("'<f4'" in cab && "False" in cab) { "npy inesperado: $cab" }
        val bb = ByteBuffer.wrap(b, 10 + lh, b.size - 10 - lh).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        return FloatArray(bb.remaining()).also { bb.get(it) }
    }

    // Orientación EXIF (ImageIO no la aplica; PIL ImageOps.exif_transpose sí).
    private fun orientar(img: PreprocesadoB0.Rgb, o: Int): PreprocesadoB0.Rgb {
        if (o == 1) return img
        val w = img.ancho; val h = img.alto
        val girado = o in 5..8
        val nw = if (girado) h else w; val nh = if (girado) w else h
        val out = IntArray(nw * nh * 3)
        for (y in 0 until nh) for (x in 0 until nw) {
            val (sx, sy) = when (o) {
                2 -> (w - 1 - x) to y
                3 -> (w - 1 - x) to (h - 1 - y)
                4 -> x to (h - 1 - y)
                5 -> y to x
                6 -> y to (h - 1 - x)
                7 -> (w - 1 - y) to (h - 1 - x)
                8 -> (w - 1 - y) to x
                else -> x to y
            }
            for (c in 0..2) out[(y * nw + x) * 3 + c] = img.rgb[(sy * w + sx) * 3 + c]
        }
        return PreprocesadoB0.Rgb(nw, nh, out)
    }

    @Test
    fun mascaraYShadesOfGray_igualQueLaTesis_exacto() {
        assumeTrue("no está el paquete de paridad en ${dir.absolutePath}", casos.isNotEmpty())
        val malos = casos.filter { d ->
            val out = PreprocesadoB0.mascaraYShadesOfGray(png(File(d, "lienzo_384.png")), caja(valores(d)))
            !out.rgb.contentEquals(png(File(d, "entrada_modelo_384.png")).rgb)
        }
        assertTrue("SoG distinto en ${malos.map { it.name }}", malos.isEmpty())
    }

    @Test
    fun preprocesadoCompletoDesdeElJpeg_cercaDeLaTesis() {
        assumeTrue(casos.isNotEmpty())
        var peor = 0; val informe = StringBuilder()
        for (d in casos) {
            val v = valores(d)
            val orig = d.listFiles { f -> f.name.startsWith("original") }!!.single()
            val rgb = orientar(ImageIO.read(orig).let { im ->
                PreprocesadoB0.Rgb.desdeArgb(im.width, im.height, im.getRGB(0, 0, im.width, im.height, null, 0, im.width))
            }, v.getValue("exif_orientacion").jsonPrimitive.int)
            val r = PreprocesadoB0.preprocesar(rgb)
            assertEquals("caja de ${d.name}", caja(v), r.caja)
            assertEquals("interpolación de ${d.name}", v.getValue("interpolacion").jsonPrimitive.content, r.interpolacion)
            val ref = png(File(d, "lienzo_384.png")).rgb
            val dif = r.lienzo.rgb.indices.map { abs(r.lienzo.rgb[it] - ref[it]) }
            val mx = dif.max(); peor = maxOf(peor, mx)
            informe.appendLine("${d.name}: ${r.interpolacion} máx |dif| $mx, valores distintos ${"%.4f".format(dif.count { it > 0 }.toDouble() / dif.size)}")
        }
        println(informe)
        // El decodificador JPEG de Java no es el de PIL: se aceptan diferencias pequeñas de unas unidades.
        assertTrue("diferencia máxima $peor en el lienzo\n$informe", peor <= 8)
    }

    @Test
    fun posprocesadoDelMapa_igualQueExplicarFinal() {
        assumeTrue(casos.isNotEmpty())
        for (d in casos) {
            val v = valores(d)
            val s = SalidaModelo(0f, FloatArray(144), npy(File(d, "mapa_relu_384.npy")), npy(File(d, "mapa_signo_384.npy")))
            val m = PosprocesadoB0.mapaEnCaja(s, caja(v))
            val ref = npy(File(d, "mapa_final_caja.npy"))
            assertEquals(ref.size, m.valores.size)
            val mx = ref.indices.maxOf { abs(ref[it] - m.valores[it]) }
            assertTrue("${d.name}: máx |dif| del mapa $mx", mx < 1e-5f)
            assertEquals("${d.name}: fallback", v.getValue("fallback_sin_relu").jsonPrimitive.boolean, m.fallbackSinRelu)
        }
    }
}
