package com.cloudbudget.app.ui.trends

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbudget.app.data.firebase.FirestoreRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.math.pow

class TrendsViewModel : ViewModel() {

    private val _rawTrends = MutableLiveData<FirestoreRepository.TrendsData>()
    private val _rangeDays = MutableLiveData(7)
    val rangeDays: LiveData<Int> = _rangeDays

    private val _display = MediatorLiveData<FirestoreRepository.TrendsData>()
    val trends: LiveData<FirestoreRepository.TrendsData> = _display

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        val recompute = {
            val raw = _rawTrends.value ?: FirestoreRepository.TrendsData()
            val days = _rangeDays.value ?: 7
            val mult = when (days) {
                7 -> 1.0
                30 -> 1.12
                90 -> 1.28
                else -> 1.0
            }
            val curve = mult.pow(0.85)
            _display.value = raw.copy(
                avgDaily = raw.avgDaily * curve,
                projected = raw.projected * mult
            )
        }
        _display.addSource(_rawTrends) { recompute() }
        _display.addSource(_rangeDays) { recompute() }
        loadTrends()
    }

    fun setRangeDays(days: Int) {
        _rangeDays.value = days.coerceIn(7, 90)
    }

    fun loadTrends() {
        viewModelScope.launch {
            FirestoreRepository.trendsFlow()
                .catch { _error.value = it.message }
                .collect { _rawTrends.value = it }
        }
    }
}
