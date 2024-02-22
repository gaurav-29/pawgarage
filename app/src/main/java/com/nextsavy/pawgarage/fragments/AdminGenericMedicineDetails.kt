package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentAdminGenericMedicinListBinding
import com.nextsavy.pawgarage.databinding.FragmentAdminGenericMedicineDetailsBinding
import com.nextsavy.pawgarage.models.TeamLeadersModel
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionMedicalConditionsList
import com.nextsavy.pawgarage.utils.CollectionMedicinesList
import com.nextsavy.pawgarage.utils.CollectionVaccination
import com.nextsavy.pawgarage.utils.CollectionVaccinesList
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.MedicineType


class AdminGenericMedicineDetails : Fragment() {

    private lateinit var binding: FragmentAdminGenericMedicineDetailsBinding
    private val args: AdminGenericMedicineDetailsArgs by navArgs()
    private lateinit var genericType: MedicineType
    private var itemDocId: String? = null
    private var nameList = arrayListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminGenericMedicineDetailsBinding.inflate(inflater, container, false)

        itemDocId = arguments?.getString("itemDocId")
        Log.e("MED_ID", itemDocId.toString())

        genericType = MedicineType.valueOf(args.genericMedicineType!!)
        when (genericType) {
            MedicineType.VACCINE -> {
                if (itemDocId != null) {
                    binding.toolbarOne.titleToolbarOne.text = "Edit Vaccine"
                    getVaccineDetails()
                } else {
                    binding.toolbarOne.titleToolbarOne.text = "Add New Vaccine"
                    getVaccineDetails()
                }
            }
            MedicineType.DEWORMING -> {
                if (itemDocId != null) {
                    binding.toolbarOne.titleToolbarOne.text = "Edit Medicine"
                    getMedicineDetails()
                } else {
                    binding.toolbarOne.titleToolbarOne.text = "Add New Medicines"
                    getMedicineDetails()
                }
            }
            MedicineType.MEDICAL_CONDITION -> {
                if (itemDocId != null) {
                    binding.toolbarOne.titleToolbarOne.text = "Edit Medical Conditions"
                    getConditionDetails()
                } else {
                    binding.toolbarOne.titleToolbarOne.text = "Add New Medical Conditions"
                    getConditionDetails()
                }
            }
        }
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.saveBTN.setOnClickListener(saveButtonTapped)
        return binding.root
    }

    private fun getVaccineDetails() {
        if (Helper.isInternetAvailable(requireContext())) {
            Firebase.firestore
                .collection(CollectionVaccinesList.name)
                .whereEqualTo(CollectionVaccinesList.kIsArchive, false)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        Toast.makeText(requireContext(), "No data found.", Toast.LENGTH_SHORT).show()
                    } else {
                        for (document in result) {
                            val name = document.data[CollectionVaccinesList.kName] as String
                            nameList.add(name)
                            nameList.add(name.lowercase())
                            if (itemDocId != null) {
                                if (document.id == itemDocId!!) {
                                    binding.nameET.setText(name)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ERROR", "Error getting documents: ", exception)
                    Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }

        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }
    private fun getMedicineDetails() {

        if (Helper.isInternetAvailable(requireContext())) {
            Firebase.firestore
                .collection(CollectionMedicinesList.name)
                .whereEqualTo(CollectionMedicinesList.kIsArchive, false)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        Toast.makeText(requireContext(), "No data found.", Toast.LENGTH_SHORT).show()
                    } else {
                        for (document in result) {
                            val name = document.data[CollectionMedicinesList.kName] as String
                            nameList.add(name)
                            nameList.add(name.lowercase())
                            if (itemDocId != null) {
                                if (document.id == itemDocId!!) {
                                    binding.nameET.setText(name)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ERROR", "Error getting documents: ", exception)
                    Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }
    private fun getConditionDetails() {

        if (Helper.isInternetAvailable(requireContext())) {
            Firebase.firestore
                .collection(CollectionMedicalConditionsList.name)
                .whereEqualTo(CollectionMedicalConditionsList.kIsArchive, false)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        Toast.makeText(requireContext(), "No data found.", Toast.LENGTH_SHORT).show()
                    } else {
                        for (document in result) {
                            val name = document.data[CollectionMedicalConditionsList.kName] as String
                            nameList.add(name)
                            nameList.add(name.lowercase())
                            if (itemDocId != null) {
                                if (document.id == itemDocId!!) {
                                    binding.nameET.setText(name)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ERROR", "Error getting documents: ", exception)
                    Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private val saveButtonTapped = View.OnClickListener {
        if (checkValidation()) {
            val name = binding.nameET.text.trim().toString()
            Log.e("TEST", name)
            addOrUpdateMedicineDetails()
        }
    }
    private fun generateSearchKeywords(title: String): List<String> {
        var inputString = title.lowercase()
        val keywords = mutableListOf<String>()
        val words = inputString.split(" ")

        for (word in words) {
            var appendString = ""
            for (charPosition in inputString.indices) {
                appendString += inputString[charPosition].toString()
                keywords.add(appendString)
            }
            // Remove word once its characters are added to list
            inputString = inputString.replace("$word ", "")
        }
        return keywords
    }

    private fun addOrUpdateMedicineDetails() {

        binding.progressBar.root.visibility = View.VISIBLE
        val searchKeywords = generateSearchKeywords(binding.nameET.text.trim().toString())

        var genericName = ""

        val collectionName = when (genericType) {
            MedicineType.VACCINE -> {
                genericName = "Vaccine"
                CollectionVaccinesList.name
            }
            MedicineType.DEWORMING -> {
                genericName = "Medicine"
                CollectionMedicinesList.name
            }
            MedicineType.MEDICAL_CONDITION -> {
                genericName = "Medical Condition"
                CollectionMedicalConditionsList.name
            }
        }

        var medicineData: HashMap<String, Any?> = hashMapOf()
        if (itemDocId == null) {
            if (!nameList.contains(binding.nameET.text.trim().toString())) {
                medicineData = hashMapOf(
                    CollectionVaccinesList.kName to binding.nameET.text.trim().toString(),
                    CollectionVaccinesList.kIsArchive to false,
                    CollectionVaccinesList.kSearchKeywords to searchKeywords,
                    CollectionVaccinesList.kCreatedAt to FieldValue.serverTimestamp(),
                    CollectionVaccinesList.kCreatedBy to Firebase.auth.currentUser?.uid,
                )
            }
        } else {
            medicineData = hashMapOf(
                CollectionVaccinesList.kName to binding.nameET.text.trim().toString(),
                CollectionVaccinesList.kSearchKeywords to searchKeywords,
                CollectionVaccinesList.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionVaccinesList.kUpdatedBy to Firebase.auth.currentUser?.uid,
            )
        }

        if (itemDocId != null) {
            Firebase.firestore.collection(collectionName).document(itemDocId!!)
                .set(medicineData, SetOptions.merge()).addOnSuccessListener {
                    binding.progressBar.root.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "$genericName saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().popBackStack()
                }.addOnFailureListener { e ->
                    binding.progressBar.root.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        e.localizedMessage ?: ("Unable to save $genericName"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            if (nameList.contains(binding.nameET.text.trim().toString())) {
                binding.progressBar.root.visibility = View.GONE
                Toast.makeText(requireContext(), "Name already exist.", Toast.LENGTH_SHORT).show()
            } else {
                Firebase.firestore.collection(collectionName).document()
                    .set(medicineData, SetOptions.merge()).addOnSuccessListener {
                        binding.progressBar.root.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "$genericName saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                    }.addOnFailureListener { e ->
                        binding.progressBar.root.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            e.localizedMessage ?: ("Unable to save $genericName"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }
    private fun checkValidation(): Boolean {
        if (!binding.nameET.text.isNullOrBlank()) {
            binding.nameET.error = null
        } else {
            binding.nameET.error = "Required"
            return false
        }
        return true
    }
}