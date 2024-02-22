package com.nextsavy.pawgarage.models

import android.util.Log
import com.google.firebase.Timestamp

data class ReleaseDTO(
    val id: String,
    val animalDocId: String,
    val comment: String?,
    val adopterId: String?,
    val isArchive: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val releaseStatus: String,
    val releaseDate: Timestamp,
    val createdAt: Timestamp,
    val createdBy: String,
    var updatedAt: Timestamp?,
    var updatedBy: String?,
    var adopter: GenericMemberDTO? = null
) {
    companion object {
        fun create(id: String, input: Map<String, Any>?): ReleaseDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return ReleaseDTO(
                    id = id,
                    animalDocId = input["animal_doc_id"] as String? ?: throw NullPointerException("animal_doc_id can not be null"),
                    comment = input["comment"] as String?,
                    adopterId = input["adopter_id"] as String?,
                    latitude = input["latitude"] as Double?,
                    longitude = input["longitude"] as Double?,
                    address = input["location_address"] as String?,
                    releaseStatus = input["released_status"] as String? ?: throw NullPointerException("released_status can not be null"),
                    releaseDate = input["released_date"] as Timestamp? ?: throw NullPointerException("released_date can not be null"),
                    isArchive = input["is_archive"] as Boolean? ?: false,
                    createdAt = input["created_at"] as Timestamp? ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input["created_by"] as String? ?: throw NullPointerException("created_by can not be null"),
                    updatedAt = input["updated_at"] as Timestamp?,
                    updatedBy = input["updated_by"] as String?
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: ReleaseDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }
}
