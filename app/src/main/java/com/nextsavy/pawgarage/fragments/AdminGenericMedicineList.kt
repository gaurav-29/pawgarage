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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.adapters.GenericMedicineAdapter
import com.nextsavy.pawgarage.databinding.FragmentAdminGenericMedicinListBinding
import com.nextsavy.pawgarage.models.MedicalConditionDTO
import com.nextsavy.pawgarage.models.MedicalConditionsModel
import com.nextsavy.pawgarage.models.VaccineListDTO
import com.nextsavy.pawgarage.utils.CollectionMedicalConditionsList
import com.nextsavy.pawgarage.utils.CollectionMedicinesList
import com.nextsavy.pawgarage.utils.CollectionVaccinesList
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.MedicineType
import com.nextsavy.pawgarage.utils.MenuClickListener
import java.lang.reflect.Field
import java.lang.reflect.Method


class AdminGenericMedicineList : Fragment(), MenuClickListener {

    private lateinit var binding: FragmentAdminGenericMedicinListBinding
    private val args: AdminGenericMedicineListArgs by navArgs()
    private lateinit var adapter: GenericMedicineAdapter
    private lateinit var genericType: MedicineType
    var searchHandler: Handler? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminGenericMedicinListBinding.inflate(inflater, container, false)
        genericType = MedicineType.valueOf(args.genericMedicineType!!)
        when (genericType) {
            MedicineType.VACCINE -> {
                binding.toolbarOne.titleToolbarOne.text = "Vaccine List"
            }
            MedicineType.DEWORMING -> {
                binding.toolbarOne.titleToolbarOne.text = "Deworming Medicines"
            }
            MedicineType.MEDICAL_CONDITION -> {
                binding.toolbarOne.titleToolbarOne.text = "Medical Conditions"
            }
        }

        binding.progressBar2.visibility = View.VISIBLE
        binding.toolbarOne.generalImgToolbarOne.visibility = View.VISIBLE
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }

        binding.toolbarOne.generalImgToolbarOne.setImageResource(R.drawable.ic_add_round)
        binding.toolbarOne.generalImgToolbarOne.setOnClickListener {
            it.findNavController().navigate(R.id.adminGenericMedicineDetails, Bundle().apply { putString("genericMedicineType", genericType.name) })
        }

        setupGenericMedicineRV()

        //getGenericMedicineList()

        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(p0: String?): Boolean {
                if (!p0.isNullOrBlank()) {
                    searchHandler?.removeCallbacksAndMessages(null)
                    searchHandler = Handler(Looper.getMainLooper())
                    searchHandler?.postDelayed({
                        getGenericMedicineList(p0.lowercase())
                    }, 700)
                }
                else {
                    searchHandler?.removeCallbacksAndMessages(null)
                    searchHandler = Handler(Looper.getMainLooper())
                    searchHandler?.postDelayed({
                        getGenericMedicineList("")
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

    private fun setupGenericMedicineRV() {
        binding.genericMedicineRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = GenericMedicineAdapter(medicineType = genericType, arrayListOf(), this)
        binding.genericMedicineRV.adapter = adapter
    }

    private fun getGenericMedicineList(searchText: String) {

        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            val query: Query

            val collectionName = when (genericType) {
                MedicineType.VACCINE -> { CollectionVaccinesList.name }
                MedicineType.DEWORMING -> { CollectionMedicinesList.name }
                MedicineType.MEDICAL_CONDITION -> { CollectionMedicalConditionsList.name }
            }

            if (searchText.isNotEmpty()) {
                query = Firebase.firestore.collection(collectionName)
                    .whereEqualTo(CollectionVaccinesList.kIsArchive, false)
                    .whereArrayContains(CollectionVaccinesList.kSearchKeywords, searchText)
                    .orderBy(CollectionVaccinesList.kName, Query.Direction.ASCENDING)
            } else {
                query = Firebase.firestore.collection(collectionName)
                    .whereEqualTo(CollectionVaccinesList.kIsArchive, false)
                    .orderBy(CollectionVaccinesList.kName, Query.Direction.ASCENDING)
            }

                query.get()
                .addOnSuccessListener { documents ->
                    binding.progressBar2.visibility = View.GONE
                    val list = documents.mapNotNull { document ->
                        VaccineListDTO.create(document.id, document.data)
                    }
                    adapter.replaceDataSource(list)
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("ADMIN", "Error getting admin details: ", exception)
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_LONG)
                        .show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onMenuClick(view: View, docId: String) {
        val popup = PopupMenu(requireContext(), view)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.edit -> {
                    findNavController().navigate(R.id.adminGenericMedicineDetails, Bundle().apply {
                        putString("itemDocId", docId)
                        putString("genericMedicineType", genericType.name)
                    })
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
        inflater.inflate(R.menu.popup_menu, popup.menu)

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
            Log.e("ID-to delete", documentId)
            deleteGenericMedicine(documentId)
        }
        builder.setNegativeButton("No") { dialog, which ->
        }
        builder.show()
    }

    private fun deleteGenericMedicine(documentId: String) {
        binding.progressBar2.visibility = View.VISIBLE

        val collectionName = when (genericType) {
            MedicineType.VACCINE -> {
                CollectionVaccinesList.name
            }
            MedicineType.DEWORMING -> {
                CollectionMedicinesList.name
            }
            MedicineType.MEDICAL_CONDITION -> {
                CollectionMedicalConditionsList.name
            }
        }

            val data = hashMapOf(
                CollectionVaccinesList.kIsArchive to true,
                CollectionVaccinesList.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionVaccinesList.kUpdatedBy to Firebase.auth.currentUser?.uid,
            )
            Firebase.firestore.collection(collectionName).document(documentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    binding.progressBar2.visibility = View.GONE
                    getGenericMedicineList("")
                    Log.e("UPDATE", "DocumentSnapshot successfully archived!")
                    Toast.makeText(requireContext(), "Deleted successfully.", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("UPDATE", "Error in deleting document", e)
                    Toast.makeText(requireContext(), "${e.message}.", Toast.LENGTH_LONG).show()
                }
    }
}