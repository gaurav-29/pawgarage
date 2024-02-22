package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemVaccinationDewormingBinding
import com.nextsavy.pawgarage.models.VaccinationDTO
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaccinationDewormingAdapter(val animalDocId: String, var dataList:ArrayList<VaccinationDTO>, private var type: String,
                                  val listener: RecyclerViewPagingInterface<VaccinationDTO>?):
    RecyclerView.Adapter<VaccinationDewormingAdapter.MyViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemVaccinationDewormingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(item = dataList[position], number = (itemCount - position).toString())
    }
    fun updateDataSource(newData: List<VaccinationDTO>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(dataList.size)
    }
    fun injectNextBatch(newData: List<VaccinationDTO>) {
        dataList.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(dataList.size)
    }
    override fun getItemCount(): Int {
        return dataList.size
    }
    inner class MyViewHolder(private val binding: ItemVaccinationDewormingBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener?.didSelectItem(dataList[absoluteAdapterPosition], absoluteAdapterPosition)
            }
        }
        fun bind(item: VaccinationDTO, number: String) {
            binding.titleTV.text = "Vaccine $number"
            val date: Date = item.vaccinationDate.toDate()
            val dateToShow = SimpleDateFormat("dd MMM, yyyy", Locale.US).format(date)

            binding.dateTV.text = dateToShow
            if (!item.vaccineName.isNullOrBlank()) {
                binding.vaccineTV.visibility = View.VISIBLE
                binding.vaccineIV.visibility = View.VISIBLE
                binding.vaccineTV.text = item.vaccineName
            } else {
                binding.vaccineTV.visibility = View.GONE
                binding.vaccineIV.visibility = View.GONE
            }
            binding.statusTV.text = item.vaccinationStatus
            binding.weightTV.visibility = View.GONE
            binding.weightIV.visibility = View.GONE

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