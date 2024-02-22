package com.nextsavy.pawgarage.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentEditAnimalProfileBinding
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.FeedType
import com.nextsavy.pawgarage.retrofit.RetrofitClient
import com.nextsavy.pawgarage.retrofit.RetrofitInterface
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionArchived
import com.nextsavy.pawgarage.utils.CollectionDeworming
import com.nextsavy.pawgarage.utils.CollectionFeedEvents
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionRelease
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionTreatment
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Constants
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.utils.SharedPrefKeys
import org.apache.poi.hssf.usermodel.HSSFRow
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import retrofit2.Call
import retrofit2.Callback
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class EditAnimalProfileFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentEditAnimalProfileBinding
    private var animalDocId: String? = null
    private var animalModel: AnimalDTO? = null
    private lateinit var animalStatusSpinnerAdapter: ArrayAdapter<String>
    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val userType: String = Helper.sharedPreference?.getString("USER_TYPE", "") ?: ""
    private var nameExists: Boolean = false
    /**
     * 0 = No option selected.
     * 1 = Gallery
     * 2 = Camera
     */
    var lastSelected = 0
    var animalImagePath: Uri? = null
    var animalCapturedBitmap: Bitmap? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            Log.e("PhotoPicker", "Selected URI: $uri")
        } else {
            Log.e("PhotoPicker", "No media selected")
        }
        animalImagePath = uri
        binding.animalIV.setImageURI(animalImagePath)
    }

    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val bm = it.data?.extras?.get("data") as Bitmap
            // Below code is to solve the problem- when we capture image in portrait mode,
            // it will appear in landscape mode in imageview in Pixel 6a.
            if(bm.width > bm.height) {
                var bMapRotate: Bitmap? = null
                val mat = Matrix()
                mat.postRotate(90F)
                bMapRotate = Bitmap.createBitmap(bm, 0, 0,bm.width,bm.height, mat, true)
                bm.recycle()
                // bm = null
                animalCapturedBitmap = bMapRotate
                binding.animalIV.setImageBitmap(bMapRotate)
            } else {
                animalCapturedBitmap = bm
                binding.animalIV.setImageBitmap(bm)
            }
        } else {
            animalCapturedBitmap = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditAnimalProfileBinding.inflate(inflater, container, false)
        setupUI()
        val animalName = arguments?.getString("animalName", "") ?: ""
        binding.toolbarOne.titleToolbarOne.text = "Edit $animalName's Profile"

        animalDocId = arguments?.getString("animalDocID")

        if (animalModel == null) {
            getAnimalDetails()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.locationET.setText(AppDelegate.animalModel.locationAddress)
        when (lastSelected) {
            1 -> {
                if (animalImagePath != null) {
                    binding.animalIV.setImageURI(animalImagePath!!)
                }
            }
            2 -> {
                if (animalCapturedBitmap != null) {
                    binding.animalIV.setImageBitmap(animalCapturedBitmap!!)
                }
            }
            else -> {
                if (animalModel != null) {
                    Glide.with(requireContext()).load(animalModel!!.downloadUrl).placeholder(R.drawable.paw_placeholder).into(binding.animalIV)
                }
            }
        }
    }

    private fun setupUI() {
        setupStatusSpinner()
        binding.dogRB.setOnClickListener(dogRBTapped)
        binding.catRB.setOnClickListener(catRBTapped)
        binding.otherRB.setOnClickListener(otherRBTapped)
        binding.saveBTN.setOnClickListener(saveButtonTapped)
        // Remark: We have used ViewFlipper here!
        (binding.deleteVF[1] as RelativeLayout).setBackgroundColor(ContextCompat.getColor(binding.deleteVF.context, R.color.app_error))
        binding.deleteVF.setOnClickListener(deleteButtonTapped)
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.animalIV.setOnClickListener(this)
        binding.addImageBTN.setOnClickListener(this)
        binding.locationET.setOnClickListener {
            it.findNavController().navigate(R.id.locationFragment, bundleOf("latitude" to AppDelegate.animalModel.latitude.toFloat(), "longitude" to AppDelegate.animalModel.longitude.toFloat()))
        }
        if (userType == CollectionWhitelistedNumbers.ADMIN || userType == CollectionWhitelistedNumbers.TEAM_LEADER) {
            binding.spinnerRL.visibility = View.VISIBLE
            binding.statusTV.visibility = View.VISIBLE
        } else {
            binding.spinnerRL.visibility = View.GONE
            binding.statusTV.visibility = View.GONE
        }
    }

    private fun setupStatusSpinner() {
        val statusList = resources.getStringArray(R.array.status2)
        animalStatusSpinnerAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner, statusList)
        binding.spinnerStatus.adapter = animalStatusSpinnerAdapter
    }

    private fun getAnimalDetails() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            if (animalDocId != null) {
                Firebase.firestore
                    .collection(CollectionAnimals.name)
                    .document(this.animalDocId!!)
                    .get()
                    .addOnSuccessListener { docSnap ->
                        binding.progressBar2.visibility = View.GONE
                        this.animalModel = AnimalDTO.create(docSnap.id, docSnap.data)
                        if (animalModel != null) {
                            configureUI()
                        } else {
                            Toast.makeText(requireContext(), "Unable to fetch animal details", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar2.visibility = View.GONE
                        Log.e("NST-M", "Exception: EditAnimalProfileFrag > ${exception.localizedMessage}")
                    }
            } else {
                binding.progressBar2.visibility = View.GONE
                Toast.makeText(requireContext(), "Animal id is null. Please pass from parent fragment", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureUI() {
        Glide.with(requireContext()).load(animalModel!!.downloadUrl).placeholder(R.drawable.paw_placeholder).into(binding.animalIV)

        binding.nameET.setText(animalModel!!.name)

        binding.descriptionET.setText(animalModel!!.description)

        val index = animalStatusSpinnerAdapter.getPosition(animalModel!!.state)
        binding.spinnerStatus.setSelection(index)

        binding.locationET.setText(animalModel!!.address)
        AppDelegate.animalModel.latitude = animalModel!!.latitude
        AppDelegate.animalModel.longitude = animalModel!!.longitude
        AppDelegate.animalModel.locationAddress = animalModel!!.address

        if (animalModel!!.gender == "Male") {
            binding.genderRG.check(binding.maleRB.id)
        } else {
            binding.genderRG.check(binding.femaleRB.id)
        }

        when (animalModel!!.species) {
            "Dog" -> {
                binding.dogRB.isChecked = true
                binding.catRB.isChecked = false
                binding.otherRB.isChecked = false
            }
            "Cat" -> {
                binding.dogRB.isChecked = false
                binding.catRB.isChecked = true
                binding.otherRB.isChecked = false
            }
            else -> {
                binding.dogRB.isChecked = false
                binding.catRB.isChecked = false
                binding.otherRB.isChecked = true
            }
        }
    }

    private val dogRBTapped = View.OnClickListener {
        binding.dogRB.isChecked = true
        binding.catRB.isChecked = false
        binding.otherRB.isChecked = false
    }

    private val catRBTapped = View.OnClickListener {
        binding.dogRB.isChecked = false
        binding.catRB.isChecked = true
        binding.otherRB.isChecked = false
    }

    private val otherRBTapped = View.OnClickListener {
        binding.dogRB.isChecked = false
        binding.catRB.isChecked = false
        binding.otherRB.isChecked = true
    }

    private val saveButtonTapped = View.OnClickListener {
        if (checkValidation()) {
            val nameFromDatabase = animalModel!!.name.lowercase().replace(" ", "")
            val enteredName = binding.nameET.text.trim().toString().lowercase().replace(" ", "")

            if (animalModel!!.state != binding.spinnerStatus.selectedItem.toString() && binding.spinnerStatus.selectedItem.toString() == "Terminated") {
                Constants.showAlertWithListeners(
                    requireContext(),
                    "",
                    "Are you sure you want to set Animal status to Terminated?",
                    "Yes",
//                    {_, _ -> uploadAnimalImage2() },
                    {_, _ ->
                        if (nameFromDatabase == enteredName) {
                            uploadAnimalImage()
                        } else {
                            checkAnimalNameInDatabase()
                        }
                    },
                    "No",
                    {_, _ -> }
                )
            } else {
                if (nameFromDatabase == enteredName) {
                    uploadAnimalImage()
                } else {
                    checkAnimalNameInDatabase()
                }
            }
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
                    val enteredName = binding.nameET.text.trim().toString().lowercase().replace(" ", "")
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

    private val deleteButtonTapped = View.OnClickListener {
        Constants.showAlertWithListeners(
            requireContext(),
            "Delete",
            "Are you sure you want to Delete this Animal? This action can not be undone.",
            "Yes",
            {_, _ -> deleteAnimal() },
            "No",
            {_, _ -> }
        )
    }

    private fun deleteAnimal() {
        val batch = db.batch()
        binding.deleteVF.displayedChild = 1
        val animalData = hashMapOf(
            CollectionAnimals.kIsArchive to true,
            CollectionAnimals.kUpdatedAt to FieldValue.serverTimestamp(),
            CollectionAnimals.kUpdatedBy to Firebase.auth.currentUser?.uid,
        )
        val animalDocRef = db.collection(CollectionAnimals.name).document(animalDocId!!)
        batch.set(animalDocRef, animalData, SetOptions.merge())

        val archivedData = hashMapOf(
            CollectionArchived.kAnimalDocId to animalDocId!!,
            CollectionAnimals.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionAnimals.kCreatedBy to Firebase.auth.currentUser?.uid,
            CollectionAnimals.kUpdatedAt to FieldValue.serverTimestamp(),
            CollectionAnimals.kUpdatedBy to Firebase.auth.currentUser?.uid,
        )
        val archivedDocRef = db.collection(CollectionArchived.name).document()
        batch.set(archivedDocRef, archivedData)

        batch.commit().addOnCompleteListener { batchTask ->
            if (batchTask.isSuccessful) {
                binding.deleteVF.displayedChild = 0
                Toast.makeText(requireContext(), "Animal profile deleted successfully.", Toast.LENGTH_LONG).show()
                findNavController().popBackStack(R.id.homeFragment, false)
            } else {
                binding.deleteVF.displayedChild = 0
                Toast.makeText(requireContext(), batchTask.exception?.message, Toast.LENGTH_SHORT).show()
                Log.e("NST", "Exception: Edit Animal Profile BatchTask: ${batchTask.exception?.localizedMessage}")
            }
        }
       /*
        db.collection(CollectionAnimals.name)
            .document(animalDocId!!)
            .set(animalData, SetOptions.merge())
            .addOnSuccessListener {
                binding.deleteVF.displayedChild = 0
                Toast.makeText(requireContext(), "Animal profile deleted successfully.", Toast.LENGTH_LONG).show()
                findNavController().popBackStack(R.id.homeFragment, false)
            }
            .addOnFailureListener { e ->
                Log.e("NST-M", "EditAdminProfileFragment: Delete Animal: ${e.localizedMessage}")
                binding.deleteVF.displayedChild = 0
            }
        */
    }

    private fun uploadAnimalImage() {
        binding.progressBar.root.visibility = View.VISIBLE
        if (lastSelected == 1) {
            if (animalImagePath != null) {
                val storageRef = storage.reference
                val imageRef = storageRef.child("Images")
                val childRef = imageRef.child("Animal Images")
                val animalRef = childRef.child("$${animalModel!!.animalId}.jpg")
                val uploadTask = animalRef.putFile(animalImagePath!!)
                try {
                    uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let {
                                throw it
                            }
                        }
                        return@Continuation animalRef.downloadUrl
                    }).addOnCompleteListener { uriTask ->
                        if (uriTask.isSuccessful) {
                            Log.e("NST", uriTask.result.toString())
                            updateAnimalDataToDatabase(uriTask.result.toString())
                        } else {
                            uriTask.exception?.let {
                                throw it
                            }
                        }
                    }
                } catch (exception: Exception) {
                    Log.e("NST", "Edit Animal Image Exception: ${exception.message.toString()}")
                    updateAnimalDataToDatabase("")
                }
            } else {
                updateAnimalDataToDatabase("")
            }
        } else if (lastSelected == 2) {
            if (animalCapturedBitmap != null) {
                val baos = ByteArrayOutputStream()
                animalCapturedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()
                val storageRef = storage.reference
                val imageRef = storageRef.child("Images")
                val childRef = imageRef.child("Animal Images")
                val animalRef = childRef.child("$${animalModel!!.animalId}.jpg")
                val uploadTask = animalRef.putBytes(data)
                try {
                    uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let {
                                throw it
                            }
                        }
                        return@Continuation animalRef.downloadUrl
                    }).addOnCompleteListener { uriTask ->
                        if (uriTask.isSuccessful) {
                            Log.e("NST", uriTask.result.toString())
                            updateAnimalDataToDatabase(uriTask.result.toString())
                        } else {
                            uriTask.exception?.let {
                                throw it
                            }
                        }
                    }
                } catch (exception: Exception) {
                    Log.e("NST", "Edit Animal Exception: ${exception.message.toString()}")
                    updateAnimalDataToDatabase("")
                }
            } else {
                updateAnimalDataToDatabase("")
            }
        } else {
            updateAnimalDataToDatabase("")
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

    private fun updateAnimalDataToDatabase(stringUrl: String) {
        val batch = Firebase.firestore.batch()
        val textKeywords = generateSearchKeywords(binding.nameET.text.trim().toString())
        val animalData = hashMapOf(
            CollectionAnimals.kName to binding.nameET.text.trim().toString(),
            CollectionAnimals.kSearchKeywords to textKeywords,
            CollectionAnimals.kDescription to binding.descriptionET.text.trim().toString(),
            CollectionAnimals.kState to binding.spinnerStatus.selectedItem.toString(),
            CollectionAnimals.kGender to if (binding.genderRG.checkedRadioButtonId == binding.maleRB.id) "Male" else "Female",
            CollectionAnimals.kSpecies to if (binding.dogRB.isChecked) "Dog" else if (binding.catRB.isChecked) "Cat" else "Other",
            CollectionAnimals.kLatitude to AppDelegate.animalModel.latitude,
            CollectionAnimals.kLongitude to AppDelegate.animalModel.longitude,
            CollectionAnimals.kLocationAddress to AppDelegate.animalModel.locationAddress,
            CollectionAnimals.kUpdatedAt to FieldValue.serverTimestamp(),
            CollectionAnimals.kUpdatedBy to Firebase.auth.currentUser?.uid,
        )
        if (stringUrl != "") {
            animalData[CollectionAnimals.kDownloadUrl] = stringUrl
        }
        val animalDocRef = db.collection(CollectionAnimals.name).document(animalDocId!!)
        batch.set(animalDocRef, animalData, SetOptions.merge())

        // Check if State is changed
        val feedEventData = hashMapOf<String, Any?>()
        if (animalModel!!.state != binding.spinnerStatus.selectedItem.toString()) {
            feedEventData[CollectionFeedEvents.kAnimalDocId] = animalDocId!!
            feedEventData[CollectionFeedEvents.kFeedObjectId] = null
            feedEventData[CollectionTreatment.kIsArchive] = false
            feedEventData[CollectionTreatment.kCreatedAt] = FieldValue.serverTimestamp()
            feedEventData[CollectionTreatment.kCreatedBy] = Firebase.auth.currentUser?.uid
            if (binding.spinnerStatus.selectedItem.toString() == "Terminated") {
                feedEventData[CollectionFeedEvents.kFeedType] = FeedType.TERMINATED.value
            } else {
                feedEventData[CollectionFeedEvents.kFeedType] = FeedType.ACTIVATED.value
            }
            val feedEventDocRef = Firebase.firestore.collection(CollectionFeedEvents.name).document()
            batch.set(feedEventDocRef, feedEventData, SetOptions.merge())

            // Notification
            val notificationDocRef = db.collection(CollectionNotifications.name).document()
            val notificationType = if (binding.spinnerStatus.selectedItem.toString() == "Terminated") {
                CollectionNotifications.TERMINATED
            } else {
                CollectionNotifications.ACTIVATED
            }
            val notificationData = hashMapOf(
                CollectionRelease.kAnimalDocId to animalDocId!!,
                CollectionNotifications.kNotificationDate to FieldValue.serverTimestamp(),
                CollectionNotifications.kNotificationType to notificationType,
                CollectionNotifications.kIsArchive to false,
                CollectionNotifications.kCreatedAt to FieldValue.serverTimestamp(),
                CollectionNotifications.kCreatedBy to Firebase.auth.currentUser?.uid,
            )
            batch.set(notificationDocRef, notificationData, SetOptions.merge())
        }

        batch.commit().addOnCompleteListener { batchTask ->
            if (batchTask.isSuccessful) {
                if (feedEventData[CollectionFeedEvents.kFeedType] != null && feedEventData[CollectionFeedEvents.kFeedType] == FeedType.TERMINATED.value) {
                    AppDelegate.state = CollectionAnimals.TERMINATED
//                    turnOffReminders()
                    val message = "${animalModel!!.name} has been terminated."
                    sendPushNotification(message, animalDocId!!)
                } else if (feedEventData[CollectionFeedEvents.kFeedType] != null && feedEventData[CollectionFeedEvents.kFeedType] == FeedType.ACTIVATED.value) {
                    val message = "${animalModel!!.name} has been activated."
                    sendPushNotification(message, animalDocId!!)
                    AppDelegate.state = CollectionAnimals.ACTIVE
                }
                executeAnimalUpdateSuccess()
            } else {
                binding.progressBar.root.visibility = View.GONE
                Toast.makeText(requireContext(), batchTask.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Manthan: 23-01-2024
    // Do not Turn Off reminder if Animal is terminated.
    // Instead, do not show the reminders of Terminated animals in Reminders list

    // Turn Off Reminders for this animal if status is set to Terminated
    private fun turnOffReminders() {
        FirebaseFirestore
            .getInstance()
            .collection(CollectionReminders.name)
            .whereEqualTo(CollectionReminders.kAnimalDocId, animalDocId!!)
            .whereEqualTo(CollectionReminders.kIsArchive, false)
            .whereEqualTo(CollectionReminders.kIsComplete, false)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    executeAnimalUpdateSuccess()
                } else {
                    val updateRemindersData = hashMapOf(
                        CollectionReminders.kIsComplete to true,
                        CollectionReminders.kUpdatedAt to FieldValue.serverTimestamp(),
                        CollectionReminders.kUpdatedBy to Firebase.auth.currentUser?.uid
                    )
                    val batch = Firebase.firestore.batch()
                    for (doc in querySnapshot.documents) {
                        batch.set(db.collection(CollectionReminders.name).document(doc.id), updateRemindersData, SetOptions.merge())
                    }
                    batch.commit().addOnCompleteListener { batchTask ->
                        if (batchTask.isSuccessful) {
                            executeAnimalUpdateSuccess()
                        } else {
                            binding.progressBar.root.visibility = View.GONE
                            Toast.makeText(requireContext(), batchTask.exception?.message, Toast.LENGTH_SHORT).show()
                            Log.e("NST-M", "Exception: EditAnimalProfileFrag > Turn off reminders > ${batchTask.exception?.message}")
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.root.visibility = View.GONE
                Log.e("NST-M", "Exception: EditAnimalProfileFrag > Turn off reminders > ${exception.localizedMessage}")
                Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_SHORT).show()
            }
    }

    private fun executeAnimalUpdateSuccess() {
        binding.progressBar.root.visibility = View.GONE
        Toast.makeText(requireContext(), "Animal profile updated successfully.", Toast.LENGTH_SHORT).show()
        Log.e("DATA", "Animal profile updated successfully.")
        clearData()
        findNavController().popBackStack()
    }

    private fun sendPushNotification(message: String, animalDocIdForDeepLink: String) {
        val baseUrl = "https://fcm.googleapis.com/"
        val retrofitInterface: RetrofitInterface = RetrofitClient.getRetrofitInstance(baseUrl).create(RetrofitInterface::class.java)

        if (Helper.isInternetAvailable(requireContext())) {

            val payLoad = JsonObject()
            val notification = JsonObject()
            val data = JsonObject()

            try {
                notification.addProperty("title", "Status")
                notification.addProperty("body", message)

                data.addProperty("animal_doc_id", animalDocIdForDeepLink)

                payLoad.addProperty("to", "/topics/" + CollectionNotifications.TOPIC)
                payLoad.add("notification", notification)
                payLoad.add("data", data)
            } catch (e: JsonIOException) {
                Log.e("TAG", "EditAnimalProfileFrag sendPushNotification(): " + e.message)
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

    private fun clearData() {
        binding.nameET.setText("")
        binding.descriptionET.setText("")
        binding.spinnerStatus.setSelection(0)

        binding.animalIV.setImageURI(null)
        binding.animalIV.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.paw_placeholder))
        AppDelegate.animalModel.cameraImage = null
        AppDelegate.animalModel.galleryImage = null
        AppDelegate.isImageSelected = false
    }

    override fun onClick(p0: View?) {
        var items = arrayOf<String>()
            items = arrayOf("Camera", "Gallery")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Image")
            .setItems(items, DialogInterface.OnClickListener { dialog, which ->
                when(which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            })
        builder.create()
        builder.show()
    }
    private fun openCamera() {
        lastSelected = 2
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        getResult.launch(cameraIntent)
    }
    private fun openGallery() {
        lastSelected = 1
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    private fun checkValidation(): Boolean {
        val name = binding.nameET.text.trim().toString()
        val description = binding.descriptionET.text.toString()

        if (name.isEmpty() || name.isBlank()) {
            binding.nameET.requestFocus()
            binding.nameET.error = "Name is required."
            return false
        }
        if (description.isEmpty() || description.isBlank()) {
            binding.descriptionET.requestFocus()
            binding.descriptionET.error = "Description is required."
            return false
        }
        if (AppDelegate.animalModel.locationAddress.isBlank()) {
            binding.locationET.requestFocus()
            binding.locationET.error = "Location is required."
            return false
        }
        if (animalModel == null) {
            Toast.makeText(requireContext(), "Animal details not available", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    fun getIPDAnimals() {
        Firebase.firestore.collection(CollectionAnimals.name)
            .whereEqualTo(CollectionAnimals.kIsArchive, false)
            .whereEqualTo(CollectionAnimals.kState, "Active")
            .whereEqualTo(CollectionAnimals.kIsDead, false)
            .whereEqualTo(CollectionAnimals.kType, CollectionAnimals.IPD)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val names = querySnapshot.documents.mapNotNull { it.data?.get("name") as String }
                createSheet(names)
            }
            .addOnFailureListener { exception ->
                Log.e("NST", "Error getting getActiveCases Data: ", exception)
            }
    }

    fun createSheet(nameList: List<String>) {
        val  hssfWorkbook = HSSFWorkbook()
        val hssfSheet: HSSFSheet = hssfWorkbook.createSheet("Active-Dogs")

        val hssfRow: HSSFRow = hssfSheet.createRow(0)
        hssfRow.createCell(0).setCellValue("Name")

        for (i in 0 until nameList.size) {
            val hssfRow1: HSSFRow = hssfSheet.createRow(i+1)
            hssfRow1.createCell(0).setCellValue(nameList[i])
        }
        saveWorkBook(hssfWorkbook)
    }

    private fun saveWorkBook(hssfWorkbook: HSSFWorkbook) {

        val dir = File(Environment.getExternalStorageDirectory(), "Paw Garage")
        if (!dir.exists()) {
            dir.mkdir()
        }
        val subDir = File(dir, "Active Dogs")
        if (!subDir.exists()) {
            subDir.mkdir()
        }
        val file = File(subDir, "${System.currentTimeMillis()}.xls")

        try {
            val fileOutputStream = FileOutputStream(file)
            hssfWorkbook.write(fileOutputStream)
            fileOutputStream.close()
            hssfWorkbook.close()
            Toast.makeText(requireContext(), "File created successfully", Toast.LENGTH_LONG).show();

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "File creation failed", Toast.LENGTH_LONG).show();
            throw RuntimeException(e)
        }
    }

}