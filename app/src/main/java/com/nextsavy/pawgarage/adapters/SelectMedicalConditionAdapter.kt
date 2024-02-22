package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewItemCheckInterface
import com.nextsavy.pawgarage.models.MedicalConditionDTO

class SelectMedicalConditionAdapter(
    var dataSource: ArrayList<MedicalConditionDTO>,
    val selectedData: List<MedicalConditionDTO>,
    val delegate: RecyclerViewItemCheckInterface<MedicalConditionDTO>?
): RecyclerView.Adapter<SelectMedicalConditionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_medical_condition, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSource[position])
    }

    fun updateDataSource(newData: List<MedicalConditionDTO>) {
        dataSource.clear()
        dataSource.addAll(newData)
        notifyDataSetChanged()
        delegate?.dataSourceDidUpdate(dataSource.size)
    }

    fun filterDataList(filterList: ArrayList<MedicalConditionDTO>) {
        dataSource = filterList
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val medicalConditionNameTV: TextView = itemView.findViewById(R.id.titleTV)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

        init {
            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
                delegate?.updateCheckFor(dataSource[absoluteAdapterPosition], absoluteAdapterPosition, checkBox.isChecked)
            }
        }

        fun bind(dataItem: MedicalConditionDTO) {
            medicalConditionNameTV.text = dataItem.name
            checkBox.isChecked = selectedData.contains(dataItem)
        }
    }


}