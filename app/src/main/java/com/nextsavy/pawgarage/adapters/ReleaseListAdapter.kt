package com.nextsavy.pawgarage.adapters

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.databinding.ItemAdmissionReleaseBinding
import com.nextsavy.pawgarage.models.ReleaseDTO
import com.nextsavy.pawgarage.utils.CollectionReleaseStatus
import java.text.SimpleDateFormat
import java.util.Locale

class ReleaseListAdapter(
    private val dataList: ArrayList<ReleaseDTO>,
    val listener: RecyclerViewPagingInterface<ReleaseDTO>?): RecyclerView.Adapter<ReleaseListAdapter.ViewHolder>() {

    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemAdmissionReleaseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position], (itemCount - position).toString())
    }

    fun updateDataSource(newData: List<ReleaseDTO>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
        listener?.dataSourceDidUpdate(dataList.size)
    }

    inner class ViewHolder(private val binding: ItemAdmissionReleaseBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener?.didSelectItem(dataList[absoluteAdapterPosition], absoluteAdapterPosition)
            }
        }

        fun bind(item: ReleaseDTO, number: String) {
            binding.titleTV.text = "Status Details $number"
            binding.dateTV.text = dateFormat.format(item.releaseDate.toDate())

            when (item.releaseStatus) {
                CollectionReleaseStatus.RELEASED -> {
                    binding.locationIV.visibility = View.VISIBLE
                    binding.addressTV.visibility = View.VISIBLE

                    binding.mobileTV.visibility = View.GONE
                    binding.mobileIV.visibility = View.GONE

                    binding.personIV.visibility = View.GONE
                    binding.personTV.visibility = View.GONE

                    val mSpannableString = SpannableString(item.address)
                    mSpannableString.setSpan(UnderlineSpan(), 0, mSpannableString.length, 0)
                    binding.addressTV.text = mSpannableString

                    binding.addressTV.setOnClickListener {
                        val uri = "https://www.google.com.tw/maps/place/" + item.latitude + "," + item.longitude
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        itemView.context.startActivity(intent)
                    }
                }
                CollectionReleaseStatus.ADOPTED -> {
                    binding.locationIV.visibility = View.GONE
                    binding.addressTV.visibility = View.GONE

                    binding.personIV.visibility = View.VISIBLE
                    binding.personTV.visibility = View.VISIBLE

                    binding.mobileIV.visibility = View.VISIBLE
                    binding.mobileTV.visibility = View.VISIBLE

                    binding.personTV.text = item.adopter?.name
                    binding.mobileTV.text = item.adopter?.phoneNumber
                }
                CollectionReleaseStatus.DEATH -> {
                    binding.locationIV.visibility = View.GONE
                    binding.addressTV.visibility = View.GONE

                    binding.mobileTV.visibility = View.GONE
                    binding.mobileIV.visibility = View.GONE

                    binding.personIV.visibility = View.GONE
                    binding.personTV.visibility = View.GONE
                }
            }

            binding.conditionTV.text = item.releaseStatus

            if (!item.comment.isNullOrBlank()) {
                binding.noteLL.visibility = View.VISIBLE
                binding.noteTV.text = item.comment
            } else {
                binding.noteLL.visibility = View.GONE
            }
        }
    }

}