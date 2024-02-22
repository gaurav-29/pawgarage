package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewDelegate
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemProfileBinding
import com.nextsavy.pawgarage.models.AnimalDTO

class AnimalRVAdapter(
    val dataSource: ArrayList<AnimalDTO>,
    val listener: RecyclerViewPagingInterface<AnimalDTO>?,
    var delegate: RecyclerViewDelegate<AnimalDTO>? = null): RecyclerView.Adapter<AnimalRVAdapter.ViewHolder>() {

    lateinit var recyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == dataSource.size - 1) {
            listener?.didScrolledToEnd(position)
        }
        holder.bind(dataSource[position])
    }

    fun updateDataSource(newData: List<AnimalDTO>) {
        dataSource.clear()
        dataSource.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(dataSource.size)
    }

    fun injectNextBatch(newData: List<AnimalDTO>) {
        dataSource.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(dataSource.size)
    }

    inner class ViewHolder(val binding: ItemProfileBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener?.didSelectItem(dataSource[absoluteAdapterPosition], absoluteAdapterPosition)
                delegate?.didSelectItem(recyclerView, dataSource[absoluteAdapterPosition], absoluteAdapterPosition)
            }
        }

        fun bind(item: AnimalDTO) {
            binding.nameTV.text = item.name
            binding.idTV.text = item.animalId
            binding.locationTV.text = item.address
            Glide.with(binding.root.context).load(item.downloadUrl).placeholder(R.drawable.paw_placeholder).into(binding.profileIV)
        }
    }

}