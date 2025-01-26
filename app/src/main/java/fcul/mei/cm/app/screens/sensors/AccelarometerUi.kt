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
fun AccelerometerUi(viewModel: SensorsViewModel) {
    val accelerometerValues = viewModel.accelerometerValues.value

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (accelerometerValues != null) {
            Text(
                text = "X: ${String.format(stringResource(R.string._2f), accelerometerValues[0])}\n" +
                        "Y: ${String.format(stringResource(R.string._2f), accelerometerValues[1])}\n" +
                        "Z: ${String.format(stringResource(R.string._2f), accelerometerValues[2])}",
                fontSize = 18.sp
            )
        } else {
            Text(
                text = stringResource(R.string.accelerometer_data_not_available),
                fontSize = 18.sp
            )
        }
    }
}
