package com.nextsavy.pawgarage.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class RemindersModel(
    var document_id: String = "",
    var animal_doc_id: String = "",
    var animal_name: String = "",
    var isDead: Boolean = false,
    var state: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var location_address: String = "",
    var animal_image: String = "",
    @ServerTimestamp
    var reminder_date: Timestamp? = null,
    var reminder_type: String = "",
    var reminder_type_object_id: String = "",
    var animalDTO: AnimalDTO? = null,
    var profileLeadDTO: ProfileLeadDTO? = null
): Parcelable
