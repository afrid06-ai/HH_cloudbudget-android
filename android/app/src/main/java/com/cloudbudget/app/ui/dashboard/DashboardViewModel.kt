package com.cloudbudget.app.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbudget.app.data.firebase.FirestoreRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _dashboard = MutableLiveData<FirestoreRepository.DashboardData>()
    val dashboard: LiveData<FirestoreRepository.DashboardData> = _dashboard

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _loading.value = true
            FirestoreRepository.dashboardFlow()
                .catch { _error.value = it.message; _loading.value = false }
                .collect { data ->
                    _dashboard.value = data
                    _loading.value = false
                }
        }
    }
}
