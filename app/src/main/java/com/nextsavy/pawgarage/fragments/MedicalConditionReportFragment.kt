package com.nextsavy.pawgarage.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentMedicalConditionReportBinding
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAnimals
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MedicalConditionReportFragment : Fragment() {

    private lateinit var binding: FragmentMedicalConditionReportBinding
    private var selectedConditionsList: ArrayList<String> = arrayListOf()
    var text: StringBuilder? = null
    var fromDate: Date? = null
    var toDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMedicalConditionReportBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "Medical Condition Report"

        onClickListeners()

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        selectedConditionsList = AppDelegate.selectedList2
        // To set the values of arraylist in edittext.
        text = StringBuilder()

        for (i in selectedConditionsList) {
            if (i != selectedConditionsList.last()) {
                if (text != null) {
                    text!!.append(i).append(", ")
                }
            } else {
                if (text != null) {
                    text!!.append(i)
                }
            }
        }
        // setText() from another fragment will only work in onResume().
        Log.e("TEXT", text.toString())
        binding.conditionsET.setText(text)
    }

    private fun onClickListeners() {
        binding.fromDateET.setOnClickListener {

            val cal = Calendar.getInstance()

            val dateSetListener =
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, monthOfYear)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    val sdf = SimpleDateFormat("dd/MM/yy", Locale.US)
                    binding.fromDateET.setText(sdf.format(cal.time))
                    fromDate = cal.time
                }
            val dialog = DatePickerDialog(
                requireContext(),
                dateSetListener,
                // set DatePickerDialog to point to today's date when it loads up
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )

            val cal2 = Calendar.getInstance()
            cal2.set(2022, 3, 1)  // To start the date picker from 1.4.2022.
            dialog.datePicker.minDate = cal2.timeInMillis
            if (toDate != null) {
                dialog.datePicker.maxDate = toDate!!.time
            }
            //dialog.datePicker.maxDate = System.currentTimeMillis()  // To disable the days after current day.
            dialog.show()
        }
        binding.toDateET.setOnClickListener {

            val cal = Calendar.getInstance()

            val dateSetListener =
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, monthOfYear)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    val sdf = SimpleDateFormat("dd/MM/yy", Locale.US)
                    binding.toDateET.setText(sdf.format(cal.time))
                    toDate = cal.time
                }
            val dialog = DatePickerDialog(
                requireContext(),
                dateSetListener,
                // set DatePickerDialog to point to today's date when it loads up
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )

            val cal2 = Calendar.getInstance()
            cal2.set(2022, 3, 1)  // To start the date picker from 1.4.2022.
            if (fromDate != null) {
                dialog.datePicker.minDate = fromDate!!.time
            } else {
                dialog.datePicker.minDate = cal2.timeInMillis
            }
            //dialog.datePicker.maxDate = System.currentTimeMillis()  // To disable the days after current day.
            dialog.show()
        }

        binding.generateReportBTN.setOnClickListener {
            if (checkValidation()) {

                val dog = if (binding.dogCB.isChecked) CollectionAnimals.DOG else null
                val cat = if (binding.catCB.isChecked) CollectionAnimals.CAT else null
                val other = if (binding.otherCB.isChecked) CollectionAnimals.OTHER else null

                val ipd = if (binding.ipdCB.isChecked) CollectionAnimals.IPD else null
                val opd = if (binding.opdCB.isChecked) CollectionAnimals.OPD else null

                val male = if (binding.maleCB.isChecked) CollectionAnimals.MALE else null
                val female = if (binding.femaleCB.isChecked) CollectionAnimals.FEMALE else null

                val conditionsList = if (selectedConditionsList.isNotEmpty()) selectedConditionsList else null

                val fromDate = binding.fromDateET.text.toString()
                val toDate = binding.toDateET.text.toString()

                val bundle = Bundle()
                bundle.putString(CollectionAnimals.DOG, dog)
                bundle.putString(CollectionAnimals.CAT, cat)
                bundle.putString(CollectionAnimals.OTHER, other)
                bundle.putString(CollectionAnimals.IPD, ipd)
                bundle.putString(CollectionAnimals.OPD, opd)
                bundle.putString(CollectionAnimals.MALE, male)
                bundle.putString(CollectionAnimals.FEMALE, female)
                bundle.putStringArrayList("Conditions", conditionsList)
                bundle.putString("FromDate", fromDate)
                bundle.putString("ToDate", toDate)
                it.findNavController().navigate(R.id.generatedMedicalConditionReportFragment, bundle)
            }
        }
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.conditionsET.setOnClickListener {
            it.findNavController().navigate(R.id.medicalConditionsFragment)
        }
        binding.resetAllTV.setOnClickListener {
            binding.dogCB.isChecked = false
            binding.catCB.isChecked = false
            binding.otherCB.isChecked = false
            binding.ipdCB.isChecked = false
            binding.opdCB.isChecked = false
            binding.maleCB.isChecked = false
            binding.femaleCB.isChecked = false
            binding.fromDateET.setText("")
            binding.toDateET.setText("")
            binding.conditionsET.setText("")
            AppDelegate.selectedList2 = arrayListOf()
            text = null
        }
    }
    private fun checkValidation(): Boolean {
        if (!binding.dogCB.isChecked && !binding.catCB.isChecked && !binding.otherCB.isChecked) {
            Toast.makeText(requireContext(), "Species required.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!binding.ipdCB.isChecked && !binding.opdCB.isChecked) {
            Toast.makeText(requireContext(), "Type required.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!binding.maleCB.isChecked && !binding.femaleCB.isChecked) {
            Toast.makeText(requireContext(), "Gender required.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.fromDateET.text.toString() == "" || binding.toDateET.text.toString() == "") {
            Toast.makeText(requireContext(), "Date required.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}