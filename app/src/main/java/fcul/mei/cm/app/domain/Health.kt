package fcul.mei.cm.app.domain

data class Health (
    val userId: String = "",
    val temperature: Float? = 0f,
    val humidity: Float? = 0f,
    val heartBeat: Int? = 0,
    val stepCount: Float? = 0f ,
    val isPlayerAlive: Boolean = true ,
)