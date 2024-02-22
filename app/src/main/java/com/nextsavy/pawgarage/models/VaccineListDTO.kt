package com.nextsavy.pawgarage.models

import android.os.Parcelable
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class VaccineListDTO(
    val id: String,
    val name: String,
    val isArchive: Boolean,
    val createdAt: Timestamp,
    val createdBy: String,
    var updatedAt: Timestamp?,
    var updatedBy: String?
): Parcelable {
    companion object {
        fun create(id: String, input: Map<String, Any>?): VaccineListDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return VaccineListDTO(
                    id = id,
                    name = input["name"] as String?
                        ?: throw NullPointerException("Name can not be null"),
                    isArchive = input["is_archive"] as Boolean? ?: false,
                    createdAt = input["created_at"] as Timestamp?
                        ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input["created_by"] as String?
                        ?: throw NullPointerException("created_by can not be null"),
                    updatedAt = input["updated_at"] as Timestamp?,
                    updatedBy = input["updated_by"] as String?,
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: VaccineListDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }
}
