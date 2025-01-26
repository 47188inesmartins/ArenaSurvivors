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
fun HumidityUi(viewModel: SensorsViewModel) {
    val humidity = viewModel.humidity.value

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (humidity != null) {
            Text(
                text = stringResource(
                    R.string.current_humidity,
                    String.format(stringResource(R.string._1f), humidity)
                ),
                fontSize = 24.sp
            )
        } else {
            Text(
                text = stringResource(R.string.humidity_sensor_not_available),
                fontSize = 24.sp
            )
        }
    }
}