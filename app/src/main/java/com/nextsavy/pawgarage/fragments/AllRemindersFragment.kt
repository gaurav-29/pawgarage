package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewMenuInterface
import com.nextsavy.pawgarage.adapters.RemindersGeneralAdapter
import com.nextsavy.pawgarage.databinding.FragmentAllRemindersBinding
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.ProfileLeadDTO
import com.nextsavy.pawgarage.models.ReminderDTO
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionProfileLeads
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import java.lang.Exception


class AllRemindersFragment : Fragment(), RecyclerViewMenuInterface<ReminderDTO> {

    private lateinit var binding: FragmentAllRemindersBinding

    var lastDocument: DocumentSnapshot? = null
    var reachedEnd = false
    var pageSize: Long = 20

    private lateinit var adapter: RemindersGeneralAdapter

    val userType: String
        get() = Helper.sharedPreference?.getString("USER_TYPE", CollectionWhitelistedNumbers.TEAM_MEMBER) ?: CollectionWhitelistedNumbers.TEAM_MEMBER

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAllRemindersBinding.inflate(inflater, container, false)

        initiateRemindersData()

        getAllReminders()

        return binding.root
    }

    private fun initiateRemindersData() {
        reachedEnd = false
        lastDocument = null
        binding.allRemindersRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = RemindersGeneralAdapter(arrayListOf(), userType, this)
        binding.allRemindersRV.adapter = adapter
    }

    private fun getAllReminders() {
        if (Helper.isInternetAvailable(requireContext())) {

            val remindersList = arrayListOf<ReminderDTO>()
            val distinctAnimalIdList = arrayListOf<String>()
            val distinctProfileLeadIdList = arrayListOf<String>()

            if (!reachedEnd) {
                binding.progressBar.visibility = View.VISIBLE

                val query = if (lastDocument == null) {
                    Firebase.firestore.collection(CollectionReminders.name)
                        .whereEqualTo(CollectionReminders.kIsComplete, false)
                        .whereEqualTo(CollectionReminders.kIsArchive, false)
                        .orderBy(CollectionReminders.kReminderDate, Query.Direction.ASCENDING)
                        .limit(pageSize)
                } else {
                    Firebase.firestore.collection(CollectionReminders.name)
                        .whereEqualTo(CollectionReminders.kIsComplete, false)
                        .whereEqualTo(CollectionReminders.kIsArchive, false)
                        .orderBy(CollectionReminders.kReminderDate, Query.Direction.ASCENDING)
                        .startAfter(lastDocument!!)
                        .limit(pageSize)
                }

                query.get().addOnSuccessListener { querySnapshot ->
                    reachedEnd = querySnapshot.documents.size < pageSize
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

                            remindersList.removeIf { reminder ->
                                (reminder.animalDTO == null && reminder.profileLeadDTO == null) ||
                                        (reminder.animalDTO != null && (reminder.animalDTO!!.isArchive || reminder.animalDTO!!.isDead || reminder.animalDTO!!.state != "Active"))
                            }
                            updateDataSource(remindersList, querySnapshot.documents.lastOrNull())
                        }
                    }
                }.addOnFailureListener { qException ->
                    binding.progressBar.visibility = View.GONE
                    Log.e("NST-M", "Exception: AllRemindersFragment > ${qException.localizedMessage}")
                    Toast.makeText(requireContext(), "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("NST-M", "Reached end in load more")
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

    private fun updateDataSource(remindersList: List<ReminderDTO>, lastDocSnapshot: DocumentSnapshot?) {
        if (remindersList.isEmpty()) {
            lastDocument = lastDocSnapshot
            getAllReminders()
            binding.progressBar.visibility = View.GONE
        } else {
            if (this.lastDocument == null) {
                adapter.updateDataSource(remindersList)
            } else {
                adapter.injectNextBatch(remindersList)
            }
            lastDocument = lastDocSnapshot
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun turnOffReminder(reminder: ReminderDTO) {
        binding.progressBar.visibility = View.VISIBLE
        Firebase.firestore
            .collection(CollectionReminders.name)
            .document(reminder.id)
            .set(hashMapOf(CollectionReminders.kIsArchive to true), SetOptions.merge())
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                adapter.removeReminder(reminder)
                Toast.makeText(requireContext(), "Reminder turned off", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Unable to turn off the reminder!", Toast.LENGTH_SHORT).show()
                Log.e("NST-M", "Exception: AllReminderFrag > turnOffReminder: ${reminder.id}: ${exception.localizedMessage}")
            }
    }

    override fun didSelectMenuItem(view: View, dataItem: ReminderDTO, position: Int) {
        Log.e("NST", "Selected Reminder Id: ${dataItem.id}")
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
        getAllReminders()
    }

    override fun dataSourceDidUpdate(size: Int) {
        if (size < 1) {
            binding.noRemindersTV.visibility = View.VISIBLE
        } else {
            binding.noRemindersTV.visibility = View.GONE
        }
    }

    override fun didSelectItem(dataItem: ReminderDTO, position: Int) {
        val userType = Helper.sharedPreference?.getString("USER_TYPE", "").toString()
        when (userType) {
            CollectionWhitelistedNumbers.ADMIN,
            CollectionWhitelistedNumbers.TEAM_LEADER-> {
                if (dataItem.profileLeadDTO != null) {
                    findNavController().navigate(RemindersFragmentDirections.actionRemindersFragmentToNewProfileFragment(profileLead = dataItem.profileLeadDTO))
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
}
/*
private fun getDataFromProfileLeads(
        remindersList: ArrayList<RemindersModel>,
        profileLeadList: ArrayList<String>
    ) {
        if (profileLeadList.isNotEmpty()) {
            Firebase.firestore.collection(CollectionProfileLeads.name)
                .whereIn(FieldPath.documentId(), profileLeadList)
                .whereEqualTo(CollectionProfileLeads.kIsArchive, false)
                .get()
                .addOnSuccessListener {
                    binding.progressBar.visibility = View.GONE
                    for (document in it) {
                        for (i in remindersList) {
                            if (document.id == i.reminder_type_object_id) {
                                i.animal_name = document.data[CollectionProfileLeads.kName] as String
                                i.animal_image = document.data[CollectionProfileLeads.kDownloadUrl] as String
                                i.location_address = document.data[CollectionProfileLeads.kLocationAddress] as String
                                i.latitude = document.data[CollectionProfileLeads.kLatitude] as Double
                                i.longitude = document.data[CollectionProfileLeads.kLongitude] as Double
                            }
                        }
                    }
                    if (lastDocument == null) {
                        adapter.updateDataSource(remindersList)
                    } else {
                        adapter.injectNextBatch(remindersList)
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Log.e("ERROR_HOME_REMINDERS", it.toString())
                    Toast.makeText(requireContext(), "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } else {
            if (lastDocument == null) {
                adapter.updateDataSource(remindersList)
            } else {
                adapter.injectNextBatch(remindersList)
            }
        }
    }
 */