package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentSplashBinding
import com.nextsavy.pawgarage.utils.Helper

class SplashFragment : Fragment() {

    private lateinit var binding: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSplashBinding.inflate(inflater, container, false)

        Handler(Looper.getMainLooper()).postDelayed({
            if (Firebase.auth.currentUser == null) {
                activity?.findNavController(R.id.nav_host_fragment)?.navigate(R.id.action_splashFragment_to_login_graph)
            } else {
                activity?.findNavController(R.id.nav_host_fragment)?.navigate(R.id.action_splashFragment_to_homeFragment)
            }
        },3000)

        return binding.root
    }
}