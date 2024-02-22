package com.nextsavy.pawgarage.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView.OnQueryTextListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.material.snackbar.Snackbar
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.RecyclerViewPagingInterface
import com.nextsavy.pawgarage.adapters.PlacesAdapter
import com.nextsavy.pawgarage.databinding.FragmentLocationBinding
import com.nextsavy.pawgarage.models.PlacesModel
import com.nextsavy.pawgarage.models.Result
import com.nextsavy.pawgarage.retrofit.RetrofitClient
import com.nextsavy.pawgarage.retrofit.RetrofitInterface
import com.nextsavy.pawgarage.utils.AppDelegate
import com.nextsavy.pawgarage.utils.Helper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale


class LocationFragment : Fragment(), RecyclerViewPagingInterface<Result>, OnMapReadyCallback {

    private lateinit var binding: FragmentLocationBinding
    private val args: LocationFragmentArgs by navArgs()
    private lateinit var adapter: PlacesAdapter
    private var googleMap: GoogleMap? = null
    private var lastMarker: Marker? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val mPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0
    private var flag = false
    private var locationFlag = false
    private var mapFragment: SupportMapFragment? = null
    private var locationText = ""
    lateinit var retrofitInterface: RetrofitInterface
    var canGoBack = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) &&
                        permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)-> {
                    // Precise and course location access granted.
                    getMyCurrentLocation()
                }
                else -> {
                    // No location access granted.
                    Snackbar.make(binding.root, "Location permission is required to get current location and nearby places.", Snackbar.LENGTH_LONG).show()
                }
            }
        }

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        val myLocation = LatLng(mLatitude, mLongitude)
        googleMap.addMarker(MarkerOptions().position(myLocation).title("You are here"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16f))
        googleMap.isMyLocationEnabled = true  // To enable default MyLocation button on top-right side in map view.
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLocationBinding.inflate(inflater, container, false)
        setupRecyclerView()

        binding.toolbarOne.titleToolbarOne.text = "Location"
        mLatitude = args.latitude.toDouble()
        mLongitude = args.longitude.toDouble()

        val baseUrl = "https://maps.googleapis.com/maps/api/"
        retrofitInterface = RetrofitClient.getRetrofitInstance(baseUrl).create(RetrofitInterface::class.java)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        binding.searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                val query = p0.toString()
                if (query.isNotEmpty() && query.isNotBlank()) {
                    getLocationByName(p0.toString())
                }
                return true
            }
            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })

        onClickListeners()

        return binding.root
    }

    @Suppress("DEPRECATION")
    private fun getLocationByName(query: String) {
        try {
            val geoCoder = Geocoder(requireContext(), Locale.getDefault())
            val addressList: List<Address>? =
                geoCoder.getFromLocationName(query, 1)

            if (!addressList.isNullOrEmpty()) {
                val address = addressList.first()

                if (address.hasLatitude() && address.hasLongitude()) {

                    mLatitude = address.latitude
                    mLongitude = address.longitude

                    getNearbyPlaces()
//                    mapFragment?.getMapAsync(callback)
                    addMarkerAt(LatLng(address.latitude, address.longitude), address.getAddressLine(0))

                    locationText = address.getAddressLine(0)
//                            .split(",").get(1) + "," +
//                                address.getAddressLine(0).split(",").get(2)

//                    if (!address.subLocality.isNullOrEmpty()) {
//                        if (!locationText.contains(address.subLocality)) {
//                            locationText = locationText + ", " + address.subLocality
//                        }
//                    }
//                    if (!address.locality.isNullOrEmpty()) {
//                        if (!locationText.contains(address.locality)) {
//                            locationText = locationText + ", " + address.locality
//                        }
//                    }
                    Log.e("FLOW", "Address(getLocationByName)- $locationText")
                }
            } else {
                Toast.makeText(requireContext(), "Please enter some valid keywords for search.", Toast.LENGTH_SHORT).show()
                Log.e("FLOW", "AddressList is Null or Empty")
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            Log.e("ERROR", e.toString())
        }
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED-> {
                Log.e("NST-M", "Location available")
                getMyCurrentLocation()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)-> {
                Log.e("NST-M", "Location showRationalDialog")
                showRationalDialog()
            }
            else -> {
                Log.e("NST-M", "Location Request launcher")
                requestPermissionLauncher.launch(mPermissions)
            }
        }
    }
    private fun showRationalDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission needed")
            .setMessage("Without location permission, app will not be able to get current location and nearby places.")
            .setPositiveButton("Allow from Settings") { d, _ ->
                val intent = Intent()
                intent.data = Uri.fromParts("package", requireContext().packageName, null)
                intent.action = Uri.decode(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                startActivity(intent)
                flag = true
                d.dismiss()
            }
            .setNegativeButton("Deny") { d, _ ->
                Snackbar.make(binding.root, "Location permission is required to show current location and nearby places.", Snackbar.LENGTH_LONG)
                    .setAction("Settings"){
                        val intent = Intent()
                        intent.data = Uri.fromParts("package", requireContext().packageName, null)
                        intent.action = Uri.decode(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        startActivity(intent)
                        flag = true
                    }.show()
                d.dismiss()
            }
            .create()
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun getMyCurrentLocation() {
        if (Helper.isInternetAvailable(requireContext())) {
            if (isLocationEnable()) {
                googleMap?.isMyLocationEnabled = true

                fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, object : CancellationToken() {
                    override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token
                    override fun isCancellationRequested() = false
                })
                    .addOnSuccessListener { location: Location? ->
                        if (location == null) {
                            Toast.makeText(AppDelegate.applicationContext(), "Cannot get location.", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            mLatitude = location.latitude
                            mLongitude = location.longitude
                            Log.e("FLOW", "In getMyCurrentLocation, Lat- $mLatitude, Lng- $mLongitude")
//                            mapFragment?.getMapAsync(callback)
                            getAddressFromCoordinates()
                        }
                    }
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("")
                    .setMessage("Please enable Location service from your Settings.")
                    .setPositiveButton("Settings") { d, _ ->
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        locationFlag = true
                        d.dismiss()
                    }
                    .setNegativeButton("") { d, _ ->
                        d.dismiss()
                    }
                    .create()
                    .show()
            }
        } else {
            Toast.makeText(requireContext(), R.string.internet_connectivity, Toast.LENGTH_LONG).show()
        }
    }
    @Suppress("DEPRECATION")
    private fun getAddressFromCoordinates() {
        try {
            val geoCoder = Geocoder(requireContext(), Locale.getDefault())
             val addressList: List<Address>? =
                geoCoder.getFromLocation(mLatitude, mLongitude, 1)

            if (!addressList.isNullOrEmpty()) {
                val address = addressList.first()

                if (address.hasLatitude() && address.hasLongitude()) {

                    Log.e("FLOW", "In getAddressFromCoordinates, Lat- $mLatitude, Lng- $mLongitude")
                    Log.e("NST", address.toString())

                    locationText =
                        address.getAddressLine(0)
                    addMarkerAt(LatLng(address.latitude, address.longitude), address.getAddressLine(0))
                    getNearbyPlaces()
//                            .split(",").get(1) + "," +
//                                address.getAddressLine(0).split(",").get(2)

//                    if (!address.subLocality.isNullOrEmpty()) {
//                        if (!locationText.contains(address.subLocality)) {
//                            locationText = locationText + ", " + address.subLocality
//                        }
//                    }
//                    if (!address.locality.isNullOrEmpty()) {
//                        if (!locationText.contains(address.locality)) {
//                            locationText = locationText + ", " + address.locality
//                        }
//                    }
                    Log.e("FLOW", "Address(getAddressFromCoordinates)- $locationText")
                }
            } else {
                Log.e("FLOW", "AddressList is Null or Empty")
                Toast.makeText(requireContext(), "No address found.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(AppDelegate.applicationContext(), e.message, Toast.LENGTH_SHORT).show()
            Log.e("ERROR", e.toString())
        }
    }

    private fun getNearbyPlaces() {
        binding.progressBar.visibility = View.VISIBLE

        val location = "$mLatitude,$mLongitude"
        val radius = "1000"
        val type = "all"
        val apiKey = "AIzaSyAQuMJQ8qWt6pY88T4BSV9jt6etnhIujWQ"

        if (Helper.isInternetAvailable(requireContext())) {

            retrofitInterface.getAllNearbyPlaces(location, radius, type, apiKey).enqueue(object :
                Callback<PlacesModel> {
                override fun onResponse(call: Call<PlacesModel>, response: Response<PlacesModel>) {
                    if (response.isSuccessful) {
                        binding.progressBar.visibility = View.GONE
                        val status = response.body()!!.status
                        if (status == "OK") {

                            val placesList = response.body()!!.results as ArrayList
                            if (placesList.size > 0) {
                                placesList.removeAt(0)
                                adapter.updateDataSource(placesList)
                                Log.e("DATA", placesList[1].name)
                                Log.e("DATA", "${placesList[1].geometry.location.lat}, ${placesList[1].geometry.location.lng}")
                            } else {
                                Toast.makeText(requireContext(), "No nearby placed found.", Toast.LENGTH_SHORT).show()
                            }
                        } else if (status == "ZERO_RESULTS") {
                            Toast.makeText(requireContext(), "This is a remote location, so no nearby places found", Toast.LENGTH_LONG).show()
                        } else if (status == "OVER_QUERY_LIMIT") {
                            Toast.makeText(requireContext(), "Limit to use this service is exceeded.", Toast.LENGTH_LONG).show()
                        } else if (status == "INVALID_REQUEST") {
                            Toast.makeText(requireContext(), "No proper location found to get nearby places.", Toast.LENGTH_LONG).show()
                        } else if (status == "REQUEST_DENIED") {
                            Toast.makeText(requireContext(), "Invalid API key.", Toast.LENGTH_LONG).show()
                        } else if (status == "UNKNOWN_ERROR") {
                            Toast.makeText(requireContext(), "Unknown error.", Toast.LENGTH_SHORT).show()
                        }
                        canGoBack = true
                    } else {
                        Toast.makeText(requireContext(), "Places not fetched successfully", Toast.LENGTH_LONG).show()
                        Log.e("ERROR_RESPONSE", response.raw().toString())
                        binding.progressBar.visibility = View.GONE
                    }
                }
                override fun onFailure(call: Call<PlacesModel>, t: Throwable) {
                    Toast.makeText(requireContext(), t.message, Toast.LENGTH_LONG).show()
                    Log.e("ERROR_FAILURE", t.message.toString())
                    binding.progressBar.visibility = View.GONE
                }
            })
        } else {
            Toast.makeText(requireContext(), "No internet connection.", Toast.LENGTH_LONG).show()
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun isLocationEnable(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun onClickListeners() {
        binding.currentLocationBTN.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED-> {
                    Log.e("NST-M", "Location available")
                    getMyCurrentLocation()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)-> {
                    Log.e("NST-M", "Location showRationalDialog")
                    showRationalDialog()
                }
                else -> {
                    Log.e("NST-M", "Location Request launcher")
                    requestPermissionLauncher.launch(mPermissions)
                }
            }
        }
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            if (canGoBack) it.findNavController().popBackStack()
            AppDelegate.locationDataModel.editReleaseLatitude = AppDelegate.animalModel.latitude
            AppDelegate.locationDataModel.editReleaseLongitude = AppDelegate.animalModel.longitude
            AppDelegate.locationDataModel.editReleaseLocation = AppDelegate.animalModel.locationAddress
        }
        binding.saveBTN.setOnClickListener {
            if (canGoBack) it.findNavController().popBackStack()
            AppDelegate.locationDataModel.editReleaseLatitude = AppDelegate.animalModel.latitude
            AppDelegate.locationDataModel.editReleaseLongitude = AppDelegate.animalModel.longitude
            AppDelegate.locationDataModel.editReleaseLocation = AppDelegate.animalModel.locationAddress
        }
    }

    private fun setupRecyclerView() {
        binding.placesRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = PlacesAdapter(arrayListOf(), this)
        binding.placesRV.adapter = adapter
    }

    override fun dataSourceDidUpdate(size: Int) {
    }

    override fun didScrolledToEnd(position: Int) {
    }

    override fun didSelectItem(dataItem: Result, position: Int) {
        Log.e("LATLONG-PlacesAdapter", "${dataItem.geometry.location.lat}, ${dataItem.geometry.location.lng}")
        AppDelegate.animalModel.latitude = dataItem.geometry.location.lat
        AppDelegate.animalModel.longitude = dataItem.geometry.location.lng
        AppDelegate.animalModel.locationAddress = "${dataItem.name}, ${dataItem.vicinity}"

        AppDelegate.locationDataModel.addReleaseLatitude = dataItem.geometry.location.lat
        AppDelegate.locationDataModel.addReleaseLongitude = dataItem.geometry.location.lng
        AppDelegate.locationDataModel.addReleaseLocation = "${dataItem.name}, ${dataItem.vicinity}"

        AppDelegate.locationDataModel.editReleaseLatitude = dataItem.geometry.location.lat
        AppDelegate.locationDataModel.editReleaseLongitude = dataItem.geometry.location.lng
        AppDelegate.locationDataModel.editReleaseLocation = "${dataItem.name}, ${dataItem.vicinity}"

        findNavController().popBackStack()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap) {
        Log.e("NST-M", "Google map is ready")
        googleMap = p0

        googleMap?.setOnMapClickListener { tapCords ->
            mLatitude = tapCords.latitude
            mLongitude = tapCords.longitude
            getAddressFromCoordinates()
        }

        googleMap?.setOnMyLocationButtonClickListener { // called when user clicks "Current Location" button.
            requestLocationPermission()
            return@setOnMyLocationButtonClickListener true
        }

        if (mLatitude == 0.0 && mLongitude == 0.0) {
//            mLatitude = 23.0416766
//            mLongitude = 72.5492499
            mLatitude = 20.4691860
            mLongitude = 73.0681680
        }
        getAddressFromCoordinates()
    }

    private fun addMarkerAt(cords: LatLng, address: String) {
        lastMarker?.remove()
        lastMarker = googleMap?.addMarker(MarkerOptions()
            .position(cords)
            .title(address))
        googleMap?.moveCamera(CameraUpdateFactory.newLatLng(cords))
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(cords, 16f))

        Log.e("MARKER", "${cords.latitude}, ${cords.longitude}, $address")
        AppDelegate.animalModel.latitude = cords.latitude
        AppDelegate.animalModel.longitude = cords.longitude
        AppDelegate.animalModel.locationAddress = address
    }
}