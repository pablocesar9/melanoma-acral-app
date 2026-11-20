package pe.edu.pucp.melanomaacral.domain.repository

import pe.edu.pucp.melanomaacral.domain.model.ReferralExport
import pe.edu.pucp.melanomaacral.domain.model.ReferralPackage

// Genera el documento de derivación (PDF) y las imágenes como archivos aparte, y devuelve sus URIs para compartirlos
// con el menú de Android (RF-15). Sin conexión: todo se arma en el teléfono.
interface ReferralExporter {
    suspend fun exportPdf(pkg: ReferralPackage): ReferralExport
}
