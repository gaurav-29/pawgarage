package com.nextsavy.pawgarage.models

import android.os.Parcelable
import android.util.Log
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GenericUserDTO(
    val id: String,
    val name: String,
    val phoneNumber: String
): Parcelable, Comparable<GenericUserDTO> {
    companion object {
        fun create(id: String, input: Map<String, Any>?): GenericUserDTO? {
            try {
                if (input == null) {
                    throw NullPointerException("input is null")
                }
                return GenericUserDTO(
                    id = id,
                    name = input["user_name"] as String? ?: input["name"] as String? ?: throw NullPointerException("user_name can not be null"),
                    phoneNumber = input["contact_number"] as String? ?: throw NullPointerException("contact_number can not be null")
                )
            } catch (e: Exception) {
                Log.e("NST-M", "Exception: GenericUserDTO.create(): $id ${e.localizedMessage}")
                return null
            }
        }
    }

    override fun compareTo(other: GenericUserDTO): Int {
        if (id == other.id) {
            return 0
        }
        return -1
    }

    override fun toString(): String {
        return name
    }

}
