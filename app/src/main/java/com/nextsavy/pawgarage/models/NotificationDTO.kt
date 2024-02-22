package com.nextsavy.pawgarage.models

import android.os.Parcelable
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NotificationDTO(
    val id: String,
    val animalDocId: String?,
    val notificationObjectId: String?,
    val notificationDate: Timestamp,
    val notificationType: String,
    val notificationSubType: String?,
    val isArchive: Boolean,
    val createdAt: Timestamp,
    val createdBy: String,
    var updatedAt: Timestamp?,
    var updatedBy: String?,
    var animalDTO: AnimalDTO? = null,
    var profileLeadDTO: ProfileLeadDTO? = null,
    var creator: GenericUserDTO? = null
): Parcelable {
    companion object {
        fun create(id: String, input: Map<String, Any>?): NotificationDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return NotificationDTO(
                    id = id,
                    animalDocId = input["animal_doc_id"] as String?,
                    notificationObjectId = input["notification_type_object_id"] as String?,
                    notificationDate = input["notification_date"] as Timestamp? ?: throw NullPointerException("notification_date can not be null"),
                    notificationType = input["notification_type"] as String? ?: throw NullPointerException("notification_type can not be null"),
                    notificationSubType = input["notification_subtype"] as String?,
                    isArchive = input["is_archive"] as Boolean? ?: false,
                    createdAt = input["created_at"] as Timestamp? ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input["created_by"] as String? ?: throw NullPointerException("created_by can not be null"),
                    updatedAt = input["updated_at"] as Timestamp?,
                    updatedBy = input["updated_by"] as String?,
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: NotificationDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }
}
