package fcul.mei.cm.app

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import fcul.mei.cm.app.ui.theme.AppTheme
import fcul.mei.cm.app.utils.createNotificationChannel
import fcul.mei.cm.app.viewmodel.FitnessViewModel
import fcul.mei.cm.app.viewmodel.SensorsViewModel
import fcul.mei.cm.app.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val fitnessViewModel: FitnessViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Log.e("AccelerometerGame", "Accelerometer not available on this device.")
        } else {
            Log.d("AccelerometerGame", "Accelerometer found and ready.")
        }
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val navHostController = rememberNavController()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                                actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            title = {
                                Text(
                                    text = stringResource(R.string.title),
                                    color = Color.White,
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { navHostController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Menu Icon",
                                        tint = Color(0xFFFFA726)
                                    )
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    UiNav(
                        navController = navHostController,
                        sensorManager,
                        accelerometer,
                        fitnessViewModel,
                        SensorsViewModel(LocalContext.current),
                        UserViewModel(LocalContext.current),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}