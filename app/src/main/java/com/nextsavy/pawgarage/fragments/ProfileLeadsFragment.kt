package com.nextsavy.pawgarage.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.nextsavy.pawgarage.MainActivity
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentProfileLeadsBinding
import com.nextsavy.pawgarage.models.ProfileLeadDTO
import com.nextsavy.pawgarage.retrofit.RetrofitClient
import com.nextsavy.pawgarage.retrofit.RetrofitInterface
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionNotifications
import com.nextsavy.pawgarage.utils.CollectionProfileLeads
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.Helper
import retrofit2.Call
import retrofit2.Callback
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileLeadsFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentProfileLeadsBinding
    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private var nameExists: Boolean = false
    lateinit var retrofitInterface: RetrofitInterface

    /**
     * 0 = No option selected.
     * 1 = Camera
     * 2 = Gallery
     */
    var lastSelected = 0
    var cameraImage: Bitmap? = null
    var galleryImage: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileLeadsBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "New Profile"

        onClickListeners()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            (requireActivity() as MainActivity?)?.binding?.bottomNav?.selectedItemId = R.id.homeFragment
        }

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        AppDelegate.animalModel.locationAddress = binding.locationET.text.toString()
    }

    override fun onResume() {
        super.onResume()
        if (cameraImage != null) {
            binding.animalIV.setImageBitmap(cameraImage)
        } else if (galleryImage != null) {
            binding.animalIV.setImageURI(galleryImage)
        }
        binding.locationET.setText(AppDelegate.animalModel.locationAddress)
    }


    override fun onClick(p0: View?) {
        var items = arrayOf<String>()
        if (cameraImage == null && galleryImage == null) {
            items = arrayOf("Camera", "Gallery")
        } else {
            items = arrayOf("Camera", "Gallery", "Remove Image")
        }
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Image")
            .setItems(items, DialogInterface.OnClickListener { dialog, which ->
                when(which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> removeImage()
                }
            })
        builder.create()
        builder.show()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        getResult.launch(cameraIntent)
    }
    private fun removeImage() {
        binding.animalIV.setImageURI(null)
        binding.animalIV.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.paw_placeholder))
        cameraImage = null
        galleryImage = null
    }
    private fun openGallery() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    private fun checkValidation(): Boolean {
        val name = binding.nameET.text.toString()
        val location = binding.locationET.text.toString()
        if (name.isBlank()) {
            binding.nameET.requestFocus()
            binding.nameET.error = "Required"
            return false
        } else {
            binding.nameET.error = null
        }
        if (cameraImage == null && galleryImage == null) {
            Toast.makeText(requireContext(), "Please select image.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (location.isEmpty() || location.isBlank()) {
            binding.locationET.requestFocus()
            binding.locationET.error = "Location is required."
            Toast.makeText(requireContext(), "Location is required.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
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

        if (lastSelected == 2) {
            if (galleryImage != null) {
                val uploadTask = animalRef.putFile(galleryImage!!)

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
        }
        if (lastSelected == 1) {
            if (cameraImage != null) {
                val baos = ByteArrayOutputStream()
                cameraImage!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
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
        }
    }

    private fun addAnimalDataToDatabase(downloadUrl: String, animalId: String) {

        binding.progressBar.root.visibility = View.VISIBLE

        val profileLeadDocRef = db.collection(CollectionProfileLeads.name).document()
        val profileLeadsData = hashMapOf(
            CollectionProfileLeads.kAnimalId to animalId,
            CollectionProfileLeads.kName to binding.nameET.text.trim().toString(),
            CollectionProfileLeads.kDownloadUrl to downloadUrl,
            CollectionProfileLeads.kLatitude to AppDelegate.animalModel.latitude,
            CollectionProfileLeads.kLongitude to AppDelegate.animalModel.longitude,
            CollectionProfileLeads.kLocationAddress to binding.locationET.text.toString(),
            CollectionProfileLeads.kIsArchive to false,
            CollectionProfileLeads.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionProfileLeads.kCreatedBy to Firebase.auth.currentUser?.uid,
        )

        val reminderDocRef = db.collection(CollectionReminders.name).document()
        val remindersData = hashMapOf(
            CollectionReminders.kAnimalDocId to "",
            CollectionReminders.kReminderDate to FieldValue.serverTimestamp(),
            CollectionReminders.kReminderType to CollectionReminders.COMPLETE_PROFILE,
            CollectionReminders.kReminderTypeObjectId to profileLeadDocRef.id,
            CollectionReminders.kIsComplete to false,
            CollectionReminders.kIsArchive to false,
            CollectionReminders.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionReminders.kCreatedBy to Firebase.auth.currentUser?.uid,
        )

        // Gaurav
        val notificationDocRef = Firebase.firestore.collection(CollectionNotifications.name).document()
        val notificationData = hashMapOf(
            CollectionNotifications.kAnimalDocId to "",
            CollectionNotifications.kNotificationDate to FieldValue.serverTimestamp(),
            CollectionNotifications.kNotificationType to CollectionNotifications.PROFILE_LEADS,
            CollectionNotifications.kNotificationSubtype to null,
            CollectionNotifications.kNotificationTypeObjectId to profileLeadDocRef.id,
            CollectionNotifications.kIsArchive to false,
            CollectionNotifications.kCreatedAt to FieldValue.serverTimestamp(),
            CollectionNotifications.kCreatedBy to Firebase.auth.currentUser?.uid,
        )

        val batch = Firebase.firestore.batch()
        batch.set(profileLeadDocRef, profileLeadsData)
        batch.set(reminderDocRef, remindersData)
        batch.set(notificationDocRef, notificationData)

        batch.commit().addOnCompleteListener { batchTask ->
            if (batchTask.isSuccessful) {
                binding.progressBar.root.visibility = View.GONE
                //
                val profileLeadDTO = ProfileLeadDTO(
                    id = profileLeadDocRef.id,
                    animalId = "",
                    name = binding.nameET.text.trim().toString(),
                    downloadUrl = downloadUrl,
                    address = binding.locationET.text.toString(),
                    latitude = AppDelegate.animalModel.latitude,
                    longitude = AppDelegate.animalModel.longitude,
                    isArchive = false,
                    createdAt = Timestamp.now(),
                    createdBy = ""
                )
                sendPushNotification("Half profile of ${binding.nameET.text.trim()} has been created. Please complete the profile.",
                    profileLeadDTO)
                Toast.makeText(requireContext(), "Animal data added successfully.", Toast.LENGTH_SHORT).show()
                clearData()
                findNavController().popBackStack()
            } else {
                binding.progressBar.root.visibility = View.GONE
                Log.e("NST", "Exception: Profile Lead-Reminder addition BatchTask: ${batchTask.exception?.localizedMessage}")
                Toast.makeText(requireContext(), "${batchTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun sendPushNotification(message: String, profileLeadDTO: ProfileLeadDTO) {

        val baseUrl = "https://fcm.googleapis.com/"
        retrofitInterface = RetrofitClient.getRetrofitInstance(baseUrl).create(RetrofitInterface::class.java)

        if (Helper.isInternetAvailable(requireContext())) {

            val payLoad = JsonObject()
            val notification = JsonObject()
            val data = JsonObject()

            try {
                payLoad.addProperty("to", "/topics/" + CollectionNotifications.TOPIC)
                notification.addProperty("title", CollectionNotifications.NEW_PROFILE)
                notification.addProperty("body", message)
                payLoad.add("notification", notification)
//                data.addProperty("animal_doc_id", animalData.animal_doc_id)
//                data.addProperty("reminder_doc_id", animalData.document_id)
                data.addProperty("profile_lead_doc_id", profileLeadDTO.id)
                data.addProperty("animal_name", profileLeadDTO.name)
                data.addProperty("animal_image_url", profileLeadDTO.downloadUrl)
                data.addProperty("location", profileLeadDTO.address)
                data.addProperty("latitude", profileLeadDTO.latitude)
                data.addProperty("longitude", profileLeadDTO.longitude)
                data.addProperty("notification_type", CollectionNotifications.PROFILE_LEADS)
                data.addProperty("notification_version", "v2")

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

    private fun clearData() {
        removeImage()
        binding.nameET.setText("")
        binding.locationET.setText("")
    }

    private fun onClickListeners() {
        binding.saveBTN.setOnClickListener {
            if (Helper.isInternetAvailable(requireContext())) {
                if (checkValidation()) {
                    checkAnimalNameInDatabase()
                    //uploadAnimalImage()
                }
            } else {
                Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
            }
        }
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().popBackStack(R.id.homeFragment, false)
        }
        binding.locationET.setOnClickListener {
            it.findNavController().navigate(R.id.locationFragment)
        }
        binding.animalIV.setOnClickListener(this)
        binding.addImageBTN.setOnClickListener(this)
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the photo picker.
        if (uri != null) {
            Log.e("PhotoPicker", "Selected URI: $uri")
            binding.animalIV.setImageURI(uri)
            galleryImage = uri
            lastSelected = 2
            cameraImage = null
        } else {
            Log.e("PhotoPicker", "No media selected")
        }
    }

    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == Activity.RESULT_OK){
            val bm = it.data?.extras?.get("data") as Bitmap
            if(bm.width > bm.height)
            {
                var bMapRotate: Bitmap? = null
                val mat = Matrix()
                mat.postRotate(90F)
                bMapRotate = Bitmap.createBitmap(bm, 0, 0,bm.width,bm.height, mat, true)
                bm.recycle()
                cameraImage = bMapRotate
                binding.animalIV.setImageBitmap(bMapRotate)
            }
            else {
                cameraImage = bm
                binding.animalIV.setImageBitmap(bm)
            }
        }
        lastSelected = 1
        galleryImage = null
    }

}