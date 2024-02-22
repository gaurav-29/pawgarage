package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.ItemReportsBinding
import com.nextsavy.pawgarage.models.SettingsModel


class ReportsAdapter(private var dataList:ArrayList<SettingsModel>): RecyclerView.Adapter<ReportsAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemReportsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(dataList[position])
    }
    override fun getItemCount(): Int {
        return dataList.size
    }
    inner class MyViewHolder(private val binding: ItemReportsBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingsModel) {
            if (absoluteAdapterPosition == 3) {
                binding.reportsIV.rotation = -180F
            }
            binding.reportsIV.setImageResource(item.image)
            binding.reportsTV.text = item.title

            itemView.setOnClickListener {
                when(item.title) {
                    "Vaccination" -> it.findNavController().navigate(R.id.vaccinationReportFragment)
                    "Deworming" -> it.findNavController().navigate(R.id.dewormingReportFragment)
                    "Admission" -> it.findNavController().navigate(R.id.admissionReportFragment)
                    "Status" -> it.findNavController().navigate(R.id.releaseReportFragment)
                    "Medical Condition" -> it.findNavController().navigate(R.id.medicalConditionReportFragment)
                }
            }
        }
    }
}