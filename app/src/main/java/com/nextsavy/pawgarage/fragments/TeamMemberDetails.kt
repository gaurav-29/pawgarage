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
import com.nextsavy.pawgarage.databinding.FragmentTeamMemberDetailsBinding
import com.nextsavy.pawgarage.models.TeamLeadersModel
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper


class TeamMemberDetails : Fragment() {

    private lateinit var binding: FragmentTeamMemberDetailsBinding
    private val args: TeamMemberDetailsArgs by navArgs()
    private var teamMemberDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTeamMemberDetailsBinding.inflate(inflater, container, false)
        teamMemberDocId = args.userDocId
        binding.toolbarOne.titleToolbarOne.text = if (teamMemberDocId == null) "Create Profile" else "Edit Profile"
        binding.saveTV.text = if (teamMemberDocId == null) "Create" else "Update"
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.saveBTN.setOnClickListener(saveButtonTapped)
        if (teamMemberDocId != null) {
            binding.numberET.isEnabled = false
            getTeamMemberDetails()
        } else {
            binding.numberET.isEnabled = true
        }
        Log.e("FLOW", "TeamMemberDetails fragment.")
        return binding.root
    }

    private val saveButtonTapped = View.OnClickListener {
        if (checkValidation()) {
            binding.progressBar.root.visibility = View.VISIBLE
            if (teamMemberDocId != null) {
                addOrUpdateTeamMember()
            } else {
                Firebase.firestore
                    .collection(CollectionWhitelistedNumbers.name)
                    .whereEqualTo(CollectionWhitelistedNumbers.kContactNumber, "+91${binding.numberET.text.trim()}")
                    .whereEqualTo(CollectionWhitelistedNumbers.kIsArchive, false)
                    .get()
                    .addOnSuccessListener {querySnapshot ->
                        if (querySnapshot.documents.isNotEmpty()) {
                            Toast.makeText(requireContext(), "Phone number already exist.", Toast.LENGTH_SHORT).show()
                            binding.progressBar.root.visibility = View.GONE
                        } else {
                            addOrUpdateTeamMember()
                        }
                    }.addOnFailureListener { e ->
                        Log.e("NST-M", "Exception: TeamMemberDetails > ${e.localizedMessage}")
                        binding.progressBar.root.visibility = View.GONE
                    }
            }
        } else {
            Log.e("NST-M", "Team member validation not pass")
        }
    }

    private fun getTeamMemberDetails() {
        if (Helper.isInternetAvailable(requireContext())) {
            Firebase.firestore
                .collection(CollectionWhitelistedNumbers.name)
                .document(this.teamMemberDocId!!)
                .get()
                .addOnSuccessListener { docSnap ->
                    val model = TeamLeadersModel()
                    model.documentId = docSnap.id
                    model.name = docSnap.data?.get(CollectionWhitelistedNumbers.kUserName) as String
                    model.number = docSnap.data?.get(CollectionWhitelistedNumbers.kContactNumber) as String
                    configureUI(model)
                }
                .addOnFailureListener { exception ->
                    Log.e("NST-M", "Exception: TeamMemberDetailsFrag > ${exception.localizedMessage}")
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureUI(teamMemberModel: TeamLeadersModel) {
        binding.nameET.setText(teamMemberModel.name)
        binding.numberET.setText(teamMemberModel.number.substring(teamMemberModel.number.length - 10))
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
    private fun addOrUpdateTeamMember() {

        val textKeywords = generateSearchKeywords(binding.nameET.text.trim().toString())
        val numKeywords = generateSearchKeywords(binding.numberET.text.trim().toString())
        val searchKeywords = textKeywords.plus(numKeywords)

        val teamMemberData = hashMapOf<String, Any?>()
        if (teamMemberDocId == null) {
            teamMemberData[CollectionWhitelistedNumbers.kUserName] = binding.nameET.text.toString().trim()
            teamMemberData[CollectionWhitelistedNumbers.kContactNumber] = "+91${binding.numberET.text.trim()}"
            teamMemberData[CollectionWhitelistedNumbers.kSearchKeywords] = searchKeywords
            teamMemberData[CollectionWhitelistedNumbers.kIsArchive] = false
            teamMemberData[CollectionWhitelistedNumbers.kUserType] = CollectionWhitelistedNumbers.TEAM_MEMBER
            teamMemberData[CollectionWhitelistedNumbers.kCreatedAt] = FieldValue.serverTimestamp()
            teamMemberData[CollectionWhitelistedNumbers.kCreatedBy] = Firebase.auth.currentUser?.uid
        } else {
            teamMemberData[CollectionWhitelistedNumbers.kUserName] = binding.nameET.text.toString().trim()
            teamMemberData[CollectionWhitelistedNumbers.kSearchKeywords] = searchKeywords
            teamMemberData[CollectionWhitelistedNumbers.kUpdatedAt] = FieldValue.serverTimestamp()
            teamMemberData[CollectionWhitelistedNumbers.kUpdatedBy] = Firebase.auth.currentUser?.uid
        }

        val teamMemberRef = if (teamMemberDocId != null) {
            Firebase.firestore.collection(CollectionWhitelistedNumbers.name).document(teamMemberDocId!!)
        } else {
            Firebase.firestore.collection(CollectionWhitelistedNumbers.name).document()
        }
        teamMemberRef.set(teamMemberData, SetOptions.merge()).addOnSuccessListener {
            binding.progressBar.root.visibility = View.GONE
            Toast.makeText(
                requireContext(),
                "Team member" + if (teamMemberDocId != null) " updated" else " added" + " successfully",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        }.addOnFailureListener { e ->
            binding.progressBar.root.visibility = View.GONE
            Toast.makeText(
                requireContext(), e.localizedMessage
                    ?: ("Unable to" + if (teamMemberDocId != null) " update " else " added " + "team member"), Toast.LENGTH_SHORT
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
}