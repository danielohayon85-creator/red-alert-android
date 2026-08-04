package com.shomerapp.alerts.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shomerapp.alerts.data.local.db.AlertHistoryDao
import com.shomerapp.alerts.data.local.db.AlertHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(historyDao: AlertHistoryDao) : ViewModel() {
    val history = historyDao.recent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<AlertHistoryEntity>())
}
