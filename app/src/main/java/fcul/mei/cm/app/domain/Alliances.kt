package fcul.mei.cm.app.domain

data class Alliances(
    val id: String = "",
    val chatName: String = "",
    val creationTime: Long = System.currentTimeMillis(),
    val description: String = "",
    val owner: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val memberRequest: List<String> = emptyList(),
    val messages: List<Message> = emptyList()
)