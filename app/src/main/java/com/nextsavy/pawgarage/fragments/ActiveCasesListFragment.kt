package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.nextsavy.pawgarage.databinding.FragmentActiveCasesListBinding


class ActiveCasesListFragment : Fragment() {

    private lateinit var binding: FragmentActiveCasesListBinding
    private val args: ActiveCasesListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentActiveCasesListBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "Active Cases (${args.activeCasesList.size})"

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().popBackStack()
        }

        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, args.activeCasesList)
        binding.listView.adapter = adapter

        binding.searchView.setOnQueryTextListener(object: android.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                adapter.filter.filter(p0)
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                adapter.filter.filter(p0)
                return false
            }
        })

        if (args.activeCasesList.isEmpty()) {
            binding.viewFlipper.displayedChild = 1
        } else {
            binding.viewFlipper.displayedChild = 0
        }

        return binding.root
    }
}