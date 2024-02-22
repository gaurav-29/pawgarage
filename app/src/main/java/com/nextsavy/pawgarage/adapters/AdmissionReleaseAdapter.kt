package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemAdmissionReleaseBinding
import com.nextsavy.pawgarage.models.AdmissionDTO
import java.text.SimpleDateFormat
import java.util.Locale

class AdmissionReleaseAdapter(
    var dataList:ArrayList<AdmissionDTO>,
    val listener: RecyclerViewPagingInterface<AdmissionDTO>?): RecyclerView.Adapter<AdmissionReleaseAdapter.MyViewHolder>(){

    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemAdmissionReleaseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(dataList[position], (itemCount - position).toString())
    }
    override fun getItemCount(): Int {
        return dataList.size
    }
    fun updateDataSource(newData: List<AdmissionDTO>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(dataList.size)
    }
    inner class MyViewHolder(private val binding: ItemAdmissionReleaseBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener?.didSelectItem(dataList[absoluteAdapterPosition], absoluteAdapterPosition)
            }
        }

        fun bind(item: AdmissionDTO, number: String) {
            binding.titleTV.text = "Admission Details $number"
            binding.locationIV.setImageResource(R.drawable.ic_weight)
            binding.locationIV.setColorFilter(R.color.black)
            binding.addressTV.text = "${item.weight} kgs"
            binding.addressTV.setTextColor(itemView.context.getColor(R.color.black))

            binding.dateTV.text = dateFormat.format(item.admissionDate.toDate())

//            binding.conditionTV.text = item.medical_conditions
            if (item.medicalConditionNames.isNullOrEmpty()) {
                binding.conditionIV.visibility = View.GONE
                binding.conditionTV.visibility = View.GONE
            }  else {
                binding.conditionIV.visibility = View.VISIBLE
                binding.conditionTV.visibility = View.VISIBLE
                binding.conditionTV.text = item.medicalConditionNames!!.joinToString(", ")
            }

            binding.personTV.text = item.reportingPerson?.name
            binding.mobileTV.text = item.reportingPerson?.phoneNumber

            binding.noteLL.visibility = View.GONE
        }
    }
}