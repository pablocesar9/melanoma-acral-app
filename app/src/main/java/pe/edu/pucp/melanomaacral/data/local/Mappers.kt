package pe.edu.pucp.melanomaacral.data.local

import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.CapturedImages
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.Evaluation
import pe.edu.pucp.melanomaacral.domain.model.EvolutionTime
import pe.edu.pucp.melanomaacral.domain.model.ImageSource
import pe.edu.pucp.melanomaacral.domain.model.Lesion
import pe.edu.pucp.melanomaacral.domain.model.LesionImage
import pe.edu.pucp.melanomaacral.domain.model.ManagementAction
import pe.edu.pucp.melanomaacral.domain.model.Patient
import pe.edu.pucp.melanomaacral.domain.model.Prediction
import pe.edu.pucp.melanomaacral.domain.model.Recommendation
import pe.edu.pucp.melanomaacral.domain.model.RecommendationReason
import pe.edu.pucp.melanomaacral.domain.model.ReportedChange
import pe.edu.pucp.melanomaacral.domain.model.Sex
import java.time.LocalDate

// Conversión entre las filas de Room y los modelos del dominio.

fun PatientEntity.toDomain() = Patient(id, LocalDate.ofEpochDay(createdOnEpochDay))

fun LesionEntity.toDomain() = Lesion(id, patientId, nextFollowUpEpochDay?.let(LocalDate::ofEpochDay))

fun EvaluationEntity.toDomain(): Evaluation {
    val context = ctxUri?.let {
        LesionImage(
            uri = it,
            source = ImageSource.valueOf(ctxSource ?: ImageSource.CAMERA.name),
            widthPx = ctxWidth ?: 0,
            heightPx = ctxHeight ?: 0,
            sizeBytes = ctxSize ?: 0,
            capturedAtMillis = ctxCapturedAt ?: 0,
            sharpness = ctxSharpness,
            meanLuma = ctxLuma
        )
    }
    return Evaluation(
        id = id,
        lesionId = lesionId,
        date = LocalDate.ofEpochDay(dateEpochDay),
        images = CapturedImages(
            dermoscopic = LesionImage(
                uri = dermUri,
                source = ImageSource.valueOf(dermSource),
                widthPx = dermWidth,
                heightPx = dermHeight,
                sizeBytes = dermSize,
                capturedAtMillis = dermCapturedAt,
                sharpness = dermSharpness,
                meanLuma = dermLuma
            ),
            context = context
        ),
        clinicalData = ClinicalData(
            location = AcralLocation.valueOf(location),
            maxDiameterMm = maxDiameterMm,
            isElevatedOrPalpable = isElevatedOrPalpable,
            isNodularOrBlue = isNodularOrBlue,
            hadPreviousProcedure = hadPreviousProcedure,
            reportedChange = ReportedChange.valueOf(reportedChange),
            evolutionTime = EvolutionTime.valueOf(evolutionTime),
            ageYears = ageYears,
            sex = sex?.let(Sex::valueOf),
            hasSkinCancerHistory = hasSkinCancerHistory,
            hasBleeding = hasBleeding,
            hasPain = hasPain,
            hasItching = hasItching,
            notes = notes.orEmpty()
        ),
        prediction = Prediction(probMelanoma, isOutOfScope, modelVersion, threshold, isSimulated, score, scoreThreshold),
        recommendation = Recommendation(
            action = ManagementAction.valueOf(action),
            reason = RecommendationReason.valueOf(reason),
            followUpAllowed = followUpAllowed
        ),
        editedAtMillis = editedAtMillis
    )
}

fun Evaluation.toEntity(): EvaluationEntity {
    val d = images.dermoscopic
    val c = images.context
    return EvaluationEntity(
        id = id,
        lesionId = lesionId,
        dateEpochDay = date.toEpochDay(),
        dermUri = d.uri, dermSource = d.source.name, dermWidth = d.widthPx, dermHeight = d.heightPx,
        dermSize = d.sizeBytes, dermCapturedAt = d.capturedAtMillis, dermSharpness = d.sharpness, dermLuma = d.meanLuma,
        ctxUri = c?.uri, ctxSource = c?.source?.name, ctxWidth = c?.widthPx, ctxHeight = c?.heightPx,
        ctxSize = c?.sizeBytes, ctxCapturedAt = c?.capturedAtMillis, ctxSharpness = c?.sharpness, ctxLuma = c?.meanLuma,
        location = clinicalData.location.name,
        maxDiameterMm = clinicalData.maxDiameterMm,
        isElevatedOrPalpable = clinicalData.isElevatedOrPalpable,
        isNodularOrBlue = clinicalData.isNodularOrBlue,
        hadPreviousProcedure = clinicalData.hadPreviousProcedure,
        reportedChange = clinicalData.reportedChange.name,
        evolutionTime = clinicalData.evolutionTime.name,
        ageYears = clinicalData.ageYears,
        sex = clinicalData.sex?.name,
        hasSkinCancerHistory = clinicalData.hasSkinCancerHistory,
        hasBleeding = clinicalData.hasBleeding,
        hasPain = clinicalData.hasPain,
        hasItching = clinicalData.hasItching,
        probMelanoma = prediction.probMelanoma,
        isOutOfScope = prediction.isOutOfScope,
        modelVersion = prediction.modelVersion,
        threshold = prediction.threshold,
        isSimulated = prediction.isSimulated,
        action = recommendation.action.name,
        reason = recommendation.reason.name,
        followUpAllowed = recommendation.followUpAllowed,
        notes = clinicalData.notes.ifBlank { null },
        score = prediction.score,
        scoreThreshold = prediction.scoreThreshold,
        editedAtMillis = editedAtMillis
    )
}
