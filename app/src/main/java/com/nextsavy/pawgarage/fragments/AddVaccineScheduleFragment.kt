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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentAddVaccinationScheduleBinding
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionVaccination
import com.nextsavy.pawgarage.utils.CollectionVaccinesList
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddVaccineScheduleFragment : Fragment() {

    private lateinit var binding: FragmentAddVaccinationScheduleBinding
    val args: AddVaccineScheduleFragmentArgs by navArgs()
    private var cal = Calendar.getInstance()
    private val db = Firebase.firestore
    var dateToDatabase: Date = Date()
    lateinit var previousVaccineDate: Date
    val dateFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
    private var userIdList = arrayListOf<String>()
    private var personId: String? = null
    private var animalDocId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddVaccinationScheduleBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = args.animalName ?: ""
        binding.vaccineTV.text = "Vaccine ${args.vaccineNumber+1}"
        animalDocId = args.animalDocID

        if (args.vaccineNumber == 0) {
            binding.durationTV.visibility = View.GONE
            binding.durationRL.visibility = View.GONE
            binding.dateET.visibility = View.GONE
            binding.dateET2.visibility = View.VISIBLE
        } else {
            val duration = resources.getStringArray(R.array.duration)
            val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, duration)
            binding.spinnerDuration.adapter = adapter

            getPreviousVaccineDate()
        }
        setUpUsernameSpinner()
        setUpVaccineTypeSpinner()
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
                        val userId = document.id
                        val userName = document.data[CollectionWhitelistedNumbers.kUserName] as String
                        userList.add(userName)
                        userIdList.add(userId)
                    }
                    val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, userList)
                    binding.spinnerUser.adapter = adapter
                    binding.spinnerUser.setSelection(AppDelegate.vaccinationDataModel.userPosition)
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

    private fun getPreviousVaccineDate() {
        if (Helper.isInternetAvailable(requireContext())) {

            binding.progressBar2.visibility = View.VISIBLE

            db.collection(CollectionVaccination.name)
                .whereEqualTo(CollectionVaccination.kIsArchive, false)
                .whereEqualTo(CollectionVaccination.kAnimalDocId, animalDocId)
                .orderBy(CollectionVaccination.kVaccinationDate, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    binding.progressBar2.visibility = View.GONE
                    val prevDate = documents.documents.last().data?.get(CollectionVaccination.kVaccinationDate) as Timestamp
                    previousVaccineDate = prevDate.toDate()

                    dateToDatabase = Date(previousVaccineDate.time + 604800000L + 604800000L + 604800000L)
                    val dateInString = dateFormat.format(dateToDatabase)
                    binding.dateET.setText(dateInString)

                    Log.e("PREV", previousVaccineDate.toString())

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

    override fun onPause() {
        super.onPause()
        AppDelegate.vaccinationDataModel.date = binding.dateET2.text.trim().toString()
        AppDelegate.vaccinationDataModel.vaccinePosition = binding.spinnerVaccine.selectedItemPosition
        AppDelegate.vaccinationDataModel.userPosition = binding.spinnerUser.selectedItemPosition
    }

    override fun onResume() {
        super.onResume()
        binding.dateET2.setText(AppDelegate.vaccinationDataModel.date)
        binding.spinnerVaccine.setSelection(AppDelegate.vaccinationDataModel.vaccinePosition)
        binding.spinnerUser.setSelection(AppDelegate.vaccinationDataModel.userPosition)
    }

    private fun onClickListeners() {

        binding.saveBTN.setOnClickListener {
            if (Helper.isInternetAvailable(requireContext())) {
                if (checkValidation()) {
                    if (personId != null) {
                        addVaccinationScheduleToDatabase()
                    } else {
                        Toast.makeText(requireContext(), "Person id not fetched successfully.", Toast.LENGTH_SHORT).show()
                    }
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

        if (args.vaccineNumber == 0) {
            if (date.isEmpty() || date.isBlank()) {
                binding.dateET2.requestFocus()
                binding.dateET2.error = "Vaccination date is required."
                Toast.makeText(requireContext(), "Date is required.", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun addVaccinationScheduleToDatabase() {

        binding.progressBar.root.visibility = View.VISIBLE

        var durationType = ""
        if (args.vaccineNumber != 0) {
            durationType = binding.spinnerDuration.selectedItem.toString()
        }
        val vaccinationData = hashMapOf(
            CollectionVaccination.kAnimalDocId to animalDocId,
            CollectionVaccination.kVaccinationDate to dateToDatabase,
            CollectionVaccination.kVaccineType to binding.spinnerVaccine.selectedItem.toString(),
            CollectionVaccination.kPersonAdministratedId to personId,
            CollectionVaccination.kDurationType to durationType,
            CollectionVaccination.kVaccinationStatus to CollectionVaccination.PENDING,
            CollectionVaccination.kIsArchive to false,
            CollectionVaccination.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionVaccination.kCreatedBy to Firebase.auth.currentUser?.uid,
        )
        val vaccinationDocRef = db.collection(CollectionVaccination.name).document()

        val remindersDocRef = db.collection(CollectionReminders.name).document()
        val remindersData = hashMapOf(
            CollectionReminders.kAnimalDocId to animalDocId,
            CollectionReminders.kReminderDate to dateToDatabase,
            CollectionReminders.kReminderType to CollectionReminders.VACCINATION,
            CollectionReminders.kReminderTypeObjectId to vaccinationDocRef.id,
            CollectionReminders.kIsComplete to false,
            CollectionReminders.kIsArchive to false,
            CollectionReminders.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionReminders.kCreatedBy to Firebase.auth.currentUser?.uid,
        )

        val batch = Firebase.firestore.batch()
        batch.set(vaccinationDocRef, vaccinationData)
        batch.set(remindersDocRef, remindersData)

        batch.commit().addOnCompleteListener { batchTask ->
            if (batchTask.isSuccessful) {
                binding.progressBar.root.visibility = View.GONE
                Toast.makeText(requireContext(), "Vaccination schedule added successfully.", Toast.LENGTH_SHORT).show()
                clearData()
                findNavController().popBackStack()
            } else {
                binding.progressBar.root.visibility = View.GONE
                Log.e("NST", "Exception: Vaccination-Reminder addition BatchTask: ${batchTask.exception?.localizedMessage}")
                Toast.makeText(requireContext(), "${batchTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearData() {
        binding.dateET.setText("")
        binding.spinnerVaccine.setSelection(0)
        binding.spinnerUser.setSelection(0)
    }

    private fun updateDateInView(cal: Date) {
        dateToDatabase = cal
        binding.dateET2.setText(dateFormat.format(cal.time))
    }
    private fun setUpDurationSpinner() {

        binding.spinnerDuration.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (binding.spinnerDuration.selectedItem.toString() == CollectionVaccination.DURATION_DAYS) {
                    dateToDatabase = Date(previousVaccineDate.time + 604800000L + 604800000L + 604800000L)
                    val dateInString2 = dateFormat.format(dateToDatabase)
                    binding.dateET.setText(dateInString2)
                } else {
                    val calender = Calendar.getInstance()
                    calender.time = previousVaccineDate
                    calender.add(Calendar.YEAR, 1)
                    val nYear = calender.time
                    dateToDatabase = nYear
                    val dateInString2 = dateFormat.format(nYear)
                    binding.dateET.setText(dateInString2)
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }
    private fun setUpVaccineTypeSpinner() {
        if (Helper.isInternetAvailable(requireContext())) {
            val vaccineList = arrayListOf<String>()
            binding.progressBar2.visibility = View.VISIBLE

            db.collection(CollectionVaccinesList.name)
                .whereEqualTo(CollectionVaccinesList.kIsArchive, false)
                .orderBy(CollectionVaccinesList.kName, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { result ->
                    binding.progressBar2.visibility = View.GONE
                    for (document in result) {
                        vaccineList.add(document[CollectionVaccinesList.kName] as String)
                    }
                    val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, vaccineList)
                    binding.spinnerVaccine.adapter = adapter
                    binding.spinnerVaccine.setSelection(AppDelegate.vaccinationDataModel.vaccinePosition)
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
}