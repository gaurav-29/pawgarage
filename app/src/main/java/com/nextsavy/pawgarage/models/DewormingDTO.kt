package com.nextsavy.pawgarage.models

import android.os.Parcelable
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DewormingDTO(
    val id: String,
    val animalDocId: String,
    val weight: String?,
    val durationType: String?,
    val administratorId: String?,
    val dewormingDate: Timestamp,
    val dewormingStatus: String,
    val medicineName: String?,
    val adminNotes: String,
    val isArchive: Boolean,
    val createdAt: Timestamp,
    val createdBy: String,
    var updatedAt: Timestamp?,
    var updatedBy: String?,
    var administratorPerson: GenericUserDTO?
): Parcelable {
    companion object {
        fun create(id: String, input: Map<String, Any>?): DewormingDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return DewormingDTO(
                    id = id,
                    animalDocId = input["animal_doc_id"] as String? ?: throw NullPointerException("animal_doc_id can not be null"),
                    weight = input["weight"] as String?,// ?: throw NullPointerException("weight can not be null"),
                    durationType = input["duration_type"] as String?,
                    administratorId = input["person_administrated_id"] as String?, //? ?: throw NullPointerException("person_administrated_id can not be null"),
                    dewormingDate = input["deworming_date"] as Timestamp? ?: throw NullPointerException("deworming_date can not be null"),
                    dewormingStatus = input["deworming_status"] as String? ?: throw NullPointerException("deworming_status can not be null"),
                    medicineName = input["medicine_type"] as String?, //? ?: throw NullPointerException("vaccine_type can not be null"),
                    adminNotes = input["admin_note"] as String? ?: "",
                    isArchive = input["is_archive"] as Boolean? ?: false,
                    createdAt = input["created_at"] as Timestamp? ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input["created_by"] as String? ?: throw NullPointerException("created_by can not be null"),
                    updatedAt = input["updated_at"] as Timestamp?,
                    updatedBy = input["updated_by"] as String?,
                    administratorPerson = null
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: DewormingDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }
}
