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
import android.widget.Toast
import androidx.fragment.app.activityViewModels
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
import com.nextsavy.pawgarage.adapters.GenericMemberAdapter
import com.nextsavy.pawgarage.databinding.FragmentAdopterListBinding
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.utils.CollectionAdopters
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.MenuClickListener
import com.nextsavy.pawgarage.viewModels.SharedViewModel
import java.lang.reflect.Field
import java.lang.reflect.Method


class AdopterListFragment :
    Fragment(),
    MenuClickListener,
    RecyclerViewPagingInterface<GenericMemberDTO>,
    android.widget.SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentAdopterListBinding
    private val adapter = GenericMemberAdapter(arrayListOf(), false, this, this)
    var searchHandler: Handler? = null
    var allowPicking: Boolean = true
    val userType: String
        get() = Helper.sharedPreference?.getString("USER_TYPE", CollectionWhitelistedNumbers.TEAM_MEMBER) ?: CollectionWhitelistedNumbers.TEAM_MEMBER

    private val sharedViewModel: SharedViewModel by activityViewModels<SharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdopterListBinding.inflate(inflater, container, false)
        arguments?.takeIf { it.containsKey("allowPicking") }?.apply {
            allowPicking = getBoolean("allowPicking", true)
        }
        setupUI()
        initialiseAdoptersRV()
        if (binding.searchView.query.trim().isBlank()) {
            getAdoptersList("")
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
//        binding.searchView.setQuery("", false)
    }

    private fun initialiseAdoptersRV() {
        binding.adoptersRV.layoutManager = LinearLayoutManager(requireContext())
        binding.adoptersRV.adapter = adapter
    }

    private fun setupUI() {
        binding.toolbarOne.titleToolbarOne.text = "Adopters List"
        binding.toolbarOne.generalImgToolbarOne.visibility = View.VISIBLE
        binding.toolbarOne.generalImgToolbarOne.setImageResource(R.drawable.ic_add_round)

        binding.searchView.setOnQueryTextListener(this)

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().popBackStack()
        }
        binding.toolbarOne.generalImgToolbarOne.setOnClickListener {
            it.findNavController().navigate(R.id.adopterDetailsFragment, Bundle().apply { putString("adopterId", null) })
        }
    }

    private fun getAdoptersList(searchText: String) {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            val query = if (searchText.isNotBlank()) {
                Firebase.firestore
                    .collection(CollectionAdopters.name)
                    .whereEqualTo(CollectionAdopters.kIsArchive, false)
                    .whereArrayContains(CollectionAdopters.kSearchKeywords, searchText)
                    .orderBy(CollectionAdopters.kUserName, Query.Direction.ASCENDING)
            } else {
                Firebase.firestore
                    .collection(CollectionAdopters.name)
                    .whereEqualTo(CollectionAdopters.kIsArchive, false)
                    .orderBy(CollectionAdopters.kUserName, Query.Direction.ASCENDING)
            }
            query.get().addOnSuccessListener { querySnapshot ->
                val adopters = querySnapshot.documents.mapNotNull { doc -> GenericMemberDTO.create(doc.id, doc.data) }
                adapter.allowEdit = !allowPicking && userType != CollectionWhitelistedNumbers.TEAM_MEMBER
                adapter.replaceDataSource(adopters)
                binding.progressBar2.visibility = View.GONE
            }.addOnFailureListener { e ->
                binding.progressBar2.visibility = View.GONE
                Log.e("NST-M", "AdopterList > getAdoptersList:\t${e.localizedMessage}")
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    override fun didScrolledToEnd(position: Int) {

    }

    override fun dataSourceDidUpdate(size: Int) {

    }

    override fun didSelectItem(dataItem: GenericMemberDTO, position: Int) {
        if (allowPicking) {
            sharedViewModel.setAdopter(dataItem)
            findNavController().popBackStack()
        }
    }

    override fun onMenuClick(view: View, docId: String) {
        val popup = PopupMenu(requireContext(), view)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.edit -> {
                    findNavController().navigate(R.id.adopterDetailsFragment, Bundle().apply { putString("adopterId", docId) })
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

    private fun showAlertDialog(adopterId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Do you really want to delete ?")
        builder.setPositiveButton("Yes") { dialog, which ->
            Log.e("ID", adopterId)
            deleteReportingPerson(adopterId)
        }
        builder.setNegativeButton("No") { dialog, which ->
        }
        builder.show()
    }

    private fun deleteReportingPerson(adopterId: String) {
        binding.progressBar2.visibility = View.VISIBLE
        val data = hashMapOf(
            CollectionAdopters.kIsArchive to true,
            CollectionAdopters.kUpdatedAt to FieldValue.serverTimestamp(),
            CollectionAdopters.kUpdatedBy to Firebase.auth.currentUser?.uid,
        )
        Firebase.firestore.collection(CollectionAdopters.name).document(adopterId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                binding.progressBar2.visibility = View.GONE
                getAdoptersList("")
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
                getAdoptersList(newText.lowercase())
            }, 700)
        }
        else {
            searchHandler?.removeCallbacksAndMessages(null)
            searchHandler = Handler(Looper.getMainLooper())
            searchHandler?.postDelayed({
                getAdoptersList("")
            }, 700)
        }
        return false
    }


}