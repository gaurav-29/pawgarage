package com.nextsavy.pawgarage.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentAddDewormingScheduleBinding
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionDeworming
import com.nextsavy.pawgarage.utils.CollectionMedicinesList
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionVaccination
import com.nextsavy.pawgarage.utils.CollectionVaccinesList
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddDewormingScheduleFragment : Fragment() {

    private lateinit var binding: FragmentAddDewormingScheduleBinding
    private var cal = Calendar.getInstance()
    val args: AddDewormingScheduleFragmentArgs by navArgs()
    private val db = Firebase.firestore
    var dateToDatabase: Date = Date()
    lateinit var previousDewormDate: Date
    val dateFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
    private var animalDocId: String? = null
    private var userIdList = arrayListOf<String>()
    private var personId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddDewormingScheduleBinding.inflate(inflater, container, false)

        animalDocId = args.animalDocID
        binding.toolbarOne.titleToolbarOne.text = args.animalName ?: ""
        binding.dewormingTV.text = "Deworming ${args.dewormNumber+1}"

        if (args.dewormNumber == 0) {
            binding.durationTV.visibility = View.GONE
            binding.durationRL.visibility = View.GONE
            binding.dateET.visibility = View.GONE
            binding.dateET2.visibility = View.VISIBLE
        } else {
            val duration = resources.getStringArray(R.array.dewormDuration)
            val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, duration)
            binding.spinnerDuration.adapter = adapter

            getPreviousDewormDate()
        }
        setUpSpinners()
        setUpUsernameSpinner()
        onClickListeners()
        return binding.root
    }
    private fun setUpUsernameSpinner() {
        if (Helper.isInternetAvailable(requireContext())) {
            val userList = arrayListOf<String>()
            binding.progressBar2.visibility = View.VISIBLE

            db.collection(CollectionWhitelistedNumbers.name)
                .whereEqualTo(CollectionWhitelistedNumbers.kIsArchive, false)
                .orderBy(CollectionWhitelistedNumbers.kUserName, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { result ->
                    binding.progressBar2.visibility = View.GONE
                    for (document in result) {
                        val userName = document.data[CollectionWhitelistedNumbers.kUserName] as String
                        userList.add(userName)
                        userIdList.add(document.id)
                    }
                    val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, userList)
                    binding.spinnerUser.adapter = adapter
                    binding.spinnerUser.setSelection(AppDelegate.dewormingDataModel.userPosition)
                    setUpPersonId()
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("ERROR", "Error getting documents: $exception")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpPersonId() {
        binding.spinnerUser.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                personId = userIdList[position]
                Log.e("PERSONID", personId.toString())
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                Log.e("NOTHING", "onNothingSelected called")
            }
        }
    }

    private fun getPreviousDewormDate() {
        if (Helper.isInternetAvailable(requireContext())) {

            binding.progressBar2.visibility = View.VISIBLE

            db.collection(CollectionDeworming.name)
                .whereEqualTo(CollectionDeworming.kIsArchive, false)
                .whereEqualTo(CollectionDeworming.kAnimalDocId, animalDocId)
                .orderBy(CollectionDeworming.kDewormingDate, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    binding.progressBar2.visibility = View.GONE
                    val prevDate = documents.documents.last().data?.get(CollectionDeworming.kDewormingDate) as Timestamp
                    previousDewormDate = prevDate.toDate()

                    dateToDatabase = Date(previousDewormDate.time + 604800000L + 604800000L + 604800000L + 604800000L + 172800000L)
                    val dateInString = dateFormat.format(dateToDatabase)
                    binding.dateET.setText(dateInString)

                    Log.e("PREV", previousDewormDate.toString())

                    setUpDurationSpinner()
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("DOC", "Error getting documents: ${exception.message}")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpDurationSpinner() {
        binding.spinnerDuration.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (binding.spinnerDuration.selectedItem.toString()) {
                    CollectionDeworming.DURATION_30 -> { // 7 days = 7*24*60*60*1000 = 604800000L miliseconds
                        dateToDatabase = Date(previousDewormDate.time + 604800000L + 604800000L + 604800000L + 604800000L + 172800000L)
                        val dateInString2 = dateFormat.format(dateToDatabase)
                        binding.dateET.setText(dateInString2)
                    }
                    CollectionDeworming.DURATION_90 -> {
                        val calender = Calendar.getInstance()
                        calender.time = previousDewormDate
                        calender.add(Calendar.DATE, 90)
                        dateToDatabase = calender.time
                        val dateInString2 = dateFormat.format(calender.time)
                        binding.dateET.setText(dateInString2)
                    }
                    CollectionDeworming.DURATION_YEAR -> {
                        val calender = Calendar.getInstance()
                        calender.time = previousDewormDate
                        calender.add(Calendar.YEAR, 1)
                        dateToDatabase = calender.time
                        val dateInString2 = dateFormat.format(calender.time)
                        binding.dateET.setText(dateInString2)
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }

    override fun onPause() {
        super.onPause()
        AppDelegate.dewormingDataModel.medicinePosition = binding.spinner.selectedItemPosition
        AppDelegate.dewormingDataModel.userPosition = binding.spinnerUser.selectedItemPosition
        AppDelegate.dewormingDataModel.weight = binding.weightET.text.trim().toString()
    }
    override fun onResume() {
        super.onResume()
        binding.spinner.setSelection(AppDelegate.dewormingDataModel.medicinePosition)
        binding.spinnerUser.setSelection(AppDelegate.dewormingDataModel.userPosition)
        binding.weightET.setText(AppDelegate.dewormingDataModel.weight)
    }
    private fun onClickListeners() {

        binding.saveBTN.setOnClickListener {
            if (Helper.isInternetAvailable(requireContext())) {
                if (checkValidation()) {
                    addDewormingScheduleToDatabase()
                }
            } else {
                Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
            }
        }
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.dateET2.setOnClickListener {
            val dateSetListener =
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, monthOfYear)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateInView(cal.time)
                }
            val dialog =  DatePickerDialog(requireContext(),
                dateSetListener,
                // set DatePickerDialog to point to today's date when it loads up
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH))

            val cal2 = Calendar.getInstance()
            cal2.set(2022, 3, 1)  // To start the date picker from 1.4.2022.
            dialog.datePicker.minDate = cal2.timeInMillis
            //dialog.datePicker.minDate = System.currentTimeMillis()  // To disable the days before today.
            dialog.show()
        }
    }

    private fun checkValidation(): Boolean {

        val date = binding.dateET2.text.toString()
        val weight = binding.weightET.text.trim().toString()

        if (args.dewormNumber == 0) {
            if (date.isEmpty() || date.isBlank()) {
                binding.dateET2.requestFocus()
                binding.dateET2.error = "Deworming date is required."
                Toast.makeText(requireContext(), "Date is required.", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        if (weight.isEmpty() || weight.isBlank()) {
            binding.weightET.requestFocus()
            binding.weightET.error = "Weight is required."
            return false
        }
        if (weight == "0" || weight == "00" || weight == "0.0" || weight == "0.00") {
            binding.weightET.requestFocus()
            binding.weightET.error = "Weight should not be 0."
            return false
        }
        return true
    }

    private fun addDewormingScheduleToDatabase() {
        binding.progressBar.root.visibility = View.VISIBLE

        var durationType = ""
        if (args.dewormNumber != 0) {
            durationType = binding.spinnerDuration.selectedItem.toString()
        }

        val dewormingDocRef = db.collection(CollectionDeworming.name).document()
        val dewormingData = hashMapOf(
            CollectionDeworming.kAnimalDocId to animalDocId,
            CollectionDeworming.kDewormingDate to dateToDatabase,
            CollectionDeworming.kMedicineType to binding.spinner.selectedItem.toString(),
            CollectionVaccination.kPersonAdministratedId to personId,
            CollectionDeworming.kDurationType to durationType,
            CollectionDeworming.kWeight to binding.weightET.text.trim().toString(),
            CollectionDeworming.kDewormingStatus to CollectionDeworming.PENDING,
            CollectionDeworming.kIsArchive to false,
            CollectionDeworming.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionDeworming.kCreatedBy to Firebase.auth.currentUser?.uid,
        )

        val remindersDocRef = db.collection(CollectionReminders.name).document()
        val remindersData = hashMapOf(
            CollectionReminders.kAnimalDocId to animalDocId,
            CollectionReminders.kReminderDate to dateToDatabase,
            CollectionReminders.kReminderType to CollectionReminders.DEWORMING,
            CollectionReminders.kReminderTypeObjectId to dewormingDocRef.id,
            CollectionReminders.kIsComplete to false,
            CollectionReminders.kIsArchive to false,
            CollectionReminders.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionReminders.kCreatedBy to Firebase.auth.currentUser?.uid,
        )

        val batch = Firebase.firestore.batch()
        batch.set(dewormingDocRef, dewormingData)
        batch.set(remindersDocRef, remindersData)

        batch.commit().addOnCompleteListener { batchTask ->
            if (batchTask.isSuccessful) {
                binding.progressBar.root.visibility = View.GONE
                Toast.makeText(requireContext(), "Deworming schedule added successfully.", Toast.LENGTH_SHORT).show()
                clearData()
                findNavController().popBackStack()
            } else {
                binding.progressBar.root.visibility = View.GONE
                Log.e("NST", "Exception: Deworming-Reminder addition BatchTask: ${batchTask.exception?.localizedMessage}")
                Toast.makeText(requireContext(), "${batchTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun clearData() {
        binding.dateET.setText("")
        binding.spinner.setSelection(0)
        binding.spinnerUser.setSelection(0)
        binding.weightET.setText("")
    }
    private fun setUpSpinners() {

        if (Helper.isInternetAvailable(requireContext())) {
            val medicineList = arrayListOf<String>()
            binding.progressBar2.visibility = View.VISIBLE

            db.collection(CollectionMedicinesList.name)
                .whereEqualTo(CollectionMedicinesList.kIsArchive, false)
                .orderBy(CollectionMedicinesList.kName, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { result ->
                    binding.progressBar2.visibility = View.GONE
                    // TODO : Pass vaccine id
                    for (document in result) {
                        medicineList.add(document.data[CollectionMedicinesList.kName] as String)
                    }
                    val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, medicineList)
                    binding.spinner.adapter = adapter
                    binding.spinner.setSelection(AppDelegate.dewormingDataModel.medicinePosition)
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("ERROR", "Error getting documents: $exception")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateDateInView(cal: Date) {
        dateToDatabase = cal
        binding.dateET2.setText(dateFormat.format(cal.time))
    }
}