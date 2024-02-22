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
import com.nextsavy.pawgarage.databinding.FragmentEditTreatmentBinding
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.models.MedicalConditionDTO
import com.nextsavy.pawgarage.models.TreatmentDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionMedicalConditionsList
import com.nextsavy.pawgarage.utils.CollectionReportingPersons
import com.nextsavy.pawgarage.utils.CollectionTreatment
import com.nextsavy.pawgarage.utils.CollectionUser
import com.nextsavy.pawgarage.utils.Helper
import com.nextsavy.pawgarage.viewModels.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditTreatmentFragment : Fragment() {

    private lateinit var binding: FragmentEditTreatmentBinding
    private val args: EditTreatmentFragmentArgs by navArgs()

    private var treatmentDocId: String? = null

    private var treatmentDTO: TreatmentDTO? = null
    private var reportingPersonDTO: GenericMemberDTO? = null

    private var treatmentDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)

    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditTreatmentBinding.inflate(inflater, container, false)

        registerObserver()

        treatmentDocId = args.treatmentId

        if (treatmentDocId != null) {
            setupUI()
            if (treatmentDTO == null) {
                getTreatmentDetails()
            } else {
                configureReportingPersonWidget(reportingPersonDTO)
                setupAndConfigureCreatedByUI(treatmentDTO!!.createdBy)
                setupAndConfigureUpdatedByUI(treatmentDTO!!.updatedBy)
            }
        }

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

    private fun configureReportingPersonWidget(rPerson: GenericMemberDTO?) {
        rPerson?.let {
            binding.personET.setText(it.name)
            if (it.phoneNumber.contains("+")) {
                binding.numberET.setText(it.phoneNumber.substring(it.phoneNumber.length - 10))
            } else {
                binding.numberET.setText(it.phoneNumber)
            }
        }
    }

    private fun setupUI() {
        binding.toolbarOne.titleToolbarOne.text = args.animalName
        binding.detailsTV.text = "OPD Details ${(args.totalAdmissionCount - args.currentAdmissionIndex)}"
        binding.countryCodeET.setText(CollectionUser.COUNTRY_CODE)

        if (AppDelegate.isDead ||
            AppDelegate.state == CollectionAnimals.TERMINATED ||
            (args.totalAdmissionCount > 1 && args.currentAdmissionIndex != 0)) {
            binding.dateET.isEnabled = false
            binding.personET.isEnabled = false
            binding.addMedicalConditionsBTN.isEnabled = false
            binding.saveBTN.visibility = View.GONE
        }

        setupTreatmentDatePicker()

        binding.addMedicalConditionsBTN.setOnClickListener {
            it.findNavController().navigate(R.id.medicalConditionListFragment)
        }
        binding.personET.setOnClickListener {
            it.findNavController().navigate(R.id.reportingPersonsListFragment, Bundle().apply { putString("From", "Others") })
        }
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
        binding.saveBTN.setOnClickListener(saveButtonTapped)
    }

    private val saveButtonTapped = View.OnClickListener {
        if (Helper.isInternetAvailable(requireContext())) {
            if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                if (checkValidation()) {
                    updateTreatmentDetailsToDatabase()
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTreatmentDetailsToDatabase() {
        binding.progressBar.root.visibility = View.VISIBLE
        val medicalConditions = if (!binding.notApplicableCB.isChecked) binding.conditionsET.text.trim().toString() else ""
        val treatmentData = hashMapOf(
            CollectionTreatment.kTreatmentDate to treatmentDTO,
            CollectionTreatment.kReportingPerson to binding.personET.text.trim().toString(),
            CollectionTreatment.kReportingPersonId to viewModel.getReportingPerson()!!.id,
            CollectionTreatment.kContactNumber to CollectionUser.COUNTRY_CODE + binding.numberET.text.toString(),
            CollectionTreatment.kMedicalConditions to medicalConditions,
            CollectionTreatment.kMedicalConditionIds to if (binding.notApplicableCB.isChecked) listOf<String>() else viewModel.getSelectedMedicalCondition().map { it.id },
            CollectionTreatment.kAdminNote to binding.note2ET.text.trim().toString(),
            CollectionTreatment.kIsArchive to false,
            CollectionTreatment.kUpdatedAt to FieldValue.serverTimestamp(),
            CollectionTreatment.kUpdatedBy to Firebase.auth.currentUser?.uid,
        )
        Firebase.firestore.collection(CollectionTreatment.name).document(args.treatmentId!!)
            .set(treatmentData, SetOptions.merge())
            .addOnSuccessListener {
                binding.progressBar.root.visibility = View.GONE
                Toast.makeText(requireContext(), "OPD details updated successfully.", Toast.LENGTH_SHORT).show()
                Log.e("DATA", "OPD details updated successfully.")

                findNavController().popBackStack()
            }
            .addOnFailureListener {
                binding.progressBar.root.visibility = View.GONE
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                Log.e("ERROR-EditAdmission", "Error writing document-$it")
            }
    }

    private fun checkValidation(): Boolean {
        if (!binding.notApplicableCB.isChecked) {
            if (viewModel.getSelectedMedicalCondition().isEmpty()) {
                binding.conditionsET.requestFocus()
                binding.conditionsET.error = "Conditions required."
                Toast.makeText(requireContext(), "Medical conditions required.", Toast.LENGTH_LONG).show()
                return false
            } else {
                binding.conditionsET.error = null
            }
        } else {
            binding.conditionsET.error = null
        }

        if (viewModel.getReportingPerson() == null) {
            binding.personET.requestFocus()
            binding.personET.error = "Reporting person is required."
            return false
        } else {
            binding.personET.error = null
        }
        return true
    }

    private fun getTreatmentDetails() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            Firebase.firestore.collection(CollectionTreatment.name).document(args.treatmentId!!)
                .get()
                .addOnSuccessListener { document ->
                    treatmentDTO = TreatmentDTO.create(document.id, document.data)
                    configureUI()
                    treatmentDTO?.let {
                        getMedicalConditions(treatmentDTO!!)
                    }
                    treatmentDTO?.reportingPersonId?.let { rPersonId ->
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

    private fun configureUI() {
        treatmentDTO?.let {
            setupAndConfigureCreatedByUI(it.createdBy)
            setupAndConfigureUpdatedByUI(it.updatedBy)

            treatmentDate = it.treatmentDate.toDate()
            configureTreatmentDateTV()

            binding.note2ET.setText(it.adminNotes)
        }
    }

    private fun setupTreatmentDatePicker() {
        binding.dateET.setOnClickListener {
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                treatmentDate = calendar.time
                binding.dateET.setText(dateFormat.format(calendar.time))
            }

            val calendarForDateToSet = Calendar.getInstance()
            treatmentDate?.let { date ->
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

    private fun configureTreatmentDateTV() {
        treatmentDate?.let {
            binding.dateET.setText(dateFormat.format(it))
        }
    }

    private fun getReportingPersonDetails(personId: String) {
        binding.progressBar2.visibility = View.VISIBLE
        Firebase.firestore.collection(CollectionReportingPersons.name)
            .document(personId)
            .get()
            .addOnSuccessListener { docSnapshot ->
                reportingPersonDTO = GenericMemberDTO.create(docSnapshot.id, docSnapshot.data)
                viewModel.setReportingPerson(reportingPersonDTO)
                binding.progressBar2.visibility = View.GONE
            }.addOnFailureListener {
                binding.progressBar2.visibility = View.GONE
            }
    }

    private fun getMedicalConditions(treatmentDTO: TreatmentDTO) {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar2.visibility = View.VISIBLE
            val medicalConditionsTaskSnapshotList = treatmentDTO.medicalConditionIds.map { Firebase.firestore.collection(CollectionMedicalConditionsList.name).document(it).get() }
            val allMedicalConditionTasks = Tasks.whenAllSuccess<DocumentSnapshot>(medicalConditionsTaskSnapshotList)
            allMedicalConditionTasks.addOnSuccessListener { docSnapshotList ->
                val existingMC = docSnapshotList.mapNotNull { MedicalConditionDTO.create(it.id, it.data) }.toMutableList()
                existingMC.removeIf { it.isArchive }
                viewModel.addAllMedicalCondition(existingMC)
                binding.progressBar2.visibility = View.GONE
            }.addOnFailureListener { exception ->
                Log.e("NST", "Error EditTreatmentFragment > getMedicalConditions:\t${exception.localizedMessage}")
                binding.progressBar2.visibility = View.GONE
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_LONG).show()
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
}