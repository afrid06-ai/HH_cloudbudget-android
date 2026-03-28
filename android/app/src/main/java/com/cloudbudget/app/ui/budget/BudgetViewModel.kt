package com.cloudbudget.app.ui.budget

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbudget.app.data.firebase.FirestoreRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class BudgetViewModel : ViewModel() {

    private val _budget = MutableLiveData<FirestoreRepository.BudgetData>()
    val budget: LiveData<FirestoreRepository.BudgetData> = _budget

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadBudget()
    }

    fun loadBudget() {
        viewModelScope.launch {
            FirestoreRepository.budgetFlow()
                .catch { _error.value = it.message }
                .collect { _budget.value = it }
        }
    }
}
