package pe.edu.pucp.melanomaacral.data.fake

import kotlinx.coroutines.delay
import pe.edu.pucp.melanomaacral.domain.model.ImageKind
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.QualityReport
import pe.edu.pucp.melanomaacral.domain.repository.ImageQualityRepository

// Revisión de calidad de prueba: siempre aprueba y lo marca como simulado.
class FakeImageQualityRepository(
    private val delayMillis: Long = 500
) : ImageQualityRepository {

    override suspend fun check(image: LesionImage, kind: ImageKind): QualityReport {
        delay(delayMillis)
        return QualityReport(focusOk = true, lightingOk = true, framingOk = true, isSimulated = true)
    }
}
