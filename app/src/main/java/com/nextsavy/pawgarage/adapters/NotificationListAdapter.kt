package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemAllNotificationsBinding
import com.nextsavy.pawgarage.models.NotificationDTO
import com.nextsavy.pawgarage.utils.CollectionNotifications
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationListAdapter(
    val dataSource: ArrayList<NotificationDTO>,
    val delegate: RecyclerViewPagingInterface<NotificationDTO>?): RecyclerView.Adapter<NotificationListAdapter.ViewHolder>() {

    val dateFormat = SimpleDateFormat("hh:mm a ,  dd/MM/yy", Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemAllNotificationsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == dataSource.size - 1) {
            delegate?.didScrolledToEnd(position)
        }
        holder.bind(dataSource[position])
    }

    fun updateDataSource(newData: List<NotificationDTO>) {
        dataSource.clear()
        dataSource.addAll(newData)
        notifyDataSetChanged()
        delegate?.dataSourceDidUpdate(dataSource.size)
    }

    fun injectNextBatch(newData: List<NotificationDTO>) {
        dataSource.addAll(newData)
        notifyDataSetChanged()
        delegate?.dataSourceDidUpdate(dataSource.size)
    }

    inner class ViewHolder(private val binding: ItemAllNotificationsBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                delegate?.didSelectItem(dataSource[absoluteAdapterPosition], absoluteAdapterPosition)
            }
        }

        fun bind(item: NotificationDTO) {
            if (item.profileLeadDTO != null) {
                Glide.with(itemView.context).load(item.profileLeadDTO!!.downloadUrl).into(binding.animalIV)
            } else if (item.animalDTO != null) {
                Glide.with(itemView.context).load(item.animalDTO!!.downloadUrl).into(binding.animalIV)
            } else {
                Glide.with(itemView.context).load(R.drawable.paw_placeholder).into(binding.animalIV)
            }

            binding.typeTV.text = item.notificationType.uppercase()

            binding.dateTV.text = dateFormat.format(item.notificationDate.toDate())

            when (item.notificationType) {
                CollectionNotifications.NEW_PROFILE -> {
                    var titleText = ""
                    if (item.animalDTO != null) {
                        titleText = "New animal named " + item.animalDTO!!.name + " is registered."
                    } else {
                        titleText = "New animal is registered."
                    }
                    if (item.creator != null) {
                        titleText += "\n${item.creator!!.name}"
                    }
                    binding.titleTV.text = titleText
                }
                CollectionNotifications.VACCINATION -> {
                    if (item.animalDTO != null) {
                        binding.titleTV.text = item.animalDTO!!.name  + " has been vaccinated."
                    } else {
                        binding.titleTV.text = "Animal has been vaccinated."
                    }
                }
                CollectionNotifications.DEWORMING -> {
                    if (item.animalDTO != null) {
                        binding.titleTV.text = item.animalDTO!!.name  + " has been dewormed."
                    } else {
                        binding.titleTV.text = "Animal has been dewormed."
                    }
                }
                CollectionNotifications.RELEASED -> {
                    var titleText = ""
                    if (item.animalDTO != null) {
                        titleText = item.animalDTO!!.name  + " has been released."
                    } else {
                        titleText = "Animal has been released."
                    }
                    if (item.creator != null) {
                        titleText += "\n${item.creator!!.name}"
                    }
                    binding.titleTV.text = titleText
                }
                CollectionNotifications.ADOPTED -> {
                    var titleText = ""
                    if (item.animalDTO != null) {
                        titleText = item.animalDTO!!.name  + " has been adopted."
                    } else {
                        titleText = "Animal has been adopted."
                    }
                    if (item.creator != null) {
                        titleText += "\n${item.creator!!.name}"
                    }
                    binding.titleTV.text = titleText
                }
                CollectionNotifications.DEATH -> {
                    var titleText = ""
                    if (item.animalDTO != null) {
                        titleText = item.animalDTO!!.name  + " has died."
                    } else {
                        titleText = "Animal has died."
                    }
                    if (item.creator != null) {
                        titleText += "\n${item.creator!!.name}"
                    }
                    binding.titleTV.text = titleText
                }
                CollectionNotifications.PROFILE_LEADS -> {
                    if (item.profileLeadDTO != null) {
                        binding.titleTV.text = "Please complete " + item.profileLeadDTO!!.name + "'s profile."
                    } else {
                        binding.titleTV.text = "Please complete profile."
                    }
                }
                CollectionNotifications.ACTIVATED -> {
                    if (item.animalDTO != null) {
                        binding.titleTV.text = item.animalDTO!!.name  + " has been activated."
                    } else {
                        binding.titleTV.text = "Animal has been activated."
                    }
                }
                CollectionNotifications.TERMINATED -> {
                    if (item.animalDTO != null) {
                        binding.titleTV.text = item.animalDTO!!.name  + " has been terminated."
                    } else {
                        binding.titleTV.text = "Animal has been terminated."
                    }
                }
            }
        }
    }
}