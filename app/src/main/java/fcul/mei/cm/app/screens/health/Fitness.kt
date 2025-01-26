package fcul.mei.cm.app.screens.health

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import fcul.mei.cm.app.viewmodel.FitnessViewModel
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun Fitness(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    var stepCount by remember { mutableFloatStateOf(0f) }
    var restTime by remember { mutableStateOf(0f) }
    var humidity by remember { mutableStateOf(0f) }
    var temperature by remember { mutableStateOf(0f) }

    // Check and request permissions
  //  LaunchedEffect(Unit) {
  //      activity?.let {
  //          requestSensorPermissions(it)
  //      }
  //  }

    LaunchedEffect(Unit) {
        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        val temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_STEP_COUNTER -> stepCount = event.values[0]
                    Sensor.TYPE_ACCELEROMETER -> restTime = calculateRestTime(event.values)
                    Sensor.TYPE_RELATIVE_HUMIDITY -> humidity = event.values[0]
                    Sensor.TYPE_AMBIENT_TEMPERATURE -> temperature = event.values[0]
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        stepCounter?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        humiditySensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        temperatureSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Step Count: $stepCount")
            Text(text = "Rest Time: $restTime s")
            Text(text = "Humidity: $humidity %")
            Text(text = "Temperature: $temperature °C")
        }
    }
}

private fun calculateRestTime(values: FloatArray): Float {
    // Implement logic to detect resting state (low acceleration values).
    return 0f // Replace with actual calculation.
}


@Composable
fun AccelerometerGame(sensorManager: SensorManager, accelerometer: Sensor?, fitnessViewModel: FitnessViewModel) {
    var sprintDetected by remember { mutableStateOf(false) }
    var dodgeDetected by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val viewModel = fitnessViewModel
    var listeningEnabled by remember { mutableStateOf(true) } // Track if listening is enabled

    if (accelerometer == null) {
        Log.e("AccelerometerGame", "Accelerometer not available on this device.")
        return
    } else {
        Log.d("AccelerometerGame", "Accelerometer found and ready.")
    }



    // Listener to detect sensor events
    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {


                event?.let {
                    // Calculate movement intensity
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    // Calculate the magnitude of acceleration
                    val accelerationMagnitude = sqrt(x * x + y * y + z * z)

                    // Detect sprint (shaking rapidly)
                    if (accelerationMagnitude > 15) { // Adjust threshold as needed
                        sprintDetected = true
                        listeningEnabled = false // Disable listening temporarily



                    } else {
                       // if (!listeningEnabled) return
                        sprintDetected = false
                    }

                    // Detect dodge (tilt phone left/right)
                    if (x > 5 || x < -5) { // Adjust tilt threshold as needed
                        dodgeDetected = true
                    } else {
                        dodgeDetected = false
                    }
                    viewModel.onSensorChanged(accelerationMagnitude, x)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // Register the sensor listener
    DisposableEffect(Unit) {
        val success = sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        if (!success) {
            Log.e("AccelerometerGame", "Failed to register accelerometer listener.")
        } else {
            Log.d("AccelerometerGame", "Success registering the accelerometer listener.")
        }

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    // Handle the delay to re-enable listening using LaunchedEffect
//    LaunchedEffect(sprintDetected) {
//        if (!listeningEnabled) {
//            delay(1000) // Wait for 1 second
//            listeningEnabled = true // Re-enable listening
//        }
//    }
    // UI
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (sprintDetected) "Sprinting!" else "Not Sprinting",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (dodgeDetected) "Dodged!" else "Not Dodging",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Time spent sprinting: ${"%.2f".format(viewModel.sprintTime)} seconds",
                style = MaterialTheme.typography.bodyLarge
            )

            Log.d("AccelerometerGame", "UI recomposed. Sprint: $sprintDetected, Dodge: $dodgeDetected")
        }
    }

}

@Composable
fun Request() {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context = context) }
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )
    val viewModel = remember { PermissionsViewModel(healthConnectManager) }

    PermissionScreen(
        permissions = permissions,
        viewModel = viewModel,
        onPermissionsGranted = {
            Log.d("yes","grantedddddd")
            // Proceed with accessing data
        },
        onPermissionsDenied = {
            Log.d("not","obviously not granted")
            // Show a message or handle denied permissions
        }
    )
}


@Composable
fun PermissionScreen(
    permissions: Set<String>,
    viewModel: PermissionsViewModel,
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val permissionsGranted by viewModel.permissionsGranted.collectAsState()

    // Permission request launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = viewModel.healthConnectManager.requestPermissionsActivityContract()
    ) { grantedPermissions ->
        if (grantedPermissions.containsAll(permissions)) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }

    // Check permissions on first render
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            viewModel.checkPermissions(permissions)
        }
    }
    Log.d("granted", permissionsGranted.toString())
    // UI
    if (permissionsGranted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Text("Permissions Granted! Accessing data...")}
        onPermissionsGranted()
    } else {
        Log.d("w","tf")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Permissions are required to use this feature.")
            Button(onClick = {
                viewModel.requestPermissions(permissions, requestPermissionLauncher)
            }) {
                Text("Request Permissions")
            }
        }
    }
}

