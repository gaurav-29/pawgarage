package com.nextsavy.pawgarage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.databinding.ActivityMainBinding


interface RecyclerViewItemCheckInterface<T>: RecyclerViewPagingInterface<T> {
    fun updateCheckFor(dataItem: T, position: Int, isChecked: Boolean)
}

interface RecyclerViewMenuInterface<T>: RecyclerViewPagingInterface<T> {
    fun didSelectMenuItem(view: View, dataItem: T, position: Int)
}

interface RecyclerViewPagingInterface<T> {
    fun didScrolledToEnd(position: Int)
    fun didSelectItem(dataItem: T, position: Int)
    fun dataSourceDidUpdate(size: Int)
}

interface RecyclerViewDelegate<T> {
    fun didSelectItem(recyclerView: RecyclerView, dataItem: T, position: Int)
}

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // If app is in background, the notification deep link will be handled from below code :
        if (intent != null && intent.extras != null) {
            val animalDocId = intent.extras!!.getString("animal_doc_id")
            if (animalDocId != null) {
                if (animalDocId == "") {
                    Log.e("NOTIFICATION", "In main activity, animalDocId == Empty")
//                    val model = RemindersModel()
//                    model.animal_doc_id = ""
//                    model.animal_name = intent.extras!!.getString("animal_name").toString()
//                    model.animal_image = intent.extras!!.getString("animal_image_url").toString()
//                    model.location_address = intent.extras!!.getString("location").toString()
//                    model.latitude = intent.extras!!.getString("latitude")!!.toDouble()
//                    model.longitude = intent.extras!!.getString("longitude")!!.toDouble()
//
//                    val bundle = bundleOf("profileLeadsData" to model)
//
//                    navController.navigate(R.id.newProfileFragment, bundle)
                } else {
                    Log.e("NOTIFICATION", "In main activity, $animalDocId")
                    val bundle = Bundle()
                    bundle.putString("animalDocId", animalDocId)
                    navController.popBackStack()     //  After opening of AnimalProfileFragment, the SplashFragment is opened. To avoid it, this line is added.
                    navController.navigate(R.id.animalProfileFragment, bundle)
                }
            }
        }


        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.menu.getItem(2).isEnabled = false

        navController.addOnDestinationChangedListener{_, destination, _ ->
            Log.e("NAVIGATION", destination.label.toString())
            when(destination.id) {
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.verifyOtpFragment,
                R.id.newProfileFragment,
                R.id.addAdmissionFragment,
                R.id.addTreatmentFragment,
                R.id.editAdmissionFragment,
                R.id.editTreatmentFragment,
                R.id.medicalConditionsFragment,
                R.id.medicalConditionListFragment,
                R.id.addReleaseDetailsFragment,
                R.id.editReleaseFragment,
                R.id.adopterListFragment,
                R.id.adopterDetailsFragment,
                R.id.activeCasesListFragment
                -> binding.componentCL.visibility = View.GONE
                else -> binding.componentCL.visibility = View.VISIBLE
            }
        }

        binding.addFAB.setOnClickListener {
//            navController.navigate(R.id.newProfileFragment)
            navController.navigate(R.id.action_global_new_animal_profile)
        }
    }

    fun showBottomNavigation() {
        binding.bottomNav.visibility = View.VISIBLE
        binding.addFAB.visibility = View.VISIBLE
    }
    fun hideBottomNavigation() {
        binding.bottomNav.visibility = View.GONE
        binding.addFAB.visibility = View.GONE
    }
}