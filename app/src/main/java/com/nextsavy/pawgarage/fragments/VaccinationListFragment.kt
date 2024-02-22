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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.VaccinationDewormingAdapter
import com.nextsavy.pawgarage.databinding.FragmentVaccinationListBinding
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.GenericUserDTO
import com.nextsavy.pawgarage.models.VaccinationDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionVaccination
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import io.reactivex.rxjava3.plugins.RxJavaPlugins

class VaccinationListFragment : Fragment(), RecyclerViewPagingInterface<VaccinationDTO> {

    private lateinit var binding: FragmentVaccinationListBinding
    private lateinit var adapter: VaccinationDewormingAdapter
    private var isEnabled: Boolean = true
    private val animalDocId: String?
        get() = (this.parentFragment as AnimalProfileFragment?)?.animalDocId

    private val animalDTO: AnimalDTO?
        get() = (this.parentFragment as AnimalProfileFragment?)?.animalDTO

    val userType: String
        get() = Helper.sharedPreference?.getString("USER_TYPE", CollectionWhitelistedNumbers.TEAM_MEMBER) ?: CollectionWhitelistedNumbers.TEAM_MEMBER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This code is to solve the crash (following error) when we try to navigate to this fragment.
        // Stackoverflow link : https://stackoverflow.com/questions/52631581/rxjava2-undeliverableexception-when-orientation-change-is-happening-while-fetchi

        // FATAL EXCEPTION: RxCachedThreadScheduler-2
        // Process: com.nextsavy.pawgarage, PID: 22607
        // io.reactivex.rxjava3.exceptions.UndeliverableException: The exception could not be delivered to the consumer because it
        // has already canceled/disposed the flow or the exception has nowhere to go to begin with.
        RxJavaPlugins.setErrorHandler {} // nothing or some logging
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVaccinationListBinding.inflate(inflater, container, false)

        if (userType == CollectionWhitelistedNumbers.TEAM_MEMBER) {
            binding.addScheduleBTN.visibility = View.GONE
            binding.addScheduleTV.setText(R.string.no_vaccine_schedule_text)
        }

        initiateVaccinationList()
        getVaccinationDetailList()
        onClickListeners()
        return binding.root
    }
    private fun initiateVaccinationList() {
        binding.vaccinationListRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = VaccinationDewormingAdapter(animalDocId!!, arrayListOf(), "Vaccination", this)
        binding.vaccinationListRV.adapter = adapter
    }
    private fun getVaccinationDetailList() {
        if (Helper.isInternetAvailable(requireContext())) {

            binding.progressBar.visibility = View.VISIBLE

            Firebase.firestore.collection(CollectionVaccination.name)
                .whereEqualTo(CollectionVaccination.kIsArchive, false)
                .whereEqualTo(CollectionVaccination.kAnimalDocId, animalDocId)
//                .orderBy(CollectionVaccination.kVaccinationDate, Query.Direction.ASCENDING)
                .orderBy(CollectionVaccination.kVaccinationDate, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val vaccinationList = querySnapshot.documents.mapNotNull { VaccinationDTO.create(it.id, it.data) }
                    val userIds: List<String> = vaccinationList.mapNotNull { it.administratorId }.distinct()
                    val customerTaskSnapshotList = userIds.map { Firebase.firestore.collection(CollectionWhitelistedNumbers.name).document(it).get() }
                    val finalTask = Tasks.whenAllSuccess<DocumentSnapshot>(customerTaskSnapshotList)
                    finalTask.addOnSuccessListener { docSnapshotList ->
                        for (vaccination in vaccinationList) {
                            for (docSnap in docSnapshotList) {
                                if (vaccination.administratorId == docSnap.id) {
                                    val userDTO = GenericUserDTO.create(docSnap.id, docSnap.data)
                                    if (userDTO != null) {
                                        vaccination.administratorPerson = userDTO
                                        break
                                    }
                                }
                            }
                        }
                        binding.progressBar.visibility = View.GONE
                        adapter.updateDataSource(vaccinationList)
                    }.addOnFailureListener { exception ->
                        Log.e("NST", "Error in Order task chains\n${exception.message ?: "Unknown error"}")
                        binding.progressBar.visibility = View.GONE
                        adapter.updateDataSource(vaccinationList)
                    }
                    if (vaccinationList.size > 1) {
                        isEnabled = vaccinationList[vaccinationList.size - 2].vaccinationStatus != CollectionVaccination.PENDING
                    }
                }
                .addOnFailureListener { qException ->
                    binding.progressBar.visibility = View.GONE
                    Log.e("NST-M", "Exception: VaccinationListFragment > ${qException.localizedMessage}")
                    Toast.makeText(requireContext(), "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onClickListeners() {
        // Add first Vaccine
        binding.addScheduleBTN.setOnClickListener {
            if (!AppDelegate.isDead) {
                if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                    it.findNavController().navigate(AnimalProfileFragmentDirections
                        .actionAnimalProfileFragmentToAddVaccinationScheduleFragment(0, animalDocId, animalName = animalDTO?.name))
                } else {
                    Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_dead, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun didScrolledToEnd(position: Int) {
        //getVaccinationDetailList()
    }
    override fun dataSourceDidUpdate(size: Int) {
        if (size > 0) {
            binding.firstScheduleCL.visibility = View.GONE
            binding.scheduleListCL.visibility = View.VISIBLE
            binding.footerFL.visibility = View.VISIBLE
        } else {
            Log.e("DOC_EMPTY", "Empty List")
            binding.firstScheduleCL.visibility = View.VISIBLE
            binding.scheduleListCL.visibility = View.GONE
        }
    }
    override fun didSelectItem(dataItem: VaccinationDTO, position: Int) {
        Log.e("NST-M", "Selected vaccine id: ${dataItem.id}")
        if (userType == CollectionWhitelistedNumbers.ADMIN || userType == CollectionWhitelistedNumbers.TEAM_LEADER) {
            var previousVaccinationDate: Long = 0
            if (adapter.itemCount > 2) {
                if (position == 0) {
                    previousVaccinationDate = adapter.dataList[1].vaccinationDate.toDate().time
                } else if (position == 1) {
                    previousVaccinationDate = adapter.dataList[2].vaccinationDate.toDate().time
                }
            }
            Log.e("NST-M", "Previous vaccine id: $previousVaccinationDate")
            findNavController()
                .navigate(
                    AnimalProfileFragmentDirections
                        .actionAnimalProfileFragmentToVaccinationEditFragment(
                            vaccinationId = dataItem.id,
                            previousVaccinationDate = previousVaccinationDate,
                            totalVaccinationCount = adapter.dataList.size,
                            currentVaccinationIndex = position,
                            animalName = animalDTO?.name
                        )
                )
        }
    }
}