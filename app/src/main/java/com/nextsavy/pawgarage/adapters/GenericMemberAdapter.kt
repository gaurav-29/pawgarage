package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemTeamLeadersBinding
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.utils.MenuClickListener

class GenericMemberAdapter(
    val dataSource: ArrayList<GenericMemberDTO>,
    var allowEdit: Boolean,
    val delegate: RecyclerViewPagingInterface<GenericMemberDTO>?,
    val menuListener: MenuClickListener?): RecyclerView.Adapter<GenericMemberAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTeamLeadersBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSource[position])
    }

    fun replaceDataSource(newData: List<GenericMemberDTO>) {
        dataSource.clear()
        dataSource.addAll(newData)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemTeamLeadersBinding): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                delegate?.didSelectItem(dataSource[absoluteAdapterPosition], absoluteAdapterPosition)
            }

            binding.optionIV.visibility = if (allowEdit) View.VISIBLE else View.GONE

            binding.optionIV.setOnClickListener {
                menuListener?.onMenuClick(it, dataSource[absoluteAdapterPosition].id)
            }
        }

        fun bind(item: GenericMemberDTO) {
            binding.nameTV.text = item.name
            binding.numberTV.text = item.phoneNumber
        }
    }

}