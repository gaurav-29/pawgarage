package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.adapters.MedicalConditionsAdapter
import com.nextsavy.pawgarage.databinding.FragmentMedicalConditionsBinding
import com.nextsavy.pawgarage.models.MedicalConditionDTO
import com.nextsavy.pawgarage.models.MedicalConditionsModel
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CellClickListener
import com.nextsavy.pawgarage.utils.CollectionMedicalConditionsList
import com.nextsavy.pawgarage.utils.Helper

class MedicalConditionsFragment : Fragment(), CellClickListener {

    private lateinit var binding: FragmentMedicalConditionsBinding
    private lateinit var conditionList: ArrayList<MedicalConditionsModel>
    private lateinit var medicalConditionsAdapter: MedicalConditionsAdapter
    var selectedConditionsList:ArrayList<String> = arrayListOf()
    val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMedicalConditionsBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "Medical Conditions"

        onClickListeners()
        setUpRecyclerView()

        if (AppDelegate.selectedList2.size > 0) {
            binding.addBTN.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_button_enable)
        }
        return binding.root
    }

    private fun onClickListeners() {
        binding.addBTN.setOnClickListener {

            selectedConditionsList = medicalConditionsAdapter.getSelectedConditionsList()
            AppDelegate.selectedList2 = selectedConditionsList
            it.findNavController().popBackStack()
            Log.e("LIST", AppDelegate.selectedList2.toString())
        }
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().popBackStack()
        }
    }
    private fun setUpRecyclerView() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar.visibility = View.VISIBLE
            conditionList = ArrayList()

            db.collection(CollectionMedicalConditionsList.name)
                .whereEqualTo(CollectionMedicalConditionsList.kIsArchive, false)
                .orderBy(CollectionMedicalConditionsList.kName, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { result ->
                    binding.progressBar.visibility = View.GONE
                    // TODO : Pass vaccine id
                    for (document in result) {
                        conditionList.add(MedicalConditionsModel(document.data[CollectionMedicalConditionsList.kName] as String))
                    }
                    binding.medicalConditionsRV.layoutManager = LinearLayoutManager(requireContext())
                    medicalConditionsAdapter = MedicalConditionsAdapter(requireContext(), conditionList, AppDelegate.selectedList2, this)
                    binding.medicalConditionsRV.adapter = medicalConditionsAdapter
                }
                .addOnFailureListener { exception ->
                    binding.progressBar.visibility = View.GONE
                    Log.e("DOC", "Error getting documents: $exception")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }

        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(p0: String?): Boolean {
                filterData(p0)
                return false
            }
        })
    }

    private fun filterData(text: String?) {
        val filteredList: ArrayList<MedicalConditionsModel> = ArrayList()
        for (item in conditionList) {
            if (text != null) {
                if (item.conditions.lowercase().contains(text.lowercase())) {
                    filteredList.add(item)
                }
            }
        }
        if (filteredList.isNotEmpty()) {
            medicalConditionsAdapter.filterDataList(filteredList)
        } else {
            //Toast.makeText(requireContext(), "No Data Found..", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onColorChangeListener(setEnabled: Boolean) {
        if (setEnabled) binding.addBTN.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_button_enable)
        else binding.addBTN.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_button_disable)
    }
}