package fcul.mei.cm.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FitnessViewModel : ViewModel() {
    private var sprintDetected by mutableStateOf(false)
    var sprintTime by mutableFloatStateOf(0f)
    private var lastTime = System.nanoTime()

    private var sprintStartTime: Long? = null // Track when the sprint starts

    init {
        // Start a coroutine to track time
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (sprintDetected) {
                    if (sprintStartTime == null) {
                        // Start tracking time when sprinting begins
                        sprintStartTime = System.nanoTime()
                    }
                } else {
                    if (sprintStartTime != null) {
                        // Stop tracking time when sprinting ends
                        val sprintEndTime = System.nanoTime()
                        sprintTime += (sprintEndTime - sprintStartTime!!) / 1_000_000_000f // Convert to seconds
                        sprintStartTime = null
                    }
                }
                delay(1000) // Update every 100ms
            }
        }
    }

    // Call this method to update sprintDetected state from sensor
    fun onSensorChanged(accelerationMagnitude: Float, x: Float) {
        sprintDetected = if (accelerationMagnitude > 15) {
            true
        } else {
            false
        }
    }
}