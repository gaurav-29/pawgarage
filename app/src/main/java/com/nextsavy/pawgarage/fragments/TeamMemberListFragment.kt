package com.nextsavy.pawgarage.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.TeamLeadersAdapter
import com.nextsavy.pawgarage.databinding.FragmentTeamMemberListBinding
import com.nextsavy.pawgarage.models.TeamLeadersModel
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.MenuClickListener
import java.lang.reflect.Field
import java.lang.reflect.Method

class TeamMemberListFragment : Fragment(), MenuClickListener,
    RecyclerViewPagingInterface<TeamLeadersModel> {

    private lateinit var binding: FragmentTeamMemberListBinding
    private lateinit var adapter: TeamLeadersAdapter
    lateinit var listForSearch: List<TeamLeadersModel>
    var searchHandler: Handler? = null
    val userType: String
        get() = Helper.sharedPreference?.getString("USER_TYPE", CollectionWhitelistedNumbers.TEAM_MEMBER) ?: CollectionWhitelistedNumbers.TEAM_MEMBER

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTeamMemberListBinding.inflate(inflater, container, false)
        binding.toolbarOne.titleToolbarOne.text = "Team Members"

        binding.progressBar2.visibility = View.VISIBLE

        binding.toolbarOne.generalImgToolbarOne.visibility = View.VISIBLE
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }

        binding.toolbarOne.generalImgToolbarOne.setImageResource(R.drawable.ic_add_round)
        binding.toolbarOne.generalImgToolbarOne.setOnClickListener {
            it.findNavController().navigate(R.id.teamMemberDetails, Bundle().apply { putString("userDocId", null) })
        }

        setupTeamMemberRV()

        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(p0: String?): Boolean {
                if (!p0.isNullOrBlank()) {
                    searchHandler?.removeCallbacksAndMessages(null)
                    searchHandler = Handler(Looper.getMainLooper())
                    searchHandler?.postDelayed({
                        getTeamMemberList(p0.lowercase())
                    }, 700)
                }
                else {
                    searchHandler?.removeCallbacksAndMessages(null)
                    searchHandler = Handler(Looper.getMainLooper())
                    searchHandler?.postDelayed({
                        getTeamMemberList("")
                    }, 700)
                }
                return false
            }
        })

        return binding.root
    }
    override fun onResume() {
        super.onResume()
        // To make searchView text (query) empty.
        binding.searchView.setQuery("", false)
    }

    private fun setupTeamMemberRV() {
        binding.teamMemberRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = TeamLeadersAdapter(arrayListOf(), this, "", this)
        binding.teamMemberRV.adapter = adapter
    }

    private fun getTeamMemberList(searchText: String) {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            val query: Query
            if (searchText.isNotEmpty()) {
                query = Firebase.firestore.collection(CollectionWhitelistedNumbers.name)
                    .whereEqualTo(CollectionWhitelistedNumbers.kUserType, CollectionWhitelistedNumbers.TEAM_MEMBER)
                    .whereEqualTo(CollectionWhitelistedNumbers.kIsArchive, false)
                    .whereArrayContains(CollectionWhitelistedNumbers.kSearchKeywords, searchText)
                    .orderBy(CollectionWhitelistedNumbers.kUserName, Query.Direction.ASCENDING)
            } else {
                query = Firebase.firestore.collection(CollectionWhitelistedNumbers.name)
                    .whereEqualTo(CollectionWhitelistedNumbers.kUserType, CollectionWhitelistedNumbers.TEAM_MEMBER)
                    .whereEqualTo(CollectionWhitelistedNumbers.kIsArchive, false)
                    .orderBy(CollectionWhitelistedNumbers.kUserName, Query.Direction.ASCENDING)
            }
            query.get()
                .addOnSuccessListener { documents ->
                    binding.progressBar2.visibility = View.GONE
                    val list = documents.map { document ->
                        val model = TeamLeadersModel()
                        model.documentId = document.id
                        model.name = document.data[CollectionWhitelistedNumbers.kUserName] as String
                        model.number = document.data[CollectionWhitelistedNumbers.kContactNumber] as String
                        model
                    }
                    adapter.replaceDataSource(list)
                    listForSearch = list
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("ADMIN", "Error getting admin details: ", exception)
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_LONG)
                        .show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMenuClick(view: View, docId: String) {
        val popup = PopupMenu(requireContext(), view)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.edit -> {
                    findNavController().navigate(R.id.teamMemberDetails, Bundle().apply { putString("userDocId", docId) })
                    return@setOnMenuItemClickListener true
                }
                R.id.delete -> {
                    showAlertDialog(docId)
                    return@setOnMenuItemClickListener true
                }
                else -> {
                    return@setOnMenuItemClickListener false
                }
            }
        }

        val inflater = popup.menuInflater
        if (userType == CollectionWhitelistedNumbers.ADMIN) {
            inflater.inflate(R.menu.popup_menu, popup.menu)
        } else if (userType == CollectionWhitelistedNumbers.TEAM_LEADER) {
            inflater.inflate(R.menu.popup_menu_team_leader, popup.menu)
        }

        // To provide icon to menu item.
        try {
            val fields: Array<Field> = popup.javaClass.getDeclaredFields()
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper: Any = field.get(popup) as Any
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons: Method = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        popup.show()
    }

    private fun showAlertDialog(documentId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Do you really want to delete ?")
        builder.setPositiveButton("Yes") { dialog, which ->
            Log.e("ID", documentId)
            deleteTeamMember(documentId)
        }
        builder.setNegativeButton("No") { dialog, which ->
        }
        builder.show()
    }

    private fun deleteTeamMember(documentId: String) {
        binding.progressBar2.visibility = View.VISIBLE
        if (documentId != "") {
            val data = hashMapOf(
                CollectionWhitelistedNumbers.kIsArchive to true,
                CollectionWhitelistedNumbers.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionWhitelistedNumbers.kUpdatedBy to Firebase.auth.currentUser?.uid,
            )
            Firebase.firestore.collection(CollectionWhitelistedNumbers.name).document(documentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    binding.progressBar2.visibility = View.GONE
                    getTeamMemberList("")
                    Log.e("UPDATE", "DocumentSnapshot successfully archived!")
                    Toast.makeText(requireContext(), "Deleted successfully.", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("UPDATE", "Error in deleting document", e)
                    Toast.makeText(requireContext(), "${e.message}.", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "No document found to delete.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun didScrolledToEnd(position: Int) {
    }

    override fun dataSourceDidUpdate(size: Int) {
    }

    override fun didSelectItem(dataItem: TeamLeadersModel, position: Int) {
    }

}