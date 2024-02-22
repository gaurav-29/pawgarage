package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemSettingsBinding
import com.nextsavy.pawgarage.models.SettingsModel
import com.nextsavy.pawgarage.utils.Helper

class SettingsAdapter(
    private var dataList: ArrayList<SettingsModel>,
    val delegate: RecyclerViewPagingInterface<SettingsModel>?): RecyclerView.Adapter<SettingsAdapter.MyViewHolder>() {

    private val userType = Helper.sharedPreference?.getString("USER_TYPE", "")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemSettingsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun updateDataSource(newData: List<SettingsModel>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
        delegate?.dataSourceDidUpdate(dataList.size)
    }

    inner class MyViewHolder(private val binding: ItemSettingsBinding): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                delegate?.didSelectItem(dataList[absoluteAdapterPosition], absoluteAdapterPosition)
            }
        }

        fun bind(item: SettingsModel) {
            binding.settingsIV.setImageResource(item.image)
            binding.settingsTV.text = item.title
        }
    }
}