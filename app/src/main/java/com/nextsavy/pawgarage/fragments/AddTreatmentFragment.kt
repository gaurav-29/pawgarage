package com.nextsavy.pawgarage.fragments

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentAddTreatmentBinding
import com.nextsavy.pawgarage.models.FeedType
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.retrofit.RetrofitClient
import com.nextsavy.pawgarage.retrofit.RetrofitInterface
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionFeedEvents
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionTreatment
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.SharedPrefKeys
import com.nextsavy.pawgarage.viewModels.SharedViewModel
import retrofit2.Call
import retrofit2.Callback
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTreatmentFragment : Fragment() {

    private lateinit var binding: FragmentAddTreatmentBinding
    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val args: AddTreatmentFragmentArgs by navArgs()
    private var animalDocId: String? = null
    lateinit var retrofitInterface: RetrofitInterface
    private var nameExists: Boolean = false

    private var reportingPerson: GenericMemberDTO? = null

    private var admissionDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)

    private val viewModel: SharedViewModel by activityViewModels<SharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddTreatmentBinding.inflate(inflater, container, false)

        registerObserver()

        animalDocId = args.animalDocId
        if (args.from == "Profile") {
            binding.toolbarOne.titleToolbarOne.text = "New Profile"
        } else {
            binding.toolbarOne.titleToolbarOne.text = args.animalName ?: ""
        }

        binding.countryCodeET.setText(CollectionUser.COUNTRY_CODE)

        setupUI()

        onClickListeners()

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetSelection()
        viewModel.setReportingPerson(null)
    }

    private fun registerObserver() {
        viewModel.selectedMedicalCondition.observe(viewLifecycleOwner) {
            updateSelectedMedicalConditions()
        }
        viewModel.reportingPerson.observe(viewLifecycleOwner) {
            configureReportingPersonWidget(it)
        }
    }

    private fun updateSelectedMedicalConditions() {
        val sb = StringBuilder()
        viewModel.getSelectedMedicalCondition().forEach {
            if (sb.isBlank()) {
                sb.append(it.name)
            } else {
                sb.append(", ")
                sb.append(it.name)
            }
        }

        binding.conditionsET.setText(sb)
    }

    private fun sendPushNotification(title: String, message: String, animalDocIdForDeepLink: String) {
        val baseUrl = "https://fcm.googleapis.com/"
        retrofitInterface = RetrofitClient.getRetrofitInstance(baseUrl).create(RetrofitInterface::class.java)

        if (Helper.isInternetAvailable(requireContext())) {
            val payLoad = JsonObject()
            val notification = JsonObject()
            val data = JsonObject()

            try {
                payLoad.addProperty("to", "/topics/" + CollectionNotifications.TOPIC)
                notification.addProperty("title", title)
                notification.addProperty("body", message)
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
    private fun uploadAnimalImage() {

        binding.progressBar.root.visibility = View.VISIBLE
        val storageRef = storage.reference
        val imageRef = storageRef.child("Images")
        val childRef = imageRef.child("Animal Images")

        val sdf = SimpleDateFormat("ddMMyyHHmmss", Locale.ENGLISH)
        val currentDateAndTime = sdf.format(Date())
        var animalId2 = ""
        animalId2 = when (AppDelegate.animalModel.species) {
            CollectionAnimals.DOG -> "D_$currentDateAndTime"
            CollectionAnimals.CAT -> "C_$currentDateAndTime"
            else -> "O_$currentDateAndTime"
        }

        val animalRef = childRef.child("$animalId2.jpg")

        if (AppDelegate.imageFrom == "Gallery") {
            Log.e("From", "From gallery")
            if (AppDelegate.animalModel.galleryImage != null) {
                val uploadTask = animalRef.putFile(AppDelegate.animalModel.galleryImage!!)

                uploadTask.addOnFailureListener {
                    Log.e("FLOW", it.toString())
                    Toast.makeText(requireContext(), "Image not uploaded due to error.", Toast.LENGTH_SHORT).show()
                }.addOnSuccessListener { taskSnapshot ->
                    Log.e("FLOW", "Image uploaded successfully")
                }
                // To download url
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                            binding.progressBar.root.visibility = View.GONE
                            throw it
                        }
                    }
                    animalRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        addAnimalDataToDatabase(downloadUri.toString(), animalId2)
                    } else {
                        binding.progressBar.root.visibility = View.GONE
                        Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else if (AppDelegate.imageFrom == "Camera") {
            Log.e("From", "From Camera")
            if (AppDelegate.animalModel.cameraImage != null) {

                val baos = ByteArrayOutputStream()
                AppDelegate.animalModel.cameraImage!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                val uploadTask = animalRef.putBytes(data)

                uploadTask.addOnFailureListener {
                    Log.e("FLOW", it.toString())
                    Toast.makeText(requireContext(), "Image not uploaded due to error.", Toast.LENGTH_SHORT).show()
                }.addOnSuccessListener { taskSnapshot ->
                    Log.e("FLOW", "Image uploaded successfully")
                }
                // To download url
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            binding.progressBar.root.visibility = View.GONE
                            throw it
                        }
                    }
                    animalRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        addAnimalDataToDatabase(downloadUri.toString(), animalId2)
                    } else {
                        binding.progressBar.root.visibility = View.GONE
                        Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.e("From", "From ProfileLeads")
            addAnimalDataToDatabase(AppDelegate.profileLeadsImage, animalId2)
        }
    }
    private fun checkAnimalNameInDatabase() {

        binding.progressBar.root.visibility = View.VISIBLE
        nameExists = false
        db.collection(CollectionAnimals.name)
            .whereEqualTo(CollectionAnimals.kIsArchive, false)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.documents.isNotEmpty()) {
                    val enteredName = AppDelegate.animalModel.name.lowercase().replace(" ", "")
                    for (document in documents.documents) {
                        val name = document[CollectionAnimals.kName] as String
                        if (name.lowercase().replace(" ", "") == enteredName) {
                            nameExists = true
                            Toast.makeText(requireContext(), "Animal name already exists.", Toast.LENGTH_SHORT).show()
                            binding.progressBar.root.visibility = View.GONE
                            break
                        }
                    }
                    if (!nameExists) {
                        uploadAnimalImage()
                    }
                } else {
                    uploadAnimalImage()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FAILURE", "Exception: AddAdmissionFragment > ${exception.localizedMessage}")
                binding.progressBar.root.visibility = View.GONE
            }
    }

    private fun generateSearchKeywords(title: String): List<String> {
        var inputString = title.lowercase()
        val keywords = mutableListOf<String>()
        val words = inputString.split(" ")

        for (word in words) {
            var appendString = ""
            for (charPosition in inputString.indices) {
                appendString += inputString[charPosition].toString()
                keywords.add(appendString)
            }
            // Remove word once its characters are added to list
            inputString = inputString.replace("$word ", "")
        }
        return keywords
    }

    private fun addAnimalDataToDatabase(downloadUrl: String, animalId: String) {

        var animalData: HashMap<String, Any?> = HashMap()
        if (args.from == "Profile") {
            Log.e("FROM", "Profile")
            val textKeywords = generateSearchKeywords(AppDelegate.animalModel.name.trim())
            animalData = hashMapOf(
                CollectionAnimals.kAnimalId to animalId,
                CollectionAnimals.kIsDead to false,
                CollectionAnimals.kSearchKeywords to textKeywords,
                CollectionAnimals.kName to AppDelegate.animalModel.name,
                CollectionAnimals.kType to AppDelegate.animalModel.type,
                CollectionAnimals.kState to CollectionAnimals.ACTIVE,
                CollectionAnimals.kDescription to AppDelegate.animalModel.description,
                CollectionAnimals.kGender to AppDelegate.animalModel.gender,
                CollectionAnimals.kSpecies to AppDelegate.animalModel.species,
                CollectionAnimals.kDownloadUrl to downloadUrl,
                CollectionAnimals.kLatitude to AppDelegate.animalModel.latitude,
                CollectionAnimals.kLongitude to AppDelegate.animalModel.longitude,
                CollectionAnimals.kLocationAddress to AppDelegate.animalModel.locationAddress,
                CollectionAnimals.kIsArchive to AppDelegate.animalModel.isArchive,
                CollectionAnimals.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionAnimals.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
            val animalDocRef = Firebase.firestore.collection(CollectionAnimals.name).document()

            val registrationFeedEventData = hashMapOf(
                CollectionFeedEvents.kAnimalDocId to animalDocRef.id,
                CollectionFeedEvents.kFeedType to FeedType.REGISTRATION.value,
                CollectionFeedEvents.kFeedObjectId to null, // We don't need much data for Registration. Only created_at date will do the work!
                CollectionTreatment.kIsArchive to false,
                CollectionTreatment.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionTreatment.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
            val registrationFeedEventDocRef = Firebase.firestore.collection(CollectionFeedEvents.name).document()

            val batch1 = Firebase.firestore.batch() // OPD feed was showing before registration feed, so to avoid this, two batches made.

            batch1.set(registrationFeedEventDocRef, registrationFeedEventData)
            batch1.set(animalDocRef, animalData)
            batch1.commit().addOnCompleteListener { batchTask ->
                if (batchTask.isSuccessful) {
                    binding.progressBar.root.visibility = View.GONE
                    writeNextBatch(animalId, animalDocRef)
                } else {
                    binding.progressBar.root.visibility = View.GONE
                    Log.e("NST", "Exception: Animal Profile Batch1-Task: ${batchTask.exception?.localizedMessage}")
                }
            }
        } else {
            Log.e("FROM", "Add")
        }
    }

    private fun writeNextBatch(animalId: String, animalDocRef: DocumentReference) {
        val batch2 = Firebase.firestore.batch()

        val notificationDocRef = db.collection(CollectionNotifications.name).document()
        val notificationData = hashMapOf(
            CollectionNotifications.kAnimalDocId to animalDocRef.id,
            CollectionNotifications.kNotificationDate to FieldValue.serverTimestamp(),
            CollectionNotifications.kNotificationType to CollectionNotifications.NEW_PROFILE,
            CollectionNotifications.kNotificationSubtype to null,
            CollectionNotifications.kNotificationTypeObjectId to null,
            CollectionNotifications.kIsArchive to false,
            CollectionNotifications.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionNotifications.kCreatedBy to Firebase.auth.currentUser?.uid,
        )
        batch2.set(notificationDocRef, notificationData)

        if (args.reminderDocId != null) { // reminder data will be updated as is_complete = true, once the half profile of Team Member will be completed.
            val reminderDocRef = Firebase.firestore.collection(CollectionReminders.name).document(args.reminderDocId!!)
            val reminderUpdateData = hashMapOf(
                CollectionReminders.kAnimalDocId to animalDocRef.id,
                CollectionReminders.kIsComplete to true,
                CollectionReminders.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionReminders.kUpdatedBy to Firebase.auth.currentUser?.uid,
            )
            batch2.set(reminderDocRef, reminderUpdateData, SetOptions.merge())
        }

        // Manthan: 19-12-2023
        // Do not create Treatment if a new animal is being admitted with NO(zero) Medical Conditions.
        if (!binding.notApplicableCB.isChecked) {
            val treatmentData = hashMapOf(
                CollectionTreatment.kAnimalDocId to animalDocRef.id,
                CollectionTreatment.kTreatmentDate to admissionDate,
                CollectionAdmission.kReportingPersonId to viewModel.getReportingPerson()!!.id,
                CollectionTreatment.kContactNumber to CollectionUser.COUNTRY_CODE + binding.numberET.text.trim().toString(),
                CollectionTreatment.kMedicalConditions to binding.conditionsET.text.trim().toString(),
                CollectionTreatment.kMedicalConditionIds to if (binding.notApplicableCB.isChecked) listOf<String>() else viewModel.getSelectedMedicalCondition().map { it.id },
                CollectionTreatment.kIsMedicalConditionsApplicable to !binding.notApplicableCB.isChecked,
                CollectionTreatment.kIsArchive to AppDelegate.animalModel.isArchive,
                CollectionTreatment.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionTreatment.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
            val treatmentDocRef = Firebase.firestore.collection(CollectionTreatment.name).document()

            val feedEventData = hashMapOf(
                CollectionFeedEvents.kAnimalDocId to animalDocRef.id,
                CollectionFeedEvents.kFeedType to FeedType.TREATMENT.value,
                CollectionFeedEvents.kFeedObjectId to treatmentDocRef.id,
                CollectionTreatment.kIsArchive to false,
                CollectionTreatment.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionTreatment.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
            val feedEventDocRef = Firebase.firestore.collection(CollectionFeedEvents.name).document()
            batch2.set(treatmentDocRef, treatmentData)
            batch2.set(feedEventDocRef, feedEventData)
        }

        batch2.commit().addOnCompleteListener { batchTask ->
            if (batchTask.isSuccessful) {
                binding.progressBar.root.visibility = View.GONE
                val message = "${AppDelegate.animalModel.name} has been treated." + if (Helper.sharedPreference?.getString(SharedPrefKeys.kUserName, null) != null) "\n${Helper.sharedPreference.getString(SharedPrefKeys.kUserName, null)}" else ""
                sendPushNotification(CollectionNotifications.NEW_PROFILE, message, animalDocRef.id)
                Toast.makeText(requireContext(), "Animal profile created successfully.", Toast.LENGTH_SHORT).show()
                clearData()
                val bundle = Bundle().apply {
                    putString("animalId", animalDocRef.id)
                    putBoolean("show_deworming_alert", true)
                }
                findNavController().navigate(R.id.action_global_animal_profile, bundle)
            } else {
                binding.progressBar.root.visibility = View.GONE
                Log.e("NST", "Exception: Animal Profile Batch2-Task: ${batchTask.exception?.localizedMessage}")
            }
        }
    }

    private fun addTreatmentDetails() {
        binding.progressBar.root.visibility = View.VISIBLE
        val treatmentData = hashMapOf(
            CollectionTreatment.kAnimalDocId to animalDocId,
            CollectionTreatment.kTreatmentDate to admissionDate,
            CollectionAdmission.kReportingPersonId to viewModel.getReportingPerson()!!.id,
            CollectionTreatment.kContactNumber to CollectionUser.COUNTRY_CODE + binding.numberET.text.trim().toString(),
            CollectionTreatment.kMedicalConditions to binding.conditionsET.text.trim().toString(),
            CollectionTreatment.kMedicalConditionIds to if (binding.notApplicableCB.isChecked) listOf<String>() else viewModel.getSelectedMedicalCondition().map { it.id },
            CollectionTreatment.kIsMedicalConditionsApplicable to !binding.notApplicableCB.isChecked,
            CollectionTreatment.kIsArchive to AppDelegate.animalModel.isArchive,
            CollectionTreatment.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionTreatment.kCreatedBy to Firebase.auth.currentUser?.uid,
        )
        val treatmentDocRef = Firebase.firestore.collection(CollectionTreatment.name).document()

        val animalDocRef = Firebase.firestore.collection(CollectionAnimals.name).document(animalDocId!!)
        val animalData = hashMapOf(
            CollectionAnimals.kType to CollectionAnimals.OPD,
            CollectionAnimals.kUpdatedAt to FieldValue.serverTimestamp(),
            CollectionAnimals.kUpdatedBy to Firebase.auth.currentUser?.uid
        )

        val feedEventData = hashMapOf(
            CollectionFeedEvents.kAnimalDocId to animalDocId!!,
            CollectionFeedEvents.kFeedType to FeedType.TREATMENT.value,
            CollectionFeedEvents.kFeedObjectId to treatmentDocRef.id,
            CollectionTreatment.kIsArchive to false,
            CollectionTreatment.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionTreatment.kCreatedBy to Firebase.auth.currentUser?.uid,
        )
        val feedEventDocRef = Firebase.firestore.collection(CollectionFeedEvents.name).document()

        val batch = Firebase.firestore.batch()

        batch.update(animalDocRef, animalData)
        batch.set(treatmentDocRef, treatmentData)
        batch.set(feedEventDocRef, feedEventData)

        batch.commit().addOnCompleteListener { batchTask ->
            if (batchTask.isSuccessful) {
                binding.progressBar.root.visibility = View.GONE
                sendPushNotification(CollectionNotifications.TREATMENT, "${args.animalName ?: ""} has been treated.", animalDocRef.id)
                Toast.makeText(requireContext(), "OPD details added successfully.", Toast.LENGTH_SHORT).show()
                clearData()
                findNavController().popBackStack()
            } else {
                binding.progressBar.root.visibility = View.GONE
                Log.e("NST", "Exception: Animal Profile BatchTask: ${batchTask.exception?.localizedMessage}")
            }
        }
    }

    private fun clearData() {
        AppDelegate.animalModel.cameraImage = null
        AppDelegate.animalModel.galleryImage = null
        AppDelegate.isImageSelected = false
        AppDelegate.profileLeadsImage = ""

        AppDelegate.animalModel.name = ""
        AppDelegate.animalModel.type = "IPD"
        AppDelegate.animalModel.description = ""
        AppDelegate.animalModel.gender = "Male"
        AppDelegate.animalModel.species = "Dog"
        AppDelegate.animalModel.latitude = 0.0
        AppDelegate.animalModel.longitude = 0.0
        AppDelegate.animalModel.locationAddress = ""
        binding.notApplicableCB.isChecked = false
        AppDelegate.animalModel.isArchive = false
        viewModel.resetSelection()
    }

    private fun checkValidation(): Boolean {
        if (admissionDate == null) {
            binding.dateET.requestFocus()
            binding.dateET.error = "Treatment date is required."
            Toast.makeText(requireContext(), "Treatment date is required.", Toast.LENGTH_SHORT).show()
            return false
        } else {
            binding.dateET.error = null
        }

        if (viewModel.getReportingPerson() == null) {
            binding.personET.requestFocus()
            binding.personET.error = "Reporting person is required."
            return false
        } else {
            binding.personET.error = null
        }

        if (!binding.notApplicableCB.isChecked) {
            if (viewModel.getSelectedMedicalCondition().isEmpty()) {
                binding.conditionsET.requestFocus()
                binding.conditionsET.error = "Select medical conditions."
                Toast.makeText(requireContext(), "Please add medical conditions.", Toast.LENGTH_SHORT).show()
                return false
            } else {
                binding.conditionsET.error = null
            }
        } else {
            binding.conditionsET.error = null
        }
        return true
    }
    private fun onClickListeners() {

        binding.saveBTN.setOnClickListener {
            if (Helper.isInternetAvailable(requireContext())) {
                if (checkValidation()) {
                    if (args.from == "Profile") {
                        checkAnimalNameInDatabase()
                    } else {
                        addTreatmentDetails()
                    }
                }
            } else {
                Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
            }
        }
        binding.personET.setOnClickListener {
            it.findNavController().navigate(R.id.reportingPersonsListFragment, Bundle().apply { putString("From", "Others") })
        }

        binding.addMedicalConditionsBTN.setOnClickListener {
            it.findNavController().navigate(R.id.medicalConditionListFragment)
        }

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
    }

    private fun setupUI() {
        setupAdmissionDatePicker()
        configureReportingPersonWidget(reportingPerson)
    }

    private fun setupAdmissionDatePicker() {
        binding.dateET.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                admissionDate = calendar.time
                binding.dateET.setText(dateFormat.format(calendar.time))
            }

            val calendarForDateToSet = Calendar.getInstance()
            admissionDate?.let { date ->
                calendarForDateToSet.time = date
            }

            val dialog = DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendarForDateToSet.get(Calendar.YEAR),
                calendarForDateToSet.get(Calendar.MONTH),
                calendarForDateToSet.get(Calendar.DAY_OF_MONTH)
            )

            calendarForDateToSet.set(2022, 3, 1)  // To start the date picker from 1.4.2022.
            dialog.datePicker.minDate = calendarForDateToSet.timeInMillis
            dialog.datePicker.maxDate = System.currentTimeMillis()  // To disable the days after today.
            dialog.show()
        }
    }

    private fun configureReportingPersonWidget(rPerson: GenericMemberDTO?) {
        rPerson?.let {
            binding.personET.setText(it.name)
            if (it.phoneNumber.contains("+")) {
                // Manthan:
                // Remove Country code
                binding.numberET.setText(it.phoneNumber.substring(it.phoneNumber.length - 10))
            } else {
                binding.numberET.setText(it.phoneNumber)
            }
        }
    }
}