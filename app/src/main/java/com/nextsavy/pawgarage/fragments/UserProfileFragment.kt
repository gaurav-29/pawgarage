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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.nextsavy.pawgarage.MainActivity
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentUserProfileBinding
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Constants
import com.nextsavy.pawgarage.utils.Helper

class UserProfileFragment : Fragment() {

    private lateinit var binding: FragmentUserProfileBinding
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "Profile"
        binding.toolbarOne.generalImgToolbarOne.visibility = View.VISIBLE
        binding.toolbarOne.generalImgToolbarOne.setImageResource(R.drawable.edit)

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

    private fun getCurrentUserProfile() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            db.collection(CollectionUser.name).document(Firebase.auth.currentUser!!.uid)
                .get()
                .addOnSuccessListener { docSnapshot ->
                    if (docSnapshot.data != null) {
                        binding.nameTV2.text = (docSnapshot.data?.get(CollectionUser.kUserName) as String?) ?: "N/A"
                        binding.numberTV2.text = (docSnapshot.data?.get(CollectionUser.kContactNumber) as String?) ?: "N/A"
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

    private fun onClickListeners() {
        binding.logoutButton.setOnClickListener(logoutButtonTapped)
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.toolbarOne.generalImgToolbarOne.setOnClickListener {
            it.findNavController().navigate(UserProfileFragmentDirections.actionUserProfileFragmentToEditUserProfileFragment())
        }
    }

    private val logoutButtonTapped = View.OnClickListener {
        Constants.showAlertWithListeners(
            requireContext(),
            "Logout",
            "Are you sure you want to logout?",
            "Yes",
            {_, _ -> logoutUser() },
            "No",
            {_, _ -> }
        )
    }

    private fun logoutUser() {
        Firebase.auth.signOut()
        unsubscribeToNotifications()
        findNavController().navigate(R.id.action_global_sign_out)
    }

    private fun unsubscribeToNotifications() {
        Firebase.messaging.unsubscribeFromTopic(CollectionNotifications.TOPIC)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("FCM", "Unsubscribe to topic failed.")
                } else {
                    Log.e("FCM", "Unsubscribe to topic successful.")
                }
            }
        }
}