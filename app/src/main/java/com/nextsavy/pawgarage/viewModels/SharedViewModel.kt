package com.nextsavy.pawgarage.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.models.MedicalConditionDTO

class SharedViewModel: ViewModel() {

    private val _selectedMedicalCondition = MutableLiveData<ArrayList<MedicalConditionDTO>>()
    val selectedMedicalCondition: LiveData<ArrayList<MedicalConditionDTO>> = _selectedMedicalCondition

    private val _reportingPerson = MutableLiveData<GenericMemberDTO?>()
    val reportingPerson: LiveData<GenericMemberDTO?> = _reportingPerson

    fun setReportingPerson(item: GenericMemberDTO?) {
        _reportingPerson.value = item
    }

    fun getReportingPerson(): GenericMemberDTO? {
        return reportingPerson.value
    }

    private val _adopter = MutableLiveData<GenericMemberDTO?>()
    val adopter: LiveData<GenericMemberDTO?> = _adopter

    fun setAdopter(item: GenericMemberDTO?) {
        _adopter.value = item
    }

    fun getAdopter(): GenericMemberDTO? {
        return _adopter.value
    }

    fun getSelectedMedicalCondition(): ArrayList<MedicalConditionDTO> {
        return _selectedMedicalCondition.value ?: arrayListOf()
    }

    fun addAllMedicalCondition(items: List<MedicalConditionDTO>) {
        val list = _selectedMedicalCondition.value ?: arrayListOf<MedicalConditionDTO>()
        list.addAll(items)
        _selectedMedicalCondition.value = list
    }

    fun addMedicalCondition(item: MedicalConditionDTO) {
        val list = _selectedMedicalCondition.value ?: arrayListOf<MedicalConditionDTO>()
        list.add(item)
        _selectedMedicalCondition.value = list
    }

    fun removeMedicalCondition(item: MedicalConditionDTO) {
        val list = _selectedMedicalCondition.value ?: arrayListOf<MedicalConditionDTO>()
        list.remove(item)
        _selectedMedicalCondition.value = list
    }

    fun resetSelection() {
        _selectedMedicalCondition.value = arrayListOf()
    }

    // Allow Release
    private val _allowRelease = MutableLiveData<Boolean>(false)
    fun getAllowRelease(): Boolean {
        return _allowRelease.value ?: false
    }
    fun setAllowRelease(flag: Boolean) {
        _allowRelease.value = flag
    }

    // Allow Admission
    private val _allowAdmission = MutableLiveData<Boolean>(false)
    fun getAllowAdmission(): Boolean {
        return _allowAdmission.value ?: false
    }
    fun setAllowAdmission(flag: Boolean) {
        _allowAdmission.value = flag
    }

}