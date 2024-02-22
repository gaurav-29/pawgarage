package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.databinding.ItemHistoryBinding
import com.nextsavy.pawgarage.models.AdmissionDTO
import com.nextsavy.pawgarage.models.DewormingDTO
import com.nextsavy.pawgarage.models.FeedEventDTO
import com.nextsavy.pawgarage.models.FeedType
import com.nextsavy.pawgarage.models.ReleaseDTO
import com.nextsavy.pawgarage.models.TreatmentDTO
import com.nextsavy.pawgarage.models.VaccinationDTO
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.Locale


class HistoryAdapter(private var animalName: String, private var dataSource:ArrayList<FeedEventDTO>):
    RecyclerView.Adapter<HistoryAdapter.MyViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(dataSource[position])
    }
    override fun getItemCount(): Int {
       return dataSource.size
    }

    fun replaceDataSource(animalName: String, newData: List<FeedEventDTO>) {
        dataSource.clear()
        dataSource.addAll(newData)
        this.animalName = animalName
        notifyDataSetChanged()
    }

    inner class MyViewHolder(private val binding: ItemHistoryBinding): RecyclerView.ViewHolder(binding.root) {
        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.US)
        fun bind(item: FeedEventDTO) {
            when (item.feedType) {
                FeedType.REGISTRATION -> {
                    binding.titleTV.text = "Registered"
                    binding.descriptionTV.text = "$animalName is registered with Paw Garage" //on ${dateFormat.format(item.createdAt.toDate())}
                }
                FeedType.ACTIVATED -> {
                    val dateStr = dateFormat.format(item.createdAt.toDate())
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("$animalName's profile is activated")
                    if (dateStr.isNotEmpty()) {
                        stringBuilder.append(" on $dateStr")
                    }
                    binding.titleTV.text = "Status - Activated"
                    binding.descriptionTV.text = stringBuilder.toString()
                }
                FeedType.TERMINATED -> {
                    val dateStr = dateFormat.format(item.createdAt.toDate())
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("$animalName's profile is terminated")
                    if (dateStr.isNotEmpty()) {
                        stringBuilder.append(" on $dateStr")
                    }
                    binding.titleTV.text = "Status - Terminated"
                    binding.descriptionTV.text = stringBuilder.toString()
                }
                FeedType.ADMISSION -> {
//                    val medicalConditions = (item.feedObject as AdmissionDTO).medical_conditions
                    val medicalConditions = (item.feedObject as AdmissionDTO).medicalConditionNames?.joinToString(", ") ?: ""
                    val dateStr = dateFormat.format((item.feedObject as AdmissionDTO).admissionDate.toDate())
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("$animalName was admitted")
                    if (medicalConditions.isNotEmpty()) {
                        stringBuilder.append(" for $medicalConditions")
                    }
                    if (dateStr.isNotEmpty()) {
                        stringBuilder.append(" on $dateStr")
                    }
                    binding.titleTV.text = "Admitted"
                    binding.descriptionTV.text = stringBuilder.toString()
                }
                FeedType.TREATMENT -> {
                    val medicalConditions = (item.feedObject as TreatmentDTO).medicalConditionNames?.joinToString(", ") ?: ""
                    val dateStr = dateFormat.format((item.feedObject as TreatmentDTO).treatmentDate.toDate())

                    val stringBuilder = StringBuilder()
                    stringBuilder.append("$animalName was treated")
                    if (medicalConditions.isNotEmpty()) {
                        stringBuilder.append(" for $medicalConditions")
                    }
                    stringBuilder.append(" on $dateStr")
                    binding.titleTV.text = "Treated"
                    binding.descriptionTV.text = stringBuilder.toString()
                }
                FeedType.VACCINE -> {
                    val vaccines = (item.feedObject as VaccinationDTO).vaccineName
                    val dateStr = dateFormat.format((item.feedObject as VaccinationDTO).vaccinationDate.toDate())
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("$animalName was vaccinated")
                    if (!vaccines.isNullOrBlank()) {
                        stringBuilder.append(" with $vaccines vaccine")
                    }
                    stringBuilder.append(" on $dateStr")
                    binding.titleTV.text = "Vaccination"
                    binding.descriptionTV.text = stringBuilder.toString()
                }
                FeedType.DEWORMING -> {
                    val medicine = (item.feedObject as DewormingDTO).medicineName
                    val dateStr = dateFormat.format((item.feedObject as DewormingDTO).dewormingDate.toDate())
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("$animalName was dewormed")
                    if (!medicine.isNullOrBlank()) {
                        stringBuilder.append(" with $medicine")
                    }
                    stringBuilder.append(" on $dateStr")
                    binding.titleTV.text = "Deworming"
                    binding.descriptionTV.text = stringBuilder.toString()
                }
                FeedType.ADOPTED -> {
                    val dateStr = dateFormat.format((item.feedObject as ReleaseDTO).releaseDate.toDate())
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("$animalName was adopted")
                    if (dateStr.isNotEmpty()) {
                        stringBuilder.append(" on $dateStr")
                    }
                    binding.titleTV.text = "Status - Adopted"
                    binding.descriptionTV.text = stringBuilder.toString()
                }
                FeedType.RELEASE -> {
                    val dateStr = dateFormat.format((item.feedObject as ReleaseDTO).releaseDate.toDate())
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("$animalName was released")
                    if (dateStr.isNotEmpty()) {
                        stringBuilder.append(" on $dateStr")
                    }
                    binding.titleTV.text = "Status - Released"
                    binding.descriptionTV.text = stringBuilder.toString()
                }
                FeedType.DEATH -> {
                    val dateStr = dateFormat.format((item.feedObject as ReleaseDTO).releaseDate.toDate())
                    val stringBuilder = StringBuilder()
                    stringBuilder.append("$animalName died")

                    if (dateStr.isNotEmpty()) {
                        stringBuilder.append(" on $dateStr")
                    }
                    binding.titleTV.text = "Status - Death"
                    binding.descriptionTV.text = stringBuilder.toString()
                }

                else -> {}
            }
            if (absoluteAdapterPosition == dataSource.size-1) {
                binding.line.visibility = View.GONE
            } else {
                binding.line.visibility = View.VISIBLE
            }
        }
    }
}