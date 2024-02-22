package com.nextsavy.pawgarage.models

import com.google.firebase.firestore.DocumentId

data class TeamLeadersModel(
    @DocumentId
    var documentId: String = "",
    var name: String = "",
    var number: String = ""
)
