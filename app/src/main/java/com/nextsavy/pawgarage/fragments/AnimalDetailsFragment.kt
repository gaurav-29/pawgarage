package com.nextsavy.pawgarage.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.adapters.HistoryAdapter
import com.nextsavy.pawgarage.databinding.FragmentAnimalDetailsBinding
import com.nextsavy.pawgarage.models.AdmissionDTO
import com.nextsavy.pawgarage.models.AnimalDTO
import com.nextsavy.pawgarage.models.DewormingDTO
import com.nextsavy.pawgarage.models.FeedEventDTO
import com.nextsavy.pawgarage.models.FeedType
import com.nextsavy.pawgarage.models.MedicalConditionDTO
import com.nextsavy.pawgarage.models.NetworkFeedEvent
import com.nextsavy.pawgarage.models.ReleaseDTO
import com.nextsavy.pawgarage.models.TreatmentDTO
import com.nextsavy.pawgarage.models.VaccinationDTO
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionDeworming
import com.nextsavy.pawgarage.utils.CollectionFeedEvents
import com.nextsavy.pawgarage.utils.CollectionMedicalConditionsList
import com.nextsavy.pawgarage.utils.CollectionRelease
import com.nextsavy.pawgarage.utils.CollectionTreatment
import com.nextsavy.pawgarage.utils.CollectionVaccination
import com.nextsavy.pawgarage.utils.Helper
import java.util.Calendar
import java.util.Collections


class AnimalDetailsFragment : Fragment() {

    private lateinit var binding: FragmentAnimalDetailsBinding
    private lateinit var adapter: HistoryAdapter

    private val animalDocId: String?
        get() = (this.parentFragment as AnimalProfileFragment?)?.animalDocId

    private val showDewormingAlert: Boolean?
        get() = (this.parentFragment as AnimalProfileFragment?)?.showDewormingAlert

    private var animalModel: AnimalDTO? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnimalDetailsBinding.inflate(inflater, container, false)
        setUpRecyclerView()
        getAnimalDetails()
        getFeedEvents()
        onClickListeners()
        return binding.root
    }
    private fun onClickListeners() {
        binding.addressTV.setOnClickListener {
            // Below code is to open google map in separate activity :

            val uri = "https://www.google.com.tw/maps/place/" + animalModel!!.latitude + "," + animalModel!!.longitude
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(intent)
        }
    }
    private fun getAnimalDetails() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar.visibility = View.VISIBLE
            if (animalDocId != null) {
                Firebase.firestore
                    .collection(CollectionAnimals.name)
                    .document(this.animalDocId!!)
                    .get()
                    .addOnSuccessListener { docSnap ->
                        binding.progressBar.visibility = View.GONE
                        this.animalModel = AnimalDTO.create(docSnap.id, docSnap.data)
                        if (animalModel != null) {
                            configureUI()
                        } else {
                            Toast.makeText(requireContext(), "Unable to fetch animal details", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar.visibility = View.GONE
                        Log.e("NST-M", "Exception: AnimalDetailsFrag > ${exception.localizedMessage}")
                    }
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Animal id is null. Please pass from parent fragment", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureUI() {
        Glide.with(binding.animalIV.context).load(animalModel!!.downloadUrl).placeholder(R.drawable.paw_placeholder).into(binding.animalIV)
        if (animalModel!!.downloadUrl.isNotBlank()) {
            binding.animalIV.setOnClickListener {
                val b = Bundle()
                b.putString("photoUrl", animalModel!!.downloadUrl)
                findNavController().navigate(R.id.photoViewDialog, b)
            }
        }
        binding.nameTV.text = animalModel!!.name
        binding.ipdTV.text = animalModel!!.type
        binding.maleTV.text = animalModel!!.gender
        binding.dogTV.text = animalModel!!.species
        binding.descriptionTV.text = animalModel!!.description
        val mSpannableString = SpannableString(animalModel!!.address)
        mSpannableString.setSpan(UnderlineSpan(), 0, mSpannableString.length, 0)
        binding.addressTV.text = mSpannableString
    }
    private fun getFeedEvents() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar.visibility = View.VISIBLE
            if (animalDocId != null) {
                Firebase.firestore
                    .collection(CollectionFeedEvents.name)
                    .whereEqualTo(CollectionFeedEvents.kAnimalDocId, animalDocId)
                    .whereEqualTo(CollectionFeedEvents.kIsArchive, false)
                    .orderBy(CollectionFeedEvents.kCreatedAt, Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { querySnapShot ->
                        binding.progressBar.visibility = View.GONE
                        if (querySnapShot.isEmpty) {
                            Log.e("NST-M", "FeedEvents are empty!!")
                        } else {
                            val feedEvents: MutableList<FeedEventDTO> = mutableListOf()
                            val networkFeedEvents = querySnapShot.toObjects(NetworkFeedEvent::class.java)
                            for (networkFeedEvent in networkFeedEvents) {
                                try {
                                    val feedEvent: FeedEventDTO? = FeedEventDTO.create(networkFeedEvent)
                                    if (feedEvent != null) {
                                        feedEvents.add(feedEvent)
                                    }
                                } catch (e: Exception) {
                                    Log.e("NST-M", "Exception: AnimalDetailsFrag > getFeedEvents: ${e.message}")
                                }
                            }
                            var count = 0
                            feedEvents.forEach { feedEvent ->
                                getFeedObject(feedEvent) { triple ->
                                    count++
                                    feedEvents.first { mFeedEvent -> mFeedEvent.id == triple.first }.feedObject = triple.third
                                    if (count == feedEvents.size) {
//                                        feedEvents.filter { nFeedEvent -> (nFeedEvent.feedType != FeedType.REGISTRATION) && nFeedEvent.feedObject == null }
                                        reloadRV(feedEvents)
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar.visibility = View.GONE
                        Log.e("NST-M", "Exception: getFeedEvents > ${exception.localizedMessage}")
                    }
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Animal id is null. Please pass from parent fragment", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun reloadRV(feedEvents: List<FeedEventDTO>) {
        Collections.sort(feedEvents, Comparator<FeedEventDTO> { f1, f2 -> f2.feedDate?.compareTo(f1.feedDate) ?: -1 })
        adapter.replaceDataSource(this.animalModel!!.name, feedEvents)

        askForDeworming()
    }

    private fun getFeedObject(feedEvent: FeedEventDTO, callback: (Triple<String, FeedType, *>) -> Unit) {
        when (feedEvent.feedType) {
            FeedType.REGISTRATION -> {
                val pastCalendar = Calendar.getInstance()
                pastCalendar.set(Calendar.YEAR, 1970)
                pastCalendar.set(Calendar.MONTH, 0)
                pastCalendar.set(Calendar.DAY_OF_YEAR, 1)
                feedEvent.feedDate = pastCalendar.time
                callback.invoke(Triple(feedEvent.id, feedEvent.feedType, null))
            }
            FeedType.ACTIVATED -> {
                feedEvent.feedDate = feedEvent.createdAt.toDate()
                callback.invoke(Triple(feedEvent.id, feedEvent.feedType, null))
            }
            FeedType.TERMINATED -> {
                feedEvent.feedDate = feedEvent.createdAt.toDate()
                callback.invoke(Triple(feedEvent.id, feedEvent.feedType, null))
            }
            FeedType.ADMISSION -> {
                Firebase.firestore
                    .collection(CollectionAdmission.name)
                    .document(feedEvent.feedObjectId!!)
                    .get()
                    .addOnSuccessListener { docSnapshot ->
                        val admission = AdmissionDTO.create(docSnapshot.id, docSnapshot.data)
                        feedEvent.feedDate = admission?.admissionDate?.toDate()
                        if (!admission?.medicalConditionIds.isNullOrEmpty()) {
                            getMedicalConditionFor(admission!!.medicalConditionIds) { pair: Pair<Exception?, List<MedicalConditionDTO>?> ->
                                if (pair.second != null) {
                                    admission.medicalConditionNames = pair.second!!.map { mc -> mc.name }
                                }
                                callback.invoke(Triple(feedEvent.id, feedEvent.feedType, admission))
                            }
                        } else {
                            callback.invoke(Triple(feedEvent.id, feedEvent.feedType, admission))
                        }
                    }.addOnFailureListener {
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, null))
                    }
            }
            FeedType.TREATMENT -> {
                Firebase.firestore
                    .collection(CollectionTreatment.name)
                    .document(feedEvent.feedObjectId!!)
                    .get()
                    .addOnSuccessListener { docSnapshot ->
                        val treatment = TreatmentDTO.create(docSnapshot.id, docSnapshot.data)
                        feedEvent.feedDate = treatment?.treatmentDate?.toDate()
                        if (!treatment?.medicalConditionIds.isNullOrEmpty()) {
                            getMedicalConditionFor(treatment!!.medicalConditionIds) { pair: Pair<Exception?, List<MedicalConditionDTO>?> ->
                                if (pair.second != null) {
                                    treatment.medicalConditionNames = pair.second!!.map { mc -> mc.name }
                                }
                                callback.invoke(Triple(feedEvent.id, feedEvent.feedType, treatment))
                            }
                        } else {
                            callback.invoke(Triple(feedEvent.id, feedEvent.feedType, treatment))
                        }
                    }.addOnFailureListener {
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, null))
                    }
            }
            FeedType.VACCINE -> {
                Firebase.firestore
                    .collection(CollectionVaccination.name)
                    .document(feedEvent.feedObjectId!!)
                    .get()
                    .addOnSuccessListener { docSnapshot ->
                        val vaccine = VaccinationDTO.create(docSnapshot.id, docSnapshot.data)
                        feedEvent.feedDate = vaccine?.vaccinationDate?.toDate()
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, vaccine))
                    }.addOnFailureListener {
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, null))
                    }
            }
            FeedType.DEWORMING -> {
                Firebase.firestore
                    .collection(CollectionDeworming.name)
                    .document(feedEvent.feedObjectId!!)
                    .get()
                    .addOnSuccessListener { docSnapshot ->
//                        val deworming = docSnapshot.toObject(VaccinationDewormingModel::class.java)
                        val deworming = DewormingDTO.create(docSnapshot.id, docSnapshot.data)
                        feedEvent.feedDate = deworming?.dewormingDate?.toDate()
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, deworming))
                    }.addOnFailureListener {
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, null))
                    }
            }
            FeedType.ADOPTED -> {
                Firebase.firestore
                    .collection(CollectionRelease.name)
                    .document(feedEvent.feedObjectId!!)
                    .get()
                    .addOnSuccessListener { docSnapshot ->
                        val adopt = ReleaseDTO.create(docSnapshot.id, docSnapshot.data)
                        feedEvent.feedDate = adopt?.releaseDate?.toDate()
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, adopt))
                    }.addOnFailureListener {
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, null))
                    }
            }
            FeedType.RELEASE -> {
                Firebase.firestore
                    .collection(CollectionRelease.name)
                    .document(feedEvent.feedObjectId!!)
                    .get()
                    .addOnSuccessListener { docSnapshot ->
                        val release = ReleaseDTO.create(docSnapshot.id, docSnapshot.data)
                        feedEvent.feedDate = release?.releaseDate?.toDate()
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, release))
                    }.addOnFailureListener {
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, null))
                    }
            }
            FeedType.DEATH -> {
                Firebase.firestore
                    .collection(CollectionRelease.name)
                    .document(feedEvent.feedObjectId!!)
                    .get()
                    .addOnSuccessListener { docSnapshot ->
                        val release = ReleaseDTO.create(docSnapshot.id, docSnapshot.data)
                        feedEvent.feedDate = release?.releaseDate?.toDate()
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, release))
                    }.addOnFailureListener {
                        callback.invoke(Triple(feedEvent.id, feedEvent.feedType, null))
                    }
            }

            else -> {}
        }
    }
    private fun setUpRecyclerView() {
        binding.historyRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistoryAdapter("", arrayListOf())
        binding.historyRV.adapter = adapter
    }

    private fun getMedicalConditionFor(medConditionIds: List<String>, callback:(Pair<Exception?, List<MedicalConditionDTO>?>) -> Unit) {
        val medicalConditionTaskSnapshotList = medConditionIds.map { Firebase.firestore.collection(CollectionMedicalConditionsList.name).document(it).get() }
        val allMedicalConditionsTask = Tasks.whenAllSuccess<DocumentSnapshot>(medicalConditionTaskSnapshotList)
        allMedicalConditionsTask.addOnSuccessListener { docSnapshotList ->
            val medConditions = docSnapshotList.mapNotNull { doc -> MedicalConditionDTO.create(doc?.id ?: "", doc?.data) }.filter { mc -> !mc.isArchive }
            callback.invoke(Pair(null, medConditions))
        }.addOnFailureListener { exception ->
            Log.e("NST", "Error AnimalDetailsFragment > Get Medical Conditions:\t${exception.message}")
            callback.invoke(Pair(exception, null))
        }
    }

    private fun askForDeworming() {
        if (showDewormingAlert != null && showDewormingAlert == true) {
            (this.parentFragment as AnimalProfileFragment?)?.showDewormingAlert = false
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Do you want to schedule deworming for this animal ?")
            builder.setPositiveButton("Yes") { dialog, which ->
                (parentFragment as AnimalProfileFragment?)?.categoryVP?.setCurrentItem(2, true)
            }
            builder.setNegativeButton("No") { dialog, which ->

            }
            builder.create()
            builder.show()
        }
    }
}