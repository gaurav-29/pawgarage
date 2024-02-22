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
import com.nextsavy.pawgarage.databinding.FragmentProfileLeadsNotificationsBinding
import com.nextsavy.pawgarage.models.NotificationDTO
import com.nextsavy.pawgarage.models.ProfileLeadDTO
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionProfileLeads
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import java.lang.Exception

class ProfileLeadsNotificationsFragment : Fragment(), RecyclerViewPagingInterface<NotificationDTO> {
    private lateinit var binding: FragmentProfileLeadsNotificationsBinding
    var lastDocument: DocumentSnapshot? = null
    var reachedEnd = false
    var pageSize: Long = 20
    private lateinit var adapter: NotificationListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileLeadsNotificationsBinding.inflate(inflater, container, false)

        initiateNotificationsData()

        getNotifications()

        return binding.root
    }

    private fun initiateNotificationsData() {
        reachedEnd = false
        lastDocument = null
        binding.notificationsRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationListAdapter(arrayListOf(), this)
        binding.notificationsRV.adapter = adapter
    }

    private fun getNotifications() {
        if (Helper.isInternetAvailable(requireContext())) {
            if (!reachedEnd) {
                val notificationList = arrayListOf<NotificationDTO>()
                val distinctProfileLeadIdList = arrayListOf<String>()
                binding.progressBar.visibility = View.VISIBLE
                val query = if (lastDocument == null) {
                    Firebase.firestore.collection(CollectionNotifications.name)
                        .whereEqualTo(CollectionNotifications.kIsArchive, false)
                        .whereEqualTo(CollectionNotifications.kNotificationType, CollectionNotifications.PROFILE_LEADS)
                        .orderBy(CollectionNotifications.kNotificationDate, Query.Direction.DESCENDING)
                        .limit(pageSize)
                } else {
                    Firebase.firestore.collection(CollectionNotifications.name)
                        .whereEqualTo(CollectionNotifications.kIsArchive, false)
                        .whereEqualTo(CollectionNotifications.kNotificationType, CollectionNotifications.PROFILE_LEADS)
                        .orderBy(CollectionNotifications.kNotificationDate, Query.Direction.DESCENDING)
                        .startAfter(lastDocument!!)
                        .limit(pageSize)
                }

                query.get().addOnSuccessListener { querySnapshot ->
                    reachedEnd = querySnapshot.documents.size < pageSize

                    notificationList.addAll(querySnapshot.documents.mapNotNull { doc -> NotificationDTO.create(doc.id, doc.data) })

                    for (notification in notificationList) {
                        if (!notification.notificationObjectId.isNullOrBlank() && notification.notificationType == CollectionNotifications.PROFILE_LEADS) {
                            distinctProfileLeadIdList.add(notification.notificationObjectId)
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

                        notificationList.removeIf { notification -> notification.profileLeadDTO == null }
                        updateDataSource(notificationList, querySnapshot.documents.lastOrNull())
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

    private fun updateDataSource(notificationList: List<NotificationDTO>, lastDocSnapshot: DocumentSnapshot?) {
        if (this.lastDocument == null) {
            adapter.updateDataSource(notificationList)
        } else {
            adapter.injectNextBatch(notificationList)
        }
        lastDocument = lastDocSnapshot
        binding.progressBar.visibility = View.GONE
        if (notificationList.isEmpty()) {
            getNotifications()
        }
        if (adapter.itemCount > 0) {
            binding.noNotificationsTV.visibility = View.GONE
        } else {
            binding.noNotificationsTV.visibility = View.VISIBLE
        }

    }

    override fun didScrolledToEnd(position: Int) {
        getNotifications()
    }

    override fun dataSourceDidUpdate(size: Int) {

    }

    override fun didSelectItem(dataItem: NotificationDTO, position: Int) {
        val userType = Helper.sharedPreference?.getString("USER_TYPE", "").toString()
        when (userType) {
            CollectionWhitelistedNumbers.ADMIN,
            CollectionWhitelistedNumbers.TEAM_LEADER-> {
                if (dataItem.profileLeadDTO != null) {
                    findNavController().navigate(R.id.newProfileFragment, bundleOf("profileLead" to dataItem.profileLeadDTO))
                }
            }
            else -> {}
        }
    }

}