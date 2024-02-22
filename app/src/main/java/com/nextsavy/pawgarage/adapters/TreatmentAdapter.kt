package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemTreatmentBinding
import com.nextsavy.pawgarage.models.TreatmentDTO
import com.nextsavy.pawgarage.utils.Helper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TreatmentAdapter(
    private var dataList: ArrayList<TreatmentDTO>,
    val delegate: RecyclerViewPagingInterface<TreatmentDTO>?
    ): RecyclerView.Adapter<TreatmentAdapter.MyViewHolder>() {

    val userType = Helper.sharedPreference?.getString("USER_TYPE", "").toString()

    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemTreatmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(dataList[position], (itemCount - position).toString())
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun updateDataSource(newData: List<TreatmentDTO>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
        delegate?.dataSourceDidUpdate(dataList.size)
    }

    inner class MyViewHolder(private val binding: ItemTreatmentBinding): RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                delegate?.didSelectItem(dataList[absoluteAdapterPosition], absoluteAdapterPosition)
            }
        }

        fun bind(item: TreatmentDTO, number: String) {
            binding.titleTV.text = "OPD Details $number"
            binding.dateTV.text = dateFormat.format(item.treatmentDate.toDate())

            //  binding.conditionTV.text = item.medical_conditions
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

            if (item.adminNotes.isNotBlank()) {
                binding.noteLL.visibility = View.VISIBLE
                binding.noteTV.text = item.adminNotes
            } else {
                binding.noteLL.visibility = View.GONE
            }
        }
    }
}