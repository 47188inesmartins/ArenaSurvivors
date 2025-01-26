package fcul.mei.cm.app.screens.sensors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fcul.mei.cm.app.R
import fcul.mei.cm.app.viewmodel.SensorsViewModel

@Composable
fun TemperatureUi(viewModel: SensorsViewModel) {
    val temperature = viewModel.temperature.value

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (temperature != null) {
            Text(
                text = stringResource(
                    R.string.current_temperature_celsius,
                    String.format(stringResource(R.string._1f), temperature)
                ),
                fontSize = 24.sp
            )
        } else {
            Text(
                text = stringResource(R.string.temperature_sensor_not_available),
                fontSize = 24.sp
            )
        }
    }
}

