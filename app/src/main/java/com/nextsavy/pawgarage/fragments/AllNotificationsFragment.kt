package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.NotificationListAdapter
import com.nextsavy.pawgarage.databinding.FragmentAllNotificationsBinding
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.GenericUserDTO
import com.nextsavy.pawgarage.models.NotificationDTO
import com.nextsavy.pawgarage.models.ProfileLeadDTO
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionProfileLeads
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import java.lang.Exception

class AllNotificationsFragment : Fragment(), RecyclerViewPagingInterface<NotificationDTO> {
    private lateinit var binding: FragmentAllNotificationsBinding

    var lastDocument: DocumentSnapshot? = null
    var reachedEnd = false
    var pageSize: Long = 20

    private lateinit var adapter: NotificationListAdapter

    val userType: String
        get() = Helper.sharedPreference?.getString("USER_TYPE", CollectionWhitelistedNumbers.TEAM_MEMBER) ?: CollectionWhitelistedNumbers.TEAM_MEMBER

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAllNotificationsBinding.inflate(inflater, container, false)

        initiateNotificationsData()

        getAllNotifications()

        return binding.root
    }

    private fun initiateNotificationsData() {
        reachedEnd = false
        lastDocument = null
        binding.allNotificationsRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationListAdapter(arrayListOf(), this)
        binding.allNotificationsRV.adapter = adapter
    }

    private fun getAllNotifications() {
        if (Helper.isInternetAvailable(requireContext())) {
            if (!reachedEnd) {
                val notificationList = arrayListOf<NotificationDTO>()
                val distinctAnimalIdList = arrayListOf<String>()
                val distinctCreatorIdList = arrayListOf<String>()
                val distinctProfileLeadIdList = arrayListOf<String>()
                binding.progressBar.visibility = View.VISIBLE
                val query = if (lastDocument == null) {
                    Firebase.firestore.collection(CollectionNotifications.name)
                        .whereEqualTo(CollectionNotifications.kIsArchive, false)
                        .orderBy(CollectionNotifications.kNotificationDate, Query.Direction.DESCENDING)
                        .limit(pageSize)
                } else {
                    Firebase.firestore.collection(CollectionNotifications.name)
                        .whereEqualTo(CollectionNotifications.kIsArchive, false)
                        .orderBy(CollectionNotifications.kNotificationDate, Query.Direction.DESCENDING)
                        .startAfter(lastDocument!!)
                        .limit(pageSize)
                }

                query.get().addOnSuccessListener { querySnapshot ->
                    reachedEnd = querySnapshot.documents.size < pageSize

                    notificationList.addAll(querySnapshot.documents.mapNotNull { doc -> NotificationDTO.create(doc.id, doc.data) })

                    for (notification in notificationList) {
                        if (!notification.animalDocId.isNullOrBlank() && notification.notificationType != CollectionNotifications.PROFILE_LEADS) {
                            distinctAnimalIdList.add(notification.animalDocId)
                            when (notification.notificationType) {
                                CollectionNotifications.NEW_PROFILE,
                                CollectionNotifications.ADOPTED,
                                CollectionNotifications.RELEASED,
                                CollectionNotifications.DEATH -> {
                                    distinctCreatorIdList.add(notification.createdBy)
                                }
                            }
                        } else if (notification.notificationType == CollectionNotifications.PROFILE_LEADS &&
                            notification.notificationObjectId != null) {
                            distinctProfileLeadIdList.add(notification.notificationObjectId)
                        }
                    }

                    getAnimalProfilesFrom(distinctAnimalIdList.distinct()) { pair: Pair<Exception?, List<AnimalDTO>?> ->
                        if (pair.second != null) {
                            for (animal in pair.second!!) {
                                for (notification in notificationList) {
                                    if (notification.animalDocId == animal.id) {
                                        notification.animalDTO = animal
                                    }
                                }
                            }
                        }
                        getAnimalProfileLeadsFrom(distinctProfileLeadIdList.distinct()) { leadsPair: Pair<Exception?, List<ProfileLeadDTO>?> ->
                            if (leadsPair.second != null) {
                                for (profileLead in leadsPair.second!!) {
                                    for (notification in notificationList) {
                                        if (notification.notificationObjectId == profileLead.id) {
                                            notification.profileLeadDTO = profileLead
                                        }
                                    }
                                }
                            }

                            if (distinctCreatorIdList.isEmpty()) {
                                notificationList.removeIf { notification -> notification.animalDTO == null && notification.profileLeadDTO == null }
                                updateDataSource(notificationList, querySnapshot.documents.lastOrNull())
                            } else {
                                getNotificationCreatorsFrom(distinctCreatorIdList.distinct()) { creatorPair: Pair<Exception?, List<GenericUserDTO>?> ->
                                    if (creatorPair.second != null) {
                                        for (creator in creatorPair.second!!) {
                                            for (notification in notificationList) {
                                                if (notification.createdBy == creator.id) {
                                                    notification.creator = creator
                                                }
                                            }
                                        }
                                    }

                                    notificationList.removeIf { notification -> notification.animalDTO == null && notification.profileLeadDTO == null }
                                    updateDataSource(notificationList, querySnapshot.documents.lastOrNull())
                                }
                            }
                        }
                    }
                }.addOnFailureListener { qException ->
                    binding.progressBar.visibility = View.GONE
                    Log.e("NST-M", "Exception: AllNotificationsFragment > ${qException.localizedMessage}")
                    Toast.makeText(requireContext(), qException.localizedMessage, Toast.LENGTH_SHORT).show()
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
            val animalList = docSnapshotList.mapNotNull { doc -> AnimalDTO.create(doc.id, doc.data) }.filter { animal -> !animal.isDead } // && animal.state != CollectionAnimals.TERMINATED
            callback.invoke(Pair(null, animalList))
        }.addOnFailureListener { exception ->
            Log.e("NST-M", "Exception: AllNotificationsFragment > getAnimalProfilesFrom: ${exception.localizedMessage}")
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
            Log.e("NST-M", "Exception: AllNotificationsFragment > getAnimalProfileLeadsFrom: ${exception.localizedMessage}")
            callback.invoke(Pair(exception, null))
        }
    }

    private fun getNotificationCreatorsFrom(creatorIds: List<String>, callback:(Pair<Exception?, List<GenericUserDTO>?>) -> Unit) {
        val creatorTaskSnapshotList = creatorIds.map { Firebase.firestore.collection(CollectionUser.name).document(it).get() }
        val allCreatorTask = Tasks.whenAllSuccess<DocumentSnapshot>(creatorTaskSnapshotList)
        allCreatorTask.addOnSuccessListener { docSnapshotList ->
            val creatorList = docSnapshotList.mapNotNull { doc -> GenericUserDTO.create(doc.id, doc.data) }
            callback.invoke(Pair(null, creatorList))
        }.addOnFailureListener { exception ->
            Log.e("NST-M", "Exception: AllNotificationsFragment > getNotificationCreatorsFrom: ${exception.localizedMessage}")
            callback.invoke(Pair(exception, null))
        }
    }

    private fun updateDataSource(notificationList: List<NotificationDTO>, lastDocSnapshot: DocumentSnapshot?) {
        if (this.lastDocument == null) {
            adapter.updateDataSource(notificationList)
        } else {
            adapter.injectNextBatch(notificationList)
        }
        lastDocument = lastDocSnapshot
        binding.progressBar.visibility = View.GONE
        if (notificationList.isEmpty()) {
            getAllNotifications()
        }
        if (adapter.itemCount > 0) {
            binding.noNotificationsTV.visibility = View.GONE
        } else {
            binding.noNotificationsTV.visibility = View.VISIBLE
        }

    }

    override fun didScrolledToEnd(position: Int) {
        getAllNotifications()
    }

    override fun dataSourceDidUpdate(size: Int) {

    }

    override fun didSelectItem(dataItem: NotificationDTO, position: Int) {
        if (dataItem.notificationType == CollectionNotifications.PROFILE_LEADS) {
            if (dataItem.profileLeadDTO != null &&
                (userType == CollectionWhitelistedNumbers.ADMIN || userType == CollectionWhitelistedNumbers.TEAM_LEADER)) {
                findNavController().navigate(R.id.newProfileFragment, bundleOf("profileLead" to dataItem.profileLeadDTO))
            }
        } else {
            // Manthan 25-11-2023
            // Send Notification Type to Animal Profile Page as 'actionType'.
            // Then scroll to desire section, if requires in Animal Profile ViewPager
            val payload = Bundle().apply {
                putString("animalId", dataItem.animalDocId)
                putString("actionType", dataItem.notificationType)
            }
            findNavController().navigate(R.id.animalProfileFragment, payload)
        }
    }
}

/*
// Manthan 25-11-2023
// An Animal does not exist yet for Profile Lead.
if (dataItem.notification_type != CollectionNotifications.PROFILE_LEADS) {
    // Manthan 25-11-2023
    // Send Type of Notification to Animal Profile Page as 'actionType'.
    // Then scroll to desire section, if requires in Animal Profile ViewPager
    val payload = Bundle().apply {
        putString("animalId", dataItem.animal_doc_id)
        putString("actionType", dataItem.notification_type)
    }
    findNavController().navigate(R.id.animalProfileFragment, payload)
}*/
