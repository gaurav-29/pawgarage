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
import com.nextsavy.pawgarage.MainActivity
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentEditUserProfileBinding
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper

class EditUserProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditUserProfileBinding
    private val db = Firebase.firestore
    private var last10Digits: String = ""
    var documentId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditUserProfileBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "Profile"

        onClickListeners()
        getCurrentUserProfile()
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        val userType = Helper.sharedPreference?.getString("USER_TYPE", "").toString()
        if (userType == CollectionWhitelistedNumbers.TEAM_MEMBER) {
            (activity as MainActivity).hideBottomNavigation()
        } else {
            (activity as MainActivity).showBottomNavigation()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).showBottomNavigation()
    }
    private fun onClickListeners() {
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.saveBTN.setOnClickListener {
            if (checkValidation()) {
                updateCurrentUserProfile()
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

    private fun getCurrentUserProfile() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            db.collection(CollectionUser.name).document(Firebase.auth.currentUser!!.uid)
                .get()
                .addOnSuccessListener { docSnapshot ->
                    if (docSnapshot.data != null) {
                        binding.nameET.setText((docSnapshot.data?.get(CollectionUser.kUserName) as String?) ?: "")
                        binding.numberET.setText((docSnapshot.data?.get(CollectionUser.kContactNumber) as String?) ?: "")
                    } else {
                        Toast.makeText(requireContext(), "User not found!", Toast.LENGTH_LONG).show()
                    }
                    binding.progressBar2.visibility = View.GONE
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("ADMIN", "Error getting admin details: ", exception)
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCurrentUserProfile() {
        if (Helper.isInternetAvailable(requireContext())) {

            val updateUserData = hashMapOf(
                CollectionUser.kUserName to binding.nameET.text.toString().trim(),
                CollectionUser.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionUser.kUpdatedBy to Firebase.auth.currentUser?.uid
            )
            val updateWhitelistData = hashMapOf(
                CollectionWhitelistedNumbers.kUserName to binding.nameET.text.toString().trim(),
                CollectionWhitelistedNumbers.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionWhitelistedNumbers.kUpdatedBy to Firebase.auth.currentUser?.uid
            )

            binding.progressBar2.visibility = View.VISIBLE
            Firebase.firestore
                .collection(CollectionUser.name)
                .document(Firebase.auth.currentUser!!.uid)
                .set(updateUserData, SetOptions.merge())
                .addOnSuccessListener {
                    Firebase.firestore
                        .collection(CollectionWhitelistedNumbers.name)
                        .whereEqualTo(CollectionWhitelistedNumbers.kContactNumber, binding.numberET.text.toString().trim())
                        .limit(1)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.documents.isNotEmpty()) {
                                Firebase.firestore
                                    .collection(CollectionWhitelistedNumbers.name)
                                    .document(querySnapshot.documents.last().id)
                                    .set(updateWhitelistData, SetOptions.merge())
                                    .addOnSuccessListener {
                                        binding.progressBar2.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Profile updated successfully.", Toast.LENGTH_LONG).show()
                                        findNavController().popBackStack()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("NST-M", "EditAdminProfileFragment: Edit profile > CollectionWhiteList: ${e.localizedMessage}")
                                        binding.progressBar2.visibility = View.GONE
                                    }
                            } else {
                                // Success > Whitelist doc not found!
                                binding.progressBar2.visibility = View.GONE
                                Toast.makeText(requireContext(), "Profile updated successfully.", Toast.LENGTH_LONG).show()
                                findNavController().popBackStack()
                            }
                        }
                }.addOnFailureListener { ex ->
                    Log.e("NST-M", "EditAdminProfileFragment: Edit profile > CollectionUser: ${ex.localizedMessage}")
                    binding.progressBar2.visibility = View.GONE
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }
}