package fcul.mei.cm.app.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import fcul.mei.cm.app.database.HealthRepository
import fcul.mei.cm.app.database.UserRepository
import fcul.mei.cm.app.domain.Health
import fcul.mei.cm.app.domain.User
import fcul.mei.cm.app.utils.SensorsUtils
import fcul.mei.cm.app.utils.UserSharedPreferences


@SuppressLint("StaticFieldLeak")
class SensorsViewModel( val context: Context) : ViewModel() {

    val userPreferences = UserSharedPreferences(context)
    val healthRepository = HealthRepository()
    val userRepository = UserRepository()

    private val _health = mutableStateOf<Map<User,Health>>(emptyMap())
    val statusHealth: State<Map<User,Health>> = _health

    private val _isPlayerDead = mutableStateOf(false)
    val isPlayerDead: State<Boolean> = _isPlayerDead

    private val _heartBeat = mutableStateOf<Float?>(null)
    val heartBeat: State<Float?> = _heartBeat

    private val _temperature = mutableStateOf<Float?>(null)
    val temperature: State<Float?> = _temperature

    private val _humidity = mutableStateOf<Float?>(null)
    val humidity: State<Float?> = _humidity

    private val _stepCount = mutableFloatStateOf(0f)
    val stepCount: State<Float> = _stepCount

    private val _accelerometerValues = mutableStateOf<FloatArray?>(null)
    val accelerometerValues: State<FloatArray?> = _accelerometerValues

    private var lastStepTime: Long = 0

    private var lastMovementTime = System.currentTimeMillis()

    init {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val heartBeatSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT)
        if(heartBeatSensor != null){
            val heartBeatListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_HEART_BEAT) {
                        _heartBeat.value = event.values[0]
                        checkPlayerDeath()
                        saveUserHealthData()
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
        }

        val temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (temperatureSensor != null) {
            val temperatureListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                        _temperature.value = event.values[0]
                        checkPlayerDeath()
                        saveUserHealthData()

                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(
                temperatureListener,
                temperatureSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        val humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        if (humiditySensor != null) {
            val humidityListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_RELATIVE_HUMIDITY) {
                        _humidity.value = event.values[0]
                        checkPlayerDeath()
                        saveUserHealthData()

                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(
                humidityListener,
                humiditySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor != null) {
            val stepCounterListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                        _stepCount.floatValue = event.values[0]
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(
                stepCounterListener,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometerSensor != null) {
            val accelerometerListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        _accelerometerValues.value = event.values

                        if (SensorsUtils.isMotionless(event.values)) {
                            lastMovementTime = System.currentTimeMillis()
                            checkPlayerDeath()
                        }

                        lastStepTime = SensorsUtils.detectStep(
                            accelerometerValues = event.values,
                            lastStepTime = lastStepTime,
                            onStepDetected = { currentTime ->
                                _stepCount.value += 1
                            }
                        )
                    }
                }

                override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

            }
            sensorManager.registerListener(
                accelerometerListener,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    private fun checkPlayerDeath() {
        _isPlayerDead.value = SensorsUtils.checkPlayerDeath(
            _accelerometerValues.value,
            _temperature.value,
            _humidity.value,
            lastMovementTime
        )
    }

    private fun saveUserHealthData() {
        val userId = userPreferences.getUserId()
        if(userId != null ){
            val health = Health(
                userId = userId,
                temperature = _temperature.value ?: 0f,
                humidity = _humidity.value ?: 0f,
                heartBeat = 0,
                stepCount = _stepCount.floatValue
            )
            healthRepository.saveHealthInformation(health)
        }

    }

    fun getHealthStatusAllianceMembers() {
        val user = userPreferences.getUserId()
        if(user!=null){
            userRepository.getUser(user){ getUser ->
                getUser?.district?.let {
                    userRepository.getUserFromSameDistrict(it){
                        healthRepository.getHealthInformationByUser(user){ health ->
                            _health.value = mapOf(
                                getUser to health!!
                            )
                        }
                    }
                }
            }
        }
    }
}