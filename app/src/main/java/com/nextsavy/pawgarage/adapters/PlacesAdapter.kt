package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemPlacesBinding
import com.nextsavy.pawgarage.models.Result

class PlacesAdapter(private var placesList:ArrayList<Result>, val listener: RecyclerViewPagingInterface<Result>?):
    RecyclerView.Adapter<PlacesAdapter.MyViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemPlacesBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(placesList[position])
    }
    override fun getItemCount(): Int {
        return placesList.size
    }

    fun updateDataSource(newData: List<Result>) {
        placesList.clear()
        placesList.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(placesList.size)
    }

    inner class MyViewHolder(private val binding: ItemPlacesBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener?.didSelectItem(placesList[absoluteAdapterPosition], absoluteAdapterPosition)
            }
        }
        fun bind(item: Result) {
            binding.nameTV.text = item.name
            binding.addressTV.text = item.vicinity
        }
    }
}