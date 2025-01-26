package fcul.mei.cm.app.domain

data class Message(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
