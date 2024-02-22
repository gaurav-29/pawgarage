package com.nextsavy.pawgarage.models

import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.firestore.DocumentId

data class AnimalProfileModel(
    @DocumentId
    var documentId: String = "",
    var name: String = "",
    var galleryImage: Uri? = null,
    var cameraImage: Bitmap? = null,
    var type: String = "",
    var description: String = "",
    var gender: String = "",
    var species: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var locationAddress: String = "",
    var isArchive: Boolean = false,
    var userPosition: Int = 0,
    var spinnerStatusItem: String = "",
)
