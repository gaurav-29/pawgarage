package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.adapters.AnimalProfileViewPagerAdapter
import com.nextsavy.pawgarage.databinding.FragmentAnimalProfileBinding
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper

class AnimalProfileFragment : Fragment() {

    private lateinit var binding: FragmentAnimalProfileBinding
    lateinit var categoryVP: ViewPager2
    var animalDocId: String? = null
    var animalDTO: AnimalDTO? = null

    var showDewormingAlert = false

    private val queryListenerList = ArrayList<ListenerRegistration>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.e("LFCYCL", "ON CREATE VIEW - ANIMAL DETAILS")
        binding = FragmentAnimalProfileBinding.inflate(inflater, container, false)
        categoryVP = binding.categoryVP

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        })

        val animalDocIdFromDeepLinkWhenAppInBackground = arguments?.getString("animalDocId")

        if (animalDocIdFromDeepLinkWhenAppInBackground == null) {
            arguments?.takeIf { it.containsKey("animalId") }?.apply {
                animalDocId = getString("animalId", "-1")
                // > containsKey is getting animal_doc_id key also (which we have sent from
                // sendPushNotification() method for API call from related fragment), because animal_doc_id contains animalId key.
                // > If any DeepLinks received and app is in foreground, this argument is received from onMessageReceived() method in MyFirebaseMessagingService.kt fragment through animal_doc_id key.
                // > If any DeepLinks received and app is in background, arguments is received from MainActivity through intent through animalDocId key as per above (animalDocIdFromDeepLinkWhenAppInBackground).
                // > If no any DeepLinks, arguments received from ProfileAdapter on item click through animalId key.
            }
        } else {
            animalDocId = animalDocIdFromDeepLinkWhenAppInBackground
        }

        showDewormingAlert = arguments?.getBoolean("show_deworming_alert", false) ?: false
        // Delete once stored in property. So it won't ask again.
        arguments?.remove("show_deworming_alert")


        Log.e("A_ID_DEEPLINK_BACKGROUND", animalDocIdFromDeepLinkWhenAppInBackground.toString())

        Log.e("A_ID", animalDocId.toString())
        val userType = Helper.sharedPreference?.getString("USER_TYPE", "").toString()

//        binding.toolbarOne.titleToolbarOne.text = "${AppDelegate.animalName}"

        if (userType == CollectionWhitelistedNumbers.ADMIN || userType == CollectionWhitelistedNumbers.TEAM_LEADER) {
            binding.toolbarOne.generalImgToolbarOne.visibility = View.VISIBLE
            binding.toolbarOne.generalImgToolbarOne.setImageResource(R.drawable.edit)
        }

        getAnimalProfile2()
        configureTabLayoutWithViewPager2()
        onClickListeners()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Log.e("LFCYCL", "ON RESUME - ANIMAL DETAILS")
    }

    override fun onPause() {
        super.onPause()
        Log.e("LFCYCL", "ON PAUSE - ANIMAL DETAILS")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("LFCYCL", "ON DESTROY VIEW - ANIMAL DETAILS")
        queryListenerList.forEach {
            it.remove()
        }
        queryListenerList.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("LFCYCL", "ON DESTROY - ANIMAL DETAILS")
    }

    fun getAnimalProfile2() {
        val queryListener = Firebase.firestore.collection(CollectionAnimals.name)
            .document(animalDocId!!)
            .addSnapshotListener { docSnapshot, error ->
                if (error != null) {
                    Log.e("ERROR", "Error-AnimalProfileFragment - $error")
                    Toast.makeText(requireContext(), "Error - ${error.message}", Toast.LENGTH_SHORT).show()
                    animalDTO = null
                    return@addSnapshotListener
                }
                if (docSnapshot != null) {
                    AppDelegate.isDead = docSnapshot.data?.get(CollectionAnimals.kIsDead) as Boolean
                    AppDelegate.state = docSnapshot.data?.get(CollectionAnimals.kState) as String
                    animalDTO = AnimalDTO.create(docSnapshot.id, docSnapshot.data)

                    binding.toolbarOne.titleToolbarOne.text =  animalDTO?.name
                } else {
                    Log.e("DOC", "No such document.")
                    Toast.makeText(requireContext(), "Animal not found.", Toast.LENGTH_SHORT).show()
                }
            }
        queryListenerList.add(queryListener)
    }

    private fun getAnimalProfile() {
        if (Helper.isInternetAvailable(requireContext())) {
            val man = Firebase.firestore.collection(CollectionAnimals.name)
                .document(animalDocId!!)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        AppDelegate.isDead = document.data?.get(CollectionAnimals.kIsDead) as Boolean
                        AppDelegate.state = document.data?.get(CollectionAnimals.kState) as String
                        animalDTO = AnimalDTO.create(document.id, document.data)

                        binding.toolbarOne.titleToolbarOne.text =  document.data?.get(CollectionAnimals.kName) as String
                    } else {
                        Log.e("DOC", "No such document.")
                        Toast.makeText(requireContext(), "Death or Terminated status not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ERROR", "Error-AnimalProfileFragment - $exception")
                    Toast.makeText(requireContext(), "Error - $exception", Toast.LENGTH_SHORT).show()
                }

        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onClickListeners() {
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            // Manthan 24-11-2023
            // Instead of navigating to Home, just pop the screen.
//            it.findNavController().navigate(R.id.homeFragment)
            findNavController().navigateUp()
        }
        binding.toolbarOne.generalImgToolbarOne.setOnClickListener {
            val bundle = bundleOf("animalDocID" to animalDocId, "animalName" to animalDTO?.name)
            it.findNavController().navigate(R.id.editAnimalProfileFragment, bundle)
        }
    }

    private fun configureTabLayoutWithViewPager2() {
        val tabTitle = arrayOf("Details", "Admission", "Deworming", "Vaccination", "Status", "OPD")

        categoryVP.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        categoryVP.adapter = AnimalProfileViewPagerAdapter(childFragmentManager, lifecycle) // we should use childFragmentManager
        // instead of supportFragmentManager whenever we use viewpager in fragment. (this is to solve the below problem,
        // onResume method not called when fragment is in viewpager2 and coming back from fragment other than viewpager2 fragment)
        TabLayoutMediator(binding.categoryTL, categoryVP) { tab, position ->
            tab.text = tabTitle[position]
        }.attach()
        arguments?.takeIf { it.containsKey("actionType") }?.apply {
            val actionType = getString("actionType")
            actionType?.let {
                when (it) {
                    CollectionNotifications.RELEASED,
                    CollectionNotifications.DEATH,
                    CollectionNotifications.ADOPTED-> {
                        // Manthan 25/11/2023
                        // If you change the sequence of Pages in ViewPager, please change the index number here too!
                        categoryVP.setCurrentItem(4, true)
                    }
                    CollectionReminders.VACCINATION -> {
                        // Manthan 24/11/2023
                        // If you change the sequence of Pages in ViewPager, please change the index number here too!
                        categoryVP.setCurrentItem(3, true)
                    }
                    CollectionReminders.DEWORMING -> {
                        // Manthan 24/11/2023
                        // If you change the sequence of Pages in ViewPager, please change the index number here too!
                        categoryVP.setCurrentItem(2, true)
                    }
                    else -> {

                    }
                }
                arguments?.remove("actionType")
            }

        }
    }
}