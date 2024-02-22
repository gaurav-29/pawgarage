package com.nextsavy.pawgarage.models

import java.util.Date

data class DewormingDataModel(
    var date: String = "",
    var medicinePosition: Int = 0,
    var person: String = "",
    var weight: String = "",
    var durationPosition: Int = 0,
    var userPosition: Int = 0,
    var dateOnResume: Date? = null
)
