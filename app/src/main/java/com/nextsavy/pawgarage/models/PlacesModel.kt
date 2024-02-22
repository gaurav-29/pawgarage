package com.nextsavy.pawgarage.models

data class PlacesModel(
    val results: List<Result>,
    val status: String
)
data class Result(
    val geometry: Geometry,
    val name: String,
    val vicinity: String
)
data class Geometry(
    val location: Location,
)
data class Location(
    val lat: Double,
    val lng: Double
)

