package com.nextsavy.pawgarage.models

import android.os.Parcelable
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProfileLeadDTO(
    val id: String,
    /// Note: This is not the Animal Document Id
    val animalId: String,
    val name: String,
    val downloadUrl: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val isArchive: Boolean,
    val createdAt: Timestamp,
    val createdBy: String,
): Parcelable {
    companion object {
        fun create(id: String, input: Map<String, Any>?): ProfileLeadDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return ProfileLeadDTO(
                    id = id,
                    animalId = input["animal_id"] as String? ?: throw NullPointerException("Animal id can not be null"),
                    name = input["name"] as String? ?: throw NullPointerException("Name can not be null"),
                    downloadUrl = input["download_url"] as String? ?: throw NullPointerException("download_url can not be null"),
                    address = input["location_address"] as String? ?: throw NullPointerException("location_address can not be null"),
                    latitude = input["latitude"] as Double? ?: throw NullPointerException("latitude can not be null"),
                    longitude = input["longitude"] as Double? ?: throw NullPointerException("longitude can not be null"),
                    isArchive = input["is_archive"] as Boolean? ?: false,
                    createdAt = input["created_at"] as Timestamp? ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input["created_by"] as String? ?: throw NullPointerException("created_by can not be null"),
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: ProfileLeadDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }
}
