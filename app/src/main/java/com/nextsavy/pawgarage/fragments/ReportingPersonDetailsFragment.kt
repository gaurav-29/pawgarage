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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentReportingPersonDetailsBinding
import com.nextsavy.pawgarage.models.TeamLeadersModel
import com.nextsavy.pawgarage.utils.CollectionReportingPersons
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper


class ReportingPersonDetailsFragment : Fragment() {

    private lateinit var binding: FragmentReportingPersonDetailsBinding
    private val args: ReportingPersonDetailsFragmentArgs by navArgs()
    private var personDocId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportingPersonDetailsBinding.inflate(inflater, container, false)

        personDocId = args.userDocId
        binding.toolbarOne.titleToolbarOne.text = if (personDocId == null) "Create Profile" else "Edit Profile"
        binding.saveTV.text = if (personDocId == null) "Create" else "Update"
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.saveBTN.setOnClickListener(saveButtonTapped)
        if (personDocId != null) {
//            binding.numberET.isEnabled = false
            getReportingPersonDetails()
        } else {
            binding.numberET.isEnabled = true
        }

        return binding.root
    }

    private val saveButtonTapped = View.OnClickListener {
        if (checkValidation()) {
            binding.progressBar.root.visibility = View.VISIBLE
//            if (personDocId != null) {
//                addOrUpdateReportingPerson()
//            } else {
                Firebase.firestore
                    .collection(CollectionReportingPersons.name)
                    .whereEqualTo(CollectionReportingPersons.kContactNumber, "+91${binding.numberET.text.trim()}")
                    .whereEqualTo(CollectionReportingPersons.kIsArchive, false)
                    .get()
                    .addOnSuccessListener {querySnapshot ->
                        if (querySnapshot.documents.isNotEmpty() && querySnapshot.documents.size > 1) {
                            Toast.makeText(requireContext(), "Phone number already exist.", Toast.LENGTH_SHORT).show()
                            binding.progressBar.root.visibility = View.GONE
                        } else if (querySnapshot.documents.isNotEmpty() && querySnapshot.documents.size == 1 && querySnapshot.documents.first().id != personDocId) {
                            Toast.makeText(requireContext(), "Phone number already exist.", Toast.LENGTH_SHORT).show()
                            binding.progressBar.root.visibility = View.GONE
                        }
                        else {
                            addOrUpdateReportingPerson()
                        }
                    }.addOnFailureListener { e ->
                        Log.e("NST-M", "Exception: ReportingPersonDetails > ${e.localizedMessage}")
                        binding.progressBar.root.visibility = View.GONE
                    }
//            }
        } else {
            Log.e("NST-M", "Reporting person's validation not pass")
        }
    }

    private fun getReportingPersonDetails() {
        if (Helper.isInternetAvailable(requireContext())) {
            Firebase.firestore
                .collection(CollectionReportingPersons.name)
                .document(this.personDocId!!)
                .get()
                .addOnSuccessListener { docSnap ->
                    val model = TeamLeadersModel()
                    model.documentId = docSnap.id
                    model.name = docSnap.data?.get(CollectionWhitelistedNumbers.kUserName) as String
                    model.number = docSnap.data?.get(CollectionWhitelistedNumbers.kContactNumber) as String
                    configureUI(model)
                }
                .addOnFailureListener { exception ->
                    Log.e("NST-M", "Exception: ReportingPersonDetailsFrag > ${exception.localizedMessage}")
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }
    private fun configureUI(teamLeaderModel: TeamLeadersModel) {
        binding.nameET.setText(teamLeaderModel.name)
        binding.numberET.setText(teamLeaderModel.number.substring(teamLeaderModel.number.length - 10))
    }

    private fun addOrUpdateReportingPerson() {

        val textKeywords = generateSearchKeywords(binding.nameET.text.trim().toString())
        val numKeywords = generateSearchKeywords(binding.numberET.text.trim().toString())
        val searchKeywords = textKeywords.plus(numKeywords)
        Log.e("SEA", searchKeywords.toString())

        val reportingPersonData: HashMap<String, Any?> = hashMapOf()
//        val reportingPersonData = hashMapOf(
//            CollectionReportingPersons.kUserName to binding.nameET.text.toString().trim(),
//            CollectionReportingPersons.kContactNumber to "+91${binding.numberET.text.trim()}",
//            CollectionReportingPersons.kSearchKeywords to searchKeywords,
//            CollectionReportingPersons.kIsArchive to false,
//            CollectionReportingPersons.kCreatedAt to FieldValue.serverTimestamp(),
//            CollectionReportingPersons.kCreatedBy to Firebase.auth.currentUser?.uid,
//        )
        if (personDocId != null) {
            reportingPersonData[CollectionReportingPersons.kUserName] = binding.nameET.text.toString().trim()
            reportingPersonData[CollectionReportingPersons.kContactNumber] = "+91${binding.numberET.text.trim()}"
            reportingPersonData[CollectionReportingPersons.kSearchKeywords] = searchKeywords
            reportingPersonData[CollectionReportingPersons.kUpdatedAt] = FieldValue.serverTimestamp()
            reportingPersonData[CollectionReportingPersons.kUpdatedBy] = Firebase.auth.currentUser?.uid
        } else {
            reportingPersonData[CollectionReportingPersons.kUserName] = binding.nameET.text.toString().trim()
            reportingPersonData[CollectionReportingPersons.kContactNumber] = "+91${binding.numberET.text.trim()}"
            reportingPersonData[CollectionReportingPersons.kSearchKeywords] = searchKeywords
            reportingPersonData[CollectionReportingPersons.kIsArchive] = false
            reportingPersonData[CollectionReportingPersons.kCreatedAt] = FieldValue.serverTimestamp()
            reportingPersonData[CollectionReportingPersons.kCreatedBy] = Firebase.auth.currentUser?.uid
        }
        val reportingPersonDocRef = if (personDocId != null) {
            Firebase.firestore.collection(CollectionReportingPersons.name).document(personDocId!!)
        } else {
            Firebase.firestore.collection(CollectionReportingPersons.name).document()
        }
        reportingPersonDocRef.set(reportingPersonData, SetOptions.merge()).addOnSuccessListener {
            binding.progressBar.root.visibility = View.GONE
            Toast.makeText(requireContext(), "Reporting person" + if (personDocId != null) " updated" else " added" + " successfully.",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        }.addOnFailureListener { e ->
            binding.progressBar.root.visibility = View.GONE
            Toast.makeText(requireContext(), e.localizedMessage ?: ("Unable to" + if (personDocId != null) " update " else " added " + "reporting person."), Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun checkValidation(): Boolean {
        if (!binding.nameET.text.isNullOrBlank()) {
            binding.nameET.error = null
        } else {
            binding.nameET.error = "Required"
            return false
        }
        if (binding.numberET.text.isNullOrBlank()) {
            binding.numberET.error = "Required"
            return false
        } else if (binding.numberET.text.trim().toString().length != 10) {
            binding.numberET.error = "Invalid number"
            return false
        } else {
            binding.numberET.error = null
        }
        return true
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
}