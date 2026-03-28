package com.cloudbudget.app.ui.trends

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbudget.app.data.firebase.FirestoreRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TrendsViewModel : ViewModel() {

    private val _trends = MutableLiveData<FirestoreRepository.TrendsData>()
    val trends: LiveData<FirestoreRepository.TrendsData> = _trends

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadTrends()
    }

    fun loadTrends() {
        viewModelScope.launch {
            FirestoreRepository.trendsFlow()
                .catch { _error.value = it.message }
                .collect { _trends.value = it }
        }
    }
}
