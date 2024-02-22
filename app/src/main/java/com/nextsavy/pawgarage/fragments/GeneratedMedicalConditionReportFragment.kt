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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.MedicalConditionReportAdapter
import com.nextsavy.pawgarage.databinding.FragmentGeneratedMedicalConditionReportBinding
import com.nextsavy.pawgarage.models.ReportsGeneralModel
import com.nextsavy.pawgarage.utils.CollectionAdmission
import com.nextsavy.pawgarage.utils.CollectionAnimals
import com.nextsavy.pawgarage.utils.CollectionTreatment
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

class GeneratedMedicalConditionReportFragment : Fragment(), RecyclerViewPagingInterface<ReportsGeneralModel> {

    private lateinit var binding: FragmentGeneratedMedicalConditionReportBinding
    private lateinit var adapter: MedicalConditionReportAdapter
    private lateinit var finalReportList: ArrayList<ReportsGeneralModel>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       binding = FragmentGeneratedMedicalConditionReportBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "Medical Condition Report"
        binding.toolbarOne.generalImgToolbarOne.visibility = View.VISIBLE
        binding.toolbarOne.generalImgToolbarOne.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_download))

        onClickListeners()
        initiateReportData()
        getReportDataFromAdmission()

        return binding.root
    }
    private fun initiateReportData() {
        binding.reportRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = MedicalConditionReportAdapter(arrayListOf(),this)
        binding.reportRV.adapter = adapter
    }

    private fun getReportDataFromAdmission() {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar.visibility = View.VISIBLE

            val fromDate = arguments?.getString("FromDate")
            val toDate = arguments?.getString("ToDate")

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

            val reportListFromAdmission = arrayListOf<ReportsGeneralModel>()

            Firebase.firestore.collection(CollectionAdmission.name)
                .whereEqualTo(CollectionAdmission.kIsArchive, false)
                .whereGreaterThanOrEqualTo(CollectionAdmission.kAdmissionDate, calendarFromDate.time)
                .whereLessThan(CollectionAdmission.kAdmissionDate, calendarToDate.time)
                .get()
                .addOnSuccessListener {
                    if (it.documents.isEmpty()) {
                        binding.progressBar.visibility = View.GONE
                        binding.noReportsTV.visibility = View.VISIBLE
                    }
                    for (document in it.documents) {
                        val model = ReportsGeneralModel()
                        model.animalDocId = document.data?.get(CollectionAdmission.kAnimalDocId) as String
                        model.date = document.data?.get(CollectionAdmission.kAdmissionDate) as Timestamp
                        model.conditions = document.data?.get(CollectionAdmission.kMedicalConditions) as String

                        reportListFromAdmission.add(model)
                    }

                    getReportDataFromTreatment(reportListFromAdmission)
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Log.e("FAILURE-getReportDataFromAdmission()", it.message.toString())
                    Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
                }
        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getReportDataFromTreatment(reportListFromAdmission: ArrayList<ReportsGeneralModel>) {
        if (Helper.isInternetAvailable(requireContext())) {
            binding.progressBar.visibility = View.VISIBLE

            finalReportList = arrayListOf()

            val fromDate = arguments?.getString("FromDate")
            val toDate = arguments?.getString("ToDate")

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

            val reportListFromTreatment = arrayListOf<ReportsGeneralModel>()

            Firebase.firestore.collection(CollectionTreatment.name)
                .whereEqualTo(CollectionTreatment.kIsArchive, false)
                .whereGreaterThanOrEqualTo(CollectionTreatment.kTreatmentDate, calendarFromDate.time)
                .whereLessThan(CollectionTreatment.kTreatmentDate, calendarToDate.time)
                .get()
                .addOnSuccessListener {
                    if (it.documents.isEmpty()) {
                        binding.progressBar.visibility = View.GONE
                        binding.noReportsTV.visibility = View.VISIBLE
                    }
                    for (document in it.documents) {
                        val model = ReportsGeneralModel()
                        model.animalDocId = document.data?.get(CollectionTreatment.kAnimalDocId) as String
                        model.date = document.data?.get(CollectionTreatment.kTreatmentDate) as Timestamp
                        model.conditions = document.data?.get(CollectionTreatment.kMedicalConditions) as String

                        reportListFromTreatment.add(model)
                    }

                    finalReportList = reportListFromAdmission.plus(reportListFromTreatment) as ArrayList

                    getReportDataFromAnimalTable2(finalReportList)
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Log.e("FAILURE-getReportDataFromTreatment()", it.message.toString())
                    Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
                }
        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getReportDataFromAnimalTable2(finalReportList: ArrayList<ReportsGeneralModel>) {
        if (Helper.isInternetAvailable(requireContext())) {

            val dog = arguments?.getString(CollectionAnimals.DOG)
            val cat = arguments?.getString(CollectionAnimals.CAT)
            val other = arguments?.getString(CollectionAnimals.OTHER)
            val ipd = arguments?.getString(CollectionAnimals.IPD)
            val opd = arguments?.getString(CollectionAnimals.OPD)
            val male = arguments?.getString(CollectionAnimals.MALE)
            val female = arguments?.getString(CollectionAnimals.FEMALE)
            val conditionList = arguments?.getStringArrayList("Conditions")

            Log.e("DATA-1", "$dog, $cat, $other, $ipd, $opd, $male, $female, $conditionList")

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

            val userSelectedType = arrayListOf<String>()
            if (ipd != null) {
                userSelectedType.add(ipd)
            }
            if (opd != null) {
                userSelectedType.add(opd)
            }

            val userSelectedGender = arrayListOf<String>()
            if (male != null) {
                userSelectedGender.add(male)
            }
            if (female != null) {
                userSelectedGender.add(female)
            }

            val animalDocIdList = finalReportList.map { it.animalDocId }

            binding.progressBar.visibility = View.VISIBLE

            val animalsTaskSnapshotList = animalDocIdList.map { Firebase.firestore.collection(CollectionAnimals.name).document(it).get() }
            val finalTask = Tasks.whenAllSuccess<DocumentSnapshot>(animalsTaskSnapshotList)

            finalTask.addOnSuccessListener { docSnapshotList ->
                if (docSnapshotList.isEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    binding.noReportsTV.visibility = View.VISIBLE
                }
                for (document in docSnapshotList) {
                    for (i in finalReportList) {
                        if (document.id == i.animalDocId) {
                            if (document.data != null) {
                                i.animalName = document.data!![CollectionAnimals.kName] as String
                                i.species = document.data!![CollectionAnimals.kSpecies] as String
                                i.type = document.data!![CollectionAnimals.kType] as String
                                i.gender = document.data!![CollectionAnimals.kGender] as String
                                i.isArchive = document.data!![CollectionAnimals.kIsArchive] as Boolean
                            } else {
                                Toast.makeText(requireContext(), "Unable to fetch the data for Excel Sheet.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                finalReportList.removeIf { item -> item.isArchive }
                finalReportList.removeIf { item -> !userSelectedSpecies.contains(item.species) }
                finalReportList.removeIf { item -> !userSelectedType.contains(item.type) }
                finalReportList.removeIf { item -> !userSelectedGender.contains(item.gender) }

                val finalReportList2 = ArrayList<ReportsGeneralModel>()
                if (conditionList != null) {
                    for (i in conditionList) {
                        for (j in finalReportList) {
                            if (j.conditions.contains(i)) {
                                if (!finalReportList2.contains(j)) {
                                    finalReportList2.add(j)
                                }
                            }
                        }
                    }
                }

                if (finalReportList2.isNotEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    finalReportList2.sortBy { item -> item.date }
                    adapter.updateDataSource(finalReportList2)

                } else if (finalReportList.isNotEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    finalReportList.removeIf { item -> item.conditions == "" }
                    finalReportList.sortBy { item -> item.date }
                    adapter.updateDataSource(finalReportList)
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.noReportsTV.visibility = View.VISIBLE
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Log.e("FAILURE-getReportDataFromAnimalTable()", it.message.toString())
                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onClickListeners() {
        binding.toolbarOne.generalImgToolbarOne.setOnClickListener {
            if (checkPermission()) {
                getAllData(finalReportList)
            } else {
                requestPermission()
            }
        }
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
    }

    private fun getAllData(finalReportList: ArrayList<ReportsGeneralModel>) {
        if (Helper.isInternetAvailable(requireContext())) {

            val dog = arguments?.getString(CollectionAnimals.DOG)
            val cat = arguments?.getString(CollectionAnimals.CAT)
            val other = arguments?.getString(CollectionAnimals.OTHER)
            val ipd = arguments?.getString(CollectionAnimals.IPD)
            val opd = arguments?.getString(CollectionAnimals.OPD)
            val male = arguments?.getString(CollectionAnimals.MALE)
            val female = arguments?.getString(CollectionAnimals.FEMALE)
            val conditionList = arguments?.getStringArrayList("Conditions")

            Log.e("DATA-1", "$dog, $cat, $other, $ipd, $opd, $male, $female, $conditionList")

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

            val userSelectedType = arrayListOf<String>()
            if (ipd != null) {
                userSelectedType.add(ipd)
            }
            if (opd != null) {
                userSelectedType.add(opd)
            }

            val userSelectedGender = arrayListOf<String>()
            if (male != null) {
                userSelectedGender.add(male)
            }
            if (female != null) {
                userSelectedGender.add(female)
            }

            val animalDocIdList = finalReportList.map { it.animalDocId }

            binding.progressBar.visibility = View.VISIBLE

            val animalsTaskSnapshotList = animalDocIdList.map { Firebase.firestore.collection(CollectionAnimals.name).document(it).get() }
            val finalTask = Tasks.whenAllSuccess<DocumentSnapshot>(animalsTaskSnapshotList)

            finalTask.addOnSuccessListener { docSnapshotList ->
                    if (docSnapshotList.isEmpty()) {
                        binding.progressBar.visibility = View.GONE
                        binding.noReportsTV.visibility = View.VISIBLE
                    }

                    for (document in docSnapshotList) {
                        for (i in finalReportList) {
                            if (document.id == i.animalDocId) {
                                if (document.data != null) {
                                    i.animalName = document.data!![CollectionAnimals.kName] as String
                                    i.species = document.data!![CollectionAnimals.kSpecies] as String
                                    i.type = document.data!![CollectionAnimals.kType] as String
                                    i.gender = document.data!![CollectionAnimals.kGender] as String
                                    i.isArchive = document.data!![CollectionAnimals.kIsArchive] as Boolean
                                } else {
                                    Toast.makeText(requireContext(), "Unable to fetch the data for Excel Sheet.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    finalReportList.removeIf { item -> item.isArchive }
                    finalReportList.removeIf { item -> !userSelectedSpecies.contains(item.species) }
                    finalReportList.removeIf { item -> !userSelectedType.contains(item.type) }
                    finalReportList.removeIf { item -> !userSelectedGender.contains(item.gender) }

                    val finalReportList2 = ArrayList<ReportsGeneralModel>()
                    if (conditionList != null) {
                        for (i in conditionList) {
                            for (j in finalReportList) {
                                if (j.conditions.contains(i)) {
                                    if (!finalReportList2.contains(j)) {
                                        finalReportList2.add(j)
                                    }
                                }
                            }
                        }
                    }

                    if (finalReportList2.isNotEmpty()) {
                        binding.progressBar.visibility = View.GONE
                        generateExcelSheet(finalReportList2)

                    } else if (finalReportList.isNotEmpty()) {
                        binding.progressBar.visibility = View.GONE
                        finalReportList.removeIf { item -> item.conditions == "" }
                        finalReportList.sortBy { item -> item.date }
                        generateExcelSheet(finalReportList)
                    } else {
                        binding.progressBar.visibility = View.GONE
                        binding.noReportsTV.visibility = View.VISIBLE
                    }
                    binding.progressBar.visibility = View.GONE
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Log.e("FAILURE-getReportDataFromAnimalTable()", it.message.toString())
                    Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
                }
        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateExcelSheet(finalReportList: java.util.ArrayList<ReportsGeneralModel>) {
        val  hssfWorkbook = HSSFWorkbook()
        val hssfSheet: HSSFSheet = hssfWorkbook.createSheet("MySheet")

        val hssfRow: HSSFRow = hssfSheet.createRow(0)
        hssfRow.createCell(0).setCellValue("Name")
        hssfRow.createCell(1).setCellValue("Date")
        hssfRow.createCell(2).setCellValue("Medical Condition")
        hssfRow.createCell(3).setCellValue("Gender")
        hssfRow.createCell(4).setCellValue("Type")

        for (i in 0 until finalReportList.size) {

            val hssfRow1: HSSFRow = hssfSheet.createRow(i+1)

            val date: Date = finalReportList[i].date!!.toDate()
            val dateToShow = SimpleDateFormat("dd/MM/yy", Locale.US).format(date)

            hssfRow1.createCell(0).setCellValue(finalReportList[i].animalName)
            hssfRow1.createCell(1).setCellValue(dateToShow)
            hssfRow1.createCell(2).setCellValue(finalReportList[i].conditions)
            hssfRow1.createCell(3).setCellValue(finalReportList[i].gender)
            hssfRow1.createCell(4).setCellValue(finalReportList[i].type)
        }
        saveWorkBook(hssfWorkbook)
    }
    private fun saveWorkBook(hssfWorkbook: HSSFWorkbook) {

        val dir = File(Environment.getExternalStorageDirectory(), "Paw Garage")
        if (!dir.exists()) {
            dir.mkdir()
        }
        val subDir = File(dir, "Medical Condition Reports")
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
                getAllData(finalReportList)
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
                    getAllData(finalReportList)
                } else {
                    Toast.makeText(requireContext(), "Storage permission is denied.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun didScrolledToEnd(position: Int) {
       // getReportDataFromAnimalTable(finalReportList)  // Because pagination is only in this method.
    }
    override fun dataSourceDidUpdate(size: Int) {

    }
    override fun didSelectItem(dataItem: ReportsGeneralModel, position: Int) {

    }
}