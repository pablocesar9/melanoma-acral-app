package pe.edu.pucp.melanomaacral.ui.screens.clinicaldata

import pe.edu.pucp.melanomaacral.domain.model.AcralLocation
import pe.edu.pucp.melanomaacral.domain.model.ClinicalData
import pe.edu.pucp.melanomaacral.domain.model.ClinicalFormDraft
import pe.edu.pucp.melanomaacral.domain.model.EvolutionTime
import pe.edu.pucp.melanomaacral.domain.model.ReportedChange
import pe.edu.pucp.melanomaacral.domain.model.Sex
import pe.edu.pucp.melanomaacral.domain.rules.ClinicalDataValidation

// Estado del formulario tal como lo llena el usuario.
// Los campos de selección empiezan en null: obligan a elegir una opción a conciencia.
data class ClinicalDataUiState(
    val location: AcralLocation? = null,
    val diameterText: String = "",
    val isElevatedOrPalpable: Boolean = false,
    val isNodularOrBlue: Boolean = false,
    val hadPreviousProcedure: Boolean = false,
    val reportedChange: ReportedChange? = null,
    val evolutionTime: EvolutionTime? = null,
    val ageText: String = "",
    val sex: Sex? = null,
    val hasSkinCancerHistory: Boolean = false,
    val hasBleeding: Boolean = false,
    val hasPain: Boolean = false,
    val hasItching: Boolean = false,
    val notes: String = ""
) {
    val ageYears: Int?
        get() = ClinicalDataValidation.parseAge(ageText)

    val showAgeError: Boolean
        get() = ageText.isNotBlank() && ageYears == null

    val diameterMm: Float?
        get() = ClinicalDataValidation.parseDiameter(diameterText)

    // Solo se marca el error cuando el usuario ya escribió algo inválido.
    val showDiameterError: Boolean
        get() = diameterText.isNotBlank() && diameterMm == null

    // Convierte el formulario en datos clínicos, o devuelve null si falta algo.
    fun toClinicalDataOrNull(): ClinicalData? {
        val selectedLocation = location ?: return null
        val diameter = diameterMm ?: return null
        val change = reportedChange ?: return null
        val evolution = evolutionTime ?: return null
        val age = ageYears ?: return null
        val selectedSex = sex ?: return null
        return ClinicalData(
            location = selectedLocation,
            maxDiameterMm = diameter,
            isElevatedOrPalpable = isElevatedOrPalpable,
            isNodularOrBlue = isNodularOrBlue,
            hadPreviousProcedure = hadPreviousProcedure,
            reportedChange = change,
            evolutionTime = evolution,
            ageYears = age,
            sex = selectedSex,
            hasSkinCancerHistory = hasSkinCancerHistory,
            hasBleeding = hasBleeding,
            hasPain = hasPain,
            hasItching = hasItching,
            notes = notes.trim()
        )
    }

    val canSubmit: Boolean
        get() = toClinicalDataOrNull() != null

    // Para guardar el formulario en la evaluación sin terminar, tal como está.
    fun toDraft() = ClinicalFormDraft(
        location = location,
        diameterText = diameterText,
        isElevatedOrPalpable = isElevatedOrPalpable,
        isNodularOrBlue = isNodularOrBlue,
        hadPreviousProcedure = hadPreviousProcedure,
        reportedChange = reportedChange,
        evolutionTime = evolutionTime,
        ageText = ageText,
        sex = sex,
        hasSkinCancerHistory = hasSkinCancerHistory,
        hasBleeding = hasBleeding,
        hasPain = hasPain,
        hasItching = hasItching,
        notes = notes
    )

    companion object {
        fun fromDraft(d: ClinicalFormDraft) = ClinicalDataUiState(
            location = d.location,
            diameterText = d.diameterText,
            isElevatedOrPalpable = d.isElevatedOrPalpable,
            isNodularOrBlue = d.isNodularOrBlue,
            hadPreviousProcedure = d.hadPreviousProcedure,
            reportedChange = d.reportedChange,
            evolutionTime = d.evolutionTime,
            ageText = d.ageText,
            sex = d.sex,
            hasSkinCancerHistory = d.hasSkinCancerHistory,
            hasBleeding = d.hasBleeding,
            hasPain = d.hasPain,
            hasItching = d.hasItching,
            notes = d.notes
        )

        // Datos de una evaluación ya guardada (para editarla).
        fun fromClinicalData(c: ClinicalData) = ClinicalDataUiState(
            location = c.location,
            diameterText = c.maxDiameterMm.toString().removeSuffix(".0"),
            isElevatedOrPalpable = c.isElevatedOrPalpable,
            isNodularOrBlue = c.isNodularOrBlue,
            hadPreviousProcedure = c.hadPreviousProcedure,
            reportedChange = c.reportedChange,
            evolutionTime = c.evolutionTime,
            ageText = c.ageYears?.toString().orEmpty(),
            sex = c.sex,
            hasSkinCancerHistory = c.hasSkinCancerHistory,
            hasBleeding = c.hasBleeding,
            hasPain = c.hasPain,
            hasItching = c.hasItching,
            notes = c.notes
        )

        // Nueva evaluación de una lesión ya evaluada: se precargan los datos que no cambian entre controles
        // (localización, sexo, edad y antecedentes). Lo que describe el estado actual (diámetro, cambios,
        // síntomas, signos de alarma) se vuelve a llenar.
        fun prefilledFrom(c: ClinicalData) = ClinicalDataUiState(
            location = c.location,
            ageText = c.ageYears?.toString().orEmpty(),
            sex = c.sex,
            hasSkinCancerHistory = c.hasSkinCancerHistory,
            hadPreviousProcedure = c.hadPreviousProcedure
        )
    }
}
