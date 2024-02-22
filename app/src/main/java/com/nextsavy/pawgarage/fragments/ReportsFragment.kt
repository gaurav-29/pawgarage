package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.adapters.ReportsAdapter
import com.nextsavy.pawgarage.databinding.FragmentReportsBinding
import com.nextsavy.pawgarage.models.SettingsModel

class ReportsFragment : Fragment() {

    private lateinit var binding: FragmentReportsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportsBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "Reports"

        setUpReportsList()
        onClickListeners()

        return binding.root
    }

    private fun onClickListeners() {
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
    }

    private fun setUpReportsList() {
        val reportsList = arrayListOf<SettingsModel>()

        reportsList.add(SettingsModel(R.drawable.ic_vaccine, "Vaccination"))
        reportsList.add(SettingsModel(R.drawable.ic_medicine, "Deworming"))
        reportsList.add(SettingsModel(R.drawable.ic_release, "Admission"))
        reportsList.add(SettingsModel(R.drawable.ic_release, "Status"))
        reportsList.add(SettingsModel(R.drawable.ic_deworming, "Medical Condition"))

        binding.reportsRV.layoutManager = LinearLayoutManager(requireContext())
        val reportsAdapter = ReportsAdapter(reportsList)
        binding.reportsRV.adapter = reportsAdapter
    }
}