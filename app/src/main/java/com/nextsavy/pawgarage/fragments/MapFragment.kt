
package com.nextsavy.pawgarage.fragments

/**
 * This fragment is not in use.
 */
/*
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentMapBinding


class MapFragment : Fragment() {

    private lateinit var binding: FragmentMapBinding
    private var mapFragment: SupportMapFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "Map"

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().popBackStack()
        }

        Log.e("LATLONG1", arguments?.getDouble("Lat").toString())
        Log.e("LATLONG2", arguments?.getDouble("Long").toString())

        return binding.root
    }

    private val callback = OnMapReadyCallback { googleMap ->
        if (arguments != null) {
            val myLocation = LatLng(requireArguments().getDouble("Lat"), requireArguments().getDouble("Long"))
            googleMap.addMarker(MarkerOptions().position(myLocation).title("You are here"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation))
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16f))
//        googleMap.isMyLocationEnabled = true  // To enable default MyLocation button on top-right side in map view.
        }
    }
}
*/