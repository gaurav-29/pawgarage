package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemVaccinationDewormingBinding
import com.nextsavy.pawgarage.models.DewormingDTO
import java.text.SimpleDateFormat
import java.util.Locale

class DewormingAdapter(val animalDocId: String,
                       var dataList:ArrayList<DewormingDTO>,
                       val listener: RecyclerViewPagingInterface<DewormingDTO>?): RecyclerView.Adapter<DewormingAdapter.ViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemVaccinationDewormingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(item = dataList[position], number = (itemCount - position).toString())
    }

    fun updateDataSource(newData: List<DewormingDTO>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(dataList.size)
    }

    inner class ViewHolder(private val binding: ItemVaccinationDewormingBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener?.didSelectItem(dataList[absoluteAdapterPosition], absoluteAdapterPosition)
            }
        }

        fun bind(item: DewormingDTO, number: String) {
            binding.titleTV.text = "Deworming $number"
            val dateToShow = SimpleDateFormat("dd MMM, yyyy", Locale.US).format(item.dewormingDate.toDate())
            binding.dateTV.text = dateToShow
            if (!item.medicineName.isNullOrBlank()) {
                binding.vaccineTV.text = item.medicineName
            } else {
                binding.vaccineTV.visibility = View.GONE
                binding.vaccineIV.visibility = View.GONE
            }
            binding.statusTV.text = item.dewormingStatus
            if (!item.weight.isNullOrBlank()) {
                binding.weightIV.visibility = View.VISIBLE
                binding.weightTV.visibility = View.VISIBLE
                binding.weightTV.text = item.weight
            } else {
                binding.weightTV.visibility = View.GONE
                binding.weightIV.visibility = View.GONE
            }
            if (item.adminNotes.isNotBlank()) {
                binding.noteLL.visibility = View.VISIBLE
                binding.noteTV.text = item.adminNotes
            } else {
                binding.noteLL.visibility = View.GONE
            }
            if (item.administratorPerson != null) {
                binding.personTV.visibility = View.VISIBLE
                binding.personIV.visibility = View.VISIBLE
                binding.personTV.text = item.administratorPerson!!.name
            } else {
                binding.personTV.visibility = View.GONE
                binding.personIV.visibility = View.GONE
            }
        }
    }

}