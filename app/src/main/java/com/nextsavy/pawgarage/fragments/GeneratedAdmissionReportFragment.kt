package com.nextsavy.pawgarage.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.AdmissionReportAdapter
import com.nextsavy.pawgarage.databinding.FragmentGeneratedAdmissionReportBinding
import com.nextsavy.pawgarage.models.GenericUserDTO
import com.nextsavy.pawgarage.models.ReportsGeneralModel
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionWhitelistedNumbers
import com.nextsavy.pawgarage.utils.Helper
import org.apache.poi.hssf.usermodel.HSSFRow
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GeneratedAdmissionReportFragment : Fragment(), RecyclerViewPagingInterface<ReportsGeneralModel> {

    private lateinit var binding: FragmentGeneratedAdmissionReportBinding
    var lastDocument: DocumentSnapshot? = null
    var reachedEnd = false
    var pageSize: Long = 15
    private lateinit var adapter: AdmissionReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGeneratedAdmissionReportBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "Admission Report"
        binding.toolbarOne.generalImgToolbarOne.visibility = View.VISIBLE
        binding.toolbarOne.generalImgToolbarOne.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_download))

        onClickListeners()
        initiateReportData()
        getAdmissionReportData()

        return binding.root
    }
    private fun onClickListeners() {
        binding.toolbarOne.generalImgToolbarOne.setOnClickListener {
            if (checkPermission()) {
                getAllData()
            } else {
                requestPermission()
            }
        }
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
    }
    private fun initiateReportData() {
        reachedEnd = false
        lastDocument = null
        binding.reportRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdmissionReportAdapter(arrayListOf(),this)
        binding.reportRV.adapter = adapter
    }
    private fun getAdmissionReportData() {
        if (Helper.isInternetAvailable(requireContext())) {

            val dog = arguments?.getString(CollectionAnimals.DOG)
            val cat = arguments?.getString(CollectionAnimals.CAT)
            val other = arguments?.getString(CollectionAnimals.OTHER)
            val male = arguments?.getString(CollectionAnimals.MALE)
            val female = arguments?.getString(CollectionAnimals.FEMALE)
            val fromDate = arguments?.getString("FromDate")
            val toDate = arguments?.getString("ToDate")

            val userSelectedSpecies = arrayListOf<String>()
            if (dog != null) {
                userSelectedSpecies.add(dog)
            }
            if (cat != null) {
                userSelectedSpecies.add(cat)
            }
            if (other != null) {
                userSelectedSpecies.add(other)
            }

            val userSelectedGender = arrayListOf<String>()
            if (male != null) {
                userSelectedGender.add(male)
            }
            if (female != null) {
                userSelectedGender.add(female)
            }

            val fromDateInDateFormat = fromDate?.let { SimpleDateFormat("dd/MM/yy", Locale.US).parse(it) }
            val toDateInDateFormat = toDate?.let { SimpleDateFormat("dd/MM/yy", Locale.US).parse(it) }

            val calendarFromDate = Calendar.getInstance()
            if (fromDateInDateFormat != null) {
                calendarFromDate.time = fromDateInDateFormat
            }
            val calendarToDate = Calendar.getInstance()
            if (toDateInDateFormat != null) {
                calendarToDate.time = toDateInDateFormat
                calendarToDate.add(Calendar.DAY_OF_MONTH, 1)  // 1 day added to get less than date of next day at 00:00:00 time.
            }
            Log.e("DATA-R1", "${calendarFromDate.time}, ${calendarToDate.time}")

            val reportList = arrayListOf<ReportsGeneralModel>()
            val animalDocIdList = arrayListOf<String>()

            if (!reachedEnd) {
                binding.progressBar.visibility = View.VISIBLE

                var query = Firebase.firestore.collection(CollectionAdmission.name)
                    .whereEqualTo(CollectionAdmission.kIsArchive, false)
                    .whereGreaterThanOrEqualTo(CollectionAdmission.kAdmissionDate, calendarFromDate.time)
                    .whereLessThan(CollectionAdmission.kAdmissionDate, calendarToDate.time)
                    .orderBy(CollectionAdmission.kAdmissionDate, Query.Direction.ASCENDING)

                if (lastDocument != null) {
                    query = query.startAfter(lastDocument!!)
                }
                query = query.limit(pageSize)
                query.get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.documents.isEmpty()) {
                            binding.progressBar.visibility = View.GONE
                            binding.noReportsTV.visibility = View.VISIBLE
                        }
                        reachedEnd = querySnapshot.documents.size < pageSize

                        if (lastDocument == null) {
                            for (document in querySnapshot.documents) {
                                val model = ReportsGeneralModel()
                                model.documentId = document.id
                                model.animalDocId = document.data?.get(CollectionAdmission.kAnimalDocId) as String
                                model.date = document.data?.get(CollectionAdmission.kAdmissionDate) as Timestamp

                                reportList.add(model)
                                animalDocIdList.add(model.animalDocId)
                            }

                            val distinctAnimalIdList = animalDocIdList.distinct()

                            if (distinctAnimalIdList.isNotEmpty()) {
                                Firebase.firestore.collection(CollectionAnimals.name)
                                    .whereIn(FieldPath.documentId(), distinctAnimalIdList)
                                    .whereEqualTo(CollectionAnimals.kIsArchive, false)
                                    .get()
                                    .addOnSuccessListener {
                                        for (document in it) {
                                            for (i in reportList) {
                                                if (document.id == i.animalDocId) {
                                                    i.animalName = document.data[CollectionAnimals.kName] as String
                                                    i.species = document.data[CollectionAnimals.kSpecies] as String
                                                    i.location = document.data[CollectionAnimals.kLocationAddress] as String
                                                    i.gender = document.data[CollectionAnimals.kGender] as String
                                                    i.isArchive = document.data[CollectionAnimals.kIsArchive] as Boolean
                                                }
                                            }
                                        }
                                        reportList.removeIf { item -> item.isArchive || !userSelectedSpecies.contains(item.species) }
                                        reportList.removeIf { item -> !userSelectedGender.contains(item.gender) }

                                        if (reportList.isNotEmpty()) {
                                            adapter.updateDataSource(reportList)
                                        } else {
                                            binding.progressBar.visibility = View.GONE
                                            binding.noReportsTV.visibility = View.VISIBLE
                                        }
                                        binding.progressBar.visibility = View.GONE
                                    }
                                    .addOnFailureListener {
                                        binding.progressBar.visibility = View.GONE
                                        Log.e("FAILURE-getAdmissionReportData()", it.message.toString())
                                        Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            for (document in querySnapshot.documents) {
                                val model = ReportsGeneralModel()
                                model.documentId = document.id
                                model.animalDocId = document.data?.get(CollectionAdmission.kAnimalDocId) as String
                                model.date = document.data?.get(CollectionAdmission.kAdmissionDate) as Timestamp

                                reportList.add(model)
                                animalDocIdList.add(model.animalDocId)
                            }

                            val distinctAnimalIdList = animalDocIdList.distinct()

                            if (distinctAnimalIdList.isNotEmpty()) {

                                Firebase.firestore.collection(CollectionAnimals.name)
                                    .whereIn(FieldPath.documentId(), distinctAnimalIdList)
                                    .whereEqualTo(CollectionAnimals.kIsArchive, false)
                                    .get()
                                    .addOnSuccessListener {
                                        binding.progressBar.visibility = View.GONE
                                        for (document in it) {
                                            for (i in reportList) {
                                                if (document.id == i.animalDocId) {
                                                    i.animalName = document.data[CollectionAnimals.kName] as String
                                                    i.species = document.data[CollectionAnimals.kSpecies] as String
                                                    i.location = document.data[CollectionAnimals.kLocationAddress] as String
                                                    i.gender = document.data[CollectionAnimals.kGender] as String
                                                    i.isArchive = document.data[CollectionAnimals.kIsArchive] as Boolean
                                                }
                                            }
                                        }
                                        reportList.removeIf { item -> item.isArchive || !userSelectedSpecies.contains(item.species) }
                                        reportList.removeIf { item -> !userSelectedGender.contains(item.gender) }

                                        if (reportList.isNotEmpty()) {
                                            adapter.injectNextBatch(reportList)
                                        } else {
                                            binding.progressBar.visibility = View.GONE
                                            binding.noReportsTV.visibility = View.VISIBLE
                                        }
                                        binding.progressBar.visibility = View.GONE
                                    }
                                    .addOnFailureListener {
                                        binding.progressBar.visibility = View.GONE
                                        Log.e("FAILURE-getAdmissionReportData()", it.message.toString())
                                        Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        lastDocument = querySnapshot.documents.lastOrNull()
                        //binding.progressBar.visibility = View.GONE
                    }
                    .addOnFailureListener { qException ->
                        binding.progressBar.visibility = View.GONE
                        Log.e("FAILURE-getAdmissionReportData()", "Exception: GeneratedAdmissionReportFrag > ${qException.localizedMessage}")
                        Toast.makeText(requireContext(), qException.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
            } else {
                Log.e("NST-M", "Reached end in load more")
            }

        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }
    private fun getAllData() {
        if (Helper.isInternetAvailable(requireContext())) {

            binding.progressBar.visibility = View.VISIBLE

            val dog = arguments?.getString(CollectionAnimals.DOG)
            val cat = arguments?.getString(CollectionAnimals.CAT)
            val other = arguments?.getString(CollectionAnimals.OTHER)
            val male = arguments?.getString(CollectionAnimals.MALE)
            val female = arguments?.getString(CollectionAnimals.FEMALE)
            val fromDate = arguments?.getString("FromDate")
            val toDate = arguments?.getString("ToDate")

            val userSelectedSpecies = arrayListOf<String>()
            if (dog != null) {
                userSelectedSpecies.add(dog)
            }
            if (cat != null) {
                userSelectedSpecies.add(cat)
            }
            if (other != null) {
                userSelectedSpecies.add(other)
            }

            val userSelectedGender = arrayListOf<String>()
            if (male != null) {
                userSelectedGender.add(male)
            }
            if (female != null) {
                userSelectedGender.add(female)
            }

            val fromDateInDateFormat = fromDate?.let { SimpleDateFormat("dd/MM/yy", Locale.US).parse(it) }
            val toDateInDateFormat = toDate?.let { SimpleDateFormat("dd/MM/yy", Locale.US).parse(it) }

            val calendarFromDate = Calendar.getInstance()
            if (fromDateInDateFormat != null) {
                calendarFromDate.time = fromDateInDateFormat
            }
            val calendarToDate = Calendar.getInstance()
            if (toDateInDateFormat != null) {
                calendarToDate.time = toDateInDateFormat
                calendarToDate.add(Calendar.DAY_OF_MONTH, 1)  // 1 day added to get less than date of next day at 00:00:00 time.
            }
            Log.e("DATA-R1", "${calendarFromDate.time}, ${calendarToDate.time}")

            val reportList = arrayListOf<ReportsGeneralModel>()
            val animalDocIdList = arrayListOf<String>()

            Firebase.firestore.collection(CollectionAdmission.name)
                .whereEqualTo(CollectionAdmission.kIsArchive, false)
                .whereGreaterThanOrEqualTo(CollectionAdmission.kAdmissionDate, calendarFromDate.time)
                .whereLessThan(CollectionAdmission.kAdmissionDate, calendarToDate.time)
                .orderBy(CollectionAdmission.kAdmissionDate, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.documents.isEmpty()) {
                        binding.progressBar.visibility = View.GONE
                        binding.noReportsTV.visibility = View.VISIBLE
                    }
                    for (document in querySnapshot.documents) {
                        val model = ReportsGeneralModel()
                        model.documentId = document.id
                        model.animalDocId = document.data?.get(CollectionAdmission.kAnimalDocId) as String
                        model.date = document.data?.get(CollectionAdmission.kAdmissionDate) as Timestamp

                        reportList.add(model)

                        animalDocIdList.add(model.animalDocId)
                    }

                    val distinctAnimalIdList = animalDocIdList.distinct()
/*
                    if (distinctAnimalIdList.isNotEmpty()) {
                        Firebase.firestore.collection(CollectionAnimals.name)
                            .whereIn(FieldPath.documentId(), distinctAnimalIdList)
                            .whereEqualTo(CollectionAnimals.kIsArchive, false)
                            .get()
                            .addOnSuccessListener {
                                for (document in it) {
                                    for (i in reportList) {
                                        if (document.id == i.animalDocId) {
                                            i.animalName = document.data[CollectionAnimals.kName] as String
                                            i.species = document.data[CollectionAnimals.kSpecies] as String
                                            i.location = document.data[CollectionAnimals.kLocationAddress] as String
                                            i.gender = document.data[CollectionAnimals.kGender] as String
                                            i.isArchive = document.data[CollectionAnimals.kIsArchive] as Boolean
                                        }
                                    }
                                }
                                reportList.removeIf { item -> item.isArchive || !userSelectedSpecies.contains(item.species) }
                                reportList.removeIf { item -> !userSelectedGender.contains(item.gender) }

                                if (reportList.isNotEmpty()) {
                                    generateExcelSheet(reportList)
                                } else {
                                    binding.progressBar.visibility = View.GONE
                                    binding.noReportsTV.visibility = View.VISIBLE
                                }
                                binding.progressBar.visibility = View.GONE
                            }
                            .addOnFailureListener {
                                binding.progressBar.visibility = View.GONE
                                Log.e("FAILURE-getAllData()-Distinct", it.toString())
                                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
                            }
                    }

 */
                    if (distinctAnimalIdList.isNotEmpty()) {

                        val animalsTaskSnapshotList = distinctAnimalIdList.map { Firebase.firestore.collection(CollectionAnimals.name).document(it).get() }
                        val finalTask = Tasks.whenAllSuccess<DocumentSnapshot>(animalsTaskSnapshotList)

                        finalTask.addOnSuccessListener { docSnapshotList ->
                            for (animal in docSnapshotList) {
                                for (i in reportList) {
                                    if (animal.id == i.animalDocId) {
                                        if (animal.data != null) {
                                            i.animalName = animal.data!![CollectionAnimals.kName] as String
                                            i.species = animal.data!![CollectionAnimals.kSpecies] as String
                                            i.location = animal.data!![CollectionAnimals.kLocationAddress] as String
                                            i.gender = animal.data!![CollectionAnimals.kGender] as String
                                            i.isArchive = animal.data!![CollectionAnimals.kIsArchive] as Boolean
                                        } else {
                                            Toast.makeText(requireContext(), "Unable to fetch the data for Excel Sheet.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                            reportList.removeIf { item -> item.isArchive || !userSelectedSpecies.contains(item.species) }
                            reportList.removeIf { item -> !userSelectedGender.contains(item.gender) }

                            if (reportList.isNotEmpty()) {
                                generateExcelSheet(reportList)
                            } else {
                                binding.progressBar.visibility = View.GONE
                                binding.noReportsTV.visibility = View.VISIBLE
                            }
                            binding.progressBar.visibility = View.GONE
                        }.addOnFailureListener { exception ->
                            Log.e("NST", "Error in Animal task chains\n${exception.message ?: "Unknown error"}")
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Log.e("FAILURE-getAllData()-All", it.message.toString())
                    Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }
    private fun generateExcelSheet(reportList: ArrayList<ReportsGeneralModel>) {

        val  hssfWorkbook = HSSFWorkbook()
        val hssfSheet: HSSFSheet = hssfWorkbook.createSheet("MySheet")

        val hssfRow: HSSFRow = hssfSheet.createRow(0)
        hssfRow.createCell(0).setCellValue("Name")
        hssfRow.createCell(1).setCellValue("Date")
        hssfRow.createCell(2).setCellValue("Location")
        hssfRow.createCell(3).setCellValue("Gender")

        for (i in 0 until reportList.size) {

            val hssfRow1: HSSFRow = hssfSheet.createRow(i+1)

            val date: Date = reportList[i].date!!.toDate()
            val dateToShow = SimpleDateFormat("dd/MM/yy", Locale.US).format(date)

            hssfRow1.createCell(0).setCellValue(reportList[i].animalName)
            hssfRow1.createCell(1).setCellValue(dateToShow)
            hssfRow1.createCell(2).setCellValue(reportList[i].location)
            hssfRow1.createCell(3).setCellValue(reportList[i].gender)
        }
        saveWorkBook(hssfWorkbook)
    }
    private fun saveWorkBook(hssfWorkbook: HSSFWorkbook) {

        val dir = File(Environment.getExternalStorageDirectory(), "Paw Garage")
        if (!dir.exists()) {
            dir.mkdir()
        }
        val subDir = File(dir, "Admission Reports")
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
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                storageActivityRequestLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                storageActivityRequestLauncher.launch(intent)
            }
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), 100)
        }
    }
    private val storageActivityRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                getAllData()
            } else {
                Toast.makeText(requireContext(), "Storage permission is denied.", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Android is below 11 (R). No requirement for code here.
        }
    }
    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val write = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val read = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty()) {
                val write = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val read = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (write && read) {
                    getAllData()
                } else {
                    Toast.makeText(requireContext(), "Storage permission is denied.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun didScrolledToEnd(position: Int) {
        getAdmissionReportData()
    }
    override fun dataSourceDidUpdate(size: Int) {

    }
    override fun didSelectItem(dataItem: ReportsGeneralModel, position: Int) {

    }

}