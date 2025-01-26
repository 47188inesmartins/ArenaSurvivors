package fcul.mei.cm.app.screens.sensors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fcul.mei.cm.app.utils.UserSharedPreferences
import fcul.mei.cm.app.viewmodel.SensorsViewModel

@Composable
fun SensorsDisplayUi(viewModel: SensorsViewModel) {
    val context = LocalContext.current

    val userId = UserSharedPreferences(context).getUserId()

    if (userId == null) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("You need to register first", style = MaterialTheme.typography.headlineMedium, color = Color.Gray)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TemperatureUi(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        HumidityUi(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        StepCountUI(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        AccelerometerUi(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        HeartBeatUi(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        PlayerStatusUi(viewModel)
    }
}