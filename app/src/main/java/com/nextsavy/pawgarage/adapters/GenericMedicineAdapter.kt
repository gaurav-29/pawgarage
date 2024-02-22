package com.nextsavy.pawgarage.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.models.VaccineListDTO
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.MedicineType
import com.nextsavy.pawgarage.utils.MenuClickListener

class GenericMedicineAdapter(val medicineType: MedicineType,
                             var dataSource: ArrayList<VaccineListDTO>,
                             private var menuClickListener: MenuClickListener): RecyclerView.Adapter<GenericMedicineAdapter.ViewHolder>() {

    private val userType = Helper.sharedPreference?.getString("USER_TYPE", "")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_generic_medicine, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    fun replaceDataSource(newData: List<VaccineListDTO>) {
        dataSource.clear()
        dataSource.addAll(newData)
        notifyDataSetChanged()
    }
    fun filterDataList(filterList: ArrayList<VaccineListDTO>) {
        dataSource = filterList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            nameTV.text = dataSource[position].name
            val image = when (medicineType) {
                MedicineType.VACCINE -> { R.drawable.ic_vaccine }
                MedicineType.DEWORMING -> { R.drawable.ic_medicine }
                MedicineType.MEDICAL_CONDITION -> { R.drawable.ic_deworming }
            }
            leadingIV.setImageResource(image)
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val nameTV: TextView = itemView.findViewById(R.id.nameTV)
        val leadingIV: ImageView = itemView.findViewById(R.id.leaderIV)
        private val optionIV: ImageView = itemView.findViewById(R.id.optionIV)

        init {
            if (userType == CollectionWhitelistedNumbers.TEAM_LEADER) {
                optionIV.visibility = View.GONE
            }
            optionIV.setOnClickListener {
                menuClickListener.onMenuClick(it, dataSource[absoluteAdapterPosition].id)
            }
        }
    }
}