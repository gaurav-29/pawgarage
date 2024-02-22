package com.nextsavy.pawgarage.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigator
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
import com.nextsavy.pawgarage.adapters.AdmissionReleaseAdapter
import com.nextsavy.pawgarage.databinding.FragmentAdmissionListBinding
import com.nextsavy.pawgarage.models.AdmissionDTO
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.models.MedicalConditionDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionMedicalConditionsList
import com.nextsavy.pawgarage.utils.CollectionRelease
import com.nextsavy.pawgarage.utils.CollectionReportingPersons
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.viewModels.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.Exception

class AdmissionListFragment : Fragment(), RecyclerViewPagingInterface<AdmissionDTO> {

    private lateinit var binding: FragmentAdmissionListBinding

    private var dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
    private var lastReleaseDate: String? = null
    var releaseCount = 0

    val userType: String
        get() = Helper.sharedPreference?.getString("USER_TYPE", CollectionWhitelistedNumbers.TEAM_MEMBER) ?: CollectionWhitelistedNumbers.TEAM_MEMBER

    lateinit var admissionReleaseAdapter: AdmissionReleaseAdapter

    private lateinit var mContext: Context
    private val animalDocId: String?
        get() = (this.parentFragment as AnimalProfileFragment?)?.animalDocId

    private val animalDTO: AnimalDTO?
        get() = (this.parentFragment as AnimalProfileFragment?)?.animalDTO

    private val queryListenerList = ArrayList<ListenerRegistration>()

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdmissionListBinding.inflate(inflater, container, false)

        setUpRecyclerView()

        if (userType == CollectionWhitelistedNumbers.TEAM_MEMBER) {
            binding.addAdmissionBTN.visibility = View.GONE
            binding.addFirstAdmissionBTN.visibility = View.GONE
            binding.addScheduleTV.setText(R.string.no_admission_text)
        }

        sharedViewModel.setAllowAdmission(false)
//        allowAdmission = false

        getReleasedList()
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

    private fun setUpRecyclerView() {
        binding.admissionRV.layoutManager = LinearLayoutManager(AppDelegate.applicationContext())
        admissionReleaseAdapter = AdmissionReleaseAdapter(arrayListOf(), this)
        binding.admissionRV.adapter = admissionReleaseAdapter
    }

    private fun getReleasedList() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar.visibility = View.VISIBLE
            releaseCount = 0
            val queryListener = Firebase.firestore.collection(CollectionRelease.name)
                .whereEqualTo(CollectionRelease.kIsArchive, false)
                .whereEqualTo(CollectionRelease.kAnimalDocId, animalDocId)
                .orderBy(CollectionRelease.kReleasedDate, Query.Direction.ASCENDING)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.e("CHECK", "Listen failed.", error)
                        return@addSnapshotListener
                    }
                    if (value!!.isEmpty) {
                        releaseCount = 0
                    } else {
                        releaseCount = value.documents.size
                        val lastReleaseDateInTimestamp = value.documents.last().get(CollectionRelease.kReleasedDate) as Timestamp
                        val date: Date = lastReleaseDateInTimestamp.toDate()
                        lastReleaseDate = dateFormat.format(date)
                    }
                    getAdmissionList()
                }
            queryListenerList.add(queryListener)
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }
    private fun getAdmissionList() {
//        admissionCount = 0
        val queryListener = Firebase.firestore.collection(CollectionAdmission.name)
            .whereEqualTo(CollectionAdmission.kIsArchive, false)
            .whereEqualTo(CollectionAdmission.kAnimalDocId, animalDocId)
            .orderBy(CollectionAdmission.kAdmissionDate, Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    binding.progressBar.visibility = View.GONE
                    Log.e("ERROR", "AdmissionListFrag. SnapshotListener-", error)
                    return@addSnapshotListener
                }
                if (value!!.isEmpty) {
                    updateDatsSource(listOf())
                } else {
                    Log.e("CHECK", "In Admission List - Admission Count - ${value.documents.size}")

                    val admissionList = value.documents.mapNotNull { d -> AdmissionDTO.create(d.id, d.data) }

                    // Get Medical Conditions for each Admission
                    getMedicalConditionForAdmissions(admissionList) { pair: Pair<Exception?, List<AdmissionDTO>?> ->
                        if (pair.second != null) {
                            // Get Reporting Person for each Admission
                            getReportingPersonForAdmissions(pair.second!!)
                        } else {
                            // Get Reporting Person for each Admission
                            getReportingPersonForAdmissions(admissionList)
                        }
                    }
                }
            }
        queryListenerList.add(queryListener)
    }

    private fun getMedicalConditionForAdmissions(admissionList: List<AdmissionDTO>, callback:(Pair<Exception?, List<AdmissionDTO>?>) -> Unit) {
        val medicalConditionIds: List<String> = admissionList.flatMap { it.medicalConditionIds }.distinct()
        val medicalConditionTaskSnapshotList = medicalConditionIds.map { Firebase.firestore.collection(CollectionMedicalConditionsList.name).document(it).get() }
        val allMedicalConditionsTask = Tasks.whenAllSuccess<DocumentSnapshot>(medicalConditionTaskSnapshotList)
        allMedicalConditionsTask.addOnSuccessListener { docSnapshotList ->
            for (admission in admissionList) {
                val medicalConditionNames = arrayListOf<String>()
                for (medConId in admission.medicalConditionIds) {
                    val doc: DocumentSnapshot? = docSnapshotList.findLast { doc -> doc.id == medConId }
                    val mcDTO: MedicalConditionDTO? = MedicalConditionDTO.create(doc?.id ?: "", doc?.data)
                    // Manthan: Do not include old Deleted/Archive Medical conditions
                    if (mcDTO != null && !mcDTO.isArchive) {
                        medicalConditionNames.add(mcDTO.name)
                    }
                }
                admission.medicalConditionNames = medicalConditionNames
            }
            callback.invoke(Pair(null, admissionList))
        }.addOnFailureListener { exception ->
            Log.e("NST", "Error AdmissionListFragment > Medical Conditions:\t${exception.message}")
            callback.invoke(Pair(exception, null))
        }
    }

    private fun getReportingPersonForAdmissions(admissionList: List<AdmissionDTO>) {
        val reportingPersonIds: List<String> = admissionList.map { it.reportingPersonId }.distinct()
        val reportingPersonTaskSnapshotList = reportingPersonIds.map { Firebase.firestore.collection(CollectionReportingPersons.name).document(it).get() }
        val allReportingPersonsTask = Tasks.whenAllSuccess<DocumentSnapshot>(reportingPersonTaskSnapshotList)
        allReportingPersonsTask.addOnSuccessListener { docSnapshotList ->
            for (admission in admissionList) {
                for (docSnap in docSnapshotList) {
                    if (admission.reportingPersonId == docSnap.id) {
                        val reportingPerson = GenericMemberDTO.create(docSnap.id, docSnap.data)
                        if (reportingPerson != null) {
                            admission.reportingPerson = reportingPerson
                            break
                        }
                    }
                }
            }
            updateDatsSource(admissionList)
        }.addOnFailureListener { exception ->
            Log.e("NST", "Error AdmissionListFragment > Reporting Person:\t${exception.message}")
            updateDatsSource(admissionList)
        }
    }

    private fun updateDatsSource(admissionList: List<AdmissionDTO>) {
        binding.progressBar.visibility = View.GONE
        admissionReleaseAdapter.updateDataSource(admissionList)
        sharedViewModel.setAllowAdmission(releaseCount >= admissionList.size)
        if (admissionList.isEmpty()) {
            binding.firstScheduleCL.visibility = View.VISIBLE
            binding.admissionRV.visibility = View.GONE
        } else {
            binding.firstScheduleCL.visibility = View.GONE
            binding.admissionRV.visibility = View.VISIBLE
        }
    }

    private fun onClickListeners() {
        binding.addAdmissionBTN.setOnClickListener {
            if (!AppDelegate.isDead) {
                if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                    if (sharedViewModel.getAllowAdmission()) {
                        val directions = AnimalProfileFragmentDirections.actionAnimalProfileFragmentToAddAdmissionFragment(
                            animalId = animalDocId,
                            reminderDocId = null,
                            from = "Add",
                            animalName = animalDTO?.name,
                            lastReleaseDate = lastReleaseDate
                        )
                        it.findNavController().navigate(directions)
                    } else {
                        Toast.makeText(requireContext(), "Animal is already admitted.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_dead, Toast.LENGTH_SHORT).show()
            }
        }
        binding.addFirstAdmissionBTN.setOnClickListener {
            if (!AppDelegate.isDead) {
                if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                    if (sharedViewModel.getAllowAdmission()) {
                        val directions = AnimalProfileFragmentDirections.actionAnimalProfileFragmentToAddAdmissionFragment(
                            animalId = animalDocId,
                            reminderDocId = null,
                            from = "Add",
                            animalName = animalDTO?.name,
                            lastReleaseDate = lastReleaseDate
                        )
                        it.findNavController().navigate(directions)
                    } else {
                        Toast.makeText(requireContext(), "Animal is already admitted.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_dead, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun didScrolledToEnd(position: Int) {}

    override fun dataSourceDidUpdate(size: Int) {
        if (size > 0) {
            binding.footerFL.visibility = View.VISIBLE
        }
    }

    override fun didSelectItem(dataItem: AdmissionDTO, position: Int) {
        // KJEFeiafdO5nLdGHjIPR
        Log.e("NST-M", "Selected Admission id: ${dataItem.id}")
        if (userType == CollectionWhitelistedNumbers.ADMIN || userType == CollectionWhitelistedNumbers.TEAM_LEADER) {
            findNavController().navigate(
                AnimalProfileFragmentDirections.actionAnimalProfileFragmentToEditAdmissionFragment(
                    admissionId = dataItem.id,
                    animalDocId = dataItem.animalDocId,
                    totalAdmissionCount = admissionReleaseAdapter.itemCount,
                    currentAdmissionIndex = position,
                    animalName = animalDTO?.name,
                    lastReleaseDate = lastReleaseDate
                )
            )
        }
    }

    private fun registerForPopBackData() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Int>("move_to_deworming")?.observe(viewLifecycleOwner) { result ->
            Log.e("MN-LD", "Admission list pop back stack Result: $result")
            if (result == 1) {
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<Int>("move_to_deworming")
                askForDeworming()
            }
        }
    }

    private fun askForDeworming() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Do you want to schedule deworming for this animal ?")
        builder.setPositiveButton("Yes") { dialog, which ->
            (parentFragment as AnimalProfileFragment?)?.categoryVP?.setCurrentItem(2, true)
        }
        builder.setNegativeButton("No") { dialog, which ->

        }
        builder.create()
        builder.show()
    }
}