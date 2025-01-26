package fcul.mei.cm.app.domain

data class MarkerData(
    val id: String? = null, // Optional, Firestore document ID
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var sharedWith: List<String> = emptyList()
)