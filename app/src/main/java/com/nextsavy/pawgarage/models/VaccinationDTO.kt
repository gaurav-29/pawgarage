package com.nextsavy.pawgarage.models
import android.os.Parcelable
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VaccinationDTO(
    val id: String,
    val animalDocId: String,
    val durationType: String?,
    val administratorId: String?,
    val vaccinationDate: Timestamp,
    val vaccinationStatus: String,
    val vaccineName: String?,
    val adminNotes: String,
    val isArchive: Boolean,
    val createdAt: Timestamp,
    val createdBy: String,
    var updatedAt: Timestamp?,
    var updatedBy: String?,
    var administratorPerson: GenericUserDTO?
): Parcelable {
    companion object {
        fun create(id: String, input: Map<String, Any>?): VaccinationDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return VaccinationDTO(
                    id = id,
                    animalDocId = input["animal_doc_id"] as String? ?: throw NullPointerException("animal_doc_id can not be null"),
                    durationType = input["duration_type"] as String?,
                    administratorId = input["person_administrated_id"] as String?, //? ?: throw NullPointerException("person_administrated_id can not be null"),
                    vaccinationDate = input["vaccination_date"] as Timestamp? ?: throw NullPointerException("vaccination_date can not be null"),
                    vaccinationStatus = input["vaccination_status"] as String? ?: throw NullPointerException("vaccination_status can not be null"),
                    vaccineName = input["vaccine_type"] as String?, //? ?: throw NullPointerException("vaccine_type can not be null"),
                    adminNotes = input["admin_note"] as String? ?: "",
                    isArchive = input["is_archive"] as Boolean? ?: false,
                    createdAt = input["created_at"] as Timestamp? ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input["created_by"] as String? ?: throw NullPointerException("created_by can not be null"),
                    updatedAt = input["updated_at"] as Timestamp?,
                    updatedBy = input["updated_by"] as String?,
                    administratorPerson = null
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: VaccinationDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }

}
