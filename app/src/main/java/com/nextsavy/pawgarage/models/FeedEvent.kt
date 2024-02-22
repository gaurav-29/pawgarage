package com.nextsavy.pawgarage.models

import android.util.Log
import com.google.firebase.Timestamp
import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.android.parcel.Parcelize
import java.util.Date


enum class FeedType(val value: String) {
    REGISTRATION("registration"),
    ACTIVATED("activated"),
    TERMINATED("terminated"),
    ADMISSION("admission"),
    TREATMENT("treatment"),
    VACCINE("vaccine"),
    DEWORMING("deworming"),
    ADOPTED("adopted"),
    RELEASE("release"),
    DEATH("death");

    companion object {
        private val types = values().associateBy { it.value }

        fun findValue(value: String) =
            types[value] ?: throw NullPointerException("No feed event type exists for $value")
    }
}

data class NetworkFeedEvent(
    @DocumentId
    val id: String = "",
    val animal_doc_id: String? = null,
    val feed_object_id: String? = null,
    val feed_type: String? = null,
    val is_archive: Boolean? = null,
    @ServerTimestamp
    val created_at: Timestamp? = null,
    val created_by: String? = null,
    @ServerTimestamp
    val updated_at: Timestamp? = null,
    val updated_by: String? = null,
)

data class FeedEventDTO(
    val id: String,
    val animalDocId: String,
    val feedType: FeedType,
    val feedObjectId: String?,
    val isArchive: Boolean,
    val createdAt: Timestamp,
    val createdBy: String,
    var updatedAt: Timestamp?,
    var updatedBy: String?,
    var feedObject: Any?,
    var feedDate: Date?
) {
    companion object {
        fun create(input: NetworkFeedEvent?): FeedEventDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return FeedEventDTO(
                    id = input.id,
                    animalDocId = input.animal_doc_id ?: throw NullPointerException("animal_doc_id can not be null"),
                    feedType = if (input.feed_type != null) FeedType.findValue(input.feed_type) else  throw NullPointerException("feed_type can not be null"),
                    feedObjectId = input.feed_object_id,
                    isArchive = input.is_archive ?: false,
                    createdAt = input.created_at ?: throw NullPointerException("created_at can not be null"),
                    createdBy = input.created_by ?: throw NullPointerException("created_by can not be null"),
                    updatedAt = input.updated_at,
                    updatedBy = input.updated_by,
                    feedObject = null,
                    feedDate = null
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: FeedEventDTO.create() ${e.localizedMessage}")
                return null
            }
        }
    }
}