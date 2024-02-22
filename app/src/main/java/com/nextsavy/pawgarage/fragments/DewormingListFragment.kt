package com.nextsavy.pawgarage.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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
import com.nextsavy.pawgarage.adapters.DewormingAdapter
import com.nextsavy.pawgarage.databinding.FragmentDewormingListBinding
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.DewormingDTO
import com.nextsavy.pawgarage.models.GenericUserDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionDeworming
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import io.reactivex.rxjava3.plugins.RxJavaPlugins


class DewormingListFragment : Fragment(), RecyclerViewPagingInterface<DewormingDTO> {

    private lateinit var binding: FragmentDewormingListBinding
    private lateinit var adapter: DewormingAdapter
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
        binding = FragmentDewormingListBinding.inflate(inflater, container, false)
        if (userType == CollectionWhitelistedNumbers.TEAM_MEMBER) {
            binding.addScheduleBTN.visibility = View.GONE
            binding.addScheduleTV.setText(R.string.no_deworm_schedule_text)
        }
        registerForPopBackData()
        initiateVaccinationList()
        getDewormingDetailList()
        onClickListeners()
        return binding.root
    }

    private fun initiateVaccinationList() {
        binding.dewormingListRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = DewormingAdapter(animalDocId!!, arrayListOf(),this)
        binding.dewormingListRV.adapter = adapter
    }

    private fun getDewormingDetailList() {
        if (Helper.isInternetAvailable(requireContext())) {

            binding.progressBar.visibility = View.VISIBLE

            Firebase.firestore.collection(CollectionDeworming.name)
                .whereEqualTo(CollectionDeworming.kIsArchive, false)
                .whereEqualTo(CollectionDeworming.kAnimalDocId, animalDocId)
                .orderBy(CollectionDeworming.kDewormingDate, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val dewormingList = querySnapshot.mapNotNull { DewormingDTO.create(it.id, it.data) }
                    val userIds: List<String> = dewormingList.mapNotNull { it.administratorId }.distinct()
                    val customerTaskSnapshotList = userIds.map { Firebase.firestore.collection(CollectionWhitelistedNumbers.name).document(it).get() }
                    val finalTask = Tasks.whenAllSuccess<DocumentSnapshot>(customerTaskSnapshotList)
                    finalTask.addOnSuccessListener { docSnapshotList ->
                        for (deworming in dewormingList) {
                            for (docSnap in docSnapshotList) {
                                if (deworming.administratorId == docSnap.id) {
                                    val userDTO = GenericUserDTO.create(docSnap.id, docSnap.data)
                                    if (userDTO != null) {
                                        deworming.administratorPerson = userDTO
                                        break
                                    }
                                }
                            }
                        }
                        binding.progressBar.visibility = View.GONE
                        adapter.updateDataSource(dewormingList)
                    }.addOnFailureListener { exception ->
                        Log.e("NST", "Error in Order task chains\n${exception.message ?: "Unknown error"}")
                        binding.progressBar.visibility = View.GONE
                        adapter.updateDataSource(dewormingList)
                    }
                    //
                    if (dewormingList.size > 1) {
                        isEnabled = dewormingList[dewormingList.size - 2].dewormingStatus != CollectionDeworming.PENDING
                    }
                }
                .addOnFailureListener { qException ->
                    binding.progressBar.visibility = View.GONE
                    Log.e("NST-M", "Exception: DewormingListFragment > ${qException.localizedMessage}")
                    Toast.makeText(requireContext(), "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onClickListeners() {
        binding.addScheduleBTN.setOnClickListener {
            if (!AppDelegate.isDead) {
                if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                    it.findNavController().navigate(AnimalProfileFragmentDirections.actionAnimalProfileFragmentToAddDewormingScheduleFragment(dewormNumber = 0, animalDocID = animalDocId, animalName = animalDTO?.name))
                } else {
                    Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_dead, Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun didScrolledToEnd(position: Int) {
        //getDewormingDetailList()
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
    override fun didSelectItem(dataItem: DewormingDTO, position: Int) {
        Log.e("NST-M", "Selected deworming id: ${dataItem.id}")
        if (userType == CollectionWhitelistedNumbers.ADMIN || userType == CollectionWhitelistedNumbers.TEAM_LEADER) {
            findNavController()
                .navigate(
                    AnimalProfileFragmentDirections
                        .actionAnimalProfileFragmentToDewormingEditFragment(
                            dewormingId = dataItem.id,
                            totalDewormingCount = adapter.dataList.size,
                            currentDewormingIndex = position,
                            animalName = animalDTO?.name
                        )
                )
        }
    }

    private fun registerForPopBackData() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Int>("move_to_vaccination")?.observe(viewLifecycleOwner) { result ->
            Log.e("MN-LD", "Deworming pop back stack Result: $result")
            if (result == 1) {
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<Int>("move_to_vaccination")
                askForVaccination()
            }
        }
    }

    private fun askForVaccination() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Do you want to schedule vaccination for this animal ?")
        builder.setPositiveButton("Yes") { dialog, which ->
            (parentFragment as AnimalProfileFragment?)?.categoryVP?.setCurrentItem(3, true)
        }
        builder.setNegativeButton("No") { dialog, which ->

        }
        builder.create()
        builder.show()
    }
}