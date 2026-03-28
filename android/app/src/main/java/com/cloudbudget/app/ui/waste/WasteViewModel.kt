package com.cloudbudget.app.ui.waste

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbudget.app.data.firebase.FirestoreRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class WasteViewModel : ViewModel() {

    private val _waste = MutableLiveData<FirestoreRepository.WasteData>()
    val waste: LiveData<FirestoreRepository.WasteData> = _waste

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadWaste()
    }

    fun loadWaste() {
        viewModelScope.launch {
            FirestoreRepository.wasteFlow()
                .catch { _error.value = it.message }
                .collect { _waste.value = it }
        }
    }
}
