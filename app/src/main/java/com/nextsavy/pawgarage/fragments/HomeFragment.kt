package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewDelegate
import com.nextsavy.pawgarage.RecyclerViewMenuInterface
import com.nextsavy.pawgarage.adapters.AnimalRVAdapter
import com.nextsavy.pawgarage.adapters.RemindersGeneralAdapter
import com.nextsavy.pawgarage.databinding.FragmentHomeBinding
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.ArchivedDTO
import com.nextsavy.pawgarage.models.DewormingDTO
import com.nextsavy.pawgarage.models.ProfileLeadDTO
import com.nextsavy.pawgarage.models.ReleaseDTO
import com.nextsavy.pawgarage.models.ReminderDTO
import com.nextsavy.pawgarage.models.VaccinationDTO
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionArchived
import com.nextsavy.pawgarage.utils.CollectionDeworming
import com.nextsavy.pawgarage.utils.CollectionProfileLeads
import com.nextsavy.pawgarage.utils.CollectionRelease
import com.nextsavy.pawgarage.utils.CollectionReleaseStatus
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionTreatment
import com.nextsavy.pawgarage.utils.CollectionVaccination
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import java.lang.Exception
import java.util.Calendar
import java.util.Date

/**
-> In this project, "Treatment" word is replaced by "OPD" whenever it is shown visually.
Related fragment and file names which includes "Treatment" are not replaced internally in project.
-> In this project, "Release" word is replaced by "Status" whenever it is shown visually.
Related fragment and file names which includes "Release" are not replaced internally in project.
 */


class HomeFragment : Fragment(), RecyclerViewMenuInterface<ReminderDTO>, RecyclerViewDelegate<AnimalDTO> {

    private lateinit var binding: FragmentHomeBinding
    lateinit var glManager: GridLayoutManager
    private lateinit var profilesAdapter: AnimalRVAdapter
    private lateinit var adapter: RemindersGeneralAdapter
    private lateinit var archivedAnimalDocIdsList: List<String>
    private val activeCasesList = arrayListOf<String>()

    val userType: String
        get() = Helper.sharedPreference?.getString("USER_TYPE", CollectionWhitelistedNumbers.TEAM_MEMBER) ?: CollectionWhitelistedNumbers.TEAM_MEMBER

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        initiateAnimalProfilesData()
        initiateRemindersData()
        getActiveCases()
        getRemindersData()
        getAnimalProfilesData()
        onClickListeners()
        getArchivedAnimalDocIds()
        //getDashboardData()

        return binding.root
    }

    private val activeCasesTapped = View.OnClickListener {
        Log.e("S", "${activeCasesList.size}")
        findNavController().navigate(R.id.activeCasesListFragment, bundleOf("active_cases_list" to activeCasesList.toTypedArray()))
    }

    private fun getActiveCases() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar.visibility = View.VISIBLE
            Firebase.firestore.collection(CollectionAnimals.name)
                .whereEqualTo(CollectionAnimals.kIsArchive, false)
                .whereEqualTo(CollectionAnimals.kState, "Active")
                .whereEqualTo(CollectionAnimals.kIsDead, false)
                .whereEqualTo(CollectionAnimals.kType, CollectionAnimals.IPD)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    binding.progressBar.visibility = View.GONE
                    binding.activeCasesTV.text = querySnapshot.documents.size.toString()
                    activeCasesList.clear()
                    val names = querySnapshot.documents.mapNotNull { it.data?.get("name") as String }
                    activeCasesList.addAll(names)
                }
                .addOnFailureListener { exception ->
                    activeCasesList.clear()
                    binding.progressBar.visibility = View.GONE
                    binding.activeCasesTV.text = "NA"
                    Toast.makeText(requireContext(), exception.message, Toast.LENGTH_SHORT).show()
                    Log.e("NST", "Error getting getActiveCases Data: ", exception)
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getArchivedAnimalDocIds() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar.visibility = View.VISIBLE
            archivedAnimalDocIdsList = listOf()
            Firebase.firestore.collection(CollectionArchived.name)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    binding.progressBar.visibility = View.GONE
                    val archivedAnimalList = querySnapshot.mapNotNull { ArchivedDTO.create(it.id, it.data) }
                    archivedAnimalDocIdsList = archivedAnimalList.map { it.animalDocId }.distinct()
                    getDashboardData()
                }
                .addOnFailureListener { exception ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), exception.message, Toast.LENGTH_SHORT).show()
                    Log.e("ARCHIVE_DATA", "Error getting Archived Data: ", exception)
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun initiateAnimalProfilesData() {
        glManager = GridLayoutManager(requireContext(), 2)
        binding.profilesRV.layoutManager = glManager
        profilesAdapter = AnimalRVAdapter(dataSource = arrayListOf(), listener = null, delegate = this)
        binding.profilesRV.adapter = profilesAdapter
    }

    private fun initiateRemindersData() {
        binding.remindersRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = RemindersGeneralAdapter(arrayListOf(), userType, this)
        binding.remindersRV.adapter = adapter
    }

    private fun onClickListeners() {
        binding.kActiveCasesTV.setOnClickListener(activeCasesTapped)
        binding.activeCasesTV.setOnClickListener(activeCasesTapped)

        binding.seeAllRemmindersTV.setOnClickListener {
//            val mainActivity = (requireActivity() as MainActivity)
//            mainActivity.binding.bottomNav.selectedItemId = R.id.remindersFragment
            it.findNavController().navigate(R.id.remindersFragment)
        }
        binding.seeAllProfilesTV.setOnClickListener {
//            val mainActivity = (requireActivity() as MainActivity)
//            mainActivity.binding.bottomNav.selectedItemId = R.id.animal_graph
            it.findNavController().navigate(R.id.animal_graph)
        }
        binding.toolbarMain.notificationsIV.setOnClickListener {
            it.findNavController().navigate(R.id.notificationsFragment)
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.finish()
            }
        })
    }

    private fun getAnimalProfilesData() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar.visibility = View.VISIBLE
            Firebase.firestore.collection(CollectionAnimals.name)
                .whereEqualTo(CollectionAnimals.kIsArchive, false)
                .orderBy(CollectionAnimals.kCreatedAt, Query.Direction.DESCENDING)
                .limit(6)
                .get()
                .addOnSuccessListener { documents ->
                    binding.progressBar.visibility = View.GONE
                    binding.footer.root.visibility = View.VISIBLE

                    val animalList = documents.documents.mapNotNull { doc -> AnimalDTO.create(doc.id, doc.data) }

                    if (animalList.isNotEmpty()) {
                        binding.profilesTV.visibility = View.VISIBLE
                        binding.seeAllProfilesTV.visibility = View.VISIBLE
                        binding.profilesRV.visibility = View.VISIBLE

                        profilesAdapter.updateDataSource(animalList)
                    }
                }
                .addOnFailureListener { exception ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), exception.message, Toast.LENGTH_SHORT).show()
                    Log.e("DATA", "Error getting documents: ", exception)
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getRemindersData() {
        if (Helper.isInternetAvailable(requireContext())) {
            val calendar = Calendar.getInstance()
            calendar.time = Date()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val todayDate = calendar.time
            calendar.add(Calendar.DAY_OF_MONTH, 3)
            val threeDaysAfterTodaysDateAtTime00 = calendar.time
            Log.e("NST-MD", "Today: ${todayDate.toString()}")
            Log.e("NST-MD", "3 days after: ${threeDaysAfterTodaysDateAtTime00.toString()}")
            val remindersList = arrayListOf<ReminderDTO>()
            val distinctAnimalIdList = arrayListOf<String>()
            val distinctProfileLeadIdList = arrayListOf<String>()

            binding.progressBar.visibility = View.VISIBLE
            Firebase.firestore.collection(CollectionReminders.name)
                .whereGreaterThanOrEqualTo(CollectionReminders.kReminderDate, todayDate)
                .whereLessThan(CollectionReminders.kReminderDate, threeDaysAfterTodaysDateAtTime00)
                .whereEqualTo(CollectionReminders.kIsComplete, false)
                .whereEqualTo(CollectionReminders.kIsArchive, false)
                .orderBy(CollectionReminders.kReminderDate, Query.Direction.ASCENDING)
                .limit(3)
                .get().addOnSuccessListener { querySnapshot ->
                    remindersList.addAll(querySnapshot.documents.mapNotNull { doc -> ReminderDTO.create(doc.id, doc.data) })
                    for (reminder in remindersList) {
                        if (!reminder.animalDocId.isNullOrBlank() && reminder.reminderType != CollectionReminders.COMPLETE_PROFILE) {
                            distinctAnimalIdList.add(reminder.animalDocId)
                        } else if (reminder.reminderType == CollectionReminders.COMPLETE_PROFILE &&
                            reminder.reminderObjectId != null) {
                            distinctProfileLeadIdList.add(reminder.reminderObjectId)
                        }
                    }
                    getAnimalProfilesFrom(distinctAnimalIdList) { pair: Pair<Exception?, List<AnimalDTO>?> ->
                        if (pair.second != null) {
                            for (animal in pair.second!!) {
                                for (reminder in remindersList) {
                                    if (reminder.animalDocId == animal.id) {
                                        reminder.animalDTO = animal
                                    }
                                }
                            }
                        }
                        getAnimalProfileLeadsFrom(distinctProfileLeadIdList) { leadsPair: Pair<Exception?, List<ProfileLeadDTO>?> ->
                            if (leadsPair.second != null) {
                                for (profileLead in leadsPair.second!!) {
                                    for (reminder in remindersList) {
                                        if (reminder.reminderObjectId == profileLead.id) {
                                            reminder.profileLeadDTO = profileLead
                                            break
                                        }
                                    }
                                }
                            }

                            remindersList.removeIf { reminder -> reminder.animalDTO == null && reminder.profileLeadDTO == null }
                            updateRemindersDataSource(remindersList)
                        }
                    }
                }
                .addOnFailureListener { qException ->
                    binding.progressBar.visibility = View.GONE
                    Log.e("NST-M", "Exception: HomeFragment > ${qException.localizedMessage}")
                    Toast.makeText(requireContext(), "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAnimalProfilesFrom(animalIds: List<String>, callback:(Pair<Exception?, List<AnimalDTO>?>) -> Unit) {
        val animalProfileTaskSnapshotList = animalIds.map { Firebase.firestore.collection(CollectionAnimals.name).document(it).get() }
        val allAnimalProfilesTask = Tasks.whenAllSuccess<DocumentSnapshot>(animalProfileTaskSnapshotList)
        allAnimalProfilesTask.addOnSuccessListener { docSnapshotList ->
            val animalList = docSnapshotList.mapNotNull { doc -> AnimalDTO.create(doc.id, doc.data) }.filter { animal -> !animal.isDead && animal.state != CollectionAnimals.TERMINATED }
            callback.invoke(Pair(null, animalList))
        }.addOnFailureListener { exception ->
            Log.e("NST-M", "Exception: AllRemindersFragment > getAnimalProfilesFrom: ${exception.localizedMessage}")
            callback.invoke(Pair(exception, null))
        }
    }

    private fun getAnimalProfileLeadsFrom(profileLeadIds: List<String>, callback:(Pair<Exception?, List<ProfileLeadDTO>?>) -> Unit) {
        val animalProfileLeadTaskSnapshotList = profileLeadIds.map { Firebase.firestore.collection(CollectionProfileLeads.name).document(it).get() }
        val allAnimalProfileLeadsTask = Tasks.whenAllSuccess<DocumentSnapshot>(animalProfileLeadTaskSnapshotList)
        allAnimalProfileLeadsTask.addOnSuccessListener { docSnapshotList ->
            val animalList = docSnapshotList.mapNotNull { doc -> ProfileLeadDTO.create(doc.id, doc.data) }.filter { animal -> !animal.isArchive }
            callback.invoke(Pair(null, animalList))
        }.addOnFailureListener { exception ->
            Log.e("NST-M", "Exception: AllRemindersFragment > getAnimalProfileLeadsFrom: ${exception.localizedMessage}")
            callback.invoke(Pair(exception, null))
        }
    }

    private fun updateRemindersDataSource(remindersList: List<ReminderDTO>) {
        adapter.updateDataSource(remindersList)
        binding.progressBar.visibility = View.GONE
    }

    private fun getDashboardData() {
        binding.progressBar.visibility = View.VISIBLE
        Firebase.firestore.collection(CollectionAdmission.name)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val admissionAnimalDocIdList = ArrayList<String>()
                for (document in querySnapshot.documents) {
                    val animalDocId = document.data?.get(CollectionAdmission.kAnimalDocId) as String
                    admissionAnimalDocIdList.add(animalDocId)
                }
                admissionAnimalDocIdList.removeIf { item -> archivedAnimalDocIdsList.contains(item) }
                binding.ipdTV.text = "${admissionAnimalDocIdList.size}"

                Firebase.firestore.collection(CollectionTreatment.name)
                    .get()
                    .addOnSuccessListener { querySnapshot2 ->
                        val treatmentAnimalDocIdList = ArrayList<String>()
                        for (document in querySnapshot2.documents) {
                            val animalDocId = document.data?.get(CollectionTreatment.kAnimalDocId) as String
                            treatmentAnimalDocIdList.add(animalDocId)
                        }
                        treatmentAnimalDocIdList.removeIf { item -> archivedAnimalDocIdsList.contains(item) }
                        binding.opdTV.text = "${treatmentAnimalDocIdList.size}"

                        binding.totalTV.text = "${admissionAnimalDocIdList.size + treatmentAnimalDocIdList.size}"
                    }
            }

        Firebase.firestore.collection(CollectionVaccination.name)
            .whereEqualTo(CollectionVaccination.kIsArchive, false)
            .get().addOnSuccessListener { querySnapshot ->
                val vaccinationList = querySnapshot.documents.mapNotNull{ doc -> VaccinationDTO.create(doc.id, doc.data) }
                val completeVaccination = arrayListOf<VaccinationDTO>()
                for (vaccination in vaccinationList) {
                    if (archivedAnimalDocIdsList.contains(vaccination.animalDocId)) {
                        continue
                    }
                    if (vaccination.vaccinationStatus == CollectionVaccination.COMPLETED) {
                        completeVaccination.add(vaccination)
                    }
                }
                binding.completeVaccineTV.text = "${completeVaccination.size}"
                Log.e("NUM-V", "Complete vaccination count:\t${completeVaccination.size}")
            }

        Firebase.firestore.collection(CollectionRelease.name)
            .whereEqualTo(CollectionRelease.kIsArchive, false)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val releaseList = querySnapshot.documents.mapNotNull { doc -> ReleaseDTO.create(doc.id, doc.data) }
                val freeAnimals = arrayListOf<ReleaseDTO>()
                val adoptedAnimals = arrayListOf<ReleaseDTO>()
                val deadAnimals = arrayListOf<ReleaseDTO>()

                for (release in releaseList) {
                    if (archivedAnimalDocIdsList.contains(release.animalDocId)) {
                        continue
                    }
                    when (release.releaseStatus) {
                        CollectionReleaseStatus.RELEASED -> {
                            freeAnimals.add(release)
                        }
                        CollectionReleaseStatus.ADOPTED -> {
                            adoptedAnimals.add(release)
                        }
                        CollectionReleaseStatus.DEATH -> {
                            deadAnimals.add(release)
                        }
                        else -> {}
                    }
                }
                Log.e("NUM-S", "Free count:\t${freeAnimals.size}")
                Log.e("NUM-S", "Adopted count:\t${adoptedAnimals.size}")
                Log.e("NUM-S", "Dead count:\t${deadAnimals.size}")
                binding.freeAnimalTV.text = "${freeAnimals.size}"
                binding.adoptedAnimalsTV.text = "${adoptedAnimals.size}"
                binding.deadAnimalTV.text = "${deadAnimals.size}"
            }

        Firebase.firestore.collection(CollectionDeworming.name)
            .whereEqualTo(CollectionDeworming.kIsArchive, false)
            .get().addOnSuccessListener { querySnapshot ->
                val dewormingList = querySnapshot.documents.mapNotNull{ doc -> DewormingDTO.create(doc.id, doc.data) }
                val completeDeworming = arrayListOf<DewormingDTO>()
                for (deworming in dewormingList) {
                    if (archivedAnimalDocIdsList.contains(deworming.animalDocId)) {
                        continue
                    }
                    if (deworming.dewormingStatus == CollectionDeworming.COMPLETED) {
                        completeDeworming.add(deworming)
                    }
                }
                binding.completeDewormingTV.text = "${completeDeworming.size}"
                Log.e("NUM-D", "Complete deworming count:\t${completeDeworming.size}")
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun turnOffReminder(reminder: ReminderDTO) {
        //binding.progressBar.visibility = View.VISIBLE
        Firebase.firestore
            .collection(CollectionReminders.name)
            .document(reminder.id)
            .set(hashMapOf(CollectionReminders.kIsArchive to true), SetOptions.merge())
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                adapter.removeReminder(reminder)
                Toast.makeText(requireContext(), "Reminder turned off", Toast.LENGTH_SHORT).show()
                getRemindersData()
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Unable to turn off the reminder!", Toast.LENGTH_SHORT).show()
                Log.e("NST-M", "Exception: AllReminderFrag > turnOffReminder: ${reminder.id}: ${exception.localizedMessage}")
            }
    }

    override fun didSelectMenuItem(view: View, dataItem: ReminderDTO, position: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.turnOffMenu -> {
                    turnOffReminder(dataItem)
                    return@setOnMenuItemClickListener true
                }
                else -> {
                    return@setOnMenuItemClickListener false
                }
            }
        }

        val inflater = popup.menuInflater
        inflater.inflate(R.menu.reminder_menu, popup.menu)

        popup.show()
    }

    override fun didScrolledToEnd(position: Int) {
    }
    override fun dataSourceDidUpdate(size: Int) {
        if (size > 0) {
            binding.remindersTV.visibility = View.VISIBLE
            binding.remindersRV.visibility = View.VISIBLE
            binding.seeAllRemmindersTV.visibility = View.VISIBLE
        }
    }

    override fun didSelectItem(dataItem: ReminderDTO, position: Int) {
        val userType = Helper.sharedPreference?.getString("USER_TYPE", "").toString()
        when (userType) {
            CollectionWhitelistedNumbers.ADMIN,
            CollectionWhitelistedNumbers.TEAM_LEADER-> {
                if (dataItem.profileLeadDTO != null) {
                    findNavController().navigate(R.id.newProfileFragment, bundleOf("profileLead" to dataItem.profileLeadDTO))
                } else if (dataItem.animalDTO != null) {
                    // Manthan 24-11-2023
                    // Send Type of Reminder to Animal Profile Page as 'actionType'.
                    // Then scroll to desire section, if requires in Animal Profile ViewPager
                    val payload = Bundle().apply {
                        putString("animalId", dataItem.animalDocId)
                        putString("actionType", dataItem.reminderType)
                    }
                    findNavController().navigate(R.id.animalProfileFragment, payload)
                }
            }
            CollectionWhitelistedNumbers.TEAM_MEMBER -> {
                if (dataItem.animalDocId == "") {
                    Toast.makeText(requireContext(), "Please inform the authorized person to complete the profile.", Toast.LENGTH_LONG).show()
                } else {
                    // Manthan 24-11-2023
                    // Send Type of Reminder to Animal Profile Page as 'actionType'.
                    // Then scroll to desire section, if requires in Animal Profile ViewPager
                    val payload = Bundle().apply {
                        putString("animalId", dataItem.animalDocId)
                        putString("actionType", dataItem.reminderType)
                    }
                    findNavController().navigate(R.id.animalProfileFragment, payload)
                }
            }
            else -> {

            }
        }
    }

    override fun didSelectItem(recyclerView: RecyclerView, dataItem: AnimalDTO, position: Int) {
        findNavController().navigate(R.id.animalProfileFragment, Bundle().apply { putString("animalId", dataItem.id) })
    }
}