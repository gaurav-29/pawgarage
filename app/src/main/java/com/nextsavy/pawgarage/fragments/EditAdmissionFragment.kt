package com.nextsavy.pawgarage.fragments

import android.app.DatePickerDialog
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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentEditAdmissionBinding
import com.nextsavy.pawgarage.models.AdmissionDTO
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.models.MedicalConditionDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionMedicalConditionsList
import com.nextsavy.pawgarage.utils.CollectionReportingPersons
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.viewModels.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditAdmissionFragment : Fragment() {

    private lateinit var binding: FragmentEditAdmissionBinding
    private val args: EditAdmissionFragmentArgs by navArgs()

    private var admissionDocId: String? = null

    private var admissionDTO: AdmissionDTO? = null
    private var reportingPersonDTO: GenericMemberDTO? = null

    private var admissionDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditAdmissionBinding.inflate(inflater, container, false)

        registerObserver()

        admissionDocId = args.admissionId

        if (admissionDocId != null) {
            setupUI()
            if (admissionDTO == null) {
                getAdmissionDetails2()
            } else {
                configureReportingPersonWidget(reportingPersonDTO)
                setupAndConfigureCreatedByUI(admissionDTO!!.createdBy)
                setupAndConfigureUpdatedByUI(admissionDTO!!.updatedBy)
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedViewModel.resetSelection()
        sharedViewModel.setReportingPerson(null)
    }

    private fun registerObserver() {
        sharedViewModel.selectedMedicalCondition.observe(viewLifecycleOwner) {
            updateSelectedMedicalConditions()
        }
        sharedViewModel.reportingPerson.observe(viewLifecycleOwner) {
            configureReportingPersonWidget(it)
        }
    }

    private fun updateSelectedMedicalConditions() {
        val sb = StringBuilder()
        sharedViewModel.getSelectedMedicalCondition().forEach {
            if (sb.isBlank()) {
                sb.append(it.name)
            } else {
                sb.append(", ")
                sb.append(it.name)
            }
        }

        binding.conditionsET.setText(sb)
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


    private fun setupUI() {
        binding.toolbarOne.titleToolbarOne.text = args.animalName ?: ""
        binding.detailsTV.text =  "Admission Details ${args.totalAdmissionCount - args.currentAdmissionIndex}"
        binding.countryCodeET.setText(CollectionUser.COUNTRY_CODE)

        if (AppDelegate.isDead ||
            AppDelegate.state == CollectionAnimals.TERMINATED ||
            (args.totalAdmissionCount > 1 && args.currentAdmissionIndex != 0)) {
            binding.weightET.isEnabled = false
            binding.dateET.isEnabled = false
            binding.personET.isEnabled = false
            binding.addMedicalConditionsBTN.isEnabled = false
            binding.saveBTN.visibility = View.GONE
        }

        setupAdmissionDatePicker()

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.addMedicalConditionsBTN.setOnClickListener {
            it.findNavController().navigate(R.id.medicalConditionListFragment)
        }

        binding.saveBTN.setOnClickListener(saveButtonTapped)

        binding.personET.setOnClickListener {
            it.findNavController().navigate(R.id.reportingPersonsListFragment, Bundle().apply { putString("From", "Others") })
        }
    }

    private val saveButtonTapped = View.OnClickListener {
        if (Helper.isInternetAvailable(requireContext())) {
            if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                if (checkValidation()) {
                    updateAdmissionDetailsToDatabase()
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    /*private fun onClickListeners() {
        binding.addMedicalConditionsBTN.setOnClickListener {
            it.findNavController().navigate(R.id.medicalConditionListFragment)
        }

        binding.saveBTN.setOnClickListener {

        }

        binding.personET.setOnClickListener {
            it.findNavController().navigate(R.id.reportingPersonsListFragment, Bundle().apply { putString("From", "Others") })
        }

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.dateET.setOnClickListener {
            val dateSetListener =
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, monthOfYear)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    val sdf = SimpleDateFormat("dd/MM/yy", Locale.US)
                    binding.dateET.setText(sdf.format(cal.time))
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

            dialog.datePicker.maxDate = System.currentTimeMillis()  // To disable the days after current day.
            dialog.show()
        }
    }*/

    private fun checkValidation(): Boolean {
        val weight = binding.weightET.text.trim().toString()


        if (weight.isBlank()) {
            binding.weightET.requestFocus()
            binding.weightET.error = "Weight is required."
            return false
        }
        if (weight.toDoubleOrNull() == null) {
            binding.weightET.requestFocus()
            binding.weightET.error = "Invalid value."
            return false
        }
        if (sharedViewModel.getReportingPerson() == null) {
            binding.personET.requestFocus()
            binding.personET.error = "Reporting person is required."
            return false
        }
        if (sharedViewModel.getSelectedMedicalCondition().isEmpty()) {
            binding.conditionsET.error = "Required"
            return false
        } else {
            binding.conditionsET.error = null
        }

        return true
    }

    private fun updateAdmissionDetailsToDatabase() {
        binding.progressBar.root.visibility = View.VISIBLE
        val admissionData = hashMapOf(
            CollectionAdmission.kWeight to binding.weightET.text.trim().toString(),
            CollectionAdmission.kAdmissionDate to admissionDate,
            CollectionAdmission.kReportingPersonId to sharedViewModel.getReportingPerson()!!.id,
            CollectionAdmission.kContactNumber to CollectionUser.COUNTRY_CODE + binding.numberET.text.toString(),
            CollectionAdmission.kMedicalConditions to binding.conditionsET.text.toString(),
            CollectionAdmission.kMedicalConditionIds to sharedViewModel.getSelectedMedicalCondition().map { it.id },
            CollectionAdmission.kIsArchive to false,
            CollectionAdmission.kUpdatedAt to FieldValue.serverTimestamp(),
            CollectionAdmission.kUpdatedBy to Firebase.auth.currentUser?.uid,
        )

        Firebase.firestore.collection(CollectionAdmission.name).document(args.admissionId!!)
            .set(admissionData, SetOptions.merge())
            .addOnSuccessListener {
                binding.progressBar.root.visibility = View.GONE
                Toast.makeText(requireContext(), "Admission details updated successfully.", Toast.LENGTH_SHORT).show()
                Log.e("DATA", "Admission details updated successfully.")

                findNavController().popBackStack()
            }
            .addOnFailureListener {
                binding.progressBar.root.visibility = View.GONE
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                Log.e("ERROR-EditAdmission", "Error writing document-$it")
            }
    }

    /*private fun getAdmissionDetails() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            Firebase.firestore.collection(CollectionAdmission.name).document(args.admissionId!!)
                .get()
                .addOnSuccessListener { document ->
                    admissionDTO = AdmissionDTO.create(document.id, document.data)
                    if (admissionDTO != null) {
                        getMedicalConditions(admissionDTO!!)
                        configureUI(admissionDTO!!)
                        setupAndConfigureUpdatedByUI(admissionDTO!!.updatedBy)
                        setupAndConfigureCreatedByUI(admissionDTO!!.createdBy)
                    } else {
                        Log.e("REL", "No such document")
                        Toast.makeText(requireContext(), "No such document", Toast.LENGTH_LONG).show()
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
    }*/

    private fun getAdmissionDetails2() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            Firebase.firestore.collection(CollectionAdmission.name).document(args.admissionId!!)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    admissionDTO = AdmissionDTO.create(documentSnapshot.id, documentSnapshot.data)
                    configureUI2()
                    admissionDTO?.let {
                        getMedicalConditions(admissionDTO!!)
                    }
                    admissionDTO?.reportingPersonId?.let { rPersonId ->
                        getReportingPersonDetails(rPersonId)
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

    /*private fun configureUI(data: AdmissionDTO) {


        binding.dateET.setText(dateToShow)

        binding.weightET.setText(data.weight)

        if (AppDelegate.reportingPersonModel.documentId == "" &&
            AppDelegate.reportingPersonModel.name == "" &&
            AppDelegate.reportingPersonModel.number == "") {

            personId = data.reportingPersonId
            getReportingPersonName(data.reportingPersonId)

        } else {
            binding.personET.setText(AppDelegate.reportingPersonModel.name)
            val number = AppDelegate.reportingPersonModel.number
            if (number.length >= 10) {
                last10Digits = number.substring(number.length - 10)
            }
            binding.numberET.setText(last10Digits)
            personId = AppDelegate.reportingPersonModel.documentId
        }
    }*/

    private fun configureUI2() {
        admissionDTO?.let {
            binding.weightET.setText(it.weight)

            setupAndConfigureCreatedByUI(it.createdBy)
            setupAndConfigureUpdatedByUI(it.updatedBy)

            admissionDate = it.admissionDate.toDate()
            configureAdmissionDateTV()
        }
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
            args.lastReleaseDate?.let { lastReleaseDateStr ->
                val lastReleaseDate: Date? = dateFormat.parse(lastReleaseDateStr)
                lastReleaseDate?.let {
                    calendarForDateToSet.time = it
                }
            }
            dialog.datePicker.minDate = calendarForDateToSet.timeInMillis
            dialog.datePicker.maxDate = System.currentTimeMillis()  // To disable the days after today.
            dialog.show()
        }
    }

    private fun configureAdmissionDateTV() {
        admissionDate?.let {
            binding.dateET.setText(dateFormat.format(it))
        }
    }

    private fun getReportingPersonDetails(adopterId: String) {
        binding.progressBar2.visibility = View.VISIBLE
        Firebase.firestore.collection(CollectionReportingPersons.name)
            .document(adopterId)
            .get()
            .addOnSuccessListener { docSnapshot ->
                reportingPersonDTO = GenericMemberDTO.create(docSnapshot.id, docSnapshot.data)
                sharedViewModel.setReportingPerson(reportingPersonDTO)
                binding.progressBar2.visibility = View.GONE
            }.addOnFailureListener {
                binding.progressBar2.visibility = View.GONE
            }
    }

    private fun getMedicalConditions(admissionDTO: AdmissionDTO) {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            val medicalConditionsTaskSnapshotList = admissionDTO.medicalConditionIds.map { Firebase.firestore.collection(CollectionMedicalConditionsList.name).document(it).get() }
            val allMedicalConditionTasks = Tasks.whenAllSuccess<DocumentSnapshot>(medicalConditionsTaskSnapshotList)
            allMedicalConditionTasks.addOnSuccessListener { docSnapshotList ->
                val existingMC = docSnapshotList.mapNotNull { MedicalConditionDTO.create(it.id, it.data) }.toMutableList()
                existingMC.removeIf { it.isArchive }
                sharedViewModel.addAllMedicalCondition(existingMC)
                binding.progressBar2.visibility = View.GONE
            }.addOnFailureListener { exception ->
                Log.e("NST", "Error EditAdmissionFragment > getMedicalConditions:\t${exception.localizedMessage}")
                binding.progressBar2.visibility = View.GONE
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupAndConfigureCreatedByUI(createdBy: String?) {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            if (createdBy != null) {
                Firebase.firestore.collection(CollectionUser.name)
                    .document(createdBy)
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

    private fun setupAndConfigureUpdatedByUI(updatedBy: String?) {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            if (updatedBy != null) {
                Firebase.firestore.collection(CollectionUser.name)
                    .document(updatedBy)
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

}
