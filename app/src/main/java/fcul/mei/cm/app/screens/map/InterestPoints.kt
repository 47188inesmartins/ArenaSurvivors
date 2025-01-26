package fcul.mei.cm.app.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil


@Composable
fun SpotsListUi(
    userLocation: LatLng,
    coordinatesMap: Map<Pair<Double, Double>, String>
) {
    // Calculate distances and sort by closest
    val sortedSpots = coordinatesMap.map { (coords, name) ->
        val spotLocation = LatLng(coords.first, coords.second)
        val distance = SphericalUtil.computeDistanceBetween(userLocation, spotLocation)
        SpotInfo(name = name, latitude = coords.first, longitude = coords.second, distance = distance)
    }.sortedBy { it.distance }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Spots List (Ordered by Distance)",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Display each spot
        items(sortedSpots.size) { index ->
            SpotRow(spot = sortedSpots[index])
        }
    }
}

@Composable
fun SpotRow(spot: SpotInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(color = Color.LightGray)
            .padding(16.dp)
    ) {
        Text(text = spot.name, fontSize = 18.sp, modifier = Modifier.padding(bottom = 4.dp))
        Text(
            text = "Coordinates: ${spot.latitude}, ${spot.longitude}",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = "Distance: ${String.format("%.2f", spot.distance / 1000)} km",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

data class SpotInfo(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Double
)