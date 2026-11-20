package pe.edu.pucp.melanomaacral.domain.repository

import pe.edu.pucp.melanomaacral.domain.model.ImageKind
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.QualityReport

// Revisa enfoque, iluminación, encuadre y resolución de una imagen (RF-03).
interface ImageQualityRepository {
    // kind CONTEXT: no se exige una lesión centrada (encuadre).
    suspend fun check(image: LesionImage, kind: ImageKind = ImageKind.DERMOSCOPIC): QualityReport
}
