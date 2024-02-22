package com.nextsavy.pawgarage.models
import android.os.Parcelable
import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReminderDTO(
    val id: String,
    /**
     * Animal Doc Id is nullable. Keeping a case in sight where Reminder is not for Animal!
     */
    val animalDocId: String?,
    val reminderObjectId: String?,
    val reminderDate: Timestamp,
    val reminderType: String,
    val isComplete: Boolean,
    val isArchive: Boolean,
    val isTurnedOff: Boolean,
    val createdAt: Timestamp,
    val createdBy: String,
    var updatedAt: Timestamp?,
    var updatedBy: String?,
    var animalDTO: AnimalDTO? = null,
    var profileLeadDTO: ProfileLeadDTO? = null
): Parcelable {
    companion object {
        fun create(id: String, input: Map<String, Any>?): ReminderDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return ReminderDTO(
                    id = id,
                    animalDocId = input["animal_doc_id"] as String?,
                    reminderObjectId = input["reminder_type_object_id"] as String?,
                    reminderDate = input["reminder_date"] as Timestamp? ?: throw NullPointerException("reminder_date can not be null"),
                    reminderType = input["reminder_type"] as String? ?: throw NullPointerException("reminder_type can not be null"),
                    isComplete = input["is_complete"] as Boolean? ?: false,
                    isArchive = input["is_archive"] as Boolean? ?: false,
                    isTurnedOff = input["is_turned_off"] as Boolean? ?: false,
                    createdAt = input["created_at"] as Timestamp? ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input["created_by"] as String? ?: throw NullPointerException("created_by can not be null"),
                    updatedAt = input["updated_at"] as Timestamp?,
                    updatedBy = input["updated_by"] as String?,
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: ReminderDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }

}