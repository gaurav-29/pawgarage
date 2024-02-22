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
import com.nextsavy.pawgarage.databinding.FragmentAddReleaseDetailsBinding
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.FeedType
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.retrofit.RetrofitClient
import com.nextsavy.pawgarage.retrofit.RetrofitInterface
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionFeedEvents
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionRelease
import com.nextsavy.pawgarage.utils.CollectionReleaseStatus
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

class AddReleaseDetailsFragment : Fragment() {

    private lateinit var binding: FragmentAddReleaseDetailsBinding
    private val args: AddReleaseDetailsFragmentArgs by navArgs()

    private var cal = Calendar.getInstance()

    private val db = Firebase.firestore

    lateinit var retrofitInterface: RetrofitInterface

    private var animalDTO: AnimalDTO? = null

    private var statusList = listOf<String>()
    private var releaseStatus: String? = null

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddReleaseDetailsBinding.inflate(inflater, container, false)

        registerObserver()

        binding.toolbarOne.titleToolbarOne.text = args.animalName ?: ""
        binding.countryCodeET.setText(CollectionUser.COUNTRY_CODE)
        binding.releaseTV.text = "Status Details ${args.releaseNumber+1}"

        if (animalDTO == null) {
            getAnimalDetails()
        } else {
            configureUI()
        }


        if (statusList.isEmpty()) {
            setupReleaseStatusSpinner()
            initiateAndConfigureReleaseStatusSpinner()
        } else {
            setupReleaseStatusSpinner()
            configureReleaseStatusSpinner()
        }

        onClickListeners()

        return binding.root
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

    private fun getAnimalDetails() {
        if (Helper.isInternetAvailable(requireContext())) {
            db.collection(CollectionAnimals.name)
                .document(args.animalDocId!!)
                .get()
                .addOnSuccessListener { document ->
                    animalDTO = AnimalDTO.create(document.id, document.data)
                    configureUI()
                }.addOnFailureListener { exception ->
                    Log.e("NAME", "Animal name failure: ", exception)
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureUI() {
        animalDTO?.let {
            binding.locationET.setText(it.address)
        }
    }

    private fun setupReleaseStatusSpinner() {
        binding.spinnerStatus.onItemSelectedListener = releaseStatusChangeListener
    }

    private fun configureReleaseStatusSpinner() {
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, statusList)
        binding.spinnerStatus.adapter = adapter
        val index = adapter.getPosition(releaseStatus)
        binding.spinnerStatus.setSelection(index)
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

                    releaseStatus = binding.spinnerStatus.selectedItem.toString()
                }
                .addOnFailureListener { exception ->
                    binding.progressBar2.visibility = View.GONE
                    Log.e("ERROR", "Error getting documents: $exception")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun onClickListeners() {
        binding.personET.setOnClickListener {
            it.findNavController().navigate(R.id.adopterListFragment, Bundle().apply { putBoolean("allowPicking", true) })
        }

        binding.saveBTN.setOnClickListener {
            if (Helper.isInternetAvailable(requireContext())) {
                if (checkValidation()) {
                    if (sharedViewModel.getAllowRelease()) {
                        if (binding.spinnerStatus.selectedItem.toString() == CollectionReleaseStatus.DEATH) {
                            Constants.showAlertWithListeners(
                                requireContext(),
                                "",
                                "This operation can not be revert back. Are you sure you want to set Animal status to Death?",
                                "Yes",
                                { _, _ -> addReleaseDetailsToDatabase() },
                                "No",
                                { _, _ -> }
                            )
                        } else {
                            addReleaseDetailsToDatabase()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Please admit the animal.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
            }
        }

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }

        binding.locationET.setOnClickListener {
            val bundle = bundleOf("from" to "AddRelease")
            it.findNavController().navigate(R.id.action_global_locationFragment, bundle)
        }

        binding.dateET.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val sdf = SimpleDateFormat("dd/MM/yy", Locale.US)
                binding.dateET.setText(sdf.format(cal.time))
                binding.dateET.error = null
            }
            val dialog =  DatePickerDialog(requireContext(),
                dateSetListener,
                // set DatePickerDialog to point to today's date when it loads up
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH))

            val formatter = SimpleDateFormat("dd MMM, yyyy", Locale.US)
            val date2: Date = formatter.parse(args.LastAdmissionDate) as Date
            Log.e("LAST4", date2.toString())
            dialog.datePicker.minDate = date2.time

            dialog.datePicker.maxDate = System.currentTimeMillis()  // To disable the days after current day.
            dialog.show()
        }
    }

    private fun checkValidation(): Boolean {
        val date = binding.dateET.text.toString()

        val comment = binding.note2ET.text.trim().toString()

        if (animalDTO == null) {
            Toast.makeText(requireContext(), "Animal details not available!", Toast.LENGTH_SHORT).show()
            return false
        }

        if (releaseStatus == null) {
            Toast.makeText(requireContext(), "Please select status", Toast.LENGTH_SHORT).show()
            return false
        } else {
            if (date.isBlank()) {
                binding.dateET.requestFocus()
                binding.dateET.error = "Date is required."
                Toast.makeText(requireContext(), "Date is required", Toast.LENGTH_SHORT).show()
                return false
            }

            if (releaseStatus == CollectionReleaseStatus.RELEASED) {
                if (animalDTO!!.address.isBlank()) {
                    binding.locationET.requestFocus()
                    binding.locationET.error = "Location is required."
                    Toast.makeText(requireContext(), "Location is required", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            if (releaseStatus == CollectionReleaseStatus.DEATH) {
                if (comment.isEmpty() || comment.isBlank()) {
                    binding.note2ET.requestFocus()
                    binding.note2ET.error = "Comment is required."
                    return false
                }
            }
            if (releaseStatus == CollectionReleaseStatus.ADOPTED) {
                if (sharedViewModel.getAdopter() == null) {
                    binding.personET.error = "Name of adopter is required."
                    return false
                } else {
                    binding.personET.error = null
                }
            }
        }

        return true
    }

    private fun addReleaseDetailsToDatabase() {
        binding.progressBar.root.visibility = View.VISIBLE

        var releaseData = hashMapOf<String, Any?>()

        val releaseDocRef = Firebase.firestore.collection(CollectionRelease.name).document()

        if (releaseStatus!! == CollectionReleaseStatus.RELEASED) {
            releaseData = hashMapOf(
                CollectionRelease.kAnimalId to animalDTO!!.animalId,
                CollectionRelease.kAnimalDocId to animalDTO!!.id,
                CollectionRelease.kReleasedDate to cal.time,
                CollectionRelease.kReleasedStatus to releaseStatus!!,
                CollectionRelease.kLatitude to animalDTO!!.latitude,
                CollectionRelease.kLongitude to animalDTO!!.longitude,
                CollectionRelease.kLocationAddress to animalDTO!!.address,
                CollectionRelease.kIsArchive to false,
                CollectionRelease.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionRelease.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
        } else if (releaseStatus!! == CollectionReleaseStatus.ADOPTED) {
            releaseData = hashMapOf(
                CollectionRelease.kAnimalId to animalDTO!!.animalId,
                CollectionRelease.kAnimalDocId to animalDTO!!.id,
                CollectionRelease.kReleasedDate to cal.time,
                CollectionRelease.kReleasedStatus to releaseStatus!!,
                CollectionRelease.kAdopterId to sharedViewModel.getAdopter()!!.id,
                CollectionRelease.kIsArchive to false,
                CollectionRelease.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionRelease.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
        } else if (releaseStatus!! == CollectionReleaseStatus.DEATH) {
            releaseData = hashMapOf(
                CollectionRelease.kAnimalId to animalDTO!!.animalId,
                CollectionRelease.kAnimalDocId to animalDTO!!.id,
                CollectionRelease.kReleasedDate to cal.time,
                CollectionRelease.kReleasedStatus to releaseStatus!!,
                CollectionRelease.kIsArchive to false,
                CollectionRelease.kComment to binding.note2ET.text.trim().toString(),
                CollectionRelease.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionRelease.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
        }

        val notificationDocRef = db.collection(CollectionNotifications.name).document()
        val notificationData = hashMapOf(
            CollectionRelease.kAnimalDocId to animalDTO!!.id,
            CollectionNotifications.kNotificationDate to FieldValue.serverTimestamp(),
            CollectionNotifications.kNotificationType to releaseStatus!!,
            CollectionNotifications.kNotificationTypeObjectId to releaseDocRef.id,
            CollectionNotifications.kIsArchive to false,
            CollectionNotifications.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionNotifications.kCreatedBy to Firebase.auth.currentUser?.uid,
        )

        val releaseFeedEventDocRef = Firebase.firestore.collection(CollectionFeedEvents.name).document()
        val releaseFeedEventData = hashMapOf(
            CollectionFeedEvents.kAnimalDocId to animalDTO!!.id,
            CollectionFeedEvents.kFeedType to getFeedType(releaseStatus!!),
            CollectionFeedEvents.kFeedObjectId to releaseDocRef.id, // Pass Release Document Id
            CollectionFeedEvents.kIsArchive to false,
            CollectionFeedEvents.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionFeedEvents.kCreatedBy to Firebase.auth.currentUser?.uid,
        )

        val animalDocRef = Firebase.firestore.collection(CollectionAnimals.name).document(animalDTO!!.id)
        val animalData: HashMap<String, Any?>
        if (releaseStatus!! == CollectionReleaseStatus.DEATH) {
            animalData = hashMapOf(
                CollectionAnimals.kIsDead to true,
                CollectionAnimals.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionAnimals.kUpdatedBy to Firebase.auth.currentUser?.uid
            )
        } else {
            // Manthan: Set Animal type to OPD, once it is released (NOT DEAD!).
            animalData = hashMapOf(
                CollectionAnimals.kType to CollectionAnimals.OPD,
                CollectionAnimals.kUpdatedAt to FieldValue.serverTimestamp(),
                CollectionAnimals.kUpdatedBy to Firebase.auth.currentUser?.uid
            )
        }

        val batch = Firebase.firestore.batch()

        batch.update(animalDocRef, animalData)
        batch.set(releaseDocRef, releaseData)
        batch.set(notificationDocRef, notificationData)
        batch.set(releaseFeedEventDocRef, releaseFeedEventData)

        batch.commit().addOnCompleteListener { batchTask ->
            if (batchTask.isSuccessful) {
                if (releaseStatus!! == CollectionReleaseStatus.ADOPTED) {
                    val message = "${animalDTO!!.name} has been ${releaseStatus!!.lowercase()} by ${sharedViewModel.getAdopter()!!.name}." + if (Helper.sharedPreference?.getString(SharedPrefKeys.kUserName, null) != null) "\n${Helper.sharedPreference.getString(SharedPrefKeys.kUserName, null)}" else ""
                    sendPushNotification(message, args.animalDocId!!)
                } else if (releaseStatus!! == CollectionReleaseStatus.RELEASED) {
                    val message = "${animalDTO!!.name} has been ${releaseStatus!!.lowercase()}." + if (Helper.sharedPreference?.getString(SharedPrefKeys.kUserName, null) != null) "\n${Helper.sharedPreference.getString(SharedPrefKeys.kUserName, null)}" else ""
                    sendPushNotification(message, args.animalDocId!!)
                } else if (releaseStatus!! == CollectionReleaseStatus.DEATH) {
                    val message = "${animalDTO!!.name} has died." + if (Helper.sharedPreference?.getString(SharedPrefKeys.kUserName, null) != null) "\n${Helper.sharedPreference.getString(SharedPrefKeys.kUserName, null)}" else ""
                    sendPushNotification(message, args.animalDocId!!)
                }
                binding.progressBar.root.visibility = View.GONE
                Toast.makeText(requireContext(), "Status details added successfully.", Toast.LENGTH_SHORT).show()
                clearData()
                findNavController().popBackStack()
            } else {
                binding.progressBar.root.visibility = View.GONE
                Log.e("NST", "Exception: Add Release Details BatchTask: ${batchTask.exception?.localizedMessage}")
                Toast.makeText(requireContext(), "Something went wrong.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearData() {
        binding.dateET.setText("")
        binding.spinnerStatus.setSelection(0)
        binding.personET.setText("")
        binding.numberET.setText("")
        binding.locationET.setText("")
        binding.note2ET.setText("")
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