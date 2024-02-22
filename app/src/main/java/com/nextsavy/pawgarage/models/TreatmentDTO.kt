package com.nextsavy.pawgarage.models

import android.os.Parcelable
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class TreatmentDTO(
    val id: String,
    val animalDocId: String,
    val reportingPersonId: String,
    var medical_conditions: String = "",
    val medicalConditionIds: ArrayList<String>,
    val isMedicalConditionsApplicable: Boolean,
    val isArchive: Boolean,
    val treatmentDate: Timestamp,
    val adminNotes: String,
    val createdAt: Timestamp,
    val createdBy: String,
    var updatedAt: Timestamp?,
    var updatedBy: String?,
    var reportingPerson: GenericMemberDTO?,
    var medicalConditionNames: List<String>? = null
): Parcelable {
    companion object {
        fun create(id: String, input: Map<String, Any>?): TreatmentDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }

                return TreatmentDTO(
                    id = id,
                    animalDocId = input["animal_doc_id"] as String? ?: throw NullPointerException("animal_doc_id can not be null"),
                    reportingPersonId = input["reporting_person_id"] as String? ?: throw NullPointerException("reporting_person_id can not be null"),
                    medical_conditions = input["medical_conditions"] as String? ?: "",
                    medicalConditionIds = input["medical_condition_ids"] as ArrayList<String>? ?: arrayListOf<String>(),
                    isMedicalConditionsApplicable = input["is_medical_conditions_applicable"] as Boolean? ?: false,
                    isArchive = input["is_archive"] as Boolean? ?: false,
                    treatmentDate = input["treatment_date"] as Timestamp? ?: throw NullPointerException("treatment_date can not be null"),
                    adminNotes = input["admin_note"] as String? ?: "",
                    createdAt = input["created_at"] as Timestamp? ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input["created_by"] as String? ?: throw NullPointerException("created_by can not be null"),
                    updatedAt = input["updated_at"] as Timestamp?,
                    updatedBy = input["updated_by"] as String?,
                    reportingPerson = null
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: TreatmentDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }
}
