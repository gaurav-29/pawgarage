package com.nextsavy.pawgarage.fragments

import android.app.AlertDialog
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentDewormingEditBinding
import com.nextsavy.pawgarage.models.DewormingDTO
import com.nextsavy.pawgarage.models.FeedType
import com.nextsavy.pawgarage.models.GenericUserDTO
import com.nextsavy.pawgarage.retrofit.RetrofitClient
import com.nextsavy.pawgarage.retrofit.RetrofitInterface
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionDeworming
import com.nextsavy.pawgarage.utils.CollectionFeedEvents
import com.nextsavy.pawgarage.utils.CollectionMedicinesList
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionScheduleStatus
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Constants
import com.nextsavy.pawgarage.utils.Helper
import retrofit2.Call
import retrofit2.Callback
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class DewormingEditFragment : Fragment() {

    private lateinit var binding: FragmentDewormingEditBinding
    private val args: DewormingEditFragmentArgs by navArgs()
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)

    private var dewormingDTO: DewormingDTO? = null
    private var reminderDocId: String? = null
    private var nextDewormingDoc: DewormingDTO? = null
    private var nextDewormingReminderDocId: String? = null
    private var feedEventDocId: String? = null

    private var dewormingDate: Date? = null
    private var completionDate: Date? = null
    private var nextDewormingDate: Date? = null

    lateinit var retrofitInterface: RetrofitInterface
    private var animalName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDewormingEditBinding.inflate(inflater, container, false)
        animalName = args.animalName ?: ""
        binding.toolbarOne.titleToolbarOne.text = animalName
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().popBackStack()
        }
        getDewormingDetails()
        return binding.root
    }
    // Gaurav
    private fun sendPushNotification(animalDocIdForDeepLink: String) {

        val baseUrl = "https://fcm.googleapis.com/"
        retrofitInterface = RetrofitClient.getRetrofitInstance(baseUrl).create(RetrofitInterface::class.java)

        if (Helper.isInternetAvailable(requireContext())) {

            val payLoad = JsonObject()
            val notification = JsonObject()
            val data = JsonObject()

            try {
                payLoad.addProperty("to", "/topics/" + CollectionNotifications.TOPIC)
                notification.addProperty("title", CollectionDeworming.name)
                notification.addProperty("body", "$animalName has been dewormed.")
                payLoad.add("notification", notification)
                data.addProperty("animal_doc_id", animalDocIdForDeepLink)
                payLoad.add("data", data)

                Log.e("TAG", "try")
            } catch (e: JsonIOException) {
                Log.e("TAG", "sendPushNotification(): " + e.message)
            }

            retrofitInterface.sendNotificationThroughAPI(payLoad).enqueue(object :
                Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: retrofit2.Response<JsonObject>) {
                    Log.e("RES", response.toString())
                    Log.e("RES_BODY", response.body().toString())
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Log.e("ERROR", t.message.toString())
                }
            })
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_LONG).show()
        }
    }
    private fun getAnimalName() {
        if (Helper.isInternetAvailable(requireContext())) {
            Firebase.firestore.collection(CollectionAnimals.name).document(dewormingDTO!!.animalDocId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        animalName = document.get(CollectionAnimals.kName) as String
                    } else {
                        Log.e("NAME", "No such document for Animal Name.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("NAME", "Animal name failure: ", exception)
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDewormingDetails() {
        if (args.dewormingId != null) {
            binding.progressBar2.visibility = View.VISIBLE
            Firebase.firestore
                .collection(CollectionDeworming.name)
                .document(args.dewormingId!!)
                .get()
                .addOnSuccessListener { docSnapshot ->
                    binding.progressBar2.visibility = View.GONE
                    dewormingDTO = DewormingDTO.create(docSnapshot.id, docSnapshot.data)
                    if (dewormingDTO != null) {
                        setupUI()
                        getReminderDocId(dewormingDTO!!.id, true)
                    } else {
                        Toast.makeText(requireContext(), "Deworming data is not correct!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("NST-M", "Exception: getDewormingDetails > $exception")
                    Toast.makeText(requireContext(), exception.message, Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Deworming Id not found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        // Top level checks: Dead, Terminated etc...
        binding.dewormingIndexTV.text = "Deworming ${args.totalDewormingCount - args.currentDewormingIndex}"
        binding.weightET.setText(dewormingDTO!!.weight)
        binding.note2ET.setText(dewormingDTO!!.adminNotes)
        setupAndConfigureCreatedByUI()
        setupAndConfigureUpdatedByUI()
        binding.saveBTN.setOnClickListener(saveButtonTapped)

        if (dewormingDTO!!.dewormingStatus == CollectionDeworming.PENDING &&
            args.currentDewormingIndex == 0 &&
            args.totalDewormingCount == 1) {
            // First pending deworming
            binding.weightET.isEnabled = true
            binding.note2ET.isEnabled = true
            binding.saveBTN.visibility = View.VISIBLE
            configurePreviousDewormingDurationSpinner(false)
            setupDewormingDatePicker(true)
            configureDewormingTypeSpinner(true)
            configurePersonAdministratedSpinner(true)
            setupDiffCompletionCheckbox(false)
            configureDewormingStatusSpinner(true)
            setupNextDewormingDurationSpinner(true)
        } else if (dewormingDTO!!.dewormingStatus == CollectionDeworming.COMPLETED &&
            args.totalDewormingCount - args.currentDewormingIndex == args.totalDewormingCount - 1) {
            // last complete deworming
            getFeedEventDocId()
            getNextDewormingDetails()
            binding.weightET.isEnabled = true
            binding.note2ET.isEnabled = true
            binding.saveBTN.visibility = View.VISIBLE
            configurePreviousDewormingDurationSpinner(false)
            setupDewormingDatePicker(true)
            configureDewormingTypeSpinner(true)
            configurePersonAdministratedSpinner(true)
            setupDiffCompletionCheckbox(false)
            configureDewormingStatusSpinner(false)
            setupNextDewormingDurationSpinner(false)
        } else if (dewormingDTO!!.dewormingStatus == CollectionDeworming.PENDING &&
            args.currentDewormingIndex == 0 &&
            args.totalDewormingCount > 1) {
            // Last pending deworming
            binding.weightET.isEnabled = true
            binding.note2ET.isEnabled = true
            binding.saveBTN.visibility = View.VISIBLE
            configurePreviousDewormingDurationSpinner(true)
            setupDewormingDatePicker(false)
            configureDewormingTypeSpinner(true)
            configurePersonAdministratedSpinner(true)
            setupDiffCompletionCheckbox(true)
            configureDewormingStatusSpinner(true)
            setupNextDewormingDurationSpinner(true)
        } else {
            // Every other case
            binding.weightET.isEnabled = false
            binding.note2ET.isEnabled = false
            binding.saveBTN.visibility = View.GONE
            configurePreviousDewormingDurationSpinner(false)
            setupDewormingDatePicker(false)
            configureDewormingTypeSpinner(false)
            configurePersonAdministratedSpinner(false)
            setupDiffCompletionCheckbox(false)
            configureDewormingStatusSpinner(false)
            setupNextDewormingDurationSpinner(false)
        }
        if (AppDelegate.isDead || AppDelegate.state == CollectionAnimals.TERMINATED) {
            binding.weightET.isEnabled = false
            binding.note2ET.isEnabled = false
            binding.saveBTN.visibility = View.GONE
            configurePreviousDewormingDurationSpinner(false)
            setupDewormingDatePicker(false)
            configureDewormingTypeSpinner(false)
            configurePersonAdministratedSpinner(false)
            setupDiffCompletionCheckbox(false)
            configureDewormingStatusSpinner(false)
            setupNextDewormingDurationSpinner(false)
        }
    }

    private fun configurePreviousDewormingDurationSpinner(isEnable: Boolean) {
        if (isEnable) {
            val duration = resources.getStringArray(R.array.dewormDuration)
            val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, duration)
            binding.previousDurationSpinner.adapter = adapter
            if (dewormingDTO!!.durationType != null) {
                val index = adapter.getPosition(dewormingDTO!!.durationType!!)
                binding.previousDurationSpinner.setSelection(index)
            }
            binding.previousDurationSpinner.onItemSelectedListener = previousDewormingDurationChangeListener
            binding.kPreDurationTV.visibility = View.VISIBLE
            binding.previousDurationRL.visibility = View.VISIBLE
        } else {
            binding.kPreDurationTV.visibility = View.GONE
            binding.previousDurationRL.visibility = View.GONE
        }

    }

    private val previousDewormingDurationChangeListener = object: AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            // Old deworming duration, as per database
            val dbDuration = dewormingDTO!!.durationType
            // Old deworming date, as per database
            val dbDewormingDate = dewormingDTO!!.dewormingDate.toDate()

            if (dbDuration != null) {
                if (dbDuration == binding.previousDurationSpinner.selectedItem.toString()) {
                    dewormingDate = dbDewormingDate
                    handleDewormingDateChange()
                    binding.dewormingDateET.setText(dateFormat.format(dbDewormingDate))
                } else {
                    // change
                    val dewormingCalendar = Calendar.getInstance()
                    dewormingCalendar.time = dbDewormingDate

                    if (dbDuration == CollectionDeworming.DURATION_30) {
                        dewormingCalendar.add(Calendar.DAY_OF_YEAR, -30)
                        if (binding.previousDurationSpinner.selectedItem.toString() == CollectionDeworming.DURATION_90) {
                            // old value = 30 => changed to 90
                            dewormingCalendar.add(Calendar.DAY_OF_YEAR, 90)
                        } else {
                            // old value = 30 => changed to 365
                            dewormingCalendar.add(Calendar.YEAR, 1)
                        }
                    } else if (dbDuration == CollectionDeworming.DURATION_90) {
                        dewormingCalendar.add(Calendar.DAY_OF_YEAR, -90)
                        if (binding.previousDurationSpinner.selectedItem.toString() == CollectionDeworming.DURATION_30) {
                            // old value = 90 => changed to 30
                            dewormingCalendar.add(Calendar.DAY_OF_YEAR, 30)
                        } else {
                            // old value = 90 => changed to 365
                            dewormingCalendar.add(Calendar.YEAR, 1)
                        }
                    } else {
                        dewormingCalendar.add(Calendar.YEAR, -1)
                        if (binding.previousDurationSpinner.selectedItem.toString() == CollectionDeworming.DURATION_30) {
                            // old value = 365 => changed to 30
                            dewormingCalendar.add(Calendar.DAY_OF_YEAR, 30)
                        } else {
                            // old value = 365 => changed to 90
                            dewormingCalendar.add(Calendar.DAY_OF_YEAR, 90)
                        }
                    }
                    dewormingDate = dewormingCalendar.time
                    binding.dewormingDateET.setText(dateFormat.format(dewormingCalendar.time))
                    handleDewormingDateChange()
                }
            } else {
                Log.e("NST-M", "PRE Spinner value changed and DB duration is null :(")
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
    }

    private fun setupDewormingDatePicker(isEnable: Boolean) {
        binding.dewormingDateET.setText(dateFormat.format(dewormingDTO!!.dewormingDate.toDate()))
        dewormingDate = dewormingDTO!!.dewormingDate.toDate()

        if (isEnable) {
            binding.dewormingDateET.setOnClickListener {
                val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, monthOfYear)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    // Send date to anther function
                    dewormingDate = calendar.time
                    handleDewormingDateChange()
                    binding.dewormingDateET.setText(dateFormat.format(calendar.time))
                }

                val calendarForDateToSet = Calendar.getInstance()
                if (dewormingDate != null) {
                    calendarForDateToSet.time = dewormingDate!!
                }
                val dialog =  DatePickerDialog(
                    requireContext(),
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    calendarForDateToSet.get(Calendar.YEAR),
                    calendarForDateToSet.get(Calendar.MONTH),
                    calendarForDateToSet.get(Calendar.DAY_OF_MONTH))

                val cal2 = Calendar.getInstance()
                cal2.set(2022, 3, 1)  // To start the date picker from 1.4.2022.
                dialog.datePicker.minDate = cal2.timeInMillis
                dialog.datePicker.maxDate = System.currentTimeMillis()  // To disable the days after today.
                dialog.show()
            }
        } else {
            binding.dewormingDateET.setOnClickListener(null)
        }
    }

    private fun setupAndConfigureCreatedByUI() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            Firebase.firestore.collection(CollectionUser.name)
                .document(dewormingDTO!!.createdBy)
                .get()
                .addOnSuccessListener { document ->
                    binding.progressBar2.visibility = View.GONE
                    val genericUserDTO = GenericUserDTO.create(document.id, document.data)
                    if (genericUserDTO != null) {
                        binding.createdByET.setText(genericUserDTO.name)
                        binding.kCreatedByTV.visibility = View.VISIBLE
                        binding.createdByET.visibility = View.VISIBLE
                    } else {
                        binding.kCreatedByTV.visibility = View.GONE
                        binding.createdByET.visibility = View.GONE
                    }
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    binding.kCreatedByTV.visibility = View.GONE
                    binding.createdByET.visibility = View.GONE
                    Log.e("NAME", "Error getting documents: ${exception.message}")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
            binding.kCreatedByTV.visibility = View.GONE
            binding.createdByET.visibility = View.GONE
        }
    }

    private fun setupAndConfigureUpdatedByUI() {
        if (dewormingDTO!!.updatedBy != null) {
            if (Helper.isInternetAvailable(requireContext())) {
                binding.progressBar2.visibility = View.VISIBLE
                Firebase.firestore.collection(CollectionUser.name)
                    .document(dewormingDTO!!.updatedBy!!)
                    .get()
                    .addOnSuccessListener { document ->
                        binding.progressBar2.visibility = View.GONE
                        val genericUserDTO = GenericUserDTO.create(document.id, document.data)
                        if (genericUserDTO != null) {
                            binding.updatedByET.setText(genericUserDTO.name)
                            binding.kUpdatedByTV.visibility = View.VISIBLE
                            binding.updatedByET.visibility = View.VISIBLE
                        } else {
                            binding.kUpdatedByTV.visibility = View.GONE
                            binding.updatedByET.visibility = View.GONE
                        }
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar2.visibility = View.GONE
                        binding.kUpdatedByTV.visibility = View.GONE
                        binding.updatedByET.visibility = View.GONE
                        Log.e("NAME", "Error getting documents: ${exception.message}")
                        Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
                binding.kUpdatedByTV.visibility = View.GONE
                binding.updatedByET.visibility = View.GONE
            }
        } else {
            binding.kUpdatedByTV.visibility = View.GONE
            binding.updatedByET.visibility = View.GONE
        }

    }

    private fun configureDewormingTypeSpinner(isEnable: Boolean) {
        val medicineList = arrayListOf<String>()
        binding.progressBar2.visibility = View.VISIBLE
        
        Firebase.firestore.collection(CollectionMedicinesList.name)
            .whereEqualTo(CollectionMedicinesList.kIsArchive, false)
            .orderBy(CollectionMedicinesList.kName, Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                binding.progressBar2.visibility = View.GONE
                // TODO : Pass vaccine id
                for (document in result) {
                    medicineList.add(document[CollectionMedicinesList.kName] as String)
                }
                val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, medicineList)
                binding.spinnerMedicine.adapter = adapter

                val spinnerPosition = adapter.getPosition(dewormingDTO!!.medicineName)
                binding.spinnerMedicine.setSelection(spinnerPosition)
                binding.spinnerMedicine.isEnabled = isEnable
            }
            .addOnFailureListener { exception ->
                binding.progressBar2.visibility = View.GONE
                Log.e("ERROR", "Error getting documents: $exception")
                Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurePersonAdministratedSpinner(isEnable: Boolean) {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            Firebase.firestore.collection(CollectionWhitelistedNumbers.name)
                .whereEqualTo(CollectionWhitelistedNumbers.kIsArchive, false)
                .orderBy(CollectionWhitelistedNumbers.kUserName, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    binding.progressBar2.visibility = View.GONE
                    val userList = querySnapshot.documents.mapNotNull { documentSnapshot -> GenericUserDTO.create(documentSnapshot.id, documentSnapshot.data) }
                    val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, userList)
                    val index = userList.indexOfFirst { it.id == dewormingDTO!!.administratorId }
                    binding.spinnerUser.adapter = adapter
                    binding.spinnerUser.setSelection(index)
                    binding.spinnerUser.isEnabled = isEnable
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

    private fun setupDiffCompletionCheckbox(isEnable: Boolean) {
        if (isEnable) {
            binding.differentDateCB.visibility = View.VISIBLE
            binding.differentDateCB.setOnCheckedChangeListener { _, p1 ->
                if (p1) {
                    binding.kCompletionDateTV.visibility = View.VISIBLE
                    binding.completionDateET.visibility = View.VISIBLE
                } else {
                    binding.kCompletionDateTV.visibility = View.GONE
                    binding.completionDateET.visibility = View.GONE
                }
                completionDate = null
                binding.completionDateET.text = null
                handleDewormingDateChange()
            }
        } else {
            binding.differentDateCB.visibility = View.GONE
            binding.kCompletionDateTV.visibility = View.GONE
            binding.completionDateET.visibility = View.GONE
        }
    }

    private fun configureDewormingStatusSpinner(isEnable: Boolean) {
        if (isEnable) {
            Firebase.firestore.collection(CollectionScheduleStatus.name)
                .get()
                .addOnSuccessListener { result ->
                    val statusList = result.documents.map { it.id }
                    val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, statusList)
                    binding.spinnerStatus.adapter = adapter
                    val spinnerPosition = adapter.getPosition(dewormingDTO!!.dewormingStatus)
                    binding.spinnerStatus.setSelection(spinnerPosition)
                    binding.spinnerStatus.onItemSelectedListener = vaccineStatusChangeListener
                    binding.spinnerStatus.isEnabled = true
                }
                .addOnFailureListener { exception ->
                    Log.e("ERROR", "Error getting CollectionScheduleStatus documents: $exception")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        } else {
            val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, listOf<String>(dewormingDTO!!.dewormingStatus))
            binding.spinnerStatus.adapter = adapter
            binding.spinnerStatus.setSelection(0)
            binding.spinnerStatus.onItemSelectedListener = null
            binding.spinnerStatus.isEnabled = false
            binding.warningTV.visibility = View.GONE
        }
    }

    private val vaccineStatusChangeListener = object: AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            if (binding.spinnerStatus.selectedItem.toString() == CollectionDeworming.COMPLETED &&
                args.currentDewormingIndex == 0 &&
                args.totalDewormingCount == 1) {
                // First vaccine toggle to Complete
                binding.differentDateCB.visibility = View.GONE
                binding.differentDateCB.isChecked = false
                binding.kCompletionDateTV.visibility = View.GONE
                binding.completionDateET.visibility = View.GONE
                setupCompletionDatePicker(false)
                binding.kNextDewormingTV.visibility = View.VISIBLE
                binding.kNextDurationTV.visibility = View.VISIBLE
                binding.nextDurationRL.visibility = View.VISIBLE
                binding.kNextDewormingDateTV.visibility = View.VISIBLE
                binding.nextDewormingDateET.visibility = View.VISIBLE
                binding.warningTV.visibility = View.GONE
            } else if (binding.spinnerStatus.selectedItem.toString() == CollectionDeworming.COMPLETED &&
                args.currentDewormingIndex == 0 &&
                args.totalDewormingCount > 1) {
                // Last vaccine toggle to Complete
                binding.differentDateCB.visibility = View.VISIBLE
                binding.differentDateCB.isChecked = false
                binding.kCompletionDateTV.visibility = View.GONE
                binding.completionDateET.visibility = View.GONE
                setupCompletionDatePicker(true)
                binding.kNextDewormingTV.visibility = View.VISIBLE
                binding.kNextDurationTV.visibility = View.VISIBLE
                binding.nextDurationRL.visibility = View.VISIBLE
                binding.kNextDewormingDateTV.visibility = View.VISIBLE
                binding.nextDewormingDateET.visibility = View.VISIBLE
                binding.warningTV.visibility = View.GONE
            } else {
                binding.differentDateCB.visibility = View.GONE
                binding.differentDateCB.isChecked = false
                binding.kCompletionDateTV.visibility = View.GONE
                binding.completionDateET.visibility = View.GONE
                setupCompletionDatePicker(false)
                binding.kNextDewormingTV.visibility = View.GONE
                binding.kNextDurationTV.visibility = View.GONE
                binding.nextDurationRL.visibility = View.GONE
                binding.kNextDewormingDateTV.visibility = View.GONE
                binding.nextDewormingDateET.visibility = View.GONE
                binding.warningTV.visibility = View.VISIBLE
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
    }

    private fun setupCompletionDatePicker(isEnable: Boolean) {
        if (isEnable) {
            binding.completionDateET.setOnClickListener {
                val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, monthOfYear)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    // Send date to anther function
                    completionDate = calendar.time
                    handleDewormingDateChange()
                    binding.completionDateET.setText(dateFormat.format(calendar.time))
                }

                val calendarForDateToSet = Calendar.getInstance()
                if (completionDate != null) {
                    calendarForDateToSet.time = completionDate!!
                }
                val dialog =  DatePickerDialog(
                    requireContext(),
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    calendarForDateToSet.get(Calendar.YEAR),
                    calendarForDateToSet.get(Calendar.MONTH),
                    calendarForDateToSet.get(Calendar.DAY_OF_MONTH))

                val cal2 = Calendar.getInstance()
                cal2.set(2022, 3, 1)  // To start the date picker from 1.4.2022.
                dialog.datePicker.minDate = cal2.timeInMillis
                //dialog.datePicker.minDate = System.currentTimeMillis()  // To disable the days before today.
                dialog.show()
            }
        } else {
            binding.completionDateET.setOnClickListener(null)
        }
    }

    private fun setupNextDewormingDurationSpinner(isEnable: Boolean) {
        if (isEnable) {
            val duration = resources.getStringArray(R.array.dewormDuration)
            val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, duration)
            binding.nextDurationSpinner.adapter = adapter
            binding.nextDurationSpinner.onItemSelectedListener = nextDewormingDurationChangeListener
        } else {
            binding.nextDurationSpinner.onItemSelectedListener = null
        }
        binding.kNextDewormingTV.visibility = View.GONE
        binding.kNextDurationTV.visibility = View.GONE
        binding.nextDurationRL.visibility = View.GONE
        binding.kNextDewormingDateTV.visibility = View.GONE
        binding.nextDewormingDateET.visibility = View.GONE
    }

    private val nextDewormingDurationChangeListener = object: AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            handleDewormingDateChange()
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
    }

    private fun handleDewormingDateChange() {
        // args.currentDewormingIndex >= 2 && args.totalDewormingCount > 2
        if (dewormingDTO!!.dewormingStatus == CollectionDeworming.COMPLETED &&
            args.totalDewormingCount - args.currentDewormingIndex == args.totalDewormingCount - 1) {
            // TODO: Handle this case!!!
        } else {
            val completionCalendar = Calendar.getInstance()
            completionCalendar.time = completionDate ?: dewormingDate!!
            if (binding.nextDurationSpinner.selectedItem.toString() == CollectionDeworming.DURATION_30) {
                completionCalendar.add(Calendar.DAY_OF_YEAR, 30)
            } else if (binding.nextDurationSpinner.selectedItem.toString() == CollectionDeworming.DURATION_90) {
                completionCalendar.add(Calendar.DAY_OF_YEAR, 90)
            } else {
                completionCalendar.add(Calendar.YEAR, 1)
            }
            nextDewormingDate = completionCalendar.time
            binding.nextDewormingDateET.setText(dateFormat.format(completionCalendar.time))
        }
    }

    private fun checkValidation(): Boolean {
        val weightStr = binding.weightET.text.trim().toString()
        val weight: Double? = weightStr.toDoubleOrNull()
        if (weightStr.isBlank()) {
            binding.weightET.requestFocus()
            binding.weightET.error = "Weight is required."
            return false
        } else if (weight == null) {
            binding.weightET.requestFocus()
            binding.weightET.error = "Please enter valid weight"
            return false
        }
        return true
    }

    private fun getFeedEventDocId() {
        binding.progressBar2.visibility = View.VISIBLE
        Firebase.firestore
            .collection(CollectionFeedEvents.name)
            .whereEqualTo(CollectionFeedEvents.kFeedObjectId, dewormingDTO!!.id)
            .get()
            .addOnSuccessListener { querySnapshot ->
                feedEventDocId = querySnapshot.documents.lastOrNull()?.id
                if (feedEventDocId != null) {
                    Log.e("NST-M", "FeedEvent Doc Id in DewormingEditFrag : $feedEventDocId")
                } else {
                    Log.e("NST-M", "FeedEvent Doc is null in DewormingEditFrag")
                }
                binding.progressBar2.visibility = View.GONE
            }
            .addOnFailureListener {
                Log.e("NST-M", "Exception: DewormingEdit > getFeedEventDocId: ${it.localizedMessage}")
                binding.progressBar2.visibility = View.GONE
            }
    }

    private fun getReminderDocId(dewormingId: String, isCurrent: Boolean) {
        binding.progressBar2.visibility = View.VISIBLE
        Firebase.firestore
            .collection(CollectionReminders.name)
            .whereEqualTo(CollectionReminders.kReminderTypeObjectId, dewormingId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (isCurrent) {
                    reminderDocId = querySnapshot.documents.lastOrNull()?.id
                    Log.e("NST-M", "Reminder Doc Id in DewormingEditFrag : $reminderDocId")
                } else {
                    nextDewormingReminderDocId = querySnapshot.documents.lastOrNull()?.id
                    Log.e("NST-M", "Next Reminder Doc Id in DewormingEditFrag : $nextDewormingReminderDocId")
                }
                binding.progressBar2.visibility = View.GONE
            }
            .addOnFailureListener {
                Log.e("NST-M", "Exception: DewormingEdit > getReminderDocId > Deworming id$dewormingId isCurrent:$isCurrent\t: ${it.localizedMessage}")
                binding.progressBar2.visibility = View.GONE
            }
    }

    // Get the only Pending Deworming
    private fun getNextDewormingDetails() {
        binding.progressBar2.visibility = View.VISIBLE
        
        Firebase.firestore
            .collection(CollectionDeworming.name)
            .whereEqualTo(CollectionDeworming.kIsArchive, false)
            .whereEqualTo(CollectionDeworming.kAnimalDocId, dewormingDTO!!.animalDocId)
            .whereEqualTo(CollectionDeworming.kDewormingStatus, CollectionDeworming.PENDING)
            .orderBy(CollectionDeworming.kDewormingDate, Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val nextDewormingId = querySnapshot.documents.firstOrNull()?.id
                if (nextDewormingId != null) {
                    nextDewormingDoc = DewormingDTO.create(nextDewormingId, querySnapshot.documents.firstOrNull()!!.data)
                    if (nextDewormingDoc != null) {
                        Log.e("NST", "Next deworming id: ${nextDewormingDoc!!.id}")
                        getReminderDocId(nextDewormingDoc!!.id, false)
                    } else {
                        binding.progressBar2.visibility = View.GONE
                        Toast.makeText(requireContext(), "Unable to find next deworming details", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("NST", "Next deworming id is null")
                    binding.progressBar2.visibility = View.GONE
                    Toast.makeText(requireContext(), "Unable to find next deworming details", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("NST", "Exception: Edit Deworming BatchTask(4): ${exception.localizedMessage}")
                binding.progressBar2.visibility = View.GONE
                Toast.makeText(requireContext(), "Error while fetching next deworming details", Toast.LENGTH_SHORT).show()
            }
    }

    private val saveButtonTapped = View.OnClickListener {
        if (checkValidation()) {
            binding.progressBar.root.visibility = View.VISIBLE
            if (binding.spinnerStatus.selectedItem.toString() == CollectionDeworming.COMPLETED &&
                dewormingDTO!!.dewormingStatus != binding.spinnerStatus.selectedItem.toString()) {
                // 1st pending deworming or last pending deworming
                // pending => complete
                // update reminder doc status to complete.
                // create reminder for next deworming
                val batch = Firebase.firestore.batch()
                val finalDewormingDate: Date = completionDate ?: dewormingDate!!
                var finalDuration: String? = null
                if (binding.previousDurationSpinner.selectedItem as String? != null) {
                    finalDuration = binding.previousDurationSpinner.selectedItem.toString()
                } else {
                    finalDuration = dewormingDTO!!.durationType
                }
                val currentDewormingData = hashMapOf(
                    CollectionDeworming.kDewormingDate to finalDewormingDate,
                    CollectionDeworming.kDurationType to finalDuration,
                    CollectionDeworming.kWeight to binding.weightET.text.trim().toString(),
                    CollectionDeworming.kMedicineType to binding.spinnerMedicine.selectedItem.toString(),
                    CollectionDeworming.kPersonAdministratedId to (binding.spinnerUser.selectedItem as GenericUserDTO).id,
                    CollectionDeworming.kDewormingStatus to binding.spinnerStatus.selectedItem.toString(),
                    CollectionDeworming.kAdminNote to binding.note2ET.text.trim().toString(),
                    CollectionDeworming.kUpdatedAt to FieldValue.serverTimestamp(),
                    CollectionDeworming.kUpdatedBy to Firebase.auth.currentUser?.uid,
                )
                val currentDewormingDocRef = Firebase.firestore.collection(CollectionDeworming.name).document(args.dewormingId!!)
                batch.set(currentDewormingDocRef, currentDewormingData, SetOptions.merge())

                val nextDewormingData = hashMapOf(
                    CollectionDeworming.kDewormingDate to nextDewormingDate,
                    CollectionDeworming.kDurationType to binding.nextDurationSpinner.selectedItem.toString(),
                    CollectionDeworming.kDewormingStatus to CollectionDeworming.PENDING,
                    CollectionDeworming.kAdminNote to "",
                    CollectionDeworming.kAnimalDocId to dewormingDTO!!.animalDocId,
                    CollectionDeworming.kIsArchive to false,
                    CollectionDeworming.kCreatedAt to FieldValue.serverTimestamp(),
                    CollectionDeworming.kCreatedBy to Firebase.auth.currentUser?.uid
                )
                val nextDewormingDocRef = Firebase.firestore.collection(CollectionDeworming.name).document()
                batch.set(nextDewormingDocRef, nextDewormingData, SetOptions.merge())

                val nextRemindersDocRef = Firebase.firestore.collection(CollectionReminders.name).document()
                val nextRemindersData = hashMapOf(
                    CollectionReminders.kAnimalDocId to dewormingDTO!!.animalDocId,
                    CollectionReminders.kReminderDate to nextDewormingDate,
                    CollectionReminders.kReminderType to CollectionReminders.DEWORMING,
                    CollectionReminders.kReminderTypeObjectId to nextDewormingDocRef.id,
                    CollectionReminders.kIsComplete to false,
                    CollectionReminders.kIsArchive to false,
                    CollectionReminders.kCreatedAt to FieldValue.serverTimestamp(),
                    CollectionReminders.kCreatedBy to Firebase.auth.currentUser?.uid,
                )
                batch.set(nextRemindersDocRef, nextRemindersData, SetOptions.merge())

                // Create FeedEvent for Complete Deworming
                val dewormingFeedEventDocRef = Firebase.firestore.collection(CollectionFeedEvents.name).document()
                val dewormingFeedEventData = hashMapOf(
                    CollectionFeedEvents.kAnimalDocId to dewormingDTO!!.animalDocId,
                    CollectionFeedEvents.kFeedType to FeedType.DEWORMING.value,
                    CollectionFeedEvents.kFeedObjectId to dewormingDTO!!.id, // Pass Deworming Document Id
                    CollectionFeedEvents.kIsArchive to false,
                    CollectionFeedEvents.kCreatedAt to FieldValue.serverTimestamp(),
                    CollectionFeedEvents.kCreatedBy to Firebase.auth.currentUser?.uid,
                )
                batch.set(dewormingFeedEventDocRef, dewormingFeedEventData)

                // update reminder date and status to complete.
                if (reminderDocId != null) {
                    val reminderData = hashMapOf(
                        CollectionReminders.kReminderDate to finalDewormingDate,
                        CollectionReminders.kIsComplete to true
                    )
                    val reminderDocRef = Firebase.firestore.collection(CollectionReminders.name).document(reminderDocId!!)
                    batch.set(reminderDocRef, reminderData, SetOptions.merge())
                }

                // Gaurav
                val notificationDocRef = Firebase.firestore.collection(CollectionNotifications.name).document()
                val notificationData = hashMapOf(
                    CollectionNotifications.kAnimalDocId to dewormingDTO!!.animalDocId,
                    CollectionNotifications.kNotificationDate to FieldValue.serverTimestamp(),
                    CollectionNotifications.kNotificationType to CollectionNotifications.DEWORMING,
                    CollectionNotifications.kNotificationSubtype to null,
                    CollectionNotifications.kNotificationTypeObjectId to currentDewormingDocRef.id,
                    CollectionNotifications.kIsArchive to false,
                    CollectionNotifications.kCreatedAt to FieldValue.serverTimestamp(),
                    CollectionNotifications.kCreatedBy to Firebase.auth.currentUser?.uid,
                )
                batch.set(notificationDocRef, notificationData)

                batch.commit().addOnCompleteListener { batchTask ->
                    if (batchTask.isSuccessful) {
                        binding.progressBar.root.visibility = View.GONE
                        sendPushNotification(dewormingDTO!!.animalDocId)
                        // Ask for Vaccination schedule after completing first deworming
                        if (args.currentDewormingIndex == 0 && args.totalDewormingCount == 1) {
                            findNavController().previousBackStackEntry?.savedStateHandle?.set("move_to_vaccination", 1)
                        }
                        Toast.makeText(requireContext(), "Deworming schedule updated successfully.", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        binding.progressBar.root.visibility = View.GONE
                        Toast.makeText(requireContext(), batchTask.exception?.message, Toast.LENGTH_SHORT).show()
                        Log.e("NST", "Exception: Edit Deworming BatchTask: ${batchTask.exception?.localizedMessage}")
                    }
                }
            } else if (dewormingDTO!!.dewormingStatus == CollectionDeworming.COMPLETED &&
                args.totalDewormingCount - args.currentDewormingIndex == args.totalDewormingCount - 1) {
                // n-1 complete deworming
                // Fetch next deworming details and make changes!!!
                // update date in FeedEvent doc
                // update reminder doc for this deworming.
                // update reminder doc for next deworming also.
                val batch = Firebase.firestore.batch()
                val finalDewormingDate: Date = completionDate ?: dewormingDate!!
                var finalDuration: String? = null
                if (binding.previousDurationSpinner.selectedItem as String? != null) {
                    finalDuration = binding.previousDurationSpinner.selectedItem.toString()
                } else {
                    finalDuration = dewormingDTO!!.durationType
                }
                val currentDewormingData = hashMapOf(
                    CollectionDeworming.kDewormingDate to finalDewormingDate,
                    CollectionDeworming.kDurationType to finalDuration,
                    CollectionDeworming.kWeight to binding.weightET.text.trim().toString(),
                    CollectionDeworming.kMedicineType to binding.spinnerMedicine.selectedItem.toString(),
                    CollectionDeworming.kPersonAdministratedId to (binding.spinnerUser.selectedItem as GenericUserDTO).id,
                    CollectionDeworming.kDewormingStatus to binding.spinnerStatus.selectedItem.toString(),
                    CollectionDeworming.kAdminNote to binding.note2ET.text.trim().toString(),
                    CollectionDeworming.kUpdatedAt to FieldValue.serverTimestamp(),
                    CollectionDeworming.kUpdatedBy to Firebase.auth.currentUser?.uid,
                )
                val currentDewormingDocRef = Firebase.firestore.collection(CollectionDeworming.name).document(args.dewormingId!!)
                batch.set(currentDewormingDocRef, currentDewormingData, SetOptions.merge())

                if (feedEventDocId != null) {
                    val feedEventData = hashMapOf(
                        CollectionFeedEvents.kUpdatedAt to finalDewormingDate,
                        CollectionFeedEvents.kUpdatedBy to FirebaseAuth.getInstance().currentUser?.uid
                    )
                    val feedEventDocRef = Firebase.firestore.collection(CollectionFeedEvents.name).document(feedEventDocId!!)
                    batch.set(feedEventDocRef, feedEventData, SetOptions.merge())
                }

                if (nextDewormingDoc != null) {
                    val nextDewormingCalendar = Calendar.getInstance()
                    nextDewormingCalendar.time = finalDewormingDate
                    if (nextDewormingDoc!!.durationType == CollectionDeworming.DURATION_30) {
                        nextDewormingCalendar.add(Calendar.DAY_OF_YEAR, 30)
                    } else if (nextDewormingDoc!!.durationType == CollectionDeworming.DURATION_90) {
                        nextDewormingCalendar.add(Calendar.DAY_OF_YEAR, 90)
                    } else {
                        nextDewormingCalendar.add(Calendar.YEAR, 1)
                    }

                    val nextDewormingData = hashMapOf(
                        CollectionDeworming.kDewormingDate to nextDewormingCalendar.time,
                    )
                    val nextDewormingDocRef = Firebase.firestore.collection(CollectionDeworming.name).document(nextDewormingDoc!!.id)
                    batch.set(nextDewormingDocRef, nextDewormingData, SetOptions.merge())

                    if (nextDewormingReminderDocId != null) {
                        val nextReminderData = hashMapOf(CollectionReminders.kReminderDate to nextDewormingCalendar.time)
                        val nextReminderDocRef = Firebase.firestore.collection(CollectionReminders.name).document(nextDewormingReminderDocId!!)
                        batch.set(nextReminderDocRef, nextReminderData, SetOptions.merge())
                    }
                }

                batch.commit().addOnCompleteListener { batchTask ->
                    if (batchTask.isSuccessful) {
                        binding.progressBar.root.visibility = View.GONE
                        Toast.makeText(requireContext(), "Deworming schedule updated successfully.", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        binding.progressBar.root.visibility = View.GONE
                        Toast.makeText(requireContext(), batchTask.exception?.message, Toast.LENGTH_SHORT).show()
                        binding.progressBar.root.visibility = View.GONE
                        Log.e("NST", "Exception: Edit Deworming BatchTask(3): ${batchTask.exception?.localizedMessage}")
                    }
                }
            } else {
                // Pending deworming edit
                // Update reminder doc
                val batch = Firebase.firestore.batch()
                val finalDewormingDate: Date = completionDate ?: dewormingDate!!
                var finalDuration: String? = null
                if (binding.previousDurationSpinner.selectedItem as String? != null) {
                    finalDuration = binding.previousDurationSpinner.selectedItem.toString()
                } else {
                    finalDuration = dewormingDTO!!.durationType
                }
                val currentDewormingData = hashMapOf(
                    CollectionDeworming.kDewormingDate to finalDewormingDate,
                    CollectionDeworming.kDurationType to finalDuration,
                    CollectionDeworming.kWeight to binding.weightET.text.trim().toString(),
                    CollectionDeworming.kMedicineType to binding.spinnerMedicine.selectedItem.toString(),
                    CollectionDeworming.kPersonAdministratedId to (binding.spinnerUser.selectedItem as GenericUserDTO).id,
                    CollectionDeworming.kDewormingStatus to binding.spinnerStatus.selectedItem.toString(),
                    CollectionDeworming.kAdminNote to binding.note2ET.text.trim().toString(),
                    CollectionDeworming.kUpdatedAt to FieldValue.serverTimestamp(),
                    CollectionDeworming.kUpdatedBy to Firebase.auth.currentUser?.uid,
                )
                val currentDewormingDocRef = Firebase.firestore.collection(CollectionDeworming.name).document(args.dewormingId!!)
                batch.set(currentDewormingDocRef, currentDewormingData, SetOptions.merge())

                if (reminderDocId != null) {
                    val reminderData = hashMapOf(CollectionReminders.kReminderDate to finalDewormingDate)
                    val reminderDocRef = Firebase.firestore.collection(CollectionReminders.name).document(reminderDocId!!)
                    batch.set(reminderDocRef, reminderData, SetOptions.merge())
                }

                batch.commit().addOnCompleteListener { batchTask ->
                    if (batchTask.isSuccessful) {
                        binding.progressBar.root.visibility = View.GONE
                        Toast.makeText(requireContext(), "Deworming schedule updated successfully.", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        binding.progressBar.root.visibility = View.GONE
                        Toast.makeText(requireContext(), batchTask.exception?.message, Toast.LENGTH_SHORT).show()
                        binding.progressBar.root.visibility = View.GONE
                        Log.e("NST", "Exception: Edit Deworming BatchTask (2): ${batchTask.exception?.localizedMessage}")
                    }
                }
            }
        }
    }
}