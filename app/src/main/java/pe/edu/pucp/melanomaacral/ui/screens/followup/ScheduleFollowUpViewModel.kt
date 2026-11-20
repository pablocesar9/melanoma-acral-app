package pe.edu.pucp.melanomaacral.ui.screens.followup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.pucp.melanomaacral.domain.model.FollowUpInterval
import pe.edu.pucp.melanomaacral.domain.model.forLesion
import pe.edu.pucp.melanomaacral.domain.repository.RecordsRepository
import pe.edu.pucp.melanomaacral.domain.repository.ReminderScheduler
import pe.edu.pucp.melanomaacral.ui.appContainer
import java.time.LocalDate

data class ScheduleFollowUpUiState(
    val isLoading: Boolean = true,
    val followUpAllowed: Boolean = true,
    val selectedInterval: FollowUpInterval = FollowUpInterval.SHORT_3_MONTHS,
    val today: LocalDate = LocalDate.now()
) {
    val date: LocalDate
        get() = today.plusMonths(selectedInterval.months)
}

class ScheduleFollowUpViewModel(
    private val lesionId: String,
    private val records: RecordsRepository,
    private val reminders: ReminderScheduler
) : ViewModel() {

    private val selectedInterval = MutableStateFlow(FollowUpInterval.SHORT_3_MONTHS)

    // RF-09: se consulta la evaluación más reciente. Si la lesión no es apta para
    // seguimiento, la pantalla bloquea la programación aunque se llegue a ella.
    val uiState: StateFlow<ScheduleFollowUpUiState> =
        combine(records.evaluations, selectedInterval) { evaluations, interval ->
            val latest = evaluations.forLesion(lesionId).lastOrNull()
            ScheduleFollowUpUiState(
                isLoading = false,
                followUpAllowed = latest?.recommendation?.followUpAllowed ?: true,
                selectedInterval = interval
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleFollowUpUiState())

    fun selectInterval(interval: FollowUpInterval) {
        selectedInterval.value = interval
    }

    fun confirm(onDone: () -> Unit) {
        val state = uiState.value
        if (!state.followUpAllowed) return
        viewModelScope.launch {
            records.scheduleFollowUp(lesionId, state.date)
            reminders.schedule(lesionId, state.date)   // RF-14
            onDone()
        }
    }

    companion object {
        fun factory(lesionId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ScheduleFollowUpViewModel(lesionId, container.recordsRepository, container.reminderScheduler)
            }
        }
    }
}
