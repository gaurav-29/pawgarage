package com.nextsavy.pawgarage.models

data class LocationDataModel(

    var addReleaseLocation: String = "",
    var addReleaseLatitude: Double = 0.0,
    var addReleaseLongitude: Double = 0.0,

    var editReleaseLocation: String = "",
    var editReleaseLatitude: Double = 0.0,
    var editReleaseLongitude: Double = 0.0
)
