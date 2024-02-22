package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.MainActivity
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.SettingsAdapter
import com.nextsavy.pawgarage.databinding.FragmentSettingsBinding
import com.nextsavy.pawgarage.models.AdmissionDTO
import com.nextsavy.pawgarage.models.MedicalConditionDTO
import com.nextsavy.pawgarage.models.SettingsModel
import com.nextsavy.pawgarage.models.TreatmentDTO
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionMedicalConditionsList
import com.nextsavy.pawgarage.utils.CollectionTreatment
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.MedicineType

class SettingsFragment : Fragment(), RecyclerViewPagingInterface<SettingsModel> {

    private lateinit var binding: FragmentSettingsBinding
    private var adapter = SettingsAdapter(arrayListOf(), this)
    private lateinit var userType: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        val info = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        binding.appVersionTV.text = "App version : ${info.versionName}(${info.versionCode})"

        userType = Helper.sharedPreference?.getString("USER_TYPE", "").toString()
        Log.e("USER", userType)

        binding.toolbarMain.titleToolbarMain.text = "Settings"

        setUpRecyclerView()
        onClickListeners()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            (requireActivity() as MainActivity?)?.binding?.bottomNav?.selectedItemId = R.id.homeFragment
        }
        return binding.root
    }

    private fun onClickListeners() {
        binding.toolbarMain.notificationsIV.setOnClickListener {
            it.findNavController().navigate(R.id.notificationsFragment)
        }
    }

    private fun setUpRecyclerView() {
        val settingsList = arrayListOf<SettingsModel>()

        if (userType == CollectionWhitelistedNumbers.ADMIN) {
            settingsList.add(SettingsModel(R.drawable.ic_person, "Profile"))
            settingsList.add(SettingsModel(R.drawable.ic_team_leaders, "Team Leaders"))
            settingsList.add(SettingsModel(R.drawable.team_members, "Team Members"))
            settingsList.add(SettingsModel(R.drawable.ic_vaccine, "Vaccine List"))
            settingsList.add(SettingsModel(R.drawable.ic_medicine, "Deworming Meds List"))
            settingsList.add(SettingsModel(R.drawable.ic_deworming, "Medical Conditions"))
            settingsList.add(SettingsModel(R.drawable.ic_team_leaders, "Reporting Persons"))
            settingsList.add(SettingsModel(R.drawable.ic_team_leaders, "Adopters"))
            settingsList.add(SettingsModel(R.drawable.ic_reports, "Reports"))
        } else if (userType == CollectionWhitelistedNumbers.TEAM_LEADER) {
            settingsList.add(SettingsModel(R.drawable.ic_person, "Profile"))
            settingsList.add(SettingsModel(R.drawable.ic_team_leaders, "Reporting Persons"))
            settingsList.add(SettingsModel(R.drawable.ic_team_leaders, "Adopters"))
        } else {
            settingsList.add(SettingsModel(R.drawable.ic_person, "Profile"))
        }

        binding.settingsRV.layoutManager = LinearLayoutManager(requireContext())
        adapter.updateDataSource(settingsList)
        binding.settingsRV.adapter = adapter
    }

    override fun didScrolledToEnd(position: Int) {}

    override fun dataSourceDidUpdate(size: Int) {}

    override fun didSelectItem(dataItem: SettingsModel, position: Int) {
        when (userType) {
            CollectionWhitelistedNumbers.ADMIN -> {
                when(dataItem.title) {
                    "Profile" -> {
                         findNavController().navigate(R.id.userProfileFragment)
                    }
                    "Team Leaders" -> findNavController().navigate(R.id.teamLeadersListFragment)
                    "Team Members" -> findNavController().navigate(R.id.teamMemberListFragment)
                    "Vaccine List" -> findNavController().navigate(R.id.adminGenericMedicineList, Bundle().apply { putString("genericMedicineType", MedicineType.VACCINE.name) })
                    "Deworming Meds List" -> findNavController().navigate(R.id.adminGenericMedicineList, Bundle().apply { putString("genericMedicineType", MedicineType.DEWORMING.name) })
                    "Medical Conditions" -> findNavController().navigate(R.id.adminGenericMedicineList, Bundle().apply { putString("genericMedicineType", MedicineType.MEDICAL_CONDITION.name) })
                    "Reporting Persons" -> findNavController().navigate(R.id.reportingPersonsListFragment, Bundle().apply { putString("From", "Settings") })
                    "Adopters" -> findNavController().navigate(R.id.adopterListFragment, Bundle().apply { putBoolean("allowPicking", false) })
                    "Reports" -> findNavController().navigate(R.id.reportsFragment)
                }
            }
            CollectionWhitelistedNumbers.TEAM_LEADER -> {
                when(dataItem.title) {
                    "Profile" -> findNavController().navigate(R.id.userProfileFragment)
                    "Reporting Persons" -> findNavController().navigate(R.id.reportingPersonsListFragment, Bundle().apply { putString("From", "Settings") })
                    "Adopters" -> findNavController().navigate(R.id.adopterListFragment, Bundle().apply { putBoolean("allowPicking", false) })
                }
            }
            CollectionWhitelistedNumbers.TEAM_MEMBER -> {
                when(dataItem.title) {
                    "Profile" -> findNavController().navigate(R.id.userProfileFragment)
                }
            }
        }
    }

    // -----
    private fun updateAdmissionForMedicalConditions() {
        binding.progressBar.visibility = View.VISIBLE
        Firebase.firestore.collection(CollectionMedicalConditionsList.name)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = Firebase.firestore.batch()
                val medConditionList = querySnapshot.mapNotNull { doc -> MedicalConditionDTO.create(doc.id, doc.data) }
                Log.e("NST-M", "Total medical conditions(A):\t${medConditionList.size}")
                Firebase.firestore.collection(CollectionAdmission.name)
                    .get()
                    .addOnSuccessListener { admissionQuerySnapshot ->
                        val admissionList = admissionQuerySnapshot.mapNotNull { doc -> AdmissionDTO.create(doc.id, doc.data) }
                        Log.e("NST-M", "Total admission doc count:\t${admissionList.size}")
                        for (admission in admissionList) {
                            if (admission.medical_conditions.isNotBlank() && admission.medicalConditionIds.isEmpty()) {
                                val medConditionNames = admission.medical_conditions.split(",").map { it.trim() }
                                if (medConditionNames.isNotEmpty()) {
                                    val medConIds = arrayListOf<String>()
                                    for (medConName in medConditionNames) {
                                        val medConModel = medConditionList.firstOrNull { it.name == medConName }
                                        medConModel?.let {
                                            medConIds.add(medConModel.id)
                                        }
                                    }
                                    if (medConIds.isNotEmpty()) {
                                        val data = hashMapOf(
                                            CollectionAdmission.kMedicalConditionIds to medConIds,
                                        )
                                        batch.set(
                                            Firebase.firestore.collection(CollectionAdmission.name).document(admission.id),
                                            data,
                                            SetOptions.merge()
                                        )
                                    }
                                }
                            }
                        }
                        batch.commit().addOnCompleteListener { batchTask ->
                            binding.progressBar.visibility = View.GONE
                            if (batchTask.isSuccessful) {
                                Toast.makeText(requireContext(), "Admission: Medical conditions updated", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("NST-M", "Settings > updateAdmissionForMedicalConditions: ${batchTask.exception?.localizedMessage}")
                            }
                        }
                    }
            }.addOnFailureListener { e ->
                Log.e("NST", "Settings > updateAdmissionForMedicalConditions: ${e.message}")
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun updateTreatmentForMedicalConditions() {
        binding.progressBar.visibility = View.VISIBLE
        Firebase.firestore.collection(CollectionMedicalConditionsList.name)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = Firebase.firestore.batch()
                val medConditionList = querySnapshot.mapNotNull { doc -> MedicalConditionDTO.create(doc.id, doc.data) }
                Log.e("NST-M", "Total medical conditions(T):\t${medConditionList.size}")
                Firebase.firestore.collection(CollectionTreatment.name)
                    .get()
                    .addOnSuccessListener { treatmentQuerySnapshot ->
                        val treatmentList = treatmentQuerySnapshot.mapNotNull { doc -> TreatmentDTO.create(doc.id, doc.data) }
                        Log.e("NST-M", "Total treatment doc count:\t${treatmentList.size}")
                        for (treatment in treatmentList) {
                            if (treatment.medical_conditions.isNotBlank() && treatment.medicalConditionIds.isEmpty()) {
                                val medConditionNames = treatment.medical_conditions.split(",").map { it.trim() }
                                if (medConditionNames.isNotEmpty()) {
                                    val medConIds = arrayListOf<String>()
                                    for (medConName in medConditionNames) {
                                        val medConModel = medConditionList.firstOrNull { it.name == medConName }
                                        medConModel?.let {
                                            medConIds.add(medConModel.id)
                                        }
                                    }
                                    if (medConIds.isNotEmpty()) {
                                        val data = hashMapOf(
                                            CollectionTreatment.kMedicalConditionIds to medConIds,
                                        )
                                        batch.set(
                                            Firebase.firestore.collection(CollectionTreatment.name).document(treatment.id),
                                            data,
                                            SetOptions.merge()
                                        )
                                    }
                                }
                            }
                        }
                        batch.commit().addOnCompleteListener { batchTask ->
                            binding.progressBar.visibility = View.GONE
                            if (batchTask.isSuccessful) {
                                Toast.makeText(requireContext(), "Treatment: Medical conditions updated", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("NST-M", "Settings > updateTreatmentForMedicalConditions: ${batchTask.exception?.localizedMessage}")
                            }
                        }
                    }
            }.addOnFailureListener { e ->
                Log.e("NST", "Settings > updateTreatmentForMedicalConditions: ${e.message}")
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun startDeleteField() {
        binding.progressBar.visibility = View.VISIBLE
        Firebase.firestore.collection(CollectionAdmission.name)
            .get()
            .addOnSuccessListener { treatmentQuerySnapshot ->
                val batch = Firebase.firestore.batch()
                val admissionList = treatmentQuerySnapshot.mapNotNull { doc -> AdmissionDTO.create(doc.id, doc.data) }
                for (admission in admissionList) {
                    val data = hashMapOf<String, Any>(
                        "first" to FieldValue.delete(),
                        "second" to FieldValue.delete(),
                    )
                    batch.update(Firebase.firestore.collection(CollectionAdmission.name).document(admission.id), data)
                }
                batch.commit().addOnCompleteListener { batchTask ->
                    binding.progressBar.visibility = View.GONE
                    if (batchTask.isSuccessful) {
                        Toast.makeText(requireContext(), "Field deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("NST-M", "Settings > startDeleteField: ${batchTask.exception?.localizedMessage}")
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("NST", "Settings > startDeleteField: ${e.message}")
                binding.progressBar.visibility = View.GONE
            }
    }
    // -----

}