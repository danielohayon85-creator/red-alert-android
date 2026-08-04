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

    // null = preferences not loaded yet (DataStore read is unavoidably async) — screens must
    // gate on this instead of treating an empty set as "user has nothing selected".
    private val _selected = MutableStateFlow<Set<String>?>(null)
    val selected: StateFlow<Set<String>?> = _selected.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch { _selected.value = appPreferences.selectedSettlements.first() }
    }

    fun settlementsFor(area: String): List<String> = areaRepository.settlementsInArea(area)

    fun visibleSettlementsFor(area: String): List<String> = settlementsFor(area).filter { matchesQuery(it) }

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
        _selected.update { (it ?: emptySet()).let { current -> if (settlement in current) current - settlement else current + settlement } }
    }

    fun isAreaFullySelected(area: String): Boolean {
        val settlements = settlementsFor(area)
        val current = _selected.value ?: emptySet()
        return settlements.isNotEmpty() && settlements.all { it in current }
    }

    /** Toggles only the settlements currently visible under a search filter, not the whole area. */
    fun toggleArea(area: String) {
        val settlements = visibleSettlementsFor(area).toSet()
        _selected.update { current ->
            val base = current ?: emptySet()
            if (settlements.isNotEmpty() && settlements.all { it in base }) base - settlements else base + settlements
        }
    }

    fun save() {
        viewModelScope.launch {
            appPreferences.setSelectedSettlements(_selected.value ?: emptySet())
            _saved.value = true
        }
    }
}
