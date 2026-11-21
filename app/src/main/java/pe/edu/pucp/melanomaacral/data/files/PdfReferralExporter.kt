package pe.edu.pucp.melanomaacral.data.files

import pe.edu.pucp.melanomaacral.ui.screens.decision.label
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.edu.pucp.melanomaacral.data.real.EscalaJet
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.domain.model.ExplanationKind
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.ReferralExport
import pe.edu.pucp.melanomaacral.domain.model.ReferralPackage
import pe.edu.pucp.melanomaacral.domain.repository.ExplanationRepository
import pe.edu.pucp.melanomaacral.domain.repository.ReferralExporter
import pe.edu.pucp.melanomaacral.ui.components.ModelScore
import pe.edu.pucp.melanomaacral.ui.description
import pe.edu.pucp.melanomaacral.ui.formatted
import pe.edu.pucp.melanomaacral.ui.label
import pe.edu.pucp.melanomaacral.ui.screens.referral.ReferralText
import pe.edu.pucp.melanomaacral.ui.yesNo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Documento de derivación (RF-15) con estructura de informe: encabezado, datos de la derivación, recomendación,
// resultado del modelo frente a su umbral, datos clínicos, anotaciones y una página de imágenes con el mapa de calor.
// Además deja las imágenes como archivos aparte (JPEG) para compartirlas junto con el PDF. Todo se genera en la
// caché de la app (cache/referrals), la única carpeta que se comparte con FileProvider; las imágenes originales siguen
// solo en el almacenamiento interno (RNF-05). Usa android.graphics.pdf.PdfDocument: sin librerías.
class PdfReferralExporter(
    private val context: Context,
    private val explanations: ExplanationRepository
) : ReferralExporter {

    override suspend fun exportPdf(pkg: ReferralPackage): ReferralExport = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "referrals").also { it.mkdirs() }
        dir.listFiles()?.forEach { it.delete() }   // no acumular derivaciones antiguas
        val ev = pkg.latest
        val base = "${pkg.lesionId}_${ev.id}_${ev.date}"

        // Imágenes sueltas: contexto, primer plano, dermoscópica, dermoscópica con el mapa y la anterior (si hay).
        val images = mutableListOf<Pair<String, File>>()   // rótulo, archivo
        ev.images.context?.let { copy(it.uri, File(dir, "${base}_foto-clinica.jpg"))?.let { f -> images += "Foto clínica de contexto" to f } }
        ev.images.closeUp?.let { copy(it.uri, File(dir, "${base}_primer-plano.jpg"))?.let { f -> images += "Foto clínica de primer plano" to f } }
        copy(ev.images.dermoscopic.uri, File(dir, "${base}_dermoscopica.jpg"))?.let { images += "Imagen dermoscópica" to it }
        val kind = if (ev.prediction.suggestsMelanoma) ExplanationKind.FOR_MELANOMA else ExplanationKind.AGAINST_MELANOMA
        val mapCaption = if (kind == ExplanationKind.FOR_MELANOMA) "Mapa de calor: zonas que llevaron al modelo a sugerir melanoma"
        else "Mapa de calor: zonas que llevaron al modelo a no sugerir melanoma"
        if (!ev.prediction.isOutOfScope) {
            runCatching {
                val exp = explanations.explain(ev.images.dermoscopic, kind)
                val f = overlay(ev.images.dermoscopic.uri, exp.grid, File(dir, "${base}_dermoscopica-mapa.jpg"))
                (mapCaption + if (exp.isSimulated) " (SIMULADO)" else "") to f
            }.getOrNull()?.let { images += it }
        }
        pkg.previous?.let { prev ->
            copy(prev.images.dermoscopic.uri, File(dir, "${base}_dermoscopica-anterior-${prev.date}.jpg"))
                ?.let { images += "Dermoscópica anterior (${prev.date.formatted()})" to it }
        }

        val pdf = File(dir, "derivacion_$base.pdf")
        val doc = PdfDocument()
        try {
            Writer(doc, ev).apply {
                firstPage(pkg)
                imagesPage(images)
                finish()
            }
            FileOutputStream(pdf).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        fun uri(f: File) = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f).toString()
        ReferralExport(pdf = uri(pdf), images = images.map { uri(it.second) })
    }

    // ------------------------------------------------------------------------------------------------ imágenes
    private fun decode(path: String, side: Int = IMAGE_SIDE_PX): Bitmap? {
        if (!File(path).isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        SecureImages.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        val o = BitmapFactory.Options().apply { inSampleSize = ImageScaling.sampleSize(bounds.outWidth, bounds.outHeight, side) }
        return SecureImages.decodeFile(path, o)
    }

    private fun copy(path: String, dst: File): File? {
        val src = File(path)
        if (!src.isFile) return null
        SecureImages.copyPlain(src, dst)   // copia en claro, solo en la caché que se comparte
        return dst
    }

    // La dermoscópica con el mapa encima: escala jet, opacidad 0,45, suavizado bilineal (como la pantalla y la tesis).
    private fun overlay(path: String, grid: List<List<Float>>, dst: File): File {
        val img = decode(path, 1600) ?: error("imagen no disponible")
        val out = img.copy(Bitmap.Config.ARGB_8888, true); img.recycle()
        val h = grid.size; val w = grid.firstOrNull()?.size ?: 0
        require(w > 0 && h > 0) { "mapa vacío" }
        val px = IntArray(w * h)
        grid.forEachIndexed { r, row -> row.forEachIndexed { c, v -> px[r * w + c] = EscalaJet.argb(v, EscalaJet.OPACIDAD) } }
        val small = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(small, null, RectF(0f, 0f, out.width.toFloat(), out.height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        small.recycle()
        FileOutputStream(dst).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        out.recycle()
        return dst
    }

    // ------------------------------------------------------------------------------------------------ PDF
    private inner class Writer(private val doc: PdfDocument, private val ev: Evaluation) {
        private var page: PdfDocument.Page? = null
        private var n = 0
        private var y = 0f
        private val c: Canvas get() = page!!.canvas

        private fun paint(size: Float, bold: Boolean = false, color: Int = INK) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

        private fun newPage() {
            page?.let { footer(); doc.finishPage(it) }
            n++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, n).create())
            header()
        }

        fun finish() { page?.let { footer(); doc.finishPage(it) }; page = null }

        private fun ensure(h: Float) { if (y + h > PAGE_H - BOTTOM) newPage() }

        private fun header() {
            c.drawRect(0f, 0f, PAGE_W.toFloat(), 64f, Paint().apply { color = BRAND })
            c.drawText("Derivación — lesión pigmentada acral", M, 30f, paint(16f, true, Color.WHITE))
            c.drawText("Prototipo de apoyo a la decisión. No reemplaza el juicio clínico ni la evaluación del especialista.",
                M, 50f, paint(8.5f, color = Color.WHITE))
            y = 90f
            if (ev.prediction.isSimulated) {
                c.drawRect(M, y - 12f, PAGE_W - M, y + 8f, Paint().apply { color = WARN })
                c.drawText(ReferralText.SIMULATED_WARNING, M + 6f, y + 2f, paint(9f, true, Color.WHITE))
                y += 24f
            }
        }

        private fun footer() {
            val p = paint(7.5f, color = MUTED)
            val gen = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date())
            c.drawLine(M, PAGE_H - 34f, PAGE_W - M, PAGE_H - 34f, Paint().apply { color = RULE })
            c.drawText("Modelo: ${ev.prediction.modelVersion} · Evaluación ${ev.id} · Generado el $gen", M, PAGE_H - 22f, p)
            val t = "Página $n"; c.drawText(t, PAGE_W - M - p.measureText(t), PAGE_H - 22f, p)
        }

        private fun section(title: String) {
            ensure(44f)
            y += 10f
            c.drawText(title.uppercase(), M, y, paint(9.5f, true, BRAND))
            y += 5f
            c.drawLine(M, y, PAGE_W - M, y, Paint().apply { color = BRAND; strokeWidth = 1f })
            y += 16f
        }

        private fun paragraph(text: String, size: Float = 9.5f, bold: Boolean = false, color: Int = INK) {
            val p = paint(size, bold, color)
            val l = StaticLayout.Builder.obtain(text, 0, text.length, p, (PAGE_W - 2 * M).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(1.5f, 1f).build()
            ensure(l.height.toFloat())
            c.save(); c.translate(M, y - size); l.draw(c); c.restore()
            y += l.height + 6f
        }

        // Tabla de dos columnas (rótulo y valor) con filas alternadas.
        private fun table(rows: List<Pair<String, String>>) {
            val lp = paint(9f, true, MUTED); val vp = paint(9.5f)
            val labelW = 170f; val valueW = PAGE_W - 2 * M - labelW - 8f
            rows.forEachIndexed { i, (k, v) ->
                val l = StaticLayout.Builder.obtain(v, 0, v.length, vp, valueW.toInt()).build()
                val h = maxOf(18f, l.height + 8f)
                ensure(h)
                if (i % 2 == 0) c.drawRect(M, y - 12f, PAGE_W - M, y - 12f + h, Paint().apply { color = ZEBRA })
                c.drawText(k, M + 6f, y, lp)
                c.save(); c.translate(M + labelW, y - 9.5f); l.draw(c); c.restore()
                y += h
            }
            y += 4f
        }

        fun firstPage(pkg: ReferralPackage) {
            newPage()
            val cl = ev.clinicalData; val rec = ev.recommendation
            section("Datos de la derivación")
            table(buildList {
                add("Establecimiento de origen" to "______________________________")
                add("Profesional que deriva" to "______________________________")
                add("Destino" to "Según el flujo de referencia de su establecimiento")
                add("Código del paciente" to "${pkg.patientId} (código del establecimiento; sin nombre ni documento)")
                add("Lesión" to pkg.lesionId)
                add("Fecha de la evaluación" to ev.date.formatted())
                ev.editedAtMillis?.let { add("Editada el" to SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(it))) }
            })

            section("Recomendación")
            val refer = rec.action == ManagementAction.REFER
            val reason = rec.reason.description()
            val rl = StaticLayout.Builder.obtain(reason, 0, reason.length, paint(9f), (PAGE_W - 2 * M - 20f).toInt()).build()
            val boxH = 26f + rl.height
            ensure(boxH + 8f)
            c.drawRoundRect(RectF(M, y - 12f, PAGE_W - M, y - 12f + boxH + 8f), 6f, 6f, Paint().apply { color = if (refer) REFER_BG else INFO_BG })
            c.drawText(rec.action.label(), M + 10f, y + 4f, paint(13f, true, if (refer) REFER_FG else INK))
            c.save(); c.translate(M + 10f, y + 10f); rl.draw(c); c.restore()
            y += boxH + 10f

            // RF-23: lo que decidió el profesional frente a la sugerencia, si lo registró.
            pkg.decision?.let { d ->
                section("Decisión del profesional")
                table(buildList {
                    add("Decisión" to d.choice.label())
                    add("Conducta decidida" to (d.finalAction?.label() ?: "Ninguna de las sugeridas"))
                    if (d.reason.isNotBlank()) add("Motivo" to d.reason)
                    add("Registrada por" to "${d.decidedBy}, ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(d.decidedAtMillis))}")
                })
            }

            section("Resultado del modelo")
            if (ev.prediction.isOutOfScope) {
                paragraph("Imagen fuera del alcance del modelo: no se muestra un resultado.")
            } else {
                paragraph(ModelScore.summary(ev.prediction) + ".", 10f, true)
                scoreBar()
                paragraph(ModelScore.NOTE, 8.5f, color = MUTED)
            }

            section("Datos clínicos")
            val sintomas = listOfNotNull("sangra".takeIf { cl.hasBleeding }, "duele".takeIf { cl.hasPain }, "pica".takeIf { cl.hasItching })
            table(buildList {
                cl.ageYears?.let { add("Edad" to "$it años") }
                cl.sex?.let { add("Sexo" to it.label()) }
                add("Localización" to cl.location.label())
                add("Diámetro máximo" to "${cl.maxDiameterMm} mm")
                add("Elevada o palpable" to cl.isElevatedOrPalpable.yesNo())
                add("No cura" to cl.isNotHealing.yesNo())
                add("Crece rápido" to cl.isGrowingRapidly.yesNo())
                add("Procedimiento previo en la zona" to cl.hadPreviousProcedure.yesNo())
                add("Cambio reportado" to cl.reportedChange.label())
                add("Tiempo de evolución" to cl.evolutionTime.label())
                add("Síntomas" to if (sintomas.isEmpty()) "Ninguno" else sintomas.joinToString(", "))
                add("Antecedente de cáncer de piel" to cl.hasSkinCancerHistory.yesNo())
            })
            if (cl.notes.isNotBlank()) {
                section("Anotaciones del profesional")
                paragraph(cl.notes)
            }
        }

        // La misma barra de la pantalla: izquierda «no sugiere», derecha «sugiere», línea central = umbral.
        private fun scoreBar() {
            val s = ev.prediction.score ?: return
            val t = ev.prediction.scoreThreshold ?: return
            ensure(40f)
            val left = M; val right = PAGE_W - M; val cx = (left + right) / 2; val top = y
            val pos = 0.5f + (s - t).coerceIn(-ModelScore.ESCALA, ModelScore.ESCALA) / (2 * ModelScore.ESCALA)
            val px = left + (right - left) * pos
            val fg = if (ev.prediction.suggestsMelanoma) REFER_FG else BRAND
            c.drawRoundRect(RectF(left, top, right, top + 8f), 4f, 4f, Paint().apply { color = ZEBRA })
            c.drawRect(minOf(cx, px), top, maxOf(cx, px), top + 8f, Paint().apply { color = fg })
            c.drawRect(cx - 0.75f, top - 5f, cx + 0.75f, top + 13f, Paint().apply { color = INK })
            c.drawCircle(px, top + 4f, 5.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fg })
            val lp = paint(7.5f, color = MUTED)
            c.drawText("No sugiere", left, top + 24f, lp)
            c.drawText("umbral", cx - lp.measureText("umbral") / 2, top + 24f, lp)
            c.drawText("Sugiere melanoma", right - lp.measureText("Sugiere melanoma"), top + 24f, lp)
            y += 40f
        }

        fun imagesPage(images: List<Pair<String, File>>) {
            if (images.isEmpty()) return
            newPage()
            section("Imágenes")
            val cell = (PAGE_W - 2 * M - GAP) / 2f
            images.chunked(2).forEach { pair ->
                val lp = paint(8.5f, true)
                val layouts = pair.map { (label, _) -> StaticLayout.Builder.obtain(label, 0, label.length, lp, cell.toInt()).build() }
                val labelH = layouts.maxOf { it.height }.toFloat()
                ensure(labelH + cell + 20f)
                val top = y
                pair.forEachIndexed { i, (_, file) ->
                    val left = M + i * (cell + GAP)
                    c.save(); c.translate(left, top - 9f); layouts[i].draw(c); c.restore()
                    val box = RectF(left, top - 9f + labelH + 4f, left + cell, top - 9f + labelH + 4f + cell)
                    val bmp = decode(file.path)
                    if (bmp != null) {
                        val k = minOf(box.width() / bmp.width, box.height() / bmp.height)
                        val dst = RectF(box.left, box.top, box.left + bmp.width * k, box.top + bmp.height * k)
                        c.drawBitmap(bmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG)); bmp.recycle()
                    } else {
                        c.drawRect(box, Paint().apply { style = Paint.Style.STROKE; color = RULE })
                    }
                }
                y = top + labelH + cell + 20f
            }
            // Leyenda de la escala del mapa
            ensure(56f)
            c.drawText("Escala del mapa de calor", M, y, paint(8.5f, true))
            val top = y + 6f
            val colors = IntArray(11) { EscalaJet.argb(it / 10f) }
            c.drawRect(M, top, M + 220f, top + 8f, Paint().apply { shader = LinearGradient(M, 0f, M + 220f, 0f, colors, null, Shader.TileMode.CLAMP) })
            val lp = paint(7.5f, color = MUTED)
            c.drawText("menor", M, top + 20f, lp)
            c.drawText("mayor influencia", M + 220f - lp.measureText("mayor influencia"), top + 20f, lp)
            y = top + 38f
            paragraph("El mapa muestra dónde se apoyó el modelo, no dónde está la lesión. Es una ayuda visual cuya utilidad está " +
                "pendiente de validación por especialistas.", 8.5f, color = MUTED)
        }
    }

    private companion object {
        const val PAGE_W = 595   // A4 en puntos (1/72 pulgada)
        const val PAGE_H = 842
        const val M = 40f
        const val GAP = 16f
        const val BOTTOM = 48f
        const val IMAGE_SIDE_PX = 1000
        val BRAND = Color.rgb(0x1F, 0x5F, 0x7A)      // semilla del tema de la app
        val INK = Color.rgb(0x1A, 0x1C, 0x1E)
        val MUTED = Color.rgb(0x5A, 0x62, 0x6A)
        val RULE = Color.rgb(0xC4, 0xC9, 0xCE)
        val ZEBRA = Color.rgb(0xF0, 0xF3, 0xF5)
        val WARN = Color.rgb(0xB3, 0x26, 0x1E)
        val REFER_BG = Color.rgb(0xFC, 0xE4, 0xE2)
        val REFER_FG = Color.rgb(0x93, 0x1C, 0x14)
        val INFO_BG = Color.rgb(0xE6, 0xEE, 0xF2)
    }
}
