package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.AnimalRVAdapter
import com.nextsavy.pawgarage.databinding.FragmentAllAnimalsBinding
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.SearchViewModel

class AllAnimalsFragment : Fragment(), RecyclerViewPagingInterface<AnimalDTO> {

    private lateinit var binding: FragmentAllAnimalsBinding
    private val viewModel: SearchViewModel by navGraphViewModels(R.id.animal_graph)
    private var adapter = AnimalRVAdapter(arrayListOf(), this)
    var searchQuery: String = ""
    var lastDocument: DocumentSnapshot? = null
    var reachedEnd = false
    var pageSize: Long = 20

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.e("VW-LFCYCLE", "AllAnimalFrag OnCreateView")
        binding = FragmentAllAnimalsBinding.inflate(inflater, container, false)

        setupAnimalRV()

        getDataSource(searchQuery)

        viewModel.queryForSearch.observe(viewLifecycleOwner, Observer { s ->
            if (s != null && searchQuery.lowercase() != s.lowercase()) {
                getDataSource(s.lowercase())
            }
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Log.e("VW-LFCYCLE", "AllAnimalFrag onResume")
    }

    private fun setupAnimalRV() {
        reachedEnd = false
        lastDocument = null
        if (viewModel.queryForSearch.value != null) {
            searchQuery = viewModel.queryForSearch.value!!
        }

        val layoutManager = GridLayoutManager(requireContext(), 2)
        binding.profilesRV.layoutManager = layoutManager
        binding.profilesRV.adapter = adapter
        // Manthan 24-11-2023
        // Clear adapter before fetching data from back-end
        adapter.updateDataSource(listOf())
    }

    private fun getDataSource(textToSearch: String = "") {
        Log.e("NST-MG", "#1")
        if (Helper.isInternetAvailable(requireContext())) {
            // If Search text is changed, treat it as Hard refresh or Reload
            if (textToSearch.lowercase() != searchQuery.lowercase()) {
                reachedEnd = false
                lastDocument = null
            }
            searchQuery = textToSearch
            if (!reachedEnd) {
                binding.progressBar.visibility = View.VISIBLE
                var query = Firebase.firestore.collection(CollectionAnimals.name)
                    .whereEqualTo(CollectionAnimals.kIsArchive, false)
                    .orderBy(CollectionAnimals.kCreatedAt, Query.Direction.DESCENDING)
                if (searchQuery.isNotBlank()) {
                    query = query.whereArrayContains(CollectionAnimals.kSearchKeywords, searchQuery)
                }
                if (lastDocument != null) {
                    query = query.startAfter(lastDocument!!)
                }
                query = query.limit(pageSize)
                query.get()
                    .addOnSuccessListener { querySnapshot ->
                        reachedEnd = querySnapshot.documents.size < pageSize
                        if (lastDocument == null) {
                            adapter.updateDataSource(querySnapshot.documents.mapNotNull { documentSnapshot -> AnimalDTO.create(documentSnapshot.id, documentSnapshot.data) })
                        } else {
                            adapter.injectNextBatch(querySnapshot.documents.mapNotNull { documentSnapshot -> AnimalDTO.create(documentSnapshot.id, documentSnapshot.data) })
                        }
                        lastDocument = querySnapshot.documents.lastOrNull()
                        binding.progressBar.visibility = View.GONE
                    }
                    .addOnFailureListener { qException ->
                        binding.progressBar.visibility = View.GONE
                        Log.e("NST-M", "Exception: AllAnimalFragment > ${qException.localizedMessage}")
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

    override fun didScrolledToEnd(position: Int) {
        Log.e("NST-M", "didScrolledToEnd: $position")
        getDataSource(searchQuery)
    }

    override fun dataSourceDidUpdate(size: Int) {
        if (size > 0) {
            binding.viewFlipper.displayedChild = 0
        } else {
            binding.viewFlipper.displayedChild = 1
        }
    }
    override fun didSelectItem(dataItem: AnimalDTO, position: Int) {
        Log.e("NST-M", "All animal > Selected Animal id: ${dataItem.id}")
        findNavController().navigate(R.id.animalProfileFragment, Bundle().apply { putString("animalId", dataItem.id) })
    }
}