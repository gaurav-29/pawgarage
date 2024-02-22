package com.nextsavy.pawgarage.utils

import android.app.Application
import android.content.Context
import com.nextsavy.pawgarage.models.AnimalProfileModel
import com.nextsavy.pawgarage.models.DewormingDataModel
import com.nextsavy.pawgarage.models.LocationDataModel
import com.nextsavy.pawgarage.models.TeamLeadersModel
import com.nextsavy.pawgarage.models.VaccinationDataModel

class AppDelegate: Application() {

    override fun onCreate() {
        super.onCreate()
        mInstance = this
    }

    companion object {

        lateinit var mInstance: AppDelegate
        fun applicationContext(): Context? {
            return mInstance.applicationContext
        }

        val animalModel = AnimalProfileModel()
        val vaccinationDataModel = VaccinationDataModel()
        val dewormingDataModel = DewormingDataModel()
        val locationDataModel = LocationDataModel()

        var profileLeadsImage: String = ""
        var isImageSelected: Boolean = false
        var imageFrom: String? = null
        var isDead = false
        var state = ""

        // Below list is Selected Medical conditions list.
        var selectedList2 = arrayListOf<String>()
    }
}