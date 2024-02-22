package com.nextsavy.pawgarage.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.TreatmentAdapter
import com.nextsavy.pawgarage.databinding.FragmentTreatmentListBinding
import com.nextsavy.pawgarage.models.AdmissionDTO
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.TreatmentDTO
import com.nextsavy.pawgarage.models.GenericMemberDTO
import com.nextsavy.pawgarage.models.MedicalConditionDTO
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionMedicalConditionsList
import com.nextsavy.pawgarage.utils.CollectionRelease
import com.nextsavy.pawgarage.utils.CollectionReportingPersons
import com.nextsavy.pawgarage.utils.CollectionTreatment
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper

class TreatmentListFragment : Fragment(), RecyclerViewPagingInterface<TreatmentDTO> {

    private lateinit var binding: FragmentTreatmentListBinding
    private var adapter = TreatmentAdapter(arrayListOf(), this)
    private val db = Firebase.firestore
    private lateinit var userType: String
    private var allowTreatment = false

    private val animalDocId: String?
        get() = (this.parentFragment as AnimalProfileFragment?)?.animalDocId

    private val animalDTO: AnimalDTO?
        get() = (this.parentFragment as AnimalProfileFragment?)?.animalDTO

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTreatmentListBinding.inflate(inflater, container, false)
        setupTreatmentRV()

        userType = Helper.sharedPreference?.getString("USER_TYPE", "").toString()
        if (userType == CollectionWhitelistedNumbers.TEAM_MEMBER) {
            binding.addFirstTreatmentBTN.visibility = View.GONE
            binding.addAnotherTreatmentBTN.visibility = View.GONE
            binding.addTreatmentTV.setText(R.string.no_treatment_text)
        }

        getCheckForAllowTreatment()
        getTreatmentDetailList()
        onClickListeners()

        return binding.root
    }
    private fun getCheckForAllowTreatment() {
        var admissionCount: Int
        var releaseCount: Int

        Firebase.firestore.collection(CollectionAdmission.name)
            .whereEqualTo(CollectionAdmission.kIsArchive, false)
            .whereEqualTo(CollectionAdmission.kAnimalDocId, animalDocId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                admissionCount = querySnapshot.documents.size

                Firebase.firestore.collection(CollectionRelease.name)
                    .whereEqualTo(CollectionRelease.kIsArchive, false)
                    .whereEqualTo(CollectionRelease.kAnimalDocId, animalDocId)
                    .get()
                    .addOnSuccessListener { querySnapshot2 ->
                        releaseCount = querySnapshot2.documents.size

                        if (admissionCount == releaseCount) {
                            allowTreatment = true
                        }
                    }
            }
    }

    private fun setupTreatmentRV() {
        binding.treatmentRV.layoutManager = LinearLayoutManager(requireContext())
        binding.treatmentRV.adapter = adapter
    }
    
    private fun getTreatmentDetailList() {
        if (Helper.isInternetAvailable(requireContext())) {

            binding.progressBar.visibility = View.VISIBLE

            db.collection(CollectionTreatment.name)
                .whereEqualTo(CollectionTreatment.kIsArchive, false)
                .whereEqualTo(CollectionTreatment.kAnimalDocId, animalDocId)
                .orderBy(CollectionTreatment.kTreatmentDate, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val treatmentList = documents.documents.mapNotNull { doc -> TreatmentDTO.create(doc.id, doc.data) }
                    if (treatmentList.isEmpty()) {
                        updateDatsSource(treatmentList)
                    } else {
                        // Get Medical Conditions for each Treatment
                        getMedicalConditionForTreatments(treatmentList) { pair: Pair<Exception?, List<TreatmentDTO>?> ->
                            if (pair.second != null) {
                                // Get Reporting Person for each Treatment
                                getReportingPersonForTreatments(pair.second!!)
                            } else {
                                // Get Reporting Person for each Treatment
                                getReportingPersonForTreatments(treatmentList)
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    binding.progressBar.visibility = View.GONE
                    Log.e("DOC", "Error TreatmentList > getTreatmentDetailList:\t${exception.message}")
                    Toast.makeText(requireContext(), "${exception.message}.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMedicalConditionForTreatments(treatmentList: List<TreatmentDTO>, callback:(Pair<Exception?, List<TreatmentDTO>?>) -> Unit) {
        val medicalConditionIds: List<String> = treatmentList.flatMap { it.medicalConditionIds }.distinct()
        val medicalConditionTaskSnapshotList = medicalConditionIds.map { Firebase.firestore.collection(CollectionMedicalConditionsList.name).document(it).get() }
        val allMedicalConditionsTask = Tasks.whenAllSuccess<DocumentSnapshot>(medicalConditionTaskSnapshotList)
        allMedicalConditionsTask.addOnSuccessListener { docSnapshotList ->
            for (treatment in treatmentList) {
                val medicalConditionNames = arrayListOf<String>()
                for (medConId in treatment.medicalConditionIds) {
                    val doc: DocumentSnapshot? = docSnapshotList.findLast { doc -> doc.id == medConId }
                    val mcDTO: MedicalConditionDTO? = MedicalConditionDTO.create(doc?.id ?: "", doc?.data)
                    // Manthan: Do not include old Deleted/Archive Medical conditions
                    if (mcDTO != null && !mcDTO.isArchive) {
                        medicalConditionNames.add(mcDTO.name)
                    }
                }
                treatment.medicalConditionNames = medicalConditionNames
            }
            callback.invoke(Pair(null, treatmentList))
        }.addOnFailureListener { exception ->
            Log.e("NST", "Error AdmissionListFragment > Medical Conditions:\t${exception.message}")
            callback.invoke(Pair(exception, null))
        }
    }

    private fun getReportingPersonForTreatments(treatmentList: List<TreatmentDTO>) {
        val reportingPersonIds: List<String> = treatmentList.map { it.reportingPersonId }.distinct()
        val reportingPersonTaskSnapshotList = reportingPersonIds.map { Firebase.firestore.collection(CollectionReportingPersons.name).document(it).get() }
        val allReportingPersonsTask = Tasks.whenAllSuccess<DocumentSnapshot>(reportingPersonTaskSnapshotList)
        allReportingPersonsTask.addOnSuccessListener { docSnapshotList ->
            for (treatment in treatmentList) {
                for (docSnap in docSnapshotList) {
                    if (treatment.reportingPersonId == docSnap.id) {
                        val reportingPerson = GenericMemberDTO.create(docSnap.id, docSnap.data)
                        if (reportingPerson != null) {
                            treatment.reportingPerson = reportingPerson
                            break
                        }
                    }
                }
            }
            updateDatsSource(treatmentList)
        }.addOnFailureListener { exception ->
            Log.e("NST", "Error AdmissionListFragment > Reporting Person:\t${exception.message}")
            updateDatsSource(treatmentList)
        }
    }

    private fun updateDatsSource(treatmentList: List<TreatmentDTO>) {
        binding.progressBar.visibility = View.GONE
        adapter.updateDataSource(treatmentList)

        if (treatmentList.isEmpty()) {
            binding.firstTreatmentCL.visibility = View.VISIBLE
            binding.treatmentListCL.visibility = View.GONE
        } else {
            binding.firstTreatmentCL.visibility = View.GONE
            binding.treatmentListCL.visibility = View.VISIBLE
        }
    }
    
    private fun onClickListeners() {
        binding.addFirstTreatmentBTN.setOnClickListener {
            if (!AppDelegate.isDead) {
                if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                    if (allowTreatment) {
                        it.findNavController().navigate(AnimalProfileFragmentDirections.actionAnimalProfileFragmentToAddTreatmentFragment
                            (animalDocId = animalDocId, from = "OPD", reminderDocId = null, animalName = animalDTO?.name))
                    } else {
                        Toast.makeText(requireContext(), "Animal is already admitted. So OPD is not allowed.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_dead, Toast.LENGTH_SHORT).show()
            }
        }
        binding.addAnotherTreatmentBTN.setOnClickListener {
            if (!AppDelegate.isDead) {
                if (AppDelegate.state != CollectionAnimals.TERMINATED) {
                    if (allowTreatment) {
                        it.findNavController().navigate(
                            AnimalProfileFragmentDirections
                                .actionAnimalProfileFragmentToAddTreatmentFragment(
                                    animalDocId = animalDocId,
                                    from = "OPD",
                                    reminderDocId = null,
                                    animalName = animalDTO?.name
                        ))
                    } else {
                        Toast.makeText(requireContext(), "Animal is already admitted. So OPD is not allowed.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.is_terminated, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), R.string.is_dead, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun didScrolledToEnd(position: Int) {}
    
    override fun dataSourceDidUpdate(size: Int) {
        if (size > 0) {
            binding.footerFL.visibility = View.VISIBLE
        }
    }

    override fun didSelectItem(dataItem: TreatmentDTO, position: Int) {
        if (userType == CollectionWhitelistedNumbers.ADMIN || userType == CollectionWhitelistedNumbers.TEAM_LEADER) {
            Log.e("NST", "Treatment id: ${dataItem.id} selected from list")
            findNavController().navigate(
                AnimalProfileFragmentDirections.actionAnimalProfileFragmentToEditTreatmentFragment(
                    treatmentId = dataItem.id,
                    animalDocId = dataItem.animalDocId,
                    totalAdmissionCount = adapter.itemCount,
                    currentAdmissionIndex = position,
                    animalName = animalDTO?.name
                )
            )
        }
    }
}

