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
import com.nextsavy.pawgarage.databinding.FragmentVaccinationEditBinding
import com.nextsavy.pawgarage.models.FeedType
import com.nextsavy.pawgarage.models.GenericUserDTO
import com.nextsavy.pawgarage.models.VaccinationDTO
import com.nextsavy.pawgarage.retrofit.RetrofitClient
import com.nextsavy.pawgarage.retrofit.RetrofitInterface
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionFeedEvents
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionScheduleStatus
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.CollectionVaccination
import com.nextsavy.pawgarage.utils.CollectionVaccinesList
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import retrofit2.Call
import retrofit2.Callback
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VaccinationEditFragment : Fragment() {

    private lateinit var binding: FragmentVaccinationEditBinding
    private val args: VaccinationEditFragmentArgs by navArgs()
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)

    private var vaccinationDTO: VaccinationDTO? = null
    private var reminderDocId: String? = null
    private var nextVaccinationDoc: VaccinationDTO? = null
    private var nextVaccinationReminderDocId: String? = null
    private var feedEventDocId: String? = null

    private var vaccinationDate: Date? = null
    private var completionDate: Date? = null
    private var nextVaccinationDate: Date? = null

    lateinit var retrofitInterface: RetrofitInterface
    private var animalName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVaccinationEditBinding.inflate(inflater, container, false)

        animalName = args.animalName ?: ""
        binding.toolbarOne.titleToolbarOne.text = animalName
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        getVaccinationDetails()
        return binding.root
    }
    private fun getAnimalName() {
        if (Helper.isInternetAvailable(requireContext())) {
            Firebase.firestore.collection(CollectionAnimals.name).document(vaccinationDTO!!.animalDocId)
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
                notification.addProperty("title", CollectionVaccination.name)
                notification.addProperty("body", "$animalName has been vaccinated.")
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

    private fun getVaccinationDetails() {
        if (args.vaccinationId != null) {
            binding.progressBar2.visibility = View.VISIBLE
            Firebase.firestore
                .collection(CollectionVaccination.name)
                .document(args.vaccinationId!!)
                .get()
                .addOnSuccessListener { docSnapshot ->
                    binding.progressBar2.visibility = View.GONE
                    vaccinationDTO = VaccinationDTO.create(docSnapshot.id, docSnapshot.data)
                    if (vaccinationDTO != null) {
                        getAnimalName()
                        setupUI()
                        getReminderDocId(vaccinationDTO!!.id, true)
                    } else {
                        Toast.makeText(requireContext(), "Vaccination data is not correct!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("NST-M", "Exception: getVaccinationDetails > $exception")
                    Toast.makeText(requireContext(), exception.message, Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Vaccination Id not found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        // Top level checks: Dead, Terminated etc...
        binding.vaccineIndexTV.text = "Vaccine ${args.totalVaccinationCount - args.currentVaccinationIndex}"

        binding.note2ET.setText(vaccinationDTO!!.adminNotes)
        setupAndConfigureCreatedByUI()
        setupAndConfigureUpdatedByUI()
        binding.saveBTN.setOnClickListener(saveButtonTapped)

        if (vaccinationDTO!!.vaccinationStatus == CollectionVaccination.PENDING &&
            args.currentVaccinationIndex == 0 &&
            args.totalVaccinationCount == 1) {
            // First and only pending vaccine
            binding.note2ET.isEnabled = true
            binding.saveBTN.visibility = View.VISIBLE
            configurePreviousVaccinationDurationSpinner(false)
            setupVaccinationDatePicker(true)
            configureVaccineTypeSpinner(true)
            configurePersonAdministratedSpinner(true)
            setupDiffCompletionCheckbox(false)
            configureVaccinationStatusSpinner(true)
            setupNextVaccinationDurationSpinner(true)
        } else if (vaccinationDTO!!.vaccinationStatus == CollectionVaccination.COMPLETED &&
            args.totalVaccinationCount - args.currentVaccinationIndex == args.totalVaccinationCount - 1) {
            // Second last complete vaccination
            getFeedEventDocId()
            getNextVaccinationDetails()
            binding.note2ET.isEnabled = true
            binding.saveBTN.visibility = View.VISIBLE
            configurePreviousVaccinationDurationSpinner(false)
            setupVaccinationDatePicker(true)
            configureVaccineTypeSpinner(true)
            configurePersonAdministratedSpinner(true)
            setupDiffCompletionCheckbox(false)
            configureVaccinationStatusSpinner(false)
            setupNextVaccinationDurationSpinner(false)
        } else if (vaccinationDTO!!.vaccinationStatus == CollectionVaccination.PENDING &&
            args.currentVaccinationIndex == 0 &&
            args.totalVaccinationCount > 1) {
            // Last pending vaccination
            binding.note2ET.isEnabled = true
            binding.saveBTN.visibility = View.VISIBLE
            configurePreviousVaccinationDurationSpinner(true)
            setupVaccinationDatePicker(false)
            configureVaccineTypeSpinner(true)
            configurePersonAdministratedSpinner(true)
            setupDiffCompletionCheckbox(true)
            configureVaccinationStatusSpinner(true)
            setupNextVaccinationDurationSpinner(true)
        } else {
            // Every other case
            binding.note2ET.isEnabled = false
            binding.saveBTN.visibility = View.GONE
            configurePreviousVaccinationDurationSpinner(false)
            setupVaccinationDatePicker(false)
            configureVaccineTypeSpinner(false)
            configurePersonAdministratedSpinner(false)
            setupDiffCompletionCheckbox(false)
            configureVaccinationStatusSpinner(false)
            setupNextVaccinationDurationSpinner(false)
        }

        if (AppDelegate.isDead || AppDelegate.state == CollectionAnimals.TERMINATED) {
            binding.note2ET.isEnabled = false
            binding.saveBTN.visibility = View.GONE
            configurePreviousVaccinationDurationSpinner(false)
            setupVaccinationDatePicker(false)
            configureVaccineTypeSpinner(false)
            configurePersonAdministratedSpinner(false)
            setupDiffCompletionCheckbox(false)
            configureVaccinationStatusSpinner(false)
            setupNextVaccinationDurationSpinner(false)
        }
    }

    private fun configurePreviousVaccinationDurationSpinner(isEnable: Boolean) {
        if (isEnable) {
            val duration = resources.getStringArray(R.array.duration)
            val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, duration)
            binding.previousDurationSpinner.adapter = adapter
            if (vaccinationDTO!!.durationType != null) {
                val index = adapter.getPosition(vaccinationDTO!!.durationType!!)
                binding.previousDurationSpinner.setSelection(index)
            }
            binding.previousDurationSpinner.onItemSelectedListener = previousVaccinationDurationChangeListener
            binding.kPreDurationTV.visibility = View.VISIBLE
            binding.previousDurationRL.visibility = View.VISIBLE
        } else {
            binding.kPreDurationTV.visibility = View.GONE
            binding.previousDurationRL.visibility = View.GONE
        }
    }

    private val previousVaccinationDurationChangeListener = object: AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            // Old vaccination duration, as per database
            val dbDuration = vaccinationDTO!!.durationType
            // Old vaccination date, as per database
            val dbVaccinationDate = vaccinationDTO!!.vaccinationDate.toDate()

            if (dbDuration != null) {
                if (dbDuration == binding.previousDurationSpinner.selectedItem.toString()) {
                    vaccinationDate = dbVaccinationDate
                    handleVaccinationDateChange()
                    binding.vaccineDateET.setText(dateFormat.format(dbVaccinationDate))
                } else {
                    // change
                    if (dbDuration == CollectionVaccination.DURATION_DAYS) {
                        // old value = 21 => changed to 365
                        val vaccinationCalendar = Calendar.getInstance()
                        vaccinationCalendar.time = dbVaccinationDate
                        vaccinationCalendar.add(Calendar.DAY_OF_YEAR, -21)
                        vaccinationCalendar.add(Calendar.YEAR, 1)
                        vaccinationDate = vaccinationCalendar.time
                        binding.vaccineDateET.setText(dateFormat.format(vaccinationCalendar.time))
                        handleVaccinationDateChange()
                    } else {
                        // old value = 365 => changed to 21
                        val vaccinationCalendar = Calendar.getInstance()
                        vaccinationCalendar.time = dbVaccinationDate
                        vaccinationCalendar.add(Calendar.YEAR, -1)
                        vaccinationCalendar.add(Calendar.DAY_OF_YEAR, 21)
                        vaccinationDate = vaccinationCalendar.time
                        binding.vaccineDateET.setText(dateFormat.format(vaccinationCalendar.time))
                        handleVaccinationDateChange()
                    }
                }
            } else {
                Log.e("NST-M", "PRE Spinner value changed and DB duration is null :(")
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
    }

    private fun setupVaccinationDatePicker(isEnable: Boolean) {
        binding.vaccineDateET.setText(dateFormat.format(vaccinationDTO!!.vaccinationDate.toDate()))
        vaccinationDate = vaccinationDTO!!.vaccinationDate.toDate()

        if (isEnable) {
            binding.vaccineDateET.setOnClickListener {
                val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, monthOfYear)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    // Send date to anther function
                    vaccinationDate = calendar.time
                    handleVaccinationDateChange()
                    binding.vaccineDateET.setText(dateFormat.format(calendar.time))
                }

                val calendarForDateToSet = Calendar.getInstance()
                if (vaccinationDate != null) {
                    calendarForDateToSet.time = vaccinationDate!!
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
                // Manthan 13-12-2023
                // This Vaccination's date should not be before previous vaccination date + 21 days.
                // This Date picker will only allow to select date to last complete Vaccination only.
                if (args.previousVaccinationDate > 0) {
                    val date = Date(args.previousVaccinationDate)
                    val pCalendar = Calendar.getInstance()
                    pCalendar.time = date
                    pCalendar.add(Calendar.DAY_OF_MONTH, 21)
                    dialog.datePicker.minDate = pCalendar.timeInMillis
                    if (date <= Date()) {
                        dialog.datePicker.maxDate = System.currentTimeMillis()
                    }
                } else {
                    dialog.datePicker.maxDate = System.currentTimeMillis() // To disable the days after today.
                }
                dialog.show()
            }
        } else {
            binding.vaccineDateET.setOnClickListener(null)
        }
    }

    private fun setupAndConfigureCreatedByUI() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            Firebase.firestore.collection(CollectionUser.name)
                .document(vaccinationDTO!!.createdBy)
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
        if (vaccinationDTO!!.updatedBy != null) {
            if (Helper.isInternetAvailable(requireContext())) {
                binding.progressBar2.visibility = View.VISIBLE
                Firebase.firestore.collection(CollectionUser.name)
                    .document(vaccinationDTO!!.updatedBy!!)
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

    private fun configureVaccineTypeSpinner(isEnable: Boolean) {
        val vaccineList = arrayListOf<String>()
        binding.progressBar2.visibility = View.VISIBLE
        Firebase.firestore.collection(CollectionVaccinesList.name)
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

                val spinnerPosition = adapter.getPosition(vaccinationDTO!!.vaccineName)
                binding.spinnerVaccine.setSelection(spinnerPosition)
                binding.spinnerVaccine.isEnabled = isEnable
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
                        val index = userList.indexOfFirst { it.id == vaccinationDTO!!.administratorId }
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
                handleVaccinationDateChange()
            }
        } else {
            binding.differentDateCB.visibility = View.GONE
            binding.kCompletionDateTV.visibility = View.GONE
            binding.completionDateET.visibility = View.GONE
        }
    }

    private fun configureVaccinationStatusSpinner(isEnable: Boolean) {
        if (isEnable) {
            Firebase.firestore.collection(CollectionScheduleStatus.name)
                .get()
                .addOnSuccessListener { result ->
                    val statusList = result.documents.map { it.id }
                    val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, statusList)
                    binding.spinnerStatus.adapter = adapter
                    val spinnerPosition = adapter.getPosition(vaccinationDTO!!.vaccinationStatus)
                    binding.spinnerStatus.setSelection(spinnerPosition)
                    binding.spinnerStatus.onItemSelectedListener = vaccineStatusChangeListener
                    binding.spinnerStatus.isEnabled = true
                }
                .addOnFailureListener { exception ->
                    Log.e("ERROR", "Error getting CollectionScheduleStatus documents: $exception")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        } else {
            val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, listOf<String>(vaccinationDTO!!.vaccinationStatus))
            binding.spinnerStatus.adapter = adapter
            binding.spinnerStatus.setSelection(0)
            binding.spinnerStatus.onItemSelectedListener = null
            binding.spinnerStatus.isEnabled = false
            binding.warningTV.visibility = View.GONE
        }
    }

    private val vaccineStatusChangeListener = object: AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            if (binding.spinnerStatus.selectedItem.toString() == CollectionVaccination.COMPLETED &&
                args.currentVaccinationIndex == 0 &&
                args.totalVaccinationCount == 1) {
                // First vaccine toggle to Complete
                binding.differentDateCB.visibility = View.GONE
                binding.differentDateCB.isChecked = false
                binding.kCompletionDateTV.visibility = View.GONE
                binding.completionDateET.visibility = View.GONE
                setupCompletionDatePicker(false)
                binding.kNextVaccineTV.visibility = View.VISIBLE
                binding.kNextDurationTV.visibility = View.VISIBLE
                binding.nextDurationRL.visibility = View.VISIBLE
                binding.kNextVaccineDateTV.visibility = View.VISIBLE
                binding.nextVaccineDateET.visibility = View.VISIBLE
                binding.warningTV.visibility = View.GONE

                //askNotificationPermission()
            } else if (binding.spinnerStatus.selectedItem.toString() == CollectionVaccination.COMPLETED &&
                args.currentVaccinationIndex == 0 &&
                args.totalVaccinationCount > 1) {
                // Last vaccine toggle to Complete
                binding.differentDateCB.visibility = View.VISIBLE
                binding.differentDateCB.isChecked = false
                binding.kCompletionDateTV.visibility = View.GONE
                binding.completionDateET.visibility = View.GONE
                setupCompletionDatePicker(true)
                binding.kNextVaccineTV.visibility = View.VISIBLE
                binding.kNextDurationTV.visibility = View.VISIBLE
                binding.nextDurationRL.visibility = View.VISIBLE
                binding.kNextVaccineDateTV.visibility = View.VISIBLE
                binding.nextVaccineDateET.visibility = View.VISIBLE
                binding.warningTV.visibility = View.GONE

                //askNotificationPermission()
            } else {
                binding.differentDateCB.visibility = View.GONE
                binding.differentDateCB.isChecked = false
                binding.kCompletionDateTV.visibility = View.GONE
                binding.completionDateET.visibility = View.GONE
                setupCompletionDatePicker(false)
                binding.kNextVaccineTV.visibility = View.GONE
                binding.kNextDurationTV.visibility = View.GONE
                binding.nextDurationRL.visibility = View.GONE
                binding.kNextVaccineDateTV.visibility = View.GONE
                binding.nextVaccineDateET.visibility = View.GONE
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
                    handleVaccinationDateChange()
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
                // Manthan 13-12-2023
                // This Vaccination's completion date should not be before previous vaccination date + 21 days.
                if (args.previousVaccinationDate > 0) {
                    val date = Date(args.previousVaccinationDate)
                    val pCalendar = Calendar.getInstance()
                    pCalendar.time = date
                    pCalendar.add(Calendar.DAY_OF_MONTH, 21)
                    dialog.datePicker.minDate = pCalendar.timeInMillis
                    // If Current Date is greater than or equal to previous vaccination date
                    if (date <= Date()) {
                        dialog.datePicker.maxDate = System.currentTimeMillis()
                    }
                } else {
                    dialog.datePicker.maxDate = System.currentTimeMillis() // To disable the days after today.
                }
                dialog.show()
            }
        } else {
            binding.completionDateET.setOnClickListener(null)
        }
    }

    private fun setupNextVaccinationDurationSpinner(isEnable: Boolean) {
        if (isEnable) {
            val duration = resources.getStringArray(R.array.duration)
            val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, duration)
            binding.nextDurationSpinner.adapter = adapter
            binding.nextDurationSpinner.onItemSelectedListener = nextVaccinationDurationChangeListener
        } else {
            binding.nextDurationSpinner.onItemSelectedListener = null
        }
        binding.kNextVaccineTV.visibility = View.GONE
        binding.kNextDurationTV.visibility = View.GONE
        binding.nextDurationRL.visibility = View.GONE
        binding.kNextVaccineDateTV.visibility = View.GONE
        binding.nextVaccineDateET.visibility = View.GONE
    }

    private val nextVaccinationDurationChangeListener = object: AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            handleVaccinationDateChange()
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
    }

    private fun handleVaccinationDateChange() {
        /*
        1. args.currentVaccinationIndex >= 2 && args.totalVaccinationCount > 2
        2. args.totalVaccinationCount - args.currentVaccinationIndex == args.totalVaccinationCount - 1
         */
        if (vaccinationDTO!!.vaccinationStatus == CollectionVaccination.COMPLETED &&
            args.totalVaccinationCount - args.currentVaccinationIndex == args.totalVaccinationCount - 1) {
            // TODO: Handle this case!!!
        } else {
            if (binding.nextDurationSpinner.selectedItem.toString() == CollectionVaccination.DURATION_DAYS) {
                // 21 Days
                val completionCalendar = Calendar.getInstance()
                if (completionDate != null) {
                    completionCalendar.time = completionDate!!
                    completionCalendar.add(Calendar.DAY_OF_MONTH, 21)
                    nextVaccinationDate = completionCalendar.time
                    binding.nextVaccineDateET.setText(dateFormat.format(completionCalendar.time))
                } else if (vaccinationDate != null) {
                    completionCalendar.time = vaccinationDate!!
                    completionCalendar.add(Calendar.DAY_OF_MONTH, 21)
                    nextVaccinationDate = completionCalendar.time
                    binding.nextVaccineDateET.setText(dateFormat.format(completionCalendar.time))
                } else {
                    Log.e("NST-M", "Both dates are null in next duration spinner")
                }
            } else if (binding.nextDurationSpinner.selectedItem.toString() == CollectionVaccination.DURATION_YEAR) {
                // 365 Days
                val completionCalendar = Calendar.getInstance()
                if (completionDate != null) {
                    completionCalendar.time = completionDate!!
                    completionCalendar.add(Calendar.YEAR, 1)
                    nextVaccinationDate = completionCalendar.time
                    binding.nextVaccineDateET.setText(dateFormat.format(completionCalendar.time))
                } else if (vaccinationDate != null) {
                    completionCalendar.time = vaccinationDate!!
                    completionCalendar.add(Calendar.YEAR, 1)
                    nextVaccinationDate = completionCalendar.time
                    binding.nextVaccineDateET.setText(dateFormat.format(completionCalendar.time))
                } else {
                    Log.e("NST-M", "Both dates are null in next duration spinner")
                }
            } else {
                // NA
            }
        }
    }
    private fun getFeedEventDocId() {
        binding.progressBar2.visibility = View.VISIBLE
        Firebase.firestore
            .collection(CollectionFeedEvents.name)
            .whereEqualTo(CollectionFeedEvents.kFeedObjectId, vaccinationDTO!!.id)
            .get()
            .addOnSuccessListener { querySnapshot ->
                feedEventDocId = querySnapshot.documents.lastOrNull()?.id
                if (feedEventDocId != null) {
                    Log.e("NST-M", "FeedEvent Doc Id in VaccinationEditFrag : $feedEventDocId")
                } else {
                    Log.e("NST-M", "FeedEvent Doc is null in VaccinationEditFrag")
                }
                binding.progressBar2.visibility = View.GONE
            }
            .addOnFailureListener {
                Log.e("NST-M", "Exception: VaccinationEdit > getFeedEventDocId: ${it.localizedMessage}")
                binding.progressBar2.visibility = View.GONE
            }
    }

    private fun getReminderDocId(vaccinationId: String, isCurrent: Boolean) {
        binding.progressBar2.visibility = View.VISIBLE
        Firebase.firestore
            .collection(CollectionReminders.name)
            .whereEqualTo(CollectionReminders.kReminderTypeObjectId, vaccinationId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (isCurrent) {
                    reminderDocId = querySnapshot.documents.lastOrNull()?.id
                    Log.e("NST-M", "Reminder Doc Id in VaccinationEditFrag : $reminderDocId")
                } else {
                    nextVaccinationReminderDocId = querySnapshot.documents.lastOrNull()?.id
                    Log.e("NST-M", "Next Reminder Doc Id in VaccinationEditFrag : $nextVaccinationReminderDocId")
                }
                binding.progressBar2.visibility = View.GONE
            }
            .addOnFailureListener {
                Log.e("NST-M", "Exception: VaccinationEdit > getReminderDocId > Vaccine id$vaccinationId isCurrent:$isCurrent\t: ${it.localizedMessage}")
                binding.progressBar2.visibility = View.GONE
            }
    }

    private fun getNextVaccinationDetails() {
        binding.progressBar2.visibility = View.VISIBLE
        Firebase.firestore
            .collection(CollectionVaccination.name)
            .whereEqualTo(CollectionVaccination.kIsArchive, false)
            .whereEqualTo(CollectionVaccination.kAnimalDocId, vaccinationDTO!!.animalDocId)
            .whereEqualTo(CollectionVaccination.kVaccinationStatus, CollectionVaccination.PENDING)
            .orderBy(CollectionVaccination.kVaccinationDate, Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val nextVaccinationId = querySnapshot.documents.firstOrNull()?.id
                if (nextVaccinationId != null) {
                    nextVaccinationDoc = VaccinationDTO.create(nextVaccinationId, querySnapshot.documents.firstOrNull()!!.data)
                    if (nextVaccinationDoc != null) {
                        Log.e("NST", "Next vaccination id: ${nextVaccinationDoc!!.id}")
                        getReminderDocId(nextVaccinationDoc!!.id, false)
                    } else {
                        binding.progressBar2.visibility = View.GONE
                        Toast.makeText(requireContext(), "Unable to find next vaccination details", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("NST", "Next vaccination id is null")
                    binding.progressBar2.visibility = View.GONE
                    Toast.makeText(requireContext(), "Unable to find next vaccination details", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("NST", "Exception: Edit Vaccine BatchTask(4): ${exception.localizedMessage}")
                binding.progressBar2.visibility = View.GONE
                Toast.makeText(requireContext(), "Error while fetching next vaccination details", Toast.LENGTH_SHORT).show()
            }
    }

    private val saveButtonTapped = View.OnClickListener {
        binding.progressBar.root.visibility = View.VISIBLE
        if (binding.spinnerStatus.selectedItem.toString() == CollectionVaccination.COMPLETED &&
            vaccinationDTO!!.vaccinationStatus != binding.spinnerStatus.selectedItem.toString()) {
            // 1st pending vaccination or last pending vaccination
            // pending => complete
            // update reminder doc status to complete.
            // create reminder for next vaccination
            val batch = Firebase.firestore.batch()
            val finalVaccineDate: Date = completionDate ?: vaccinationDate!!
            var finalDuration: String? = null
            if (binding.previousDurationSpinner.selectedItem as String? != null) {
                finalDuration = binding.previousDurationSpinner.selectedItem.toString()
            } else {
                finalDuration = vaccinationDTO!!.durationType
            }
            val currentVaccineData = hashMapOf(
                CollectionVaccination.kVaccinationDate to finalVaccineDate,
                CollectionVaccination.kDurationType to finalDuration,
                CollectionVaccination.kVaccineType to binding.spinnerVaccine.selectedItem.toString(),
                CollectionVaccination.kPersonAdministratedId to (binding.spinnerUser.selectedItem as GenericUserDTO).id,
                CollectionVaccination.kVaccinationStatus to binding.spinnerStatus.selectedItem.toString(),
                CollectionVaccination.kAdminNote to binding.note2ET.text.trim().toString(),
                CollectionVaccination.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionVaccination.kUpdatedBy to Firebase.auth.currentUser?.uid,
            )
            val currentVaccineDocRef = Firebase.firestore.collection(CollectionVaccination.name).document(args.vaccinationId!!)
            batch.set(currentVaccineDocRef, currentVaccineData, SetOptions.merge())

            val nextVaccineData = hashMapOf(
                CollectionVaccination.kVaccinationDate to nextVaccinationDate,
                CollectionVaccination.kDurationType to binding.nextDurationSpinner.selectedItem.toString(),
                CollectionVaccination.kVaccinationStatus to CollectionVaccination.PENDING,
                CollectionVaccination.kAdminNote to "",
                CollectionVaccination.kAnimalDocId to vaccinationDTO!!.animalDocId,
                CollectionVaccination.kIsArchive to false,
                CollectionVaccination.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionVaccination.kCreatedBy to Firebase.auth.currentUser?.uid
            )
            val nextVaccineDocRef = Firebase.firestore.collection(CollectionVaccination.name).document()
            batch.set(nextVaccineDocRef, nextVaccineData, SetOptions.merge())

            val nextRemindersDocRef = Firebase.firestore.collection(CollectionReminders.name).document()
            val nextRemindersData = hashMapOf(
                CollectionReminders.kAnimalDocId to vaccinationDTO!!.animalDocId,
                CollectionReminders.kReminderDate to nextVaccinationDate,
                CollectionReminders.kReminderType to CollectionReminders.VACCINATION,
                CollectionReminders.kReminderTypeObjectId to nextVaccineDocRef.id,
                CollectionReminders.kIsComplete to false,
                CollectionReminders.kIsArchive to false,
                CollectionReminders.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionReminders.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
            batch.set(nextRemindersDocRef, nextRemindersData, SetOptions.merge())

            // Create FeedEvent for Complete Vaccine
            val vaccineFeedEventDocRef = Firebase.firestore.collection(CollectionFeedEvents.name).document()
            val vaccinationFeedEventData = hashMapOf(
                CollectionFeedEvents.kAnimalDocId to vaccinationDTO!!.animalDocId,
                CollectionFeedEvents.kFeedType to FeedType.VACCINE.value,
                CollectionFeedEvents.kFeedObjectId to vaccinationDTO!!.id, // Pass Vaccine Document Id
                CollectionFeedEvents.kIsArchive to false,
                CollectionFeedEvents.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionFeedEvents.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
            batch.set(vaccineFeedEventDocRef, vaccinationFeedEventData)

            // update reminder date and status to complete.
            if (reminderDocId != null) {
                val reminderData = hashMapOf(
                    CollectionReminders.kReminderDate to finalVaccineDate,
                    CollectionReminders.kIsComplete to true
                )
                val reminderDocRef = Firebase.firestore.collection(CollectionReminders.name).document(reminderDocId!!)
                batch.set(reminderDocRef, reminderData, SetOptions.merge())
            }
            // Gaurav
            val notificationDocRef = Firebase.firestore.collection(CollectionNotifications.name).document()
            val notificationData = hashMapOf(
                CollectionNotifications.kAnimalDocId to vaccinationDTO!!.animalDocId,
                CollectionNotifications.kNotificationDate to FieldValue.serverTimestamp(),
                CollectionNotifications.kNotificationType to CollectionNotifications.VACCINATION,
                CollectionNotifications.kNotificationSubtype to null,
                CollectionNotifications.kNotificationTypeObjectId to currentVaccineDocRef.id,
                CollectionNotifications.kIsArchive to false,
                CollectionNotifications.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionNotifications.kCreatedBy to Firebase.auth.currentUser?.uid,
            )

            batch.set(notificationDocRef, notificationData)

            batch.commit().addOnCompleteListener { batchTask ->
                if (batchTask.isSuccessful) {
                    binding.progressBar.root.visibility = View.GONE
                    sendPushNotification(vaccinationDTO!!.animalDocId)
                    Toast.makeText(requireContext(), "Vaccination schedule updated successfully.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    binding.progressBar.root.visibility = View.GONE
                    Toast.makeText(requireContext(), batchTask.exception?.message, Toast.LENGTH_SHORT).show()
                    binding.progressBar.root.visibility = View.GONE
                    Log.e("NST", "Exception: Edit Vaccine BatchTask: ${batchTask.exception?.localizedMessage}")
                }
            }
        } else if (vaccinationDTO!!.vaccinationStatus == CollectionVaccination.COMPLETED &&
            args.totalVaccinationCount - args.currentVaccinationIndex == args.totalVaccinationCount - 1) {
            // n-1 complete vaccination
          // Fetch next vaccination details and make changes!!!
            // update date in FeedEvent doc
            // update reminder doc for this vaccination.
            // update reminder doc for next vaccination also.
            val batch = Firebase.firestore.batch()
            val finalVaccineDate: Date = completionDate ?: vaccinationDate!!
            var finalDuration: String? = null
            if (binding.previousDurationSpinner.selectedItem as String? != null) {
                finalDuration = binding.previousDurationSpinner.selectedItem.toString()
            } else {
                finalDuration = vaccinationDTO!!.durationType
            }
            val currentVaccineData = hashMapOf(
                CollectionVaccination.kVaccinationDate to finalVaccineDate,
                CollectionVaccination.kDurationType to finalDuration,
                CollectionVaccination.kVaccineType to binding.spinnerVaccine.selectedItem.toString(),
                CollectionVaccination.kPersonAdministratedId to (binding.spinnerUser.selectedItem as GenericUserDTO).id,
                CollectionVaccination.kVaccinationStatus to binding.spinnerStatus.selectedItem.toString(),
                CollectionVaccination.kAdminNote to binding.note2ET.text.trim().toString(),
                CollectionVaccination.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionVaccination.kUpdatedBy to Firebase.auth.currentUser?.uid,
            )
            val currentVaccineDocRef = Firebase.firestore.collection(CollectionVaccination.name).document(args.vaccinationId!!)
            batch.set(currentVaccineDocRef, currentVaccineData, SetOptions.merge())

            if (feedEventDocId != null) {
                val feedEventData = hashMapOf(
                    CollectionFeedEvents.kUpdatedAt to finalVaccineDate,
                    CollectionFeedEvents.kUpdatedBy to FirebaseAuth.getInstance().currentUser?.uid
                )
                val feedEventDocRef = Firebase.firestore.collection(CollectionFeedEvents.name).document(feedEventDocId!!)
                batch.set(feedEventDocRef, feedEventData, SetOptions.merge())
            }

            if (nextVaccinationDoc != null) {
                val nextVaccinationCalendar = Calendar.getInstance()
                nextVaccinationCalendar.time = finalVaccineDate
                if (nextVaccinationDoc!!.durationType == CollectionVaccination.DURATION_YEAR) {
                    nextVaccinationCalendar.add(Calendar.YEAR, 1)
                } else {
                    nextVaccinationCalendar.add(Calendar.DAY_OF_MONTH, 21)
                }

                val nextVaccineData = hashMapOf(
                    CollectionVaccination.kVaccinationDate to nextVaccinationCalendar.time,
                )
                val nextVaccineDocRef = Firebase.firestore.collection(CollectionVaccination.name).document(nextVaccinationDoc!!.id)
                batch.set(nextVaccineDocRef, nextVaccineData, SetOptions.merge())

                if (nextVaccinationReminderDocId != null) {
                    val nextReminderData = hashMapOf(CollectionReminders.kReminderDate to nextVaccinationCalendar.time)
                    val nextReminderDocRef = Firebase.firestore.collection(CollectionReminders.name).document(nextVaccinationReminderDocId!!)
                    batch.set(nextReminderDocRef, nextReminderData, SetOptions.merge())
                }
            }

            batch.commit().addOnCompleteListener { batchTask ->
                if (batchTask.isSuccessful) {
                    binding.progressBar.root.visibility = View.GONE
                    Toast.makeText(requireContext(), "Vaccination schedule updated successfully.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    binding.progressBar.root.visibility = View.GONE
                    Toast.makeText(requireContext(), batchTask.exception?.message, Toast.LENGTH_SHORT).show()
                    binding.progressBar.root.visibility = View.GONE
                    Log.e("NST", "Exception: Edit Vaccine BatchTask(3): ${batchTask.exception?.localizedMessage}")
                }
            }
        } else {
            // Pending vaccination edit
            // Update reminder doc
            val batch = Firebase.firestore.batch()
            val finalVaccineDate: Date = completionDate ?: vaccinationDate!!
            var finalDuration: String? = null
            if (binding.previousDurationSpinner.selectedItem as String? != null) {
                finalDuration = binding.previousDurationSpinner.selectedItem.toString()
            } else {
                finalDuration = vaccinationDTO!!.durationType
            }
            val currentVaccineData = hashMapOf(
                CollectionVaccination.kVaccinationDate to finalVaccineDate,
                CollectionVaccination.kDurationType to finalDuration,
                CollectionVaccination.kVaccineType to binding.spinnerVaccine.selectedItem.toString(),
                CollectionVaccination.kPersonAdministratedId to (binding.spinnerUser.selectedItem as GenericUserDTO).id,
                CollectionVaccination.kVaccinationStatus to binding.spinnerStatus.selectedItem.toString(),
                CollectionVaccination.kAdminNote to binding.note2ET.text.trim().toString(),
                CollectionVaccination.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionVaccination.kUpdatedBy to Firebase.auth.currentUser?.uid,
            )
            val currentVaccineDocRef = Firebase.firestore.collection(CollectionVaccination.name).document(args.vaccinationId!!)
            batch.set(currentVaccineDocRef, currentVaccineData, SetOptions.merge())

            if (reminderDocId != null) {
                val reminderData = hashMapOf(CollectionReminders.kReminderDate to finalVaccineDate)
                val reminderDocRef = Firebase.firestore.collection(CollectionReminders.name).document(reminderDocId!!)
                batch.set(reminderDocRef, reminderData, SetOptions.merge())
            }

            batch.commit().addOnCompleteListener { batchTask ->
                if (batchTask.isSuccessful) {
                    binding.progressBar.root.visibility = View.GONE
                    Toast.makeText(requireContext(), "Vaccination schedule updated successfully.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    binding.progressBar.root.visibility = View.GONE
                    Toast.makeText(requireContext(), batchTask.exception?.message, Toast.LENGTH_SHORT).show()
                    binding.progressBar.root.visibility = View.GONE
                    Log.e("NST", "Exception: Edit Vaccine BatchTask (2): ${batchTask.exception?.localizedMessage}")
                }
            }
        }
    }
}