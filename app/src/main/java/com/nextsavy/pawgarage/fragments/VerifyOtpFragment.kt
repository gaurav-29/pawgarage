package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentVerifyOtpBinding
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.Helper
import `in`.aabhasjindal.otptextview.OTPListener
import java.io.Serializable
import java.util.concurrent.TimeUnit

class VerifyOtpFragment : Fragment(), Serializable, OTPListener {

    private lateinit var binding: FragmentVerifyOtpBinding

    var verificationId: String? = null

    var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private var contactNumberWithCode: String? = null

    private var resendTimer: CountDownTimer? = null

    lateinit var userType: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVerifyOtpBinding.inflate(inflater, container, false)

        userType = Helper.sharedPreference?.getString("USER_TYPE", "").toString()

        arguments?.takeIf { it.containsKey("contact_number_with_code") }?.apply {
            contactNumberWithCode = getString("contact_number_with_code")
            binding.otpView.otpListener = this@VerifyOtpFragment
        }

        binding.numberTV.text = contactNumberWithCode

        onClickListeners()

        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.resendTimerTV.text = "  Resend in " + millisUntilFinished / 1000 +"s."
            }
            override fun onFinish() {
                binding.resendTimerTV.visibility = View.GONE
                binding.resendOtpTV.visibility = View.VISIBLE
            }
        }

        if (!contactNumberWithCode.isNullOrBlank()) {
            resendOTP()
        }

        return binding.root
    }

    override fun onInteractionListener() {

    }

    override fun onOTPComplete(otp: String) {
        if (Helper.isInternetAvailable(requireContext())) {
            if (otp.isNotBlank() && otp.length == 6) {
                if (verificationId != null) {
                    binding.progressBar.root.visibility = View.VISIBLE
                    val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
                    signInWithPhoneAuthCredential(credential)
                } else {
                    Toast.makeText(requireContext(), "Unable to verify OTP. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter OTP.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onClickListeners() {
        binding.verifyRL.setOnClickListener {
            if (Helper.isInternetAvailable(requireContext())) {
                val typedOTP = binding.otpView.otp
                if (!typedOTP.isNullOrEmpty()) {
                    if (typedOTP.isNotEmpty() && typedOTP.length == 6) {
                        if (verificationId != null) {
                            binding.progressBar.root.visibility = View.VISIBLE
                            val credential = PhoneAuthProvider.getCredential(verificationId!!, typedOTP)
                            signInWithPhoneAuthCredential(credential)
                        } else {
                            Toast.makeText(requireContext(), "Unable to verify OTP. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Please enter valid OTP.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Please enter OTP.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
            }
        }

        binding.changeNumberTV.setOnClickListener {
            it.findNavController().navigateUp()
        }

        binding.resendOtpTV.setOnClickListener {
            if (Helper.isInternetAvailable(requireContext())) {
                if (!contactNumberWithCode.isNullOrBlank()) {
                    binding.resendOtpTV.visibility = View.GONE
                    binding.resendTimerTV.visibility = View.VISIBLE
                    resendOTP()
                }
            } else {
                Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resendOTP() {
        if (contactNumberWithCode == "+918200843626") {
            FirebaseAuth.getInstance().firebaseAuthSettings.forceRecaptchaFlowForTesting(true)
        } else {
            FirebaseAuth.getInstance().firebaseAuthSettings.forceRecaptchaFlowForTesting(false)
        }
        binding.progressBar.root.visibility = View.VISIBLE
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(contactNumberWithCode!!)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)

        if (resendToken != null) {
            options.setForceResendingToken(resendToken!!)
        }

        PhoneAuthProvider.verifyPhoneNumber(options.build())
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.e("DG", "onVerificationCompleted:$credential")
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("DG", "onVerificationFailed", e)
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            binding.progressBar.root.visibility = View.GONE
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.e("DG", "onCodeSent:$verificationId")
            this@VerifyOtpFragment.verificationId = verificationId
            resendToken = token
            binding.progressBar.root.visibility = View.GONE
            resendTimer?.start()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    Log.e("USER-verify", "user-$user, userID- ${user?.uid}")
                    if (user != null) {
                        Firebase.firestore
                            .collection(CollectionUser.name)
                            .document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                val name = document.get(CollectionUser.kUserName) as String?
                                val contactNumber = document.get(CollectionUser.kContactNumber) as String?
                                val userType = document.get(CollectionUser.kUserType) as String?
                                if (name != null && contactNumber != null && userType != null) {
                                    Helper.sharedPreference?.edit()?.putString("USER_NAME", name)?.apply()
                                    Helper.sharedPreference?.edit()?.putString("USER_TYPE", userType)?.apply()

                                    Toast.makeText(requireContext(), "Sign In successful.", Toast.LENGTH_LONG).show()

                                    subscribeToNotifications()

                                    binding.progressBar.root.visibility = View.GONE

                                    findNavController().navigate(R.id.action_global_sign_in)
                                } else {
                                    subscribeToNotifications()
                                    createUserInDatabase(user.uid)
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d("NST", "get failed with ", exception)
                            }
                    }
                } else {
                    Log.e("DG", "signInWithCredential:failure", task.exception)
                    Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_LONG).show()
                    binding.progressBar.root.visibility = View.GONE
                }
            }
    }

    private fun subscribeToNotifications() {
        Firebase.messaging.subscribeToTopic(CollectionNotifications.TOPIC)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("FCM", "Subscribe to topic failed.")
                } else {
                    Log.e("FCM", "Subscribe to topic successful.")
                }
            }
    }

    private fun createUserInDatabase(userId: String) {
        val userName = Helper.sharedPreference?.getString("USER_NAME", "")

        val user = hashMapOf(
            CollectionUser.kUserName to userName,
            CollectionUser.kCountryCode to CollectionUser.COUNTRY_CODE,
            CollectionUser.kContactNumber to contactNumberWithCode,
            CollectionUser.kUserType to userType,
            CollectionUser.kIsArchive to false,
            CollectionUser.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionUser.kCreatedBy to userId
        )

        Firebase.firestore.collection(CollectionUser.name).document(userId)
            .set(user, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Sign In successful.", Toast.LENGTH_LONG).show()

                Helper.sharedPreference?.edit()?.putString("USER_NAME", userName)?.apply()
                Helper.sharedPreference?.edit()?.putString("USER_TYPE", userType)?.apply()
                Log.e("NST", "DocumentSnapshot successfully written!")
                binding.progressBar.root.visibility = View.GONE

                findNavController().navigate(R.id.action_global_sign_in)
            }
            .addOnFailureListener { e ->
                Log.e("NST", "Error writing document", e)
                binding.progressBar.root.visibility = View.GONE
            }
    }

}