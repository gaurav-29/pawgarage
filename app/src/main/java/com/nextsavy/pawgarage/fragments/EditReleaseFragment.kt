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
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentEditReleaseBinding
import com.nextsavy.pawgarage.models.FeedType
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.models.ReleaseDTO
import com.nextsavy.pawgarage.retrofit.RetrofitClient
import com.nextsavy.pawgarage.retrofit.RetrofitInterface
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAdopters
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionFeedEvents
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionRelease
import com.nextsavy.pawgarage.utils.CollectionReleaseStatus
import com.nextsavy.pawgarage.utils.CollectionTreatment
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.Constants
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.SharedPrefKeys
import com.nextsavy.pawgarage.viewModels.SharedViewModel
import retrofit2.Call
import retrofit2.Callback
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditReleaseFragment : Fragment() {

    private lateinit var binding: FragmentEditReleaseBinding
    private val args: EditReleaseFragmentArgs by navArgs()

    private var releaseDocId: String? = null
    private var animalDocId: String? = null
    private var animalName: String = ""

    private var releaseDTO: ReleaseDTO? = null
    private var adopterDTO: GenericMemberDTO? = null

    private var releaseDate: Date? = null

    private var statusList = listOf<String>()
    private var releaseStatus: String? = null

    private val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.US)

    val db = Firebase.firestore

    lateinit var retrofitInterface: RetrofitInterface

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditReleaseBinding.inflate(inflater, container, false)

        registerObserver()

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().popBackStack()
        }

        releaseDocId = args.releaseId
        animalDocId = args.animalDocId

        if (releaseDocId != null && animalDocId != null) {

            setupUI()

            if (releaseDTO == null) {
                getReleaseDetails()
            } else {
                configureAdopterWidget(adopterDTO)
                setupAndConfigureCreatedByUI()
                setupAndConfigureUpdatedByUI()
            }
        }

        animalName = args.animalName ?: ""
        binding.toolbarOne.titleToolbarOne.text = animalName
        binding.releaseTV.text = "Status Details ${args.totalReleaseCount - args.currentReleaseIndex}"
        binding.countryCodeET.setText(CollectionUser.COUNTRY_CODE)


        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.locationET.setText(AppDelegate.animalModel.locationAddress)
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedViewModel.setAdopter(null)
    }

    private fun registerObserver() {
        sharedViewModel.adopter.observe(viewLifecycleOwner, Observer {
            configureAdopterWidget(it)
        })
    }

    private fun configureAdopterWidget(adopter: GenericMemberDTO?) {
        adopter?.let {
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

    private fun getReleaseDetails() {
        if (Helper.isInternetAvailable(requireContext())) {

            binding.progressBar2.visibility = View.VISIBLE

            db.collection(CollectionRelease.name).document(releaseDocId!!)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    releaseDTO = ReleaseDTO.create(documentSnapshot.id, documentSnapshot.data)
                    configureUI()
                    releaseDTO?.adopterId?.let { adopterId ->
                        getAdoptersDetails(adopterId)
                    }
                    binding.progressBar2.visibility = View.GONE
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("REL", "get failed with ", exception)
                    Toast.makeText(requireContext(), exception.message, Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        setupReleaseDatePicker()

        setupAndConfigureReleaseStatusSpinner()

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }

        binding.locationET.setOnClickListener(locationTapped)

        binding.saveBTN.setOnClickListener(saveButtonTapped)

        binding.personET.setOnClickListener {
            it.findNavController().navigate(R.id.adopterListFragment, Bundle().apply { putBoolean("allowPicking", true) })
        }
    }

    private val saveButtonTapped = View.OnClickListener {
        if (Helper.isInternetAvailable(requireContext())) {
            if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                if (checkValidation()) {
                    if (binding.spinnerStatus.selectedItem.toString() == CollectionReleaseStatus.DEATH) {
                        Constants.showAlertWithListeners(
                            requireContext(),
                            "",
                            "This operation can not be revert back. Are you sure you want to set Animal status to Death?",
                            "Yes",
                            {_, _ -> updateReleaseDetailsToDatabase() },
                            "No",
                            {_, _ -> }
                        )
                    } else {
                        updateReleaseDetailsToDatabase()
                    }
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureUI() {
        if (releaseDTO != null) {
            if (releaseDTO!!.releaseStatus == CollectionReleaseStatus.DEATH ||
                AppDelegate.state == CollectionAnimals.TERMINATED ||
                (args.totalReleaseCount > 1 && args.currentReleaseIndex != 0)) {
                binding.dateET.isEnabled = false
                binding.spinnerStatus.isEnabled = false
                binding.personET.isEnabled = false
                binding.numberET.isEnabled = false
                binding.locationET.isEnabled = false
                binding.note2ET.isEnabled = false
                binding.saveBTN.visibility = View.GONE
            }

            setupAndConfigureCreatedByUI()
            setupAndConfigureUpdatedByUI()

            releaseDate = releaseDTO?.releaseDate?.toDate()
            configureDatePicker()

            releaseStatus = releaseDTO?.releaseStatus
            if (statusList.isEmpty()) {
                initiateAndConfigureReleaseStatusSpinner()
            }

            AppDelegate.animalModel.locationAddress = releaseDTO!!.address ?: ""
            AppDelegate.animalModel.latitude = releaseDTO!!.latitude ?: 0.0
            AppDelegate.animalModel.longitude = releaseDTO!!.longitude ?: 0.0

            configureLocationAddress()

            configureCommentTV()
        }
    }

    private val locationTapped  = View.OnClickListener {
        it.findNavController().navigate(R.id.locationFragment,
            bundleOf("latitude" to 0f  , "longitude" to 0f)
        )
    }

    private fun configureLocationAddress() {
        binding.locationET.setText(AppDelegate.animalModel.locationAddress)
    }

    private fun configureCommentTV() {
        releaseDTO?.comment?.let { comment ->
            binding.note2ET.setText(comment)
        }
    }

    private fun setupReleaseDatePicker() {
        binding.dateET.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                releaseDate = calendar.time
                binding.dateET.setText(dateFormat.format(calendar.time))
            }

            val calendarForDateToSet = Calendar.getInstance()
            if (releaseDate != null) {
                calendarForDateToSet.time = releaseDate!!
            }

            val dialog = DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendarForDateToSet.get(Calendar.YEAR),
                calendarForDateToSet.get(Calendar.MONTH),
                calendarForDateToSet.get(Calendar.DAY_OF_MONTH)
            )

            val lastAdmissionDate: Date = dateFormat.parse(args.lastAdmissionDate) as Date
            dialog.datePicker.minDate = lastAdmissionDate.time
            dialog.datePicker.maxDate = System.currentTimeMillis()  // To disable the days after today.
            dialog.show()
        }
    }

    private fun configureDatePicker() {
        releaseDate?.let {
            binding.dateET.setText(dateFormat.format(releaseDate!!))
        }
    }

    private fun setupAndConfigureReleaseStatusSpinner() {
        binding.spinnerStatus.onItemSelectedListener = releaseStatusChangeListener

        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, statusList)
        binding.spinnerStatus.adapter = adapter
        val index = adapter.getPosition(releaseStatus)
        binding.spinnerStatus.setSelection(index)
    }

    private fun initiateAndConfigureReleaseStatusSpinner() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            db.collection(CollectionReleaseStatus.name)
                .get()
                .addOnSuccessListener { result ->
                    statusList = result.documents.map { it.id }.asReversed()
                    binding.progressBar2.visibility = View.GONE

                    val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, statusList)
                    binding.spinnerStatus.adapter = adapter

                    val index = adapter.getPosition(releaseDTO!!.releaseStatus)
                    binding.spinnerStatus.setSelection(index)
                    Log.e("NST-M", "Previous Selected released status: $releaseStatus")
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("ERROR", "Error getting documents: $exception")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private val releaseStatusChangeListener = object: AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            releaseStatus = binding.spinnerStatus.selectedItem.toString()
            releaseStatusDidUpdate()
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
    }

    private fun releaseStatusDidUpdate() {
        if (releaseStatus != null) {
            if (releaseStatus!! == CollectionReleaseStatus.ADOPTED) {
                binding.locationTV.visibility = View.GONE
                binding.locationET.visibility = View.GONE

                binding.note2TV.visibility = View.GONE
                binding.note2ET.visibility = View.GONE

                binding.personTV.visibility = View.VISIBLE
                binding.personET.visibility = View.VISIBLE
                binding.numberTV.visibility = View.VISIBLE
                binding.numberLL.visibility = View.VISIBLE
            }
            if (releaseStatus!! == CollectionReleaseStatus.RELEASED) {
                binding.personTV.visibility = View.GONE
                binding.personET.visibility = View.GONE
                binding.numberTV.visibility = View.GONE
                binding.numberLL.visibility = View.GONE

                binding.note2TV.visibility = View.GONE
                binding.note2ET.visibility = View.GONE

                binding.locationTV.visibility = View.VISIBLE
                binding.locationET.visibility = View.VISIBLE
            }
            if (releaseStatus!! == CollectionReleaseStatus.DEATH) {
                binding.locationTV.visibility = View.GONE
                binding.locationET.visibility = View.GONE

                binding.personTV.visibility = View.GONE
                binding.personET.visibility = View.GONE
                binding.numberTV.visibility = View.GONE
                binding.numberLL.visibility = View.GONE

                binding.note2TV.visibility = View.VISIBLE
                binding.note2ET.visibility = View.VISIBLE
            }
        }
    }

    private fun getAdoptersDetails(adopterId: String) {
        Firebase.firestore.collection(CollectionAdopters.name)
            .document(adopterId)
            .get()
            .addOnSuccessListener { docSnapshot ->
                adopterDTO = GenericMemberDTO.create(docSnapshot.id, docSnapshot.data)
                sharedViewModel.setAdopter(adopterDTO)
                binding.progressBar2.visibility = View.GONE
            }.addOnFailureListener { e ->
                binding.progressBar2.visibility = View.GONE
            }
    }

    private fun setupAndConfigureCreatedByUI() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            if (releaseDTO?.createdBy != null) {
                db.collection(CollectionUser.name)
                    .document(releaseDTO!!.createdBy)
                    .get()
                    .addOnSuccessListener { document ->
                        binding.progressBar2.visibility = View.GONE
                        if (document.data != null) {
                            binding.createdTV.visibility = View.VISIBLE
                            binding.createdET.visibility = View.VISIBLE
                            binding.createdET.setText(document.data?.get(CollectionUser.kUserName) as String)
                        } else {
                            binding.createdTV.visibility = View.GONE
                            binding.createdET.visibility = View.GONE
                        }
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar2.visibility = View.GONE
                        binding.createdTV.visibility = View.GONE
                        binding.createdET.visibility = View.GONE
                        Log.e("UPDATED", "Error getting documents: ${exception.message}")
                        Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                binding.progressBar2.visibility = View.GONE
                binding.createdTV.visibility = View.GONE
                binding.createdET.visibility = View.GONE
            }
        } else {
            binding.progressBar2.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
            binding.createdTV.visibility = View.GONE
            binding.createdET.visibility = View.GONE
        }
    }

    private fun setupAndConfigureUpdatedByUI() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            if (releaseDTO?.updatedBy != null) {
                db.collection(CollectionUser.name)
                    .document(releaseDTO!!.updatedBy!!)
                    .get()
                    .addOnSuccessListener { document ->
                        binding.progressBar2.visibility = View.GONE
                        if (document != null) {
                            binding.updateTV.visibility = View.VISIBLE
                            binding.updateET.visibility = View.VISIBLE
                            binding.updateET.setText(document.data?.get(CollectionUser.kUserName) as String)
                        } else {
                            binding.updateTV.visibility = View.GONE
                            binding.updateET.visibility = View.GONE
                        }
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar2.visibility = View.GONE
                        binding.updateTV.visibility = View.GONE
                        binding.updateET.visibility = View.GONE
                        Log.e("UPDATED", "Error getting documents: ${exception.message}")
                        Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                binding.progressBar2.visibility = View.GONE
                binding.updateTV.visibility = View.GONE
                binding.updateET.visibility = View.GONE
            }
        } else {
            binding.progressBar2.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
            binding.updateTV.visibility = View.GONE
            binding.updateET.visibility = View.GONE
        }
    }

    private fun updateReleaseDetailsToDatabase() {
        binding.progressBar.root.visibility = View.VISIBLE

        val releaseDocRef = Firebase.firestore.collection(CollectionRelease.name).document(releaseDocId!!)

        val notificationDocRef = db.collection(CollectionNotifications.name).document()
        val notificationData = hashMapOf(
            CollectionNotifications.kAnimalDocId to args.animalDocId,
            CollectionNotifications.kNotificationDate to FieldValue.serverTimestamp(),
            CollectionNotifications.kNotificationType to binding.spinnerStatus.selectedItem.toString(),
            CollectionNotifications.kNotificationSubtype to null,
            CollectionNotifications.kNotificationTypeObjectId to releaseDocRef.id,
            CollectionNotifications.kIsArchive to false,
            CollectionNotifications.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionNotifications.kCreatedBy to Firebase.auth.currentUser?.uid,
        )

        var releaseData = hashMapOf<String, Any?>()

        if (releaseStatus!! == CollectionReleaseStatus.RELEASED) {
            releaseData = hashMapOf(
                CollectionRelease.kReleasedDate to releaseDate,
                CollectionRelease.kReleasedStatus to releaseStatus,
                CollectionRelease.kLatitude to AppDelegate.animalModel.latitude,
                CollectionRelease.kLongitude to AppDelegate.animalModel.longitude,
                CollectionRelease.kLocationAddress to AppDelegate.animalModel.locationAddress,
                CollectionRelease.kIsArchive to false,
                CollectionRelease.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionRelease.kUpdatedBy to Firebase.auth.currentUser?.uid,
            )
        } else if (releaseStatus!! == CollectionReleaseStatus.ADOPTED) {
            releaseData = hashMapOf(
                CollectionRelease.kReleasedDate to releaseDate,
                CollectionRelease.kReleasedStatus to releaseStatus,
                CollectionRelease.kAdopterId to sharedViewModel.getAdopter()!!.id,
                CollectionRelease.kIsArchive to false,
                CollectionRelease.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionRelease.kUpdatedBy to Firebase.auth.currentUser?.uid,
            )
        } else if (releaseStatus!! == CollectionReleaseStatus.DEATH) {
            releaseData = hashMapOf(
                CollectionRelease.kReleasedDate to releaseDate,
                CollectionRelease.kReleasedStatus to releaseStatus,
                CollectionRelease.kIsArchive to false,
                CollectionRelease.kComment to binding.note2ET.text.trim().toString(),
                CollectionRelease.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionRelease.kUpdatedBy to Firebase.auth.currentUser?.uid,
            )
        }

        val releaseFeedEventDocRef = Firebase.firestore.collection(CollectionFeedEvents.name).document()
        val releaseFeedEventData = hashMapOf(
            CollectionFeedEvents.kAnimalDocId to args.animalDocId!!,
            CollectionFeedEvents.kFeedType to getFeedType(releaseStatus!!),
            CollectionFeedEvents.kFeedObjectId to releaseDocRef.id, // Pass Release Document Id
            CollectionTreatment.kIsArchive to false,
            CollectionTreatment.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionTreatment.kCreatedBy to Firebase.auth.currentUser?.uid,
        )

        val batch = Firebase.firestore.batch()
        batch.update(releaseDocRef, releaseData)

        // Set animal dead if new release status is DEATH
        if (releaseStatus!! == CollectionReleaseStatus.DEATH) {
            // No need for this method because reminders will not be shown, if animal is_dead=true in animal profile. See code in Reminders Fragment.
            // turnOffReminders()
            val animalDocRef = Firebase.firestore.collection(CollectionAnimals.name).document(args.animalDocId!!)
            val animalData = hashMapOf(
                CollectionAnimals.kIsDead to true,
                CollectionAnimals.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionAnimals.kUpdatedBy to Firebase.auth.currentUser?.uid
            )
            batch.update(animalDocRef, animalData)
        }

        // Create new Feed and Send push notification only if current Release status is changed from previous one
        if (releaseStatus!! != releaseDTO!!.releaseStatus) {
            batch.set(notificationDocRef, notificationData)
            batch.set(releaseFeedEventDocRef, releaseFeedEventData)
        }

        batch.commit().addOnCompleteListener { batchTask ->
            if (batchTask.isSuccessful) {
                binding.progressBar.root.visibility = View.GONE
                if (releaseStatus!! != releaseDTO!!.releaseStatus) {
                    val userName = Helper.sharedPreference?.getString(SharedPrefKeys.kUserName, null)
                    if (releaseStatus!! == CollectionReleaseStatus.ADOPTED) {
                        val message = "$animalName has been ${releaseStatus!!.lowercase()} by ${sharedViewModel.getAdopter()!!.name}." + if (userName != null) "\n${userName}" else ""
                        sendPushNotification(message, args.animalDocId!!)
                    } else if (releaseStatus!! == CollectionReleaseStatus.RELEASED) {
                        val message = "$animalName has been ${releaseStatus!!.lowercase()}." + if (userName != null) "\n${userName}" else ""
                        sendPushNotification(message, args.animalDocId!!)
                    } else if (releaseStatus!! == CollectionReleaseStatus.DEATH) {
                        val message = "$animalName has died." + if (userName != null) "\n${userName}" else ""
                        sendPushNotification(message, args.animalDocId!!)
                    }
                }
                Toast.makeText(requireContext(), "Status schedule updated successfully.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                binding.progressBar.root.visibility = View.GONE
                Log.e("NST", "Exception: Edit Release Details BatchTask: ${batchTask.exception?.localizedMessage}")
            }
        }
    }

    private fun checkValidation(): Boolean {
        val comment = binding.note2ET.text.trim().toString()
        val location = binding.locationET.text.trim().toString()

        if (releaseDate == null) {
            binding.dateET.requestFocus()
            binding.dateET.error = "Date is required."
            Toast.makeText(requireContext(), "Date is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (releaseStatus == null) {
            Toast.makeText(requireContext(), "Please select status", Toast.LENGTH_SHORT).show()
            return false
        }
        if (releaseStatus!! == CollectionReleaseStatus.RELEASED) {
            if (location.isBlank()) {
                binding.locationET.requestFocus()
                binding.locationET.error = "Location is required."
                Toast.makeText(requireContext(), "Location is required", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        if (releaseStatus!! == CollectionReleaseStatus.DEATH) {
            if (comment.isBlank()) {
                binding.note2ET.requestFocus()
                binding.note2ET.error = "Comment is required."
                return false
            }
        }
        if (releaseStatus!! == CollectionReleaseStatus.ADOPTED) {
            if (sharedViewModel.getAdopter() == null) {
                binding.personET.requestFocus()
                binding.personET.error = "Name of adopter is required."
                return false
            }
        }
        return true
    }

    private fun sendPushNotification(message: String, animalDocIdForDeepLink: String) {

        val baseUrl = "https://fcm.googleapis.com/"
        retrofitInterface = RetrofitClient.getRetrofitInstance(baseUrl).create(RetrofitInterface::class.java)

        if (Helper.isInternetAvailable(requireContext())) {

            val payLoad = JsonObject()
            val notification = JsonObject()
            val data = JsonObject()

            try {
                payLoad.addProperty("to", "/topics/" + CollectionNotifications.TOPIC)
                notification.addProperty("title", "Status")
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

    private fun getFeedType(releaseType: String): String {
        when (releaseType) {
            CollectionReleaseStatus.RELEASED -> {
                return FeedType.RELEASE.value
            }
            CollectionReleaseStatus.ADOPTED -> {
                return FeedType.ADOPTED.value
            }
            CollectionReleaseStatus.DEATH -> {
                return FeedType.DEATH.value
            }
            else -> {
                return ""
            }
        }
    }

}