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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewItemCheckInterface
import com.nextsavy.pawgarage.adapters.SelectMedicalConditionAdapter
import com.nextsavy.pawgarage.databinding.FragmentMedicalConditionListBinding
import com.nextsavy.pawgarage.models.MedicalConditionDTO
import com.nextsavy.pawgarage.utils.CollectionMedicalConditionsList
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.viewModels.SharedViewModel


class MedicalConditionListFragment : Fragment(), RecyclerViewItemCheckInterface<MedicalConditionDTO> {

    private lateinit var binding: FragmentMedicalConditionListBinding
    private lateinit var adapter: SelectMedicalConditionAdapter
    private var masterDatsSource = listOf<MedicalConditionDTO>()
    val db = Firebase.firestore

    private val viewModel: SharedViewModel by activityViewModels<SharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMedicalConditionListBinding.inflate(inflater, container, false)
        binding.toolbarOne.titleToolbarOne.text = "Medical Conditions"

        registerObserver()

        setUpRecyclerView()

        onClickListeners()

        getData()

        return binding.root
    }

    private fun registerObserver() {
        viewModel.selectedMedicalCondition.observe(viewLifecycleOwner, Observer {
            if (it.isNullOrEmpty()) {
                binding.addBTN.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_button_disable)
            } else {
                binding.addBTN.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_button_enable)
            }
        })
    }

    private fun onClickListeners() {
        binding.addBTN.setOnClickListener {
            it.findNavController().popBackStack()
        }
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().popBackStack()
        }
    }

    private fun setUpRecyclerView() {
        binding.medicalConditionsRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = SelectMedicalConditionAdapter(arrayListOf(), viewModel.getSelectedMedicalCondition(), this)
        binding.medicalConditionsRV.adapter = adapter

        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(p0: String?): Boolean {
                filterData(p0)
                return false
            }
        })
    }

    private fun getData() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar.visibility = View.VISIBLE

            db.collection(CollectionMedicalConditionsList.name)
                .whereEqualTo(CollectionMedicalConditionsList.kIsArchive, false)
                .orderBy(CollectionMedicalConditionsList.kName, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { result ->
                    val list = result.documents.mapNotNull { doc -> MedicalConditionDTO.create(doc.id, doc.data) }
                    masterDatsSource = list
                    adapter.updateDataSource(list)
                    binding.progressBar.visibility = View.GONE
                }
                .addOnFailureListener { exception ->
                    binding.progressBar.visibility = View.GONE
                    Log.e("DOC", "Error getting documents: $exception")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterData(text: String?) {
        if (text.isNullOrBlank()) {
            adapter.updateDataSource(masterDatsSource)
        } else {
            val filteredList: ArrayList<MedicalConditionDTO> = ArrayList()
            for (item in masterDatsSource) {
                if (item.name.lowercase().contains(text.lowercase())) {
                    filteredList.add(item)
                }
            }
            if (filteredList.isNotEmpty()) {
                adapter.filterDataList(filteredList)
            }
        }

    }

    override fun updateCheckFor(dataItem: MedicalConditionDTO, position: Int, isChecked: Boolean) {
        if (isChecked) {
            viewModel.addMedicalCondition(dataItem)
        } else {
            viewModel.removeMedicalCondition(dataItem)
        }
    }

    override fun didScrolledToEnd(position: Int) {}

    override fun dataSourceDidUpdate(size: Int) {}

    override fun didSelectItem(dataItem: MedicalConditionDTO, position: Int) {}

}