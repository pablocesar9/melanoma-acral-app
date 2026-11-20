package pe.edu.pucp.melanomaacral.data.real

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

// Preprocesado del modelo final B0: réplica en Kotlin puro (sin Android, se prueba en la JVM) de
// cache_lib.py de la tesis (sha256 c8c6bf0a). Pasos, en el mismo orden:
//   1. (fuera de aquí) decodificar la imagen con la orientación EXIF ya aplicada;
//   2. escalar para que el lado largo mida 384: INTER_AREA si se reduce, INTER_CUBIC si se amplía (como OpenCV);
//   3. letterbox: lienzo de 384 × 384 relleno con (124, 116, 104) y la imagen centrada;
//   4. región válida = dentro de la caja de la imagen y a distancia <= 144 del centro;
//   5. Shades of Gray (p = 6) estimado solo con los píxeles válidos; fuera de la región, el relleno.
// La salida es la entrada del modelo: 384 × 384 × 3 valores 0-255 (RGB, fila por fila).
object PreprocesadoB0 {
    const val SIZE = 384
    const val RADIO = 144
    val RELLENO = intArrayOf(124, 116, 104)
    private const val SOG_P = 6.0

    // Imagen RGB de 8 bits, fila por fila: rgb[(y * ancho + x) * 3 + canal].
    class Rgb(val ancho: Int, val alto: Int, val rgb: IntArray) {
        init { require(rgb.size == ancho * alto * 3) }

        companion object {
            // Desde píxeles ARGB (Bitmap.getPixels o BufferedImage.getRGB).
            fun desdeArgb(ancho: Int, alto: Int, argb: IntArray): Rgb {
                val out = IntArray(ancho * alto * 3)
                for (i in argb.indices) {
                    val p = argb[i]
                    out[i * 3] = (p shr 16) and 0xFF; out[i * 3 + 1] = (p shr 8) and 0xFF; out[i * 3 + 2] = p and 0xFF
                }
                return Rgb(ancho, alto, out)
            }
        }
    }

    // Caja de la imagen dentro del lienzo: [x0, x1) × [y0, y1).
    data class Caja(val x0: Int, val y0: Int, val x1: Int, val y1: Int)

    class Resultado(val lienzo: Rgb, val entrada: Rgb, val caja: Caja, val escala: Double, val interpolacion: String)

    fun preprocesar(imagen: Rgb): Resultado {
        val (chica, escala) = escalarLadoLargo(imagen)
        val (lienzo, caja) = colocar(chica)
        return Resultado(lienzo, mascaraYShadesOfGray(lienzo, caja), caja, escala, if (escala < 1) "INTER_AREA" else "INTER_CUBIC")
    }

    // round() de Python 3: redondeo al par en las mitades (Math.rint hace lo mismo).
    private fun redondeoPython(v: Double): Int = Math.rint(v).toInt()

    // cache_lib.resize_long
    fun escalarLadoLargo(img: Rgb): Pair<Rgb, Double> {
        val s = SIZE.toDouble() / max(img.ancho, img.alto)
        val nw = max(1, redondeoPython(img.ancho * s)); val nh = max(1, redondeoPython(img.alto * s))
        val out = if (s < 1) area(img, nw, nh) else cubica(img, nw, nh)
        return out to s
    }

    // cache_lib.place
    fun colocar(img: Rgb): Pair<Rgb, Caja> {
        val top = (SIZE - img.alto) / 2; val left = (SIZE - img.ancho) / 2
        val out = IntArray(SIZE * SIZE * 3)
        for (i in 0 until SIZE * SIZE) for (c in 0..2) out[i * 3 + c] = RELLENO[c]
        for (y in 0 until img.alto) for (x in 0 until img.ancho) for (c in 0..2)
            out[((top + y) * SIZE + left + x) * 3 + c] = img.rgb[(y * img.ancho + x) * 3 + c]
        return Rgb(SIZE, SIZE, out) to Caja(left, top, left + img.ancho, top + img.alto)
    }

    // cache_lib.valid_mask con R = 144. RMAP se guarda en float32 en Python: la comparación se hace en Float.
    fun regionValida(caja: Caja, radio: Int = RADIO): BooleanArray {
        val cen = (SIZE - 1) / 2.0
        val m = BooleanArray(SIZE * SIZE)
        for (y in caja.y0 until caja.y1) for (x in caja.x0 until caja.x1)
            m[y * SIZE + x] = hypot(x - cen, y - cen).toFloat() <= radio.toFloat()
        return m
    }

    // cache_lib.finalize (parte sog) y shades_of_gray: iluminante con los píxeles válidos, en double.
    fun mascaraYShadesOfGray(lienzo: Rgb, caja: Caja): Rgb {
        val valida = regionValida(caja)
        val suma = DoubleArray(3); var n = 0
        for (i in 0 until SIZE * SIZE) if (valida[i]) {
            n++
            for (c in 0..2) suma[c] += lienzo.rgb[i * 3 + c].toDouble().pow(SOG_P)
        }
        val ilum = DoubleArray(3) { (suma[it] / n).pow(1.0 / SOG_P) }
        val norma = sqrt(ilum.sumOf { it * it }) + 1e-9
        val div = DoubleArray(3) { ilum[it] / norma * sqrt(3.0) + 1e-9 }
        val out = IntArray(SIZE * SIZE * 3)
        for (i in 0 until SIZE * SIZE) for (c in 0..2) {
            out[i * 3 + c] = if (valida[i]) (lienzo.rgb[i * 3 + c] / div[c]).coerceIn(0.0, 255.0).toInt() else RELLENO[c]
        }
        return Rgb(SIZE, SIZE, out)
    }

    // ---- INTER_AREA de OpenCV (resize.cpp): promedio ponderado por el área que cubre cada píxel de destino.
    // Como OpenCV: pesos y sumas en float32 y redondeo al par; si los dos factores son enteros usa el camino
    // rápido (factor 2: (suma + 2) >> 2; otro entero: suma * (1 / área) redondeado al par).
    private class Peso(val src: Int, val w: Float)

    private fun tablaArea(nSrc: Int, nDst: Int, escala: Double): Array<List<Peso>> = Array(nDst) { d ->
        val f1 = d * escala; val f2 = f1 + escala
        val celda = min(escala, nSrc - f1)
        val s2 = min(floor(f2).toInt(), nSrc - 1); val s1 = min(ceil(f1).toInt(), s2)
        val l = ArrayList<Peso>()
        if (s1 - f1 > 1e-3) l += Peso(s1 - 1, ((s1 - f1) / celda).toFloat())
        for (s in s1 until s2) l += Peso(s, (1.0 / celda).toFloat())
        if (f2 - s2 > 1e-3) l += Peso(s2, (min(min(f2 - s2, 1.0), celda) / celda).toFloat())
        l
    }

    fun area(img: Rgb, nw: Int, nh: Int): Rgb {
        val ex = 1.0 / (nw.toDouble() / img.ancho); val ey = 1.0 / (nh.toDouble() / img.alto)
        val ix = Math.rint(ex).toInt(); val iy = Math.rint(ey).toInt()
        if (kotlin.math.abs(ex - ix) < Math.ulp(1.0) && kotlin.math.abs(ey - iy) < Math.ulp(1.0)) return areaRapida(img, nw, nh, ix, iy)
        val tx = tablaArea(img.ancho, nw, ex); val ty = tablaArea(img.alto, nh, ey)
        val out = IntArray(nw * nh * 3)
        val buf = FloatArray(nw * 3)
        for (dy in 0 until nh) {
            val suma = FloatArray(nw * 3); var primero = true
            for (p in ty[dy]) {
                java.util.Arrays.fill(buf, 0f)
                val base = p.src * img.ancho
                for (dx in 0 until nw) for (q in tx[dx]) for (c in 0..2)
                    buf[dx * 3 + c] = buf[dx * 3 + c] + img.rgb[(base + q.src) * 3 + c] * q.w
                if (primero) { for (k in suma.indices) suma[k] = p.w * buf[k]; primero = false }
                else for (k in suma.indices) suma[k] = suma[k] + p.w * buf[k]
            }
            for (k in suma.indices) out[dy * nw * 3 + k] = Math.rint(suma[k].toDouble()).toInt().coerceIn(0, 255)
        }
        return Rgb(nw, nh, out)
    }

    private fun areaRapida(img: Rgb, nw: Int, nh: Int, fx: Int, fy: Int): Rgb {
        val out = IntArray(nw * nh * 3); val escala = 1f / (fx * fy)
        for (dy in 0 until nh) for (dx in 0 until nw) for (c in 0..2) {
            var s = 0
            for (yy in 0 until fy) for (xx in 0 until fx) s += img.rgb[((dy * fy + yy) * img.ancho + dx * fx + xx) * 3 + c]
            out[(dy * nw + dx) * 3 + c] = if (fx == 2 && fy == 2) (s + 2) shr 2 else Math.rint((s * escala).toDouble()).toInt().coerceIn(0, 255)
        }
        return Rgb(nw, nh, out)
    }

    // ---- INTER_CUBIC de OpenCV para 8 bits: a = -0.75, centros de píxel, borde replicado y aritmética de
    // punto fijo (coeficientes × 2048 redondeados; resultado (suma + 2^21) >> 22).
    private fun coefCubicos(x: Float): IntArray {
        val a = -0.75f
        val c0 = ((a * (x + 1) - 5 * a) * (x + 1) + 8 * a) * (x + 1) - 4 * a
        val c1 = ((a + 2) * x - (a + 3)) * x * x + 1
        val c2 = ((a + 2) * (1 - x) - (a + 3)) * (1 - x) * (1 - x) + 1
        val c3 = 1f - c0 - c1 - c2
        return floatArrayOf(c0, c1, c2, c3).map { Math.rint((it * 2048f).toDouble()).toInt() }.toIntArray()
    }

    fun cubica(img: Rgb, nw: Int, nh: Int): Rgb {
        if (nw == img.ancho && nh == img.alto) return Rgb(nw, nh, img.rgb.copyOf())   // OpenCV copia sin interpolar
        val sx = 1.0 / (nw.toDouble() / img.ancho); val sy = 1.0 / (nh.toDouble() / img.alto)
        fun tabla(n: Int, s: Double, nSrc: Int) = Array(n) { d ->
            var f = ((d + 0.5) * s - 0.5).toFloat(); val i = floor(f).toInt(); f -= i
            Pair(IntArray(4) { k -> (i - 1 + k).coerceIn(0, nSrc - 1) }, coefCubicos(f))
        }
        val tx = tabla(nw, sx, img.ancho); val ty = tabla(nh, sy, img.alto)
        val h = Array(img.alto) { y ->
            IntArray(nw * 3) { k ->
                val dx = k / 3; val c = k % 3; val (idx, w) = tx[dx]
                (0..3).sumOf { img.rgb[(y * img.ancho + idx[it]) * 3 + c] * w[it] }
            }
        }
        val out = IntArray(nw * nh * 3)
        for (dy in 0 until nh) {
            val (idx, w) = ty[dy]
            for (k in 0 until nw * 3) {
                val s = (0..3).sumOf { h[idx[it]][k].toLong() * w[it] }
                out[dy * nw * 3 + k] = ((s + (1L shl 21)) shr 22).toInt().coerceIn(0, 255)
            }
        }
        return Rgb(nw, nh, out)
    }
}
