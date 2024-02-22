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
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.GenericMemberAdapter
import com.nextsavy.pawgarage.databinding.FragmentReportingPersonsListBinding
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.utils.CollectionReportingPersons
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.MenuClickListener
import com.nextsavy.pawgarage.viewModels.SharedViewModel
import java.lang.reflect.Field
import java.lang.reflect.Method

class ReportingPersonsListFragment :
    Fragment(),
    MenuClickListener,
    RecyclerViewPagingInterface<GenericMemberDTO>,
    SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentReportingPersonsListBinding
    private val adapter = GenericMemberAdapter(arrayListOf(), false, this, this)

    private val args: ReportingPersonsListFragmentArgs by navArgs()

    var searchHandler: Handler? = null
    var allowPicking: Boolean = true

    val userType: String
        get() = Helper.sharedPreference?.getString("USER_TYPE", CollectionWhitelistedNumbers.TEAM_MEMBER) ?: CollectionWhitelistedNumbers.TEAM_MEMBER

    private val sharedViewModel: SharedViewModel by activityViewModels<SharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportingPersonsListBinding.inflate(inflater, container, false)

        allowPicking = (args.From != "Settings")

        setupUI()
        initialiseReportingPersonsRV()
        if (binding.searchView.query.trim().isBlank()) {
            getReportingPersonsList("")
        }
        return binding.root
    }

    private fun initialiseReportingPersonsRV() {
        binding.personsRV.layoutManager = LinearLayoutManager(requireContext())
        binding.personsRV.adapter = adapter
    }

    private fun setupUI() {
        binding.toolbarOne.titleToolbarOne.text = "Reporting Persons"
        binding.toolbarOne.generalImgToolbarOne.visibility = View.VISIBLE
        binding.toolbarOne.generalImgToolbarOne.setImageResource(R.drawable.ic_add_round)

        binding.searchView.setOnQueryTextListener(this)

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().popBackStack()
        }
        binding.toolbarOne.generalImgToolbarOne.setOnClickListener {
            it.findNavController().navigate(R.id.reportingPersonDetailsFragment, Bundle().apply { putString("userDocId", null) })
        }
    }

    private fun getReportingPersonsList(searchText: String) {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            val query = if (searchText.isNotBlank()) {
                Firebase.firestore
                    .collection(CollectionReportingPersons.name)
                    .whereEqualTo(CollectionReportingPersons.kIsArchive, false)
                    .whereArrayContains(CollectionReportingPersons.kSearchKeywords, searchText)
                    .orderBy(CollectionReportingPersons.kUserName, Query.Direction.ASCENDING)
            } else {
                Firebase.firestore
                    .collection(CollectionReportingPersons.name)
                    .whereEqualTo(CollectionReportingPersons.kIsArchive, false)
                    .orderBy(CollectionReportingPersons.kUserName, Query.Direction.ASCENDING)
            }
            query.get().addOnSuccessListener { querySnapshot ->
                val reportingPersons = querySnapshot.documents.mapNotNull { doc -> GenericMemberDTO.create(doc.id, doc.data) }
                adapter.allowEdit = !allowPicking && userType != CollectionWhitelistedNumbers.TEAM_MEMBER
                adapter.replaceDataSource(reportingPersons)
                binding.progressBar2.visibility = View.GONE
            }.addOnFailureListener { e ->
                binding.progressBar2.visibility = View.GONE
                Log.e("NST-M", "ReportingPersons > getReportingPersonsList:\t${e.localizedMessage}")
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    override fun didScrolledToEnd(position: Int) {}

    override fun dataSourceDidUpdate(size: Int) {}

    override fun didSelectItem(dataItem: GenericMemberDTO, position: Int) {
        if (allowPicking) {
            sharedViewModel.setReportingPerson(dataItem)
            findNavController().popBackStack()
        }
    }

    override fun onMenuClick(view: View, docId: String) {
        val popup = PopupMenu(requireContext(), view)
        // For menu item click.
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.edit -> {
                    findNavController().navigate(R.id.reportingPersonDetailsFragment, Bundle().apply { putString("userDocId", docId) })
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

    private fun showAlertDialog(personId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Do you really want to delete ?")
        builder.setPositiveButton("Yes") { dialog, which ->
            deleteReportingPerson(personId)
        }
        builder.setNegativeButton("No") { dialog, which ->
        }
        builder.show()
    }

    private fun deleteReportingPerson(personId: String) {
        binding.progressBar2.visibility = View.VISIBLE
        val data = hashMapOf(
            CollectionReportingPersons.kIsArchive to true,
            CollectionReportingPersons.kUpdatedAt to FieldValue.serverTimestamp(),
            CollectionReportingPersons.kUpdatedBy to Firebase.auth.currentUser?.uid,
        )
        Firebase.firestore.collection(CollectionReportingPersons.name).document(personId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                binding.progressBar2.visibility = View.GONE
                getReportingPersonsList("")
                Log.e("UPDATE", "DocumentSnapshot successfully archived!")
                Toast.makeText(requireContext(), "Deleted successfully.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                binding.progressBar2.visibility = View.GONE
                Log.e("UPDATE", "Error in deleting document", e)
                Toast.makeText(requireContext(), "${e.message}.", Toast.LENGTH_LONG).show()
            }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (!newText.isNullOrBlank()) {
            searchHandler?.removeCallbacksAndMessages(null)
            searchHandler = Handler(Looper.getMainLooper())
            searchHandler?.postDelayed({
                getReportingPersonsList(newText.lowercase())
            }, 700)
        }
        else {
            searchHandler?.removeCallbacksAndMessages(null)
            searchHandler = Handler(Looper.getMainLooper())
            searchHandler?.postDelayed({
                getReportingPersonsList("")
            }, 700)
        }
        return false
    }

}