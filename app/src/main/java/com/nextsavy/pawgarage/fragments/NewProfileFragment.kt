package com.nextsavy.pawgarage.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentNewProfileBinding
import com.nextsavy.pawgarage.models.ProfileLeadDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionReminders
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper


class NewProfileFragment : Fragment(), OnClickListener {

    private lateinit var binding: FragmentNewProfileBinding
    private val args: NewProfileFragmentArgs by navArgs()

    private var profileLeadDTO: ProfileLeadDTO? = null

    private var leadsImage: String = ""
    var imageCase = 0

    var reminderDocID: String? = null

    val userType: String
        get() = Helper.sharedPreference?.getString("USER_TYPE", CollectionWhitelistedNumbers.TEAM_MEMBER) ?: CollectionWhitelistedNumbers.TEAM_MEMBER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (userType == CollectionWhitelistedNumbers.TEAM_MEMBER) {
            findNavController().navigate(R.id.profileLeadsFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewProfileBinding.inflate(inflater, container, false)

        Log.e("FLOW", "onCreateView()- NewProfileFragment")

        binding.spinnerStatus.isEnabled = false

        // Getting profileLeadsData from HomeFragment in args.
        profileLeadDTO = args.profileLead
        profileLeadDTO?.let {
            if (it.id.isNotBlank()) {
                getReminderDoc(it.id)
            }
            leadsImage = it.downloadUrl
        }

        binding.toolbarOne.titleToolbarOne.text = "New Profile"
        AppDelegate.animalModel.type = "IPD"
        AppDelegate.animalModel.gender = "Male"
        AppDelegate.animalModel.species = "Dog"
        if (!AppDelegate.isImageSelected || AppDelegate.profileLeadsImage == "") {
            binding.animalIV.setImageURI(null)
            binding.animalIV.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.paw_placeholder))
        }

        onClickListeners()
        setStatusSpinner()
        return binding.root
    }

    private fun getReminderDoc(profileLeadDocIc: String) {
        if (Helper.isInternetAvailable(requireContext())) {
            Firebase.firestore
                .collection(CollectionReminders.name)
                .whereEqualTo(CollectionReminders.kReminderTypeObjectId, profileLeadDocIc)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.documents.isNotEmpty()) {
                        reminderDocID = querySnapshot.documents.first().id
                    }
                    Log.e("NST", "NewProfileFragment getReminderDoc > Reminder Doc Id: $reminderDocID")
                }.addOnFailureListener { e ->
                    Log.e("NST", "Exception: NewProfileFragment getReminderDoc: ${e.localizedMessage}")
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setStatusSpinner() {
        val duration = resources.getStringArray(R.array.status2)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, duration)
        binding.spinnerStatus.adapter = adapter
    }

    override fun onPause() {
        super.onPause()
        Log.e("FLOW", "onPause()-NewProfileFragment")
        AppDelegate.animalModel.name = binding.nameET.text.trim().toString()
        AppDelegate.animalModel.galleryImage = AppDelegate.animalModel.galleryImage
        AppDelegate.animalModel.cameraImage = AppDelegate.animalModel.cameraImage
        AppDelegate.animalModel.description = binding.descriptionET.text.toString()
        AppDelegate.animalModel.locationAddress = binding.locationET.text.toString()
    }

    override fun onResume() {
        super.onResume()

        if (imageCase == 1) {
            binding.animalIV.setImageURI(AppDelegate.animalModel.galleryImage)
            AppDelegate.imageFrom = "Gallery"
        }else if (imageCase == 2) {
            binding.animalIV.setImageBitmap(AppDelegate.animalModel.cameraImage)
            AppDelegate.imageFrom = "Camera"
        } else if (imageCase == 3){
            Glide.with(requireContext()).load(leadsImage).into(binding.animalIV)
            AppDelegate.profileLeadsImage = leadsImage
            AppDelegate.imageFrom = "ProfileLeads"
        } else if (imageCase == 0) {
            if (leadsImage != "") {
                Glide.with(requireContext()).load(leadsImage).into(binding.animalIV)
                AppDelegate.profileLeadsImage = leadsImage
                AppDelegate.imageFrom = "ProfileLeads"
                AppDelegate.isImageSelected = true
            } else {
                binding.animalIV.setImageURI(null)
                binding.animalIV.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.paw_placeholder))
            }
        }

        if (AppDelegate.animalModel.type == "IPD") binding.ipdRB.isChecked = true
        if (AppDelegate.animalModel.type == "OPD") binding.opdRB.isChecked = true

        if (AppDelegate.animalModel.gender == "Male") binding.maleRB.isChecked = true
        if (AppDelegate.animalModel.gender == "Female") binding.femaleRB.isChecked = true

        if(AppDelegate.animalModel.species == "Dog") {
            binding.dogRB.isChecked = true
            binding.catRB.isChecked = false
            binding.otherRB.isChecked = false
        }
        if(AppDelegate.animalModel.species == "Cat") {
            binding.dogRB.isChecked = false
            binding.catRB.isChecked = true
            binding.otherRB.isChecked = false
        }
        if(AppDelegate.animalModel.species == "Other") {
            binding.dogRB.isChecked = false
            binding.catRB.isChecked = false
            binding.otherRB.isChecked = true
        }
        if (profileLeadDTO != null) {
            binding.nameET.setText(profileLeadDTO!!.name)
            binding.locationET.setText(profileLeadDTO!!.address)
            AppDelegate.animalModel.locationAddress = profileLeadDTO!!.address
            AppDelegate.animalModel.latitude = profileLeadDTO!!.latitude
            AppDelegate.animalModel.longitude = profileLeadDTO!!.longitude
        } else {
            binding.nameET.setText(AppDelegate.animalModel.name)
            binding.locationET.setText(AppDelegate.animalModel.locationAddress)
        }
        binding.descriptionET.setText(AppDelegate.animalModel.description)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("FLOW", "onDestroy()- NewProfileFragment")
        AppDelegate.animalModel.locationAddress = ""
        AppDelegate.animalModel.longitude = 0.0
        AppDelegate.animalModel.latitude = 0.0
    }
    /*
    private fun saveImageToGallery(bm: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"

        // Output stream
        var fos: OutputStream? = null

        // For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // getting the contentResolver
            requireActivity().contentResolver?.also { resolver ->

                // Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    // putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                // Inserting the contentValues to
                // contentResolver and getting the Uri
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                // Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            // These for devices running on android < Q
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            // Finally writing the bitmap to the output stream that we opened
            bm.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(requireContext() , "Captured View and saved to Gallery" , Toast.LENGTH_SHORT).show()
        }
    }
    */

    override fun onClick(p0: View?) {
        var items = arrayOf<String>()
        if (AppDelegate.isImageSelected) {
            items = arrayOf("Camera", "Gallery", "Remove Image")
        } else {
            items = arrayOf("Camera", "Gallery")
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
        AppDelegate.animalModel.cameraImage = null
        AppDelegate.animalModel.galleryImage = null
        AppDelegate.isImageSelected = false
    }

    private fun openGallery() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    private fun checkValidation(): Boolean {

        val name = binding.nameET.text.trim().toString()
        val description = binding.descriptionET.text.toString()
        val location = binding.locationET.text.toString()

        if (!AppDelegate.isImageSelected) {
            Toast.makeText(requireContext(), "Please select image.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (name.isEmpty() || name.isBlank()) {
            binding.nameET.requestFocus()
            binding.nameET.error = "Name is required."
            return false
        }
        if (location.isEmpty() || location.isBlank()) {
            binding.locationET.requestFocus()
            binding.locationET.error = "Location is required."
            return false
        }
        if (description.isEmpty() || description.isBlank()) {
            binding.descriptionET.requestFocus()
            binding.descriptionET.error = "Description is required."
            return false
        }
        return true
    }
    private fun onClickListeners() {

        binding.locationET.setOnClickListener {
            it.findNavController().navigate(R.id.locationFragment, bundleOf("latitude" to AppDelegate.animalModel.latitude.toFloat(), "longitude" to AppDelegate.animalModel.longitude.toFloat()))
        }

        binding.animalIV.setOnClickListener(this)
        binding.addImageBTN.setOnClickListener(this)

        binding.nextBTN.setOnClickListener {
            if (checkValidation()) {
                if (binding.ipdRB.isChecked) {
                    val directions = NewProfileFragmentDirections.actionNewProfileFragmentToAddAdmissionFragment(
                        animalId = null,
                        from = "Profile",
                        reminderDocId = reminderDocID,
                        animalName = null,
                        lastReleaseDate = null
                    )
                    it.findNavController().navigate(directions)
                } else {
                    val directions = NewProfileFragmentDirections.actionNewProfileFragmentToAddTreatmentFragment(
                        animalDocId = null,
                        from = "Profile",
                        reminderDocId = reminderDocID,
                        animalName = null
                    )
                    it.findNavController().navigate(directions)
                }
            }
        }

        //val checkedRadioButtonId = binding.typeRG.checkedRadioButtonId // Returns View.NO_ID if nothing is checked.
        binding.typeRG.setOnCheckedChangeListener { buttonView, isChecked ->
            if (binding.ipdRB.isChecked) {

                AppDelegate.animalModel.type = "IPD"
                Log.e("RADIO", AppDelegate.animalModel.type)

                binding.ipdRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                TextViewCompat.setCompoundDrawableTintList(binding.ipdRB, ContextCompat.getColorStateList(requireContext(), R.color.black))
                binding.ipdRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_radio_button)
                binding.ipdRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)

                binding.opdRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
                TextViewCompat.setCompoundDrawableTintList(binding.opdRB, ContextCompat.getColorStateList(requireContext(), R.color.grey_text))
                binding.opdRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et)
                binding.opdRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_text)
            }
            if (binding.opdRB.isChecked) {

                AppDelegate.animalModel.type = "OPD"
                Log.e("RADIO", AppDelegate.animalModel.type)

                binding.opdRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                TextViewCompat.setCompoundDrawableTintList(binding.opdRB, ContextCompat.getColorStateList(requireContext(), R.color.black))
                binding.opdRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_radio_button)
                binding.opdRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)

                binding.ipdRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
                TextViewCompat.setCompoundDrawableTintList(binding.ipdRB, ContextCompat.getColorStateList(requireContext(), R.color.grey_text))
                binding.ipdRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et)
                binding.ipdRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_text)
            }
        }
        binding.genderRG.setOnCheckedChangeListener { buttonView, isChecked ->
            if (binding.maleRB.isChecked) {

                AppDelegate.animalModel.gender = "Male"
                Log.e("RADIO", AppDelegate.animalModel.gender)

                binding.maleRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                TextViewCompat.setCompoundDrawableTintList(binding.maleRB, ContextCompat.getColorStateList(requireContext(), R.color.black))
                binding.maleRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_radio_button)
                binding.maleRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)

                binding.femaleRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
                TextViewCompat.setCompoundDrawableTintList(binding.femaleRB, ContextCompat.getColorStateList(requireContext(), R.color.grey_text))
                binding.femaleRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et)
                binding.femaleRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_text)
            }
            if (binding.femaleRB.isChecked) {

                AppDelegate.animalModel.gender = "Female"
                Log.e("RADIO", AppDelegate.animalModel.gender)

                binding.femaleRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                TextViewCompat.setCompoundDrawableTintList(binding.femaleRB, ContextCompat.getColorStateList(requireContext(), R.color.black))
                binding.femaleRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_radio_button)
                binding.femaleRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)

                binding.maleRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
                TextViewCompat.setCompoundDrawableTintList(binding.maleRB, ContextCompat.getColorStateList(requireContext(), R.color.grey_text))
                binding.maleRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et)
                binding.maleRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_text)
            }
        }

        binding.dogRB.setOnClickListener {

            AppDelegate.animalModel.species = "Dog"
            Log.e("RADIO", AppDelegate.animalModel.species)

            binding.dogRB.isChecked = true
            binding.catRB.isChecked = false
            binding.otherRB.isChecked = false

            binding.dogRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            TextViewCompat.setCompoundDrawableTintList(binding.dogRB, ContextCompat.getColorStateList(requireContext(), R.color.black))
            binding.dogRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_radio_button)
            binding.dogRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)

            binding.catRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
            TextViewCompat.setCompoundDrawableTintList(binding.catRB, ContextCompat.getColorStateList(requireContext(), R.color.grey_text))
            binding.catRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et)
            binding.catRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_text)

            binding.otherRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
            TextViewCompat.setCompoundDrawableTintList(binding.otherRB, ContextCompat.getColorStateList(requireContext(), R.color.grey_text))
            binding.otherRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et)
            binding.otherRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_text)
        }
        binding.catRB.setOnClickListener {

            AppDelegate.animalModel.species = "Cat"
            Log.e("RADIO", AppDelegate.animalModel.species)

            binding.dogRB.isChecked = false
            binding.catRB.isChecked = true
            binding.otherRB.isChecked = false

            binding.catRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            TextViewCompat.setCompoundDrawableTintList(binding.catRB, ContextCompat.getColorStateList(requireContext(), R.color.black))
            binding.catRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_radio_button)
            binding.catRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)

            binding.dogRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
            TextViewCompat.setCompoundDrawableTintList(binding.dogRB, ContextCompat.getColorStateList(requireContext(), R.color.grey_text))
            binding.dogRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et)
            binding.dogRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_text)

            binding.otherRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
            TextViewCompat.setCompoundDrawableTintList(binding.otherRB, ContextCompat.getColorStateList(requireContext(), R.color.grey_text))
            binding.otherRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et)
            binding.otherRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_text)
        }
        binding.otherRB.setOnClickListener {

            AppDelegate.animalModel.species = "Other"
            Log.e("RADIO", AppDelegate.animalModel.species)

            binding.dogRB.isChecked = false
            binding.catRB.isChecked = false
            binding.otherRB.isChecked = true

            binding.otherRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            TextViewCompat.setCompoundDrawableTintList(binding.otherRB, ContextCompat.getColorStateList(requireContext(), R.color.black))
            binding.otherRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_radio_button)
            binding.otherRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)

            binding.dogRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
            TextViewCompat.setCompoundDrawableTintList(binding.dogRB, ContextCompat.getColorStateList(requireContext(), R.color.grey_text))
            binding.dogRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et)
            binding.dogRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_text)

            binding.catRB.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
            TextViewCompat.setCompoundDrawableTintList(binding.catRB, ContextCompat.getColorStateList(requireContext(), R.color.grey_text))
            binding.catRB.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_et)
            binding.catRB.buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_text)
        }

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            imageCase = 1
            leadsImage = ""
            AppDelegate.animalModel.cameraImage = null
            Log.e("PhotoPicker", "Selected URI: $uri")
            binding.animalIV.setImageURI(uri)

            AppDelegate.animalModel.galleryImage = uri
            AppDelegate.isImageSelected = true
            AppDelegate.imageFrom = "Gallery"
        } else {
            Log.e("PhotoPicker", "No media selected")
        }
    }

    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == Activity.RESULT_OK){
            val bm = it.data?.extras?.get("data") as Bitmap

            // Below code is to solve the problem- when we capture image in portrait mode,
            // it will appear in landscape mode in imageview in Pixel 6a.

            if(bm.width > bm.height)
            {
                var bMapRotate: Bitmap? = null
                val mat = Matrix()
                mat.postRotate(90F)
                bMapRotate = Bitmap.createBitmap(bm, 0, 0,bm.width,bm.height, mat, true)
                bm.recycle()
                imageCase = 2
                leadsImage = ""
                AppDelegate.animalModel.galleryImage = null
                AppDelegate.animalModel.cameraImage = bMapRotate
                binding.animalIV.setImageBitmap(bMapRotate)
            }
            else {
                imageCase = 2
                leadsImage = ""
                AppDelegate.animalModel.galleryImage = null
                AppDelegate.animalModel.cameraImage = bm
                binding.animalIV.setImageBitmap(bm)
            }
        }
        AppDelegate.isImageSelected = true
        AppDelegate.imageFrom = "Camera"
    }
}