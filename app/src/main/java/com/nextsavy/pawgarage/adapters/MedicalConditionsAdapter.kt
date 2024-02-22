package com.nextsavy.pawgarage.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.RecyclerViewItemCheckInterface
import com.nextsavy.pawgarage.databinding.ItemMedicalConditionsBinding
import com.nextsavy.pawgarage.models.AdmissionDTO
import com.nextsavy.pawgarage.models.MedicalConditionsModel
import com.nextsavy.pawgarage.utils.CellClickListener


class MedicalConditionsAdapter(
    private val context: Context,
    private var conditionList: ArrayList<MedicalConditionsModel>,
    private var selectedList: ArrayList<String>,
    private var onCellClickListener: CellClickListener,
    ): RecyclerView.Adapter<MedicalConditionsAdapter.MyViewHolder>() {

    init {
        Log.e("LISTA", selectedList.toString())
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemMedicalConditionsBinding.inflate(LayoutInflater.from(context), parent, false))
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(conditionList[position])
    }
    override fun getItemCount(): Int {
        return conditionList.size
    }
    fun filterDataList(filterList: ArrayList<MedicalConditionsModel>) {
        conditionList = filterList
        notifyDataSetChanged()
    }
    fun getSelectedConditionsList(): ArrayList<String> {
        return selectedList
    }
    inner class MyViewHolder(private val binding: ItemMedicalConditionsBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MedicalConditionsModel) {
            binding.checkbox.text = item.conditions

            binding.checkbox.setOnCheckedChangeListener(null)
            binding.checkbox.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
                override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                    item.isSelected2 = p1
                    binding.checkbox.isChecked = item.isSelected2
                    if (p0?.isChecked == true && !selectedList.contains(item.conditions)) {
                        selectedList.add(binding.checkbox.text.toString())
                        onCellClickListener.onColorChangeListener(true)
                    }
                    else if(p0?.isChecked == false && selectedList.contains(item.conditions)) {
                        selectedList.remove(binding.checkbox.text.toString())
                    }
                    Log.e("ADA", selectedList.toString())
                }
            })

            binding.checkbox.isChecked = selectedList.contains(item.conditions)
        }
    }
}

