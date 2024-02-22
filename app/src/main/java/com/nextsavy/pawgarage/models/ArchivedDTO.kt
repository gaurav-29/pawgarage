package com.nextsavy.pawgarage.models

import android.os.Parcelable
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class ArchivedDTO(
    val animalDocId: String,
    val createdAt: Timestamp,
    val createdBy: String,
    var updatedAt: Timestamp,
    var updatedBy: String
) : Parcelable {
    companion object {
        fun create(id: String, input: Map<String, Any>?): ArchivedDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return ArchivedDTO(
                    animalDocId = input["animal_doc_id"] as String? ?: throw NullPointerException("animal_doc_id can not be null"),
                    createdAt = input["created_at"] as Timestamp? ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input["created_by"] as String? ?: throw NullPointerException("created_by can not be null"),
                    updatedAt = input["updated_at"] as Timestamp? ?: throw NullPointerException("updated_at can not be null"),
                    updatedBy = input["updated_by"] as String? ?: throw NullPointerException("updated_by can not be null"),
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: ArchivedDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }
}
