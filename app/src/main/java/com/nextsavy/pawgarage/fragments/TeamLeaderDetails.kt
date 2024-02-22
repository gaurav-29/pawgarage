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
import com.nextsavy.pawgarage.databinding.FragmentTeamLeaderDetailsBinding
import com.nextsavy.pawgarage.models.TeamLeadersModel
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper

class TeamLeaderDetails : Fragment() {

    private lateinit var binding: FragmentTeamLeaderDetailsBinding
    private val args: TeamLeaderDetailsArgs by navArgs()
    private var teamLeaderDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTeamLeaderDetailsBinding.inflate(inflater, container, false)
        teamLeaderDocId = args.userDocId
        binding.toolbarOne.titleToolbarOne.text = if (teamLeaderDocId == null) "Create Profile" else "Edit Profile"
        binding.saveTV.text = if (teamLeaderDocId == null) "Create" else "Update"
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.saveBTN.setOnClickListener(saveButtonTapped)
        if (teamLeaderDocId != null) {
            binding.numberET.isEnabled = false
            getTeamLeaderDetails()
        } else {
            binding.numberET.isEnabled = true
        }
        Log.e("FLOW", "TeamLeaderDetails fragment.")
        return binding.root
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

    private val saveButtonTapped = View.OnClickListener {
        if (checkValidation()) {
            binding.progressBar.root.visibility = View.VISIBLE
            if (teamLeaderDocId != null) {
                addOrUpdateTeamLeader()
            } else {
                Firebase.firestore
                    .collection(CollectionWhitelistedNumbers.name)
                    .whereEqualTo(CollectionWhitelistedNumbers.kContactNumber, "+91${binding.numberET.text.trim()}")
                    .whereEqualTo(CollectionWhitelistedNumbers.kIsArchive, false)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.documents.isNotEmpty()) {
                            Toast.makeText(requireContext(), "Phone number already exist.", Toast.LENGTH_SHORT).show()
                            binding.progressBar.root.visibility = View.GONE
                        } else {
                            addOrUpdateTeamLeader()
                        }
                    }.addOnFailureListener { e ->
                        Log.e("NST-M", "Exception: TeamLeaderDetails > ${e.localizedMessage}")
                        binding.progressBar.root.visibility = View.GONE
                    }
            }
        } else {
            Log.e("NST-M", "Team leader validation not pass")
        }
    }

    private fun getTeamLeaderDetails() {
        if (Helper.isInternetAvailable(requireContext())) {
            Firebase.firestore
                .collection(CollectionWhitelistedNumbers.name)
                .document(this.teamLeaderDocId!!)
                .get()
                .addOnSuccessListener { docSnap ->
                    val model = TeamLeadersModel()
                    model.documentId = docSnap.id
                    model.name = docSnap.data?.get(CollectionWhitelistedNumbers.kUserName) as String
                    model.number = docSnap.data?.get(CollectionWhitelistedNumbers.kContactNumber) as String
                    configureUI(model)
                }
                .addOnFailureListener { exception ->
                    Log.e("NST-M", "Exception: AnimalDetailsFrag > ${exception.localizedMessage}")
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureUI(teamLeaderModel: TeamLeadersModel) {
        binding.nameET.setText(teamLeaderModel.name)
        binding.numberET.setText(teamLeaderModel.number.substring(teamLeaderModel.number.length - 10))
    }

    private fun addOrUpdateTeamLeader() {

        val textKeywords = generateSearchKeywords(binding.nameET.text.trim().toString())
        val numKeywords = generateSearchKeywords(binding.numberET.text.trim().toString())
        val searchKeywords = textKeywords.plus(numKeywords)
        Log.e("SEA", searchKeywords.toString())

        var teamLeaderData = hashMapOf<String, Any?>()

        if (teamLeaderDocId != null) {
            teamLeaderData[CollectionWhitelistedNumbers.kUserName] = binding.nameET.text.toString().trim()
            teamLeaderData[CollectionWhitelistedNumbers.kSearchKeywords] = searchKeywords
            teamLeaderData[CollectionWhitelistedNumbers.kUpdatedAt] = FieldValue.serverTimestamp()
            teamLeaderData[CollectionWhitelistedNumbers.kUpdatedBy] = Firebase.auth.currentUser?.uid
        } else {
            teamLeaderData = hashMapOf(
                CollectionWhitelistedNumbers.kUserName to binding.nameET.text.toString().trim(),
                CollectionWhitelistedNumbers.kContactNumber to "+91${binding.numberET.text.trim()}",
                CollectionWhitelistedNumbers.kSearchKeywords to searchKeywords,
                CollectionWhitelistedNumbers.kIsArchive to false,
                CollectionWhitelistedNumbers.kUserType to CollectionWhitelistedNumbers.TEAM_LEADER,
                CollectionWhitelistedNumbers.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionWhitelistedNumbers.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
        }
        val teamLeaderRef = if (teamLeaderDocId != null) {
            Firebase.firestore.collection(CollectionWhitelistedNumbers.name).document(teamLeaderDocId!!)
        } else {
            Firebase.firestore.collection(CollectionWhitelistedNumbers.name).document()
        }
        teamLeaderRef.set(teamLeaderData, SetOptions.merge()).addOnSuccessListener {
            binding.progressBar.root.visibility = View.GONE
            Toast.makeText(
                requireContext(),
                "Team leader" + if (teamLeaderDocId != null) " updated" else " added" + " successfully",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        }.addOnFailureListener { e ->
            binding.progressBar.root.visibility = View.GONE
            Toast.makeText(
                requireContext(), e.localizedMessage
                    ?: ("Unable to" + if (teamLeaderDocId != null) " update " else " added " + "team leader"), Toast.LENGTH_SHORT
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