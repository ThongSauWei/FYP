package com.example.fyp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.libraries.places.api.model.Place


class Map : Fragment() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private lateinit var userLocation: LatLng
    private var userLocationMarker: Marker? = null // To track the user location marker

    private val nearbyPlacesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
    private val placeDetailsUrl = "https://maps.googleapis.com/maps/api/place/details/json"
    private val directionsUrl = "https://maps.googleapis.com/maps/api/directions/json"
    private val apiKey = "AIzaSyBsG-rNDDmp_gHyWuq6oZr0iYcELCgtQ40"

    private var currentPolyline: Polyline? = null
    private val markers = mutableListOf<Marker>()

    private val callback = OnMapReadyCallback { map ->
        googleMap = map
        enableUserLocation()
        getUserLocation()

        // Add marker click listener
        googleMap.setOnMarkerClickListener { marker ->
            if (marker == userLocationMarker) {
                // Move the camera to the user's current location
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                Toast.makeText(requireContext(), "Returning to your location", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), apiKey)
        }
        val placesClient: PlacesClient = Places.createClient(requireContext())

        // Set up the AutocompleteSupportFragment
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        )
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                // Move the map camera to the selected location
                val latLng = place.latLng
                if (latLng != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    googleMap.addMarker(
                        MarkerOptions().position(latLng).title(place.name)
                    )

                    // Fetch place details and display in the modal
                    fetchPlaceDetails(place.id, latLng)

                    // Fetch directions to the selected place from the user's current location
                    fetchDirections(latLng)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(requireContext(), "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })



        // Initialize the map fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(callback)
    }


    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    userLocation = LatLng(location.latitude, location.longitude)
                    userLocationMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(userLocation)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                    fetchNearbyPlaces()
                }
            }
        }
    }

    private fun fetchNearbyPlaces() {
        val client = OkHttpClient()
        val url = "$nearbyPlacesUrl?location=${userLocation.latitude},${userLocation.longitude}&radius=1000&type=restaurant&key=$apiKey"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val jsonResponse = response.body?.string()
                if (jsonResponse != null) {
                    val results = JSONObject(jsonResponse).getJSONArray("results")
                    requireActivity().runOnUiThread {
                        for (i in 0 until results.length()) {
                            val place = results.getJSONObject(i)
                            val lat = place.getJSONObject("geometry").getJSONObject("location").getDouble("lat")
                            val lng = place.getJSONObject("geometry").getJSONObject("location").getDouble("lng")
                            val name = place.getString("name")
                            val placeId = place.getString("place_id")

                            val markerOptions = MarkerOptions()
                                .position(LatLng(lat, lng))
                                .title(name)
                            val marker = googleMap.addMarker(markerOptions)

                            marker?.let {
                                it.tag = placeId // Save the place ID for fetching details
                                markers.add(it)
                            }
                        }

                        googleMap.setOnMarkerClickListener { marker ->
                            val placeId = marker.tag as String
                            fetchPlaceDetails(placeId, marker.position)
                            true
                        }
                    }
                }
            }
        })
    }

    private fun fetchPlaceDetails(placeId: String, destination: LatLng) {
        val client = OkHttpClient()
        val url = "$placeDetailsUrl?place_id=$placeId&fields=name,rating,formatted_address,photo,geometry&key=$apiKey"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val jsonResponse = response.body?.string()
                if (jsonResponse != null) {
                    val result = JSONObject(jsonResponse).getJSONObject("result")
                    val name = result.getString("name")
                    val address = result.getString("formatted_address")
                    val photos = result.optJSONArray("photos")
                    val photoReference =
                        photos?.getJSONObject(0)?.getString("photo_reference") ?: ""
                    val photoUrl =
                        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=$photoReference&key=$apiKey"

                    requireActivity().runOnUiThread {
                        // Show modal dialog
                        showLocationDetailsModal(name, address, photoUrl)

                        // Fetch and update directions for the selected destination
                        fetchDirections(destination)
                    }
                }
            }
        })
    }




    private fun showLocationDetailsModal(name: String, address: String, photoUrl: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.modal_location_details, null)
        val dialogBuilder = AlertDialog.Builder(requireContext())

        val nameTextView: TextView = dialogView.findViewById(R.id.place_name)
        val addressTextView: TextView = dialogView.findViewById(R.id.place_address)
        val photoImageView: ImageView = dialogView.findViewById(R.id.place_image)

        nameTextView.text = name
        addressTextView.text = address
        Glide.with(this).load(photoUrl).into(photoImageView)

        dialogBuilder.setView(dialogView)
        dialogBuilder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
        dialogBuilder.create().show()
    }

    private fun fetchDirections(destination: LatLng) {
        // Remove the previous polyline from the map
        currentPolyline?.remove()
        currentPolyline = null // Reset currentPolyline to null for safety

        // Create the new direction request URL
        val client = OkHttpClient()
        val url =
            "$directionsUrl?origin=${userLocation.latitude},${userLocation.longitude}&destination=${destination.latitude},${destination.longitude}&mode=driving&key=$apiKey"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val jsonResponse = response.body?.string()
                if (jsonResponse != null) {
                    val routes = JSONObject(jsonResponse).getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
                        val legs = route.getJSONArray("legs")
                        val duration = legs.getJSONObject(0).getJSONObject("duration").getString("text")

                        requireActivity().runOnUiThread {
                            // Draw the new polyline on the map
                            drawRouteOnMap(overviewPolyline)

                            // Show the travel time in a Toast message
                            Toast.makeText(requireContext(), "Time estimate: $duration", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }




    private fun drawRouteOnMap(encodedPolyline: String) {
        // Decode the polyline string to a list of LatLng points
        val polylinePoints = decodePolyline(encodedPolyline)
        val polylineOptions = PolylineOptions()
            .addAll(polylinePoints)
            .width(10f)
            .color(android.graphics.Color.BLUE)
            .geodesic(true)

        // Clear the previous polyline before adding the new one
        currentPolyline?.remove()
        currentPolyline = googleMap.addPolyline(polylineOptions)
    }



    private fun clearMap() {
        // Remove the current polyline but keep markers intact
        currentPolyline?.remove()
    }


    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }

        return poly
    }
}
