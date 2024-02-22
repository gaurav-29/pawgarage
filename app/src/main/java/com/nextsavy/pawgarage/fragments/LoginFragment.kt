package com.nextsavy.pawgarage.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentLoginBinding
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import java.io.Serializable

class LoginFragment : Fragment(), Serializable {
    private lateinit var binding: FragmentLoginBinding
    private lateinit var contactNumber: String
    private lateinit var whitelistedNumber: String
    lateinit var contactNumberWithCode: String

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Permission granted for notifications.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "You will not be able to see notifications.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Manthan 24-01-2024
        // Test commit in develop branch
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.countryCodeET.setText(CollectionUser.COUNTRY_CODE)

        askNotificationPermission()
        onClickListeners()
        return binding.root
    }

    fun foo() {
        // Manthan 24-01-2024
        // Test commit in develop branch
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showRationalDialog()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Gaurav
    private fun showRationalDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission needed")
            .setMessage("Without notification permission, you will not be able show notifications.")
            .setPositiveButton("Allow from Settings") { d, _ ->
                val intent = Intent()
                intent.data = Uri.fromParts("package", requireContext().packageName, null)
                intent.action = Uri.decode(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                startActivity(intent)
                d.dismiss()
            }
            .setNegativeButton("Deny") { d, _ ->
                Snackbar.make(binding.root, "Notification permission is required to show notifications.", Snackbar.LENGTH_LONG)
                    .setAction("Settings"){
                        val intent = Intent()
                        intent.data = Uri.fromParts("package", requireContext().packageName, null)
                        intent.action = Uri.decode(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        startActivity(intent)
                    }.show()
                d.dismiss()
            }
            .create()
            .show()
    }

    private fun onClickListeners() {
        binding.getOtpRL.setOnClickListener {

            if (Helper.isInternetAvailable(requireContext())) {

                binding.progressBar.root.visibility = View.VISIBLE
                binding.errorMessageLL.visibility = View.GONE
                contactNumber = binding.numberET.text.trim().toString()

                if (contactNumber.isEmpty() || contactNumber.length != 10) {
                    Toast.makeText(requireContext(), "Please enter valid contact number.", Toast.LENGTH_LONG).show()
                    binding.progressBar.root.visibility = View.GONE
                } else {
                    contactNumberWithCode = CollectionUser.COUNTRY_CODE + contactNumber
                    // Manthan 28-11-2023
                    // Find WhiteList number is database with given phone-number and country code
                    // Sort the query by Created date and in Ascending order
                    // Match with last document, this way you can delete the existing number and Add the same number again
                    Firebase.firestore
                        .collection(CollectionWhitelistedNumbers.name)
                        .whereEqualTo(CollectionWhitelistedNumbers.kContactNumber, contactNumberWithCode)
                        .orderBy(CollectionWhitelistedNumbers.kCreatedAt, Query.Direction.ASCENDING)
                        .get()
                        .addOnSuccessListener { result ->
                            if (result.documents.isNotEmpty()) {
                                whitelistedNumber = result.documents.last().get(CollectionWhitelistedNumbers.kContactNumber) as String
                                val isDeleted = result.documents.last().get(CollectionWhitelistedNumbers.kIsArchive) as Boolean? ?: true
                                if ((contactNumberWithCode == whitelistedNumber) && !isDeleted) {
                                    val userType = result.documents.last().get(CollectionWhitelistedNumbers.kUserType) as String?
                                    val userName = result.documents.last().get(CollectionWhitelistedNumbers.kUserName) as String?
                                    Helper.editor?.putString("USER_TYPE", userType)
                                    Helper.editor?.putString("USER_NAME", userName)
                                    Helper.editor?.apply()

                                    binding.progressBar.root.visibility = View.GONE
                                    val bundle = bundleOf("contact_number_with_code" to contactNumberWithCode)
                                    findNavController().navigate(R.id.verifyOtpFragment, bundle)
                                } else {
                                    binding.errorMessageLL.visibility = View.VISIBLE
                                    binding.progressBar.root.visibility = View.GONE
                                }
                            } else {
                                binding.errorMessageLL.visibility = View.VISIBLE
                                binding.progressBar.root.visibility = View.GONE
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("NUM-LOGIN", "Error getting documents.", exception)
                            binding.progressBar.root.visibility = View.GONE
                        }
                }
            } else {
                Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
            }
        }
    }
}