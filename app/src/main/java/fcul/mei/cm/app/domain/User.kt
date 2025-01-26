package fcul.mei.cm.app.domain

import java.util.UUID

data class User (
    val id: String = UUID.randomUUID().toString() + "-" + System.currentTimeMillis(),
    val district: Int = 0,
    val role: String = "participant",
    val name: String = "",
    val status: String = "none",
    val joinedAt: Long = 0,
    var allianceName: String? = null,
    val districtChat:String? = null,
    var markers: List<String> = emptyList(),
    var alliances: List<String> = emptyList(),
    var sharedUserLocations: List<Map<String, Boolean>> = emptyList(),
    val stepCount: Int = 0
) {
    init {
        require(district in 0 until 13)
    }
}