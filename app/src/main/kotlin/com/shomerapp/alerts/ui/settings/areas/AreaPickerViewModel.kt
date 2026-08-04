package com.shomerapp.alerts.ui.settings.areas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shomerapp.alerts.data.areas.AreaRepository
import com.shomerapp.alerts.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MANY_SETTLEMENTS_WARNING_THRESHOLD = 50

@HiltViewModel
class AreaPickerViewModel @Inject constructor(
    private val areaRepository: AreaRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val allAreas: List<String> = areaRepository.allAreas()

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch { _selected.value = appPreferences.selectedSettlements.first() }
    }

    fun settlementsFor(area: String): List<String> = areaRepository.settlementsInArea(area)

    fun onQueryChange(query: String) {
        _query.value = query
    }

    /** Simple normalized-substring search — "תל אביב" must match "תל אביב - מרכז העיר" (§8). */
    fun matchesQuery(settlement: String): Boolean {
        val q = _query.value
        if (q.isBlank()) return true
        return areaRepository.normalize(settlement).contains(areaRepository.normalize(q))
    }

    fun toggleSettlement(settlement: String) {
        _selected.update { if (settlement in it) it - settlement else it + settlement }
    }

    fun isAreaFullySelected(area: String): Boolean {
        val settlements = settlementsFor(area)
        return settlements.isNotEmpty() && settlements.all { it in _selected.value }
    }

    fun toggleArea(area: String) {
        val settlements = settlementsFor(area).toSet()
        _selected.update { current -> if (settlements.all { it in current }) current - settlements else current + settlements }
    }

    fun save() {
        viewModelScope.launch {
            appPreferences.setSelectedSettlements(_selected.value)
            _saved.value = true
        }
    }
}
