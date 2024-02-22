package com.nextsavy.pawgarage.models

import android.os.Parcelable
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AnimalDTO (
    val id: String,
    val animalId: String,
    val name: String,
    val description: String,
    val state: String,
    val species: String,
    val type: String,
    val downloadUrl: String,
    val gender: String,
    val weight: String?,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val isArchive: Boolean,
    val isDead: Boolean,
    val createdAt: Timestamp,
    val createdBy: String,
    var updatedAt: Timestamp?,
    var updatedBy: String?
): Parcelable {
    companion object {
        fun create(id: String, input: Map<String, Any>?): AnimalDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return AnimalDTO(
                    id = id,
                    animalId = input["animal_id"] as String? ?: throw NullPointerException("Animal id can not be null"),
                    name = input["name"] as String? ?: throw NullPointerException("Name can not be null"),
                    description = input["description"] as String? ?: "",
                    state = input["state"] as String? ?: "",
                    species = input["species"] as String? ?: throw NullPointerException("Species can not be null"),
                    type = input["type"] as String? ?: throw NullPointerException("Type can not be null"),
                    downloadUrl = input["download_url"] as String? ?: throw NullPointerException("download_url can not be null"),
                    gender = input["gender"] as String? ?: throw NullPointerException("gender can not be null"),
                    weight = input["weight"] as String?,
                    address = input["location_address"] as String? ?: throw NullPointerException("location_address can not be null"),
                    latitude = input["latitude"] as Double? ?: throw NullPointerException("latitude can not be null"),
                    longitude = input["longitude"] as Double? ?: throw NullPointerException("longitude can not be null"),
                    isArchive = input["is_archive"] as Boolean? ?: false,
                    isDead = input["is_dead"] as Boolean? ?: false,
                    createdAt = input["created_at"] as Timestamp? ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input["created_by"] as String? ?: throw NullPointerException("created_by can not be null"),
                    updatedAt = input["updated_at"] as Timestamp?,
                    updatedBy = input["updated_by"] as String?,
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: AnimalDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }
}