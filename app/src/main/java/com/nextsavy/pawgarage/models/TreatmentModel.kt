package com.nextsavy.pawgarage.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class TreatmentModel(
    @DocumentId
    var documentId: String = "",
    var animal_id: String = "",
    var title: String = "",
    var treatment_date: Timestamp? = null,
    var contact_number: String = "",
    var reporting_person: String = "",
    var reporting_person_id: String = "",
    var medical_conditions: String = "",
    var is_medical_conditions_applicable: Boolean = true,
    var admin_note: String = "",
    var created_by: String? = null,
    var updated_by: String? = null,
) : Parcelable
