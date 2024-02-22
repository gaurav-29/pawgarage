package com.nextsavy.pawgarage.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewMenuInterface
import com.nextsavy.pawgarage.databinding.ItemRemindersBinding
import com.nextsavy.pawgarage.models.ReminderDTO
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RemindersGeneralAdapter(private val remindersList: ArrayList<ReminderDTO>,
                              val userType: String,
                              val listener: RecyclerViewMenuInterface<ReminderDTO>?,
                              val reminderTypes: String = ""):
    RecyclerView.Adapter<RemindersGeneralAdapter.MyViewHolder>(){

    val todaysDateAtTime00: Date
        get() {
            // Get today's date with time 00:00:00
            val calendar = Calendar.getInstance()
            calendar.time = Date() // Set your date object here
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.time
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemRemindersBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return remindersList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (position == remindersList.size - 1) {
            listener?.didScrolledToEnd(position)
        }
        holder.bind(remindersList[position])
    }
    fun updateDataSource(newData: List<ReminderDTO>) {
        remindersList.clear()
        remindersList.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(remindersList.size)
    }
    fun injectNextBatch(newData: List<ReminderDTO>) {
        remindersList.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(remindersList.size)
    }

    fun removeReminder(data: ReminderDTO) {
        val index = remindersList.indexOfFirst { it.id == data.id }
        if (index > -1) {
            remindersList.removeAt(index)
            notifyItemRemoved(index)
        }
        listener?.dataSourceDidUpdate(remindersList.size)
    }

    inner class MyViewHolder(private val binding: ItemRemindersBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener?.didSelectItem(remindersList[absoluteAdapterPosition], absoluteAdapterPosition)
            }
            binding.nextIV.setOnClickListener { iv ->
                listener?.didSelectMenuItem(iv, remindersList[absoluteAdapterPosition], absoluteAdapterPosition)
            }
            if (userType == CollectionWhitelistedNumbers.ADMIN && reminderTypes != "TurnedOff") {
                binding.nextIV.visibility = View.VISIBLE
            } else {
                binding.nextIV.visibility = View.GONE
            }
        }
        fun bind(item: ReminderDTO) {
            val date: Date = item.reminderDate.toDate()
            val dateToShow = SimpleDateFormat("dd MMM, yyyy", Locale.US).format(date)
            binding.dateTV.text = dateToShow

            if (date.before(todaysDateAtTime00)) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.pink_reminder))
            } else {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent))
            }

            when (item.reminderType) {
                CollectionReminders.DEWORMING,
                CollectionReminders.VACCINATION -> {
                    binding.titleTV.text = item.animalDTO?.name + "'s " + item.reminderType.lowercase() + " due."
                    Glide.with(itemView.context).load(item.animalDTO?.downloadUrl).into(binding.animalIV)
                }
                CollectionReminders.COMPLETE_PROFILE -> {
                    binding.titleTV.text = "Please complete " + item.profileLeadDTO?.name + "'s profile."
                    Glide.with(itemView.context).load(item.profileLeadDTO?.downloadUrl).into(binding.animalIV)
                }
            }
        }
    }
}