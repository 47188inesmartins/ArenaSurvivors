package fcul.mei.cm.app.utils

import kotlin.math.sqrt

object SensorsUtils {

    private const val STEP_THRESHOLD = 15f

    fun detectStep(
        accelerometerValues: FloatArray,
        lastStepTime: Long,
        onStepDetected: (Long) -> Unit
    ): Long {
        val x = accelerometerValues[0]
        val y = accelerometerValues[1]
        val z = accelerometerValues[2]

        val acceleration = sqrt((x * x + y * y + z * z).toDouble())

        if (acceleration > STEP_THRESHOLD) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastStepTime > 500) {
                onStepDetected(currentTime)
                return currentTime
            }
        }

        return lastStepTime
    }

    fun isMotionless(accelerometerValues: FloatArray?): Boolean {
        return accelerometerValues?.let {
            val threshold = 0.1f
            it.all { axisValue -> axisValue < threshold }
        } ?: false
    }

    private fun isExtremeTemperature(temperature: Float?): Boolean {
        return temperature != null && (temperature < 0 || temperature > 47)
    }

    private fun isExtremeHumidity(humidity: Float?): Boolean {
        return humidity != null && (humidity < 10 || humidity > 90)
    }

    fun checkPlayerDeath(accelerometerValues: FloatArray?,
                         temperature: Float?,
                         humidity: Float?,
                         lastMovementTime: Long): Boolean {
        val currentTime = System.currentTimeMillis()

        val timeSinceLastMovement = currentTime - lastMovementTime

        if (timeSinceLastMovement > 5 * 60 * 1000) {
            return true
        }

        if (isExtremeTemperature(temperature) || isExtremeHumidity(humidity)) {
            return true
        }

        if (isMotionless(accelerometerValues)) {
            return true
        }

        return false
    }
}
