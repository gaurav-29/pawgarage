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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentAdopterDetailsBinding
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.utils.CollectionAdopters
import com.nextsavy.pawgarage.utils.Helper


class AdopterDetailsFragment : Fragment() {

    private lateinit var binding: FragmentAdopterDetailsBinding
    private var adopterId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdopterDetailsBinding.inflate(inflater, container, false)

        arguments?.takeIf { it.containsKey("adopterId") }?.apply {
            adopterId = getString("adopterId") as? String
        }

        binding.toolbarOne.titleToolbarOne.text = if (adopterId == null) "Create Profile" else "Edit Profile"
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().popBackStack()
        }

        binding.saveTV.text = if (adopterId == null) "Create" else "Update"
        binding.saveBTN.setOnClickListener(saveButtonTapped)

        if (adopterId != null) {
            getAdopterDetails()
        } else {
            binding.numberET.isEnabled = true
        }

        return binding.root
    }

    private fun getAdopterDetails() {
        if (Helper.isInternetAvailable(requireContext())) {
            Firebase.firestore
                .collection(CollectionAdopters.name)
                .document(this.adopterId!!)
                .get()
                .addOnSuccessListener { docSnap ->
                    val adopter = GenericMemberDTO.create(docSnap.id, docSnap.data)
                    if (adopter != null) {
                        configureUI(adopter)
                    } else {
                        Toast.makeText(requireContext(), "Adopter not found!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("NST-M", "Exception: getAdopterDetails > ${exception.localizedMessage}")
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureUI(adopter: GenericMemberDTO) {
        binding.nameET.setText(adopter.name)
        binding.numberET.setText(adopter.phoneNumber.substring(adopter.phoneNumber.length - 10))
    }

    private val saveButtonTapped = View.OnClickListener {
        if (checkValidation()) {
            binding.progressBar.root.visibility = View.VISIBLE
            Firebase.firestore
                .collection(CollectionAdopters.name)
                .whereEqualTo(CollectionAdopters.kContactNumber, "+91${binding.numberET.text.trim()}")
                .whereEqualTo(CollectionAdopters.kIsArchive, false)
                .get()
                .addOnSuccessListener {querySnapshot ->
                    if (querySnapshot.documents.isNotEmpty() && querySnapshot.documents.size > 1) {
                        Toast.makeText(requireContext(), "Phone number already exist.", Toast.LENGTH_SHORT).show()
                        binding.progressBar.root.visibility = View.GONE
                    } else if (querySnapshot.documents.isNotEmpty() && querySnapshot.documents.size == 1 && querySnapshot.documents.first().id != adopterId) {
                        Toast.makeText(requireContext(), "Phone number already exist.", Toast.LENGTH_SHORT).show()
                        binding.progressBar.root.visibility = View.GONE
                    } else {
                        addOrUpdateAdopter()
                    }
                }.addOnFailureListener { e ->
                    Log.e("NST-M", "Exception: AdopterDetails > ${e.localizedMessage}")
                    binding.progressBar.root.visibility = View.GONE
                }
//            }
        } else {
            Log.e("NST-M", "Adopter validation not pass")
        }
    }

    private fun addOrUpdateAdopter() {
        val textKeywords = generateSearchKeywords(binding.nameET.text.trim().toString())
        val numKeywords = generateSearchKeywords(binding.numberET.text.trim().toString())
        val searchKeywords = textKeywords.plus(numKeywords)

        val adopterData: HashMap<String, Any?> = hashMapOf()
        if (adopterId != null) {
            adopterData[CollectionAdopters.kUserName] = binding.nameET.text.toString().trim()
            adopterData[CollectionAdopters.kContactNumber] = "+91${binding.numberET.text.trim()}"
            adopterData[CollectionAdopters.kSearchKeywords] = searchKeywords
            adopterData[CollectionAdopters.kUpdatedAt] = FieldValue.serverTimestamp()
            adopterData[CollectionAdopters.kUpdatedBy] = Firebase.auth.currentUser?.uid
        } else {
            adopterData[CollectionAdopters.kUserName] = binding.nameET.text.toString().trim()
            adopterData[CollectionAdopters.kContactNumber] = "+91${binding.numberET.text.trim()}"
            adopterData[CollectionAdopters.kSearchKeywords] = searchKeywords
            adopterData[CollectionAdopters.kIsArchive] = false
            adopterData[CollectionAdopters.kCreatedAt] = FieldValue.serverTimestamp()
            adopterData[CollectionAdopters.kCreatedBy] = Firebase.auth.currentUser?.uid
        }
        val adopterDocRef = if (adopterId != null) {
            Firebase.firestore.collection(CollectionAdopters.name).document(adopterId!!)
        } else {
            Firebase.firestore.collection(CollectionAdopters.name).document()
        }
        adopterDocRef.set(adopterData, SetOptions.merge()).addOnSuccessListener {
            binding.progressBar.root.visibility = View.GONE
            Toast.makeText(requireContext(), "Adopter details" + if (adopterId != null) " updated" else " added" + " successfully.",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        }.addOnFailureListener { e ->
            binding.progressBar.root.visibility = View.GONE
            Toast.makeText(requireContext(), e.localizedMessage ?: ("Unable to" + if (adopterId != null) " update " else " add " + "adopter."), Toast.LENGTH_SHORT
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