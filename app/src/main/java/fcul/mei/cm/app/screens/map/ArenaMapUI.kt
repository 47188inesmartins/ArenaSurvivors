package fcul.mei.cm.app.screens.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.database
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

import com.google.maps.android.data.kml.KmlLayer

import fcul.mei.cm.app.R

import fcul.mei.cm.app.domain.Alliances

import fcul.mei.cm.app.domain.MarkerData
import fcul.mei.cm.app.domain.User
import fcul.mei.cm.app.utils.CollectionPath
import fcul.mei.cm.app.utils.FirebaseConfig
import fcul.mei.cm.app.utils.UserSharedPreferences
import fcul.mei.cm.app.viewmodel.ArenaMapViewModel


private val LOCATION_PERMISSION_REQUEST_CODE = 1000
private val db = Firebase.database(FirebaseConfig.DB_URL)
val locationRef = db.getReference("userLocations") // For Realtime Database
val locationRefStatic = db.getReference("userLocationsStatic")


@Composable
fun ArenaMapUi(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    arenaViewModel: ArenaMapViewModel,
) {
    var placingStaticLocation by remember { mutableStateOf(false) } // State for placing static location
    var staticLocationMarker by remember { mutableStateOf<Marker?>(null) } // Temp marker for static location
    var isSharingLocation by remember { mutableStateOf(false) } // Default: not sharing
    var showStaticLocationDialog  by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var membersList by remember { mutableStateOf<List<User>>(emptyList()) }
    var isListenerSetUp by remember { mutableStateOf(false) }
    var placingMarker by remember { mutableStateOf(false) }

    var kmlLayer by remember { mutableStateOf<KmlLayer?>(null) }
    val context = LocalContext.current
    var userId = UserSharedPreferences(context).getUserId()
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var showDialog by remember { mutableStateOf(false) }
    var isManualPlacementMode by remember { mutableStateOf(false) }
    var markerTitle by remember { mutableStateOf("") }
    var markerDescription by remember { mutableStateOf("") }
    // State for user's location
    var userLatitude by remember { mutableDoubleStateOf(Double.NaN) }
    var userLongitude by remember { mutableDoubleStateOf(Double.NaN) }
    var isLocationAvailable by remember { mutableStateOf(false) } // Track if location is available
    val initialZoom = 13f
    var googleMap: GoogleMap? = null
    var hasZoomedToInitialLocation by remember { mutableStateOf(false) } // Track initial zoom
    var userMarker by remember { mutableStateOf<Marker?>(null) }
    // Lifecycle handling
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                mapView.onCreate(null)
            }

            override fun onStart(owner: LifecycleOwner) {
                mapView.onStart()
            }

            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
            }

            override fun onStop(owner: LifecycleOwner) {
                mapView.onStop()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                mapView.onDestroy()
            }
        }

        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            mapView.onDestroy()
        }
    }

    // Check and request location permissions
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            (context as ComponentActivity),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    } else {
        // Fetch the last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLatitude = location.latitude
                userLongitude = location.longitude
                isLocationAvailable = true // Mark location as available
            }
        }

        // Continuous location updates
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        ).setMinUpdateIntervalMillis(1000L)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            context.mainExecutor,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        userLatitude = location.latitude
                        userLongitude = location.longitude
                        isLocationAvailable = true

                        val locationData = mapOf(
                            "latitude" to userLatitude,
                            "longitude" to userLongitude,
                            "timestamp" to System.currentTimeMillis()
                        )
                        if (isSharingLocation) {
                            if (userId != null) {
                            locationRef.child(userId).setValue(locationData)
                        }
                            }

                        googleMap?.let { map ->
                            updateUserMarker(
                                map,
                                userLatitude,
                                userLongitude,
                                userMarker
                            ) { newMarker ->
                                userMarker = newMarker
                            }
                            // Zoom to user's location only the first time
                            if (!hasZoomedToInitialLocation) {
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(userLatitude, userLongitude),
                                        initialZoom
                                    )
                                )
                                hasZoomedToInitialLocation = true
                            }
                        }
                    }
                }
            }
        )
    }
    Log.d("omg2", placingMarker.toString())
    // Map UI
    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { view ->
            view.getMapAsync { map ->
                googleMap = map
                googleMap?.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
                )
                // Always update the map based on placingMarker state
                if (placingMarker or placingStaticLocation) {
                    kmlLayer?.removeLayerFromMap()
                } else {
                    if (kmlLayer == null) {
                        kmlLayer = KmlLayer(googleMap, R.raw.arenasurvivorlisbon, context)
                    }
                    if (kmlLayer?.isLayerOnMap == false) {
                        kmlLayer?.addLayerToMap()
                    }
                }
                if (userId != null) {
                    if (!isListenerSetUp) {
                        val markersMap = mutableMapOf<String, Marker>()
                        setupRealtimeLocationUpdates(context, userId, googleMap, markersMap)
                        isListenerSetUp = true // Mark the listener as set up
                    }
                }
                //As coordenadas dos utilizadores estao guardadas numa realtime database
                //aqui vamos buscar ao livedata essa info e é dado display na UI
//                coordinatesMap.forEach { (user, coordinates) ->
//                    coordinates?.let {
//                        googleMap?.addMarker(
//                            MarkerOptions()
//                                .position(LatLng(coordinates.latitude, coordinates.longitude))
//                                .title(user.name)
//                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.friend_user))
//                        )
//                    }
//                }

                if (userId != null) {
                    fetchMarkersFromDatabase(userId = userId) { markers ->
                        for (marker in markers) {
                            googleMap?.addMarker(
                                MarkerOptions()
                                    .position(LatLng(marker.latitude, marker.longitude))
                                    .title(marker.title)
                                    .snippet(marker.description)
                            )
                        }
                    }
                }

                map.setOnMapClickListener { latLng ->
                    if (isManualPlacementMode) {

                        val truncatedSnippet = if (markerDescription.length > 128) {
                            markerDescription.take(125) + "..."
                        } else {
                            markerDescription
                        }

                        // Add marker with user-provided title and description
                        googleMap?.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(markerTitle)
                                .snippet(truncatedSnippet) // Add description as snippet
                        )
                        val newMarker = MarkerData(
                            title = markerTitle,
                            description = markerDescription,
                            latitude = latLng.latitude,
                            longitude = latLng.longitude
                        )

                        if (userId != null) {
                            saveMarkerToDatabase(
                                newMarker,
                                onSuccess = null,
                                onFailure = { exception ->
                                    Toast.makeText(
                                        context,
                                        "Failed to save marker: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                userId = userId,
                                context = context
                            )
                        }



                        isManualPlacementMode = false // Exit manual placement mode

                        Toast.makeText(context, "Marker placed!", Toast.LENGTH_SHORT).show()

                        placingMarker = false
                        Log.d("changing", placingMarker.toString())
                    } else if (placingStaticLocation) {
                        // Clear any existing temporary marker
                        staticLocationMarker?.remove()

                        // Add a marker at the clicked location
                        staticLocationMarker = googleMap?.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title("Static Location")
                                .snippet("Confirm to save this location")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        )

                        if (userId != null) {
                            saveStaticLocationToDatabase(
                                userId = userId,
                                latitude = latLng.latitude,
                                longitude = latLng.longitude,
                                context = context
                            )
                        }
                        placingStaticLocation = false
                        staticLocationMarker?.remove()
                        staticLocationMarker = null

                    }
                }
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(userLatitude, userLongitude),
                        initialZoom
                    )
                )
            }
        }


        // Popup dialog for choosing placement type
        if (showDialog) {
            var tempTitle by remember { mutableStateOf("") } // Temporary input for title
            var tempDescription by remember { mutableStateOf("") } // Temporary input for description
            var isTitleTouched = false // Track if title field is touched
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Place a Marker") },
                text = {
                    Column {
                        Text("Place a marker to share with the members of your alliances.\n\n" +
                                "Provide a Title and Description:")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = tempTitle,
                            onValueChange = {
                                tempTitle = it
                                isTitleTouched = true // Set touched when user interacts
                            },
                            label = { Text("Title") },
                            placeholder = { Text("Enter a title for your marker") },
                            isError = tempTitle.length < 3 // Highlight error if title is too short
                        )
                        if (isTitleTouched && tempTitle.length < 3) {
                            Text(
                                "Title must be at least 3 characters",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = tempDescription,
                            onValueChange = { newValue ->
                                if (newValue.length <= 128) {
                                    tempDescription = newValue
                                }
                            },
                            label = { Text("Description") },
                            placeholder = { Text("Enter a description for your marker") },
                            isError = tempDescription.length > 128 // Highlight error if description exceeds limit
                        )
                        if (tempDescription.length > 128) {
                            Text(
                                "Description cannot exceed 128 characters",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Save title and description for current or manual placement
                            markerTitle = tempTitle
                            markerDescription = tempDescription

                            // Place marker at current location
                            if (!userLatitude.isNaN() && !userLongitude.isNaN()) {
                                val truncatedSnippet = if (markerDescription.length > 128) {
                                    markerDescription.take(125) + "..."
                                } else {
                                    markerDescription
                                }
                                googleMap?.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(userLatitude, userLongitude))
                                        .title(markerTitle)
                                        .snippet(truncatedSnippet) // Add description as snippet
                                )
                                val newMarker = MarkerData(
                                    title = markerTitle,
                                    description = markerDescription,
                                    latitude = userLatitude,
                                    longitude = userLongitude
                                )

                                if (userId != null) {
                                    saveMarkerToDatabase(newMarker,
                                        onSuccess = null,
                                        onFailure = { exception ->
                                            Toast.makeText(
                                                context,
                                                "Failed to save marker: ${exception.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        userId = userId,
                                        context = context
                                    )
                                }
                                Toast.makeText(
                                    context,
                                    "Marker placed at current location!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Location not available",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            showDialog = false // Close dialog
                        },
                        enabled = tempTitle.length >= 3 && tempDescription.length <= 128 // Enable only if inputs are valid
                    ) {
                        Text("Place at Current Location")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            // Save title and description for manual placement
                            markerTitle = tempTitle
                            markerDescription = tempDescription
                            placingMarker = true
                            Log.d("placing1", placingMarker.toString())
                            // Enable manual placement mode
                            isManualPlacementMode = true
                            showDialog = false // Close dialog
                        },
                        enabled = tempTitle.length >= 3 && tempDescription.length <= 128 // Enable only if inputs are valid
                    ) {
                        Text("Place Manually")
                    }
                }
            )
        }

        if (showMembersDialog) {
            AlertDialog(
                onDismissRequest = { showMembersDialog = false },
                title = {
                    if (membersList.isNotEmpty()) Text("Share your Location with:")
                    else Text("You have no allies to share your location with")
                },
                text = {
                    LazyColumn( // Use LazyColumn for scrollable list
                        modifier = Modifier
                            .padding(8.dp)

                    ) {
                        if (membersList.isEmpty()) {
                            item {
                                Text("")
                            }
                        } else {
                            items(membersList) { user ->
                                Row( // Use Row to align name and buttons horizontally
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(8.dp), // Add padding inside each member section
                                    verticalAlignment = Alignment.CenterVertically // Align items vertically
                                ) {
                                    // Member Name
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier
                                            .weight(1f) // Take remaining space
                                            .padding(end = 8.dp) // Add space between name and buttons
                                    )

                                    // Buttons in a Column
                                    Column(
                                        modifier = Modifier.weight(1f), // Adjust column width
                                        verticalArrangement = Arrangement.spacedBy(4.dp) // Spacing between buttons
                                    ) {
                                        Button(
                                            onClick = {
                                                if (userId != null) {
                                                    addCurrentUserToSharedUsers(user.id, userId, true, context)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Current", style = MaterialTheme.typography.bodySmall)
                                        }

                                        Button(
                                            onClick = {
                                                showStaticLocationDialog = true
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Static", style = MaterialTheme.typography.bodySmall)
                                        }

                                        Button(
                                            onClick = {
                                                if (userId != null) {
                                                    removeCurrentUserFromSharedUsers(user.id, userId, context)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Remove", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showMembersDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }



// Static Location Selection Popup
        if (showStaticLocationDialog) {
            AlertDialog(
                onDismissRequest = { showStaticLocationDialog = false },
                title = { Text("Share Static Location") },
                text = {
                    Column {
                        Text("Choose how to share the static location:")
                        Spacer(modifier = Modifier.height(16.dp))

                        // Option to use current location
                        Button(
                            onClick = {
                                // Use the current location as the static location
                                if (userLatitude.isFinite() && userLongitude.isFinite()) {
                                    saveStaticLocationToDatabase(
                                        userId = userId,
                                        latitude = userLatitude,
                                        longitude = userLongitude,
                                        context = context
                                    )
                                    showStaticLocationDialog = false
                                } else {
                                    Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Current Location")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option to click on the map
                        Button(
                            onClick = {
                                showMembersDialog = false
                                showStaticLocationDialog = false
                                placingStaticLocation = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Place on Map")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showStaticLocationDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }



        Column( modifier = Modifier
    .align(Alignment.BottomStart)) {
        // Place the Switch above the Row
        Column(
            modifier = Modifier
                // Align to the bottom start of the Box
                .padding(start = 16.dp,top = 30.dp) // Adjust padding to position above the Row
        ) {
            Text(
                text = if (isSharingLocation) "Sharing Location" else "Not Sharing Location",
                style = MaterialTheme.typography.bodySmall
            )
            Switch(
                checked = isSharingLocation,
                onCheckedChange = { isSharingLocation = it },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Bottom Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()

        ) {
            // First Column (Buttons)
            Column(
                modifier = Modifier
                    .weight(2f)
                    ,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                StyledButton(
                    onClick = {
                        val route = "spots/$userLatitude/$userLongitude"
                        navController.navigate(route)
                    },
                    text = "Landmark"
                )
                StyledButton(
                    onClick = { showDialog = true },
                    text = "Place Marker"
                )
                StyledButton(
                    onClick = {
                        if (userId != null) {
                            fetchAlliances(userId, context) { alliances ->
                                val allMemberIds = alliances.flatMap { it.members }.distinct()

                                getUsersByIds(allMemberIds) { users ->
                                    membersList = users.filter { it.id != userId } // List of User objects
                                    showMembersDialog = true
                                }
                            }
                        } else {
                            Toast.makeText(context, "User not registered", Toast.LENGTH_SHORT).show()
                        }
                    },
                    text = "Share Location"
                )
            }

            // Second Column (Zoom Controls)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 35.dp, top = 30.dp)
            ) {
                IconButton(
                    onClick = { googleMap?.animateCamera(CameraUpdateFactory.zoomIn()) },
                    modifier = Modifier.size(80.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.zoomin),
                        contentDescription = "Zoom In button",
                        modifier = Modifier.size(86.dp)
                    )
                }
                IconButton(
                    onClick = { googleMap?.animateCamera(CameraUpdateFactory.zoomOut()) },
                    modifier = Modifier.size(80.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.zoomout),
                        contentDescription = "Zoom Out button",
                        modifier = Modifier.size(86.dp)
                    )
                }
            }
        }
    }
    }
}


fun saveMarkerToDatabase(
    marker: MarkerData,
    userId: String,
    context: Context,
    onSuccess: (() -> Unit)?,
    onFailure: (Exception) -> Unit
) {
    val db = Firebase.firestore

    // Add the marker to the "markers" collection
    db.collection("markers")
        .add(marker)
        .addOnSuccessListener { documentReference ->
            val markerId = documentReference.id

            // Ensure the user's "markers" field exists as an array and add the marker ID
            val userRef = db.collection("users").document(userId)
            userRef.set(
                mapOf("markers" to FieldValue.arrayUnion(markerId)),
                SetOptions.merge()
            ).addOnSuccessListener {

                // Fetch all alliances and update marker for their members
                fetchAlliances(userId, context) { alliances ->
                    val allMemberIds = alliances.flatMap { it.members }.distinct()

                    // Update all members with the new marker
                    allMemberIds.forEach { memberId ->
                        val memberRef = db.collection("users").document(memberId)
                        memberRef.set(
                            mapOf("markers" to FieldValue.arrayUnion(markerId)),
                            SetOptions.merge()
                        )
                    }

                    // Call success callback after updating all members
                    if (onSuccess != null) {
                        onSuccess()
                    }
                }
            }.addOnFailureListener { exception ->
                onFailure(exception)
            }
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}


fun fetchMarkersFromDatabase(userId: String, onMarkersFetched: (List<MarkerData>) -> Unit) {
    val db = Firebase.firestore

    // Fetch the user's markers list
    val userRef = db.collection("users").document(userId)
    userRef.get()
        .addOnSuccessListener { userDocument ->
            val markerIds = userDocument["markers"] as? List<String>

            if (!markerIds.isNullOrEmpty()) {
                // Fetch markers from the "markers" collection where the ID is in the user's markers list
                db.collection("markers")
                    .whereIn(FieldPath.documentId(), markerIds)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val markers = querySnapshot.documents.mapNotNull { document ->
                            document.toObject(MarkerData::class.java)?.copy(id = document.id)
                        }
                        onMarkersFetched(markers)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Failed to fetch markers: ${exception.message}")
                    }
            } else {
                // If the markers list is empty or null, return an empty list
                onMarkersFetched(emptyList())
            }
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Failed to fetch user markers: ${exception.message}")
        }
}

fun updateUserMarker(
    googleMap: GoogleMap, latitude: Double, longitude: Double,
    userMarker: Marker?, onMarkerUpdated: (Marker) -> Unit
) {
    val newPosition = LatLng(latitude, longitude)

    if (userMarker == null) {
        // Create a new marker if it doesn't exist
        val newMarker = googleMap.addMarker(
            MarkerOptions()
                .position(newPosition)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_dot)) // Use your custom dot image
                .anchor(0.5f, 0.5f) // Center the dot on the location
                .zIndex(10f) // Keep the marker on top of other elements
        )
        if (newMarker != null) {
            onMarkerUpdated(newMarker)
        }
    } else {
        // Update the existing marker's position
        userMarker.position = newPosition
    }
}


//
//@Composable
//fun SendCoordinatesDialog(
//    onDismiss: () -> Unit,
//    onSubmit: (Double, Double) -> Unit
//) {
//    var latitude by remember { mutableStateOf("") }
//    var longitude by remember { mutableStateOf("") }
//
//    AlertDialog(
//        onDismissRequest = { onDismiss() },
//        title = { Text("Enter Coordinates") },
//        text = {
//            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                OutlinedTextField(
//                    value = latitude,
//                    onValueChange = { latitude = it },
//                    label = { Text("Latitude") },
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                )
//                OutlinedTextField(
//                    value = longitude,
//                    onValueChange = { longitude = it },
//                    label = { Text("Longitude") },
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                )
//            }
//        },
//        confirmButton = {
//            Button(onClick = {
//                val lat = latitude.toDoubleOrNull() ?: 0.0
//                val lng = longitude.toDoubleOrNull() ?: 0.0
//                onSubmit(lat, lng)
//                onDismiss()
//            }) {
//
//                Text("Submit")
//            }
//        },
//        dismissButton = {
//            Button(onClick = { onDismiss() }) {
//                Text("Cancel")
//            }
//        }
//    )
//}
//
//@RequiresApi(Build.VERSION_CODES.S)
//@Composable
//fun ArenaMapWithSendCoordinates(
//    modifier: Modifier = Modifier
//) {
//    var showDialog by remember { mutableStateOf(false) }
//    var userLatitude by remember { mutableDoubleStateOf(0.0) }
//    var userLongitude by remember { mutableDoubleStateOf(0.0) }
//    val coordinates = CoordinatesRepository()
//
//    Box(Modifier.fillMaxWidth()) {
//        //ArenaMapUi(
////            pointLatitude = userLatitude,
////            pointLongitude = userLongitude,
////            onSendCoordinates = { _, _ -> showDialog = true }
//        //)
//
//        if (showDialog) {
//            SendCoordinatesDialog(
//                onDismiss = { showDialog = false },
//                onSubmit = { lat, lng ->
//                    userLatitude = lat
//                    userLongitude = lng
//
//                    coordinates.saveCoordinates(
//                        "2",
//                        Coordinates(userLongitude, userLatitude)
//                    )
//                }
//            )
//        }
//    }
//}

@Composable
fun StyledButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 230.dp, height = 60.dp)
            .padding(15.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD25527), Color(0xFFFFAE0C), Color.Black
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(300f, 300f)
                )
            )
            .clickable(onClick = onClick)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(100.dp),
                ambientColor = Color.Black
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White, fontSize = 24.sp
        )
    }
}

fun fetchAlliances(userId: String?, context: Context, callback: (List<Alliances>) -> Unit) {
    if (userId != null) {
        fcul.mei.cm.app.screens.alliances.db.collection("chats")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { result ->
                val alliances = ArrayList<Alliances>()
                for (document in result) {
                    val alliance = document.toObject(Alliances::class.java)
                    alliances.add(alliance)
                }
                callback(alliances)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Failed to fetch alliances: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}

fun getUsersByIds(memberIds: List<String>, callback: (List<User>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val usersList = mutableListOf<User>()

    if (memberIds.isEmpty()) {
        callback(emptyList()) // Return empty if no member IDs
        return
    }

    // Fetch each user by ID
    memberIds.forEach { userId ->
        db.collection(CollectionPath.USERS).document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Map the document to the User object
                    val user = document.toObject(User::class.java)
                    user?.let { usersList.add(it) }

                    // When all users are fetched, call the callback
                    if (usersList.size == memberIds.size) {
                        callback(usersList)
                    }
                }
            }
            .addOnFailureListener { error ->
                Log.e("Firestore", "Error fetching user data: ${error.message}")
            }
    }

    // Handle case where memberIds is empty
    if (memberIds.isEmpty()) {
        callback(emptyList())
    }
}

fun addCurrentUserToSharedUsers(
    targetUserId: String,
    currentUserId: String,
    real: Boolean,
    context: Context
) {
    val db = FirebaseFirestore.getInstance()

    val userRef = db.collection(CollectionPath.USERS).document(targetUserId)

    userRef.get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                // Get the current sharedUserLocations list or initialize as empty
                val sharedLocations = document.get("sharedUserLocations") as? List<Map<String, Boolean>> ?: emptyList()

                // Convert the list to a mutable map for easier updates
                val locationMap = mutableMapOf<String, Boolean>()
                sharedLocations.forEach { map ->
                    locationMap.putAll(map)
                }

                // Update or add the current user ID and its real value
                locationMap[currentUserId] = real

                // Convert back to a List<Map<String, Boolean>> structure
                val updatedLocations = locationMap.map { mapOf(it.key to it.value) }

                // Update Firestore with the new list
                userRef.update("sharedUserLocations", updatedLocations)
                    .addOnSuccessListener {
                        Toast.makeText(context, "User sharing updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "Failed to update sharing: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // If the document doesn't exist, create it with the first entry
                userRef.set(
                    mapOf(
                        "sharedUserLocations" to listOf(
                            mapOf(currentUserId to real)
                        )
                    )
                ).addOnSuccessListener {
                    Toast.makeText(context, "User sharing initialized successfully!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to initialize sharing: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
        }
}




fun removeCurrentUserFromSharedUsers(
    targetUserId: String,
    currentUserId: String,
    context: Context
) {
    val db = FirebaseFirestore.getInstance()

    val userRef = db.collection(CollectionPath.USERS).document(targetUserId)

    userRef.get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                // Get the current sharedUserLocations list
                val sharedLocations = document.get("sharedUserLocations") as? List<Map<String, Boolean>> ?: emptyList()

                // Remove the entry matching the currentUserId
                val updatedLocations = sharedLocations.filterNot { it.containsKey(currentUserId) }

                // Update Firestore with the new list
                userRef.update("sharedUserLocations", updatedLocations)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Removed from shared users!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "Failed to remove user: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "No such user exists!", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Failed to fetch user data: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}


fun setupRealtimeLocationUpdates(
    context: Context,
    userId: String,
    googleMap: GoogleMap?,
    markersMap: MutableMap<String, Marker>
) {
    val firestoreDb = FirebaseFirestore.getInstance()

    // Fetch sharedUserLocations from Firestore
    firestoreDb.collection(CollectionPath.USERS).document(userId)
        .get()
        .addOnSuccessListener { document ->
            val sharedLocationsList = document.get("sharedUserLocations") as? List<Map<String, Boolean>> ?: emptyList()

            // Convert the list to a map for easier handling
            val sharedUserLocations = mutableMapOf<String, Boolean>()
            sharedLocationsList.forEach { locationMap ->
                sharedUserLocations.putAll(locationMap)
            }

            Log.d("sharedids", sharedUserLocations.keys.toString())

            if (sharedUserLocations.isNotEmpty()) {
                // Handle real-time locations
                handleRealTimeLocations(context, sharedUserLocations, googleMap, markersMap)

                // Handle static locations
                handleStaticLocations(context, sharedUserLocations, googleMap, markersMap)
            }
        }
        .addOnFailureListener { error ->
            Toast.makeText(context, "Failed to fetch shared user IDs: ${error.message}", Toast.LENGTH_SHORT).show()
        }
}

private fun handleRealTimeLocations(
    context: Context,
    sharedUserLocations: Map<String, Boolean>,
    googleMap: GoogleMap?,
    markersMap: MutableMap<String, Marker>
) {
    // Filter to only include users sharing real-time locations (true values)
    val realTimeUserIds = sharedUserLocations.filterValues { it }.keys

    locationRef.addChildEventListener(object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            handleLocationChange(snapshot, realTimeUserIds, googleMap, markersMap, context, isRealLocation = true)
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            handleLocationChange(snapshot, realTimeUserIds, googleMap, markersMap, context, isRealLocation = true)
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            val removedUserId = snapshot.key
            if (removedUserId != null && markersMap.containsKey(removedUserId)) {
                markersMap[removedUserId]?.remove()
                markersMap.remove(removedUserId)
            }
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            // Not needed for this use case
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(context, "Failed to listen for location updates: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

private fun handleStaticLocations(
    context: Context,
    sharedUserLocations: Map<String, Boolean>,
    googleMap: GoogleMap?,
    markersMap: MutableMap<String, Marker>
) {
    // Filter to only include users sharing static locations (false values)
    val staticUserIds = sharedUserLocations.filterValues { !it }.keys

    locationRefStatic.addChildEventListener(object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            handleLocationChange(snapshot, staticUserIds, googleMap, markersMap, context, isRealLocation = false)
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            handleLocationChange(snapshot, staticUserIds, googleMap, markersMap, context, isRealLocation = false)
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            val removedUserId = snapshot.key
            if (removedUserId != null && markersMap.containsKey(removedUserId)) {
                markersMap[removedUserId]?.remove()
                markersMap.remove(removedUserId)
            }
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            // Not needed for this use case
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(context, "Failed to listen for static location updates: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    })
}


private fun handleLocationChange(
    snapshot: DataSnapshot,
    userIds: Set<String>,
    googleMap: GoogleMap?,
    markersMap: MutableMap<String, Marker>,
    context: Context,
    isRealLocation: Boolean
) {
    val sharedUserId = snapshot.key
    if (sharedUserId != null && userIds.contains(sharedUserId)) {
        val latitude = snapshot.child("latitude").getValue(Double::class.java)
        val longitude = snapshot.child("longitude").getValue(Double::class.java)

        if (latitude != null && longitude != null) {
            addOrUpdateMarker(
                sharedUserId,
                LatLng(latitude, longitude),
                googleMap,
                markersMap,
                context,
                isRealLocation
            )
        }
    }
}

private fun addOrUpdateMarker(
    userId: String,
    position: LatLng,
    googleMap: GoogleMap?,
    markersMap: MutableMap<String, Marker>,
    context: Context,
    isRealLocation: Boolean
) {
    if (markersMap.containsKey(userId)) {
        // Update the marker's position
        markersMap[userId]?.position = position
    } else {
        getUserNameById(userId) { userName ->
            val marker = googleMap?.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("Shared Location")
                    .snippet(userName ?: "Unknown User")
                    .icon(
                        BitmapDescriptorFactory.fromResource(
                            R.drawable.green_dot
                        )
                    )
            )

            // Add the new marker
            if (marker != null) {
                markersMap[userId] = marker
            }
        }
    }
}

fun saveStaticLocationToDatabase(
    userId: String?,
    latitude: Double,
    longitude: Double,
    context: Context
) {
    if (userId == null) {
        Toast.makeText(context, "User ID is null", Toast.LENGTH_SHORT).show()
        return
    }

    val staticLocationRef = locationRefStatic.child(userId)

    val locationData = mapOf(
        "latitude" to latitude,
        "longitude" to longitude,
    )

    staticLocationRef.setValue(locationData)
        .addOnSuccessListener {
            Toast.makeText(context, "Static location shared successfully!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Failed to share static location: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
}


fun getUserNameById(userId: String, onResult: (String?) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection(CollectionPath.USERS).document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                Log.d("tryingtogetname",document.toString())
                val userName = document.getString("name")
                onResult(userName) // Return the user's name
            } else {
                onResult(null) // Return null if user document does not exist
            }
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error fetching user name: ${exception.message}")
            onResult(null) // Return null in case of an error
        }
}