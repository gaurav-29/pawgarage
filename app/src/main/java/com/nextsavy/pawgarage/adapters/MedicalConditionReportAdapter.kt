package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemMedicalConditionReportBinding
import com.nextsavy.pawgarage.models.ReportsGeneralModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicalConditionReportAdapter(private var dataList:ArrayList<ReportsGeneralModel>, val listener: RecyclerViewPagingInterface<ReportsGeneralModel>?)
    : RecyclerView.Adapter<MedicalConditionReportAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemMedicalConditionReportBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (position == dataList.size - 1) {
            listener?.didScrolledToEnd(position)
        }
        holder.bind(dataList[position])
    }
    override fun getItemCount(): Int {
        return dataList.size
    }
    fun updateDataSource(newData: List<ReportsGeneralModel>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(dataList.size)
    }

    fun injectNextBatch(newData: List<ReportsGeneralModel>) {
        dataList.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(dataList.size)
    }

    inner class MyViewHolder(private val binding: ItemMedicalConditionReportBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReportsGeneralModel) {
            binding.nameTV.text = item.animalName

            if (item.date != null) {
                val date: Date = item.date!!.toDate()
                val dateToShow = SimpleDateFormat("dd/MM/yy", Locale.US).format(date)
                binding.dateTV.text = dateToShow
            } else {
                binding.dateTV.text = ""
            }
            binding.genderTV.text = item.gender
            binding.conditionsTV.text = item.conditions
            binding.typeTV.text = item.type
        }
    }

}