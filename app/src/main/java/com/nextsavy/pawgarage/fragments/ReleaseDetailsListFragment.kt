package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.ReleaseListAdapter
import com.nextsavy.pawgarage.databinding.FragmentReleaseDetailsListBinding
import com.nextsavy.pawgarage.models.AdmissionDTO
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.models.ReleaseDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionAdopters
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionRelease
import com.nextsavy.pawgarage.utils.CollectionReportingPersons
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.viewModels.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReleaseDetailsListFragment : Fragment(), RecyclerViewPagingInterface<ReleaseDTO> {

    private lateinit var binding: FragmentReleaseDetailsListBinding
    private val db = Firebase.firestore
    private lateinit var releaseAdapter: ReleaseListAdapter
    private val animalDocId: String?
        get() = (this.parentFragment as AnimalProfileFragment?)?.animalDocId

    private val animalDTO: AnimalDTO?
        get() = (this.parentFragment as AnimalProfileFragment?)?.animalDTO

    var admissionCount = 0

    val userType: String
        get() = Helper.sharedPreference?.getString("USER_TYPE", CollectionWhitelistedNumbers.TEAM_MEMBER) ?: CollectionWhitelistedNumbers.TEAM_MEMBER

    private var lastAdmissionDate: String = ""

    private val queryListenerList = ArrayList<ListenerRegistration>()

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReleaseDetailsListBinding.inflate(inflater, container, false)

        setUpRecyclerView()
        /*if (userType == CollectionWhitelistedNumbers.TEAM_MEMBER) {
            binding.addReleaseBTN.visibility = View.GONE
            binding.addAnotherReleaseBTN.visibility = View.GONE
            binding.addReleaseTV.setText(R.string.no_release_text)
        }*/

//        allowRelease = false
        sharedViewModel.setAllowRelease(false)
        getAdmissionList()
        onClickListeners()
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        queryListenerList.forEach {
            it.remove()
        }
        queryListenerList.clear()
    }

    private fun onClickListeners() {
        binding.addReleaseBTN.setOnClickListener {
            if (!AppDelegate.isDead) {
                if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                    if (admissionCount > 0) {
                        val directions = AnimalProfileFragmentDirections.actionAnimalProfileFragmentToAddReleaseDetailsFragment(
                            releaseNumber = 0,
                            animalDocId = animalDocId!!,
                            animalName = animalDTO?.name,
                            LastAdmissionDate = lastAdmissionDate
                        )
                        it.findNavController().navigate(directions)
                    } else {
                        Toast.makeText(requireContext(), "Please admit the animal.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_dead, Toast.LENGTH_SHORT).show()
            }
        }
        binding.addAnotherReleaseBTN.setOnClickListener {
            if (!AppDelegate.isDead) {
                if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                    if (sharedViewModel.getAllowRelease()) {
                        val directions = AnimalProfileFragmentDirections.actionAnimalProfileFragmentToAddReleaseDetailsFragment(
                            releaseNumber = releaseAdapter.itemCount,
                            animalDocId = animalDocId!!,
                            LastAdmissionDate = lastAdmissionDate,
                            animalName = animalDTO?.name
                        )
                        it.findNavController().navigate(directions)
                    } else {
                        Toast.makeText(requireContext(), "Please admit the animal.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_dead, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getAdmissionList() {
        admissionCount = 0
        val queryListener = Firebase.firestore.collection(CollectionAdmission.name)
            .whereEqualTo(CollectionAdmission.kIsArchive, false)
            .whereEqualTo(CollectionAdmission.kAnimalDocId, animalDocId)
            .orderBy(CollectionAdmission.kAdmissionDate, Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("CHECK", "Listen failed.", error)
                    return@addSnapshotListener
                }
                if (value!!.isEmpty) {
                    admissionCount = 0
                } else {
                    admissionCount = value.documents.size
                    val lastAdmissionDateInTimestamp = value.documents.last().get(CollectionAdmission.kAdmissionDate) as Timestamp
                    val date: Date = lastAdmissionDateInTimestamp.toDate()
                    lastAdmissionDate = SimpleDateFormat("dd MMM, yyyy", Locale.US).format(date)
                    Log.e("LAST3", lastAdmissionDate)
                }
                getReleaseDetails()
            }
        queryListenerList.add(queryListener)
    }
    private fun getReleaseDetails() {
        if (Helper.isInternetAvailable(AppDelegate.applicationContext())) {
            binding.progressBar.visibility = View.VISIBLE
            val queryListener = db.collection(CollectionRelease.name)
                .whereEqualTo(CollectionRelease.kIsArchive, false)
                .whereEqualTo(CollectionRelease.kAnimalDocId, animalDocId)
                .orderBy(CollectionRelease.kReleasedDate, Query.Direction.DESCENDING)
                .addSnapshotListener { value, error ->
                    binding.progressBar.visibility = View.GONE

                    if (error != null) {
                        Log.e("ERROR", "ReleaseDetailsListFrag. SnapshotListener-", error)
                        return@addSnapshotListener
                    }

                  if (value!!.isEmpty) {
                      updateDatsSource(listOf())
                  } else {
                      val releaseList = value.mapNotNull { ReleaseDTO.create(it.id, it.data) }
                      getAdopterForRelease(releaseList)
                  }
                }
            queryListenerList.add(queryListener)
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAdopterForRelease(releaseList: List<ReleaseDTO>) {
        val adopterIds: List<String> = releaseList.mapNotNull { it.adopterId }.distinct()
        val adoptersTaskSnapshotList = adopterIds.map { Firebase.firestore.collection(CollectionAdopters.name).document(it).get() }
        val allAdoptersTask = Tasks.whenAllSuccess<DocumentSnapshot>(adoptersTaskSnapshotList)
        allAdoptersTask.addOnSuccessListener { docSnapshotList ->
            for (release in releaseList) {
                for (docSnap in docSnapshotList) {
                    if (release.adopterId == docSnap.id) {
                        val adopter = GenericMemberDTO.create(docSnap.id, docSnap.data)
                        if (adopter != null) {
                            release.adopter = adopter
                            break
                        }
                    }
                }
            }
            updateDatsSource(releaseList)
        }.addOnFailureListener { exception ->
            Log.e("NST", "Error ReleaseDetailsList > Adopter:\t${exception.message}")
            updateDatsSource(releaseList)
        }
    }

    private fun updateDatsSource(releaseList: List<ReleaseDTO>) {
        binding.progressBar.visibility = View.GONE
        releaseAdapter.updateDataSource(releaseList)
//        allowRelease = admissionCount > releaseList.size
        sharedViewModel.setAllowRelease(admissionCount > releaseList.size)
        if (releaseList.isEmpty()) {
            binding.firstReleaseCL.visibility = View.VISIBLE
            binding.releaseListCL.visibility = View.GONE
        } else {
            binding.firstReleaseCL.visibility = View.GONE
            binding.releaseListCL.visibility = View.VISIBLE
        }
    }

    private fun setUpRecyclerView() {
        binding.releaseRV.layoutManager = LinearLayoutManager(AppDelegate.applicationContext())
        releaseAdapter = ReleaseListAdapter(arrayListOf(), this)
        binding.releaseRV.adapter = releaseAdapter
    }

    override fun didScrolledToEnd(position: Int) {

    }

    override fun dataSourceDidUpdate(size: Int) {
        if (size > 0) {
            binding.footerFL.visibility = View.VISIBLE
        }
    }
    override fun didSelectItem(dataItem: ReleaseDTO, position: Int) {
        Log.e("NST-M", "Selected Status id: ${dataItem.id}")
        if (userType == CollectionWhitelistedNumbers.ADMIN || userType == CollectionWhitelistedNumbers.TEAM_LEADER) {
            findNavController().navigate(
                AnimalProfileFragmentDirections.actionAnimalProfileFragmentToEditReleaseFragment(
                    releaseId = dataItem.id,
                    animalDocId = animalDocId!!,
                    totalReleaseCount = releaseAdapter.itemCount,
                    currentReleaseIndex = position,
                    lastAdmissionDate = lastAdmissionDate,
                    animalName = animalDTO?.name
                )
            )
        }
    }
}