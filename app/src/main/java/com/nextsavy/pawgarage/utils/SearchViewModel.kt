package com.nextsavy.pawgarage.utils

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SearchViewModel: ViewModel() {

    val queryForSearch = MutableLiveData<String?>(null)

    fun searchQuery(item: String) {
        if (!shouldShowResult(item)) return
        queryForSearch.value = item
    }

    private fun shouldShowResult(searchText: String): Boolean {
        return this.queryForSearch.value?.lowercase() != searchText.lowercase()
    }
}