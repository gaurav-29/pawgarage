package com.nextsavy.pawgarage.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ReportsGeneralModel(
    var documentId: String = "",
    var animalDocId: String = "",
    var animalName: String = "",
    @ServerTimestamp
    var date: Timestamp? = null,
    var doneBy: String = "",
    var updatedBy: String = "",
    var medicineName: String = "",
    var location: String = "",
    var gender: String = "",
    var status: String = "",
    var species: String = "",
    var type: String = "",
    var conditions: String = "",
    var isArchive: Boolean = true
)
