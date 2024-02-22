package com.nextsavy.pawgarage.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemTeamLeadersBinding
import com.nextsavy.pawgarage.models.TeamLeadersModel
import com.nextsavy.pawgarage.utils.MenuClickListener

class TeamLeadersAdapter(private var dataList:ArrayList<TeamLeadersModel>,
                         private var menuClickListener: MenuClickListener,
                         private var from: String,
                         val listener: RecyclerViewPagingInterface<TeamLeadersModel>?): RecyclerView.Adapter<TeamLeadersAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemTeamLeadersBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(dataList[position])
    }
    override fun getItemCount(): Int {
       return dataList.size
    }
    fun replaceDataSource(newData: List<TeamLeadersModel>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
    }
    fun filterDataList(filterList: ArrayList<TeamLeadersModel>) {
        dataList = filterList
        notifyDataSetChanged()
    }

    inner class MyViewHolder(private val binding: ItemTeamLeadersBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener?.didSelectItem(dataList[absoluteAdapterPosition], absoluteAdapterPosition)
            }
        }
        fun bind(item: TeamLeadersModel) {
            binding.nameTV.text = item.name
            binding.numberTV.text = item.number

            if (from == "Others") {
                binding.optionIV.visibility = View.GONE
            } else {
                binding.optionIV.visibility = View.VISIBLE
            }
            binding.optionIV.setOnClickListener {
                Log.e("ID2", item.documentId)
                menuClickListener.onMenuClick(it, item.documentId)
            }
        }
    }
}