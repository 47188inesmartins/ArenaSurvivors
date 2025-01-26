package fcul.mei.cm.app.screens.sensors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fcul.mei.cm.app.R
import fcul.mei.cm.app.viewmodel.SensorsViewModel

@Composable
fun StepCountUI(viewModel: SensorsViewModel) {
    val stepCount = viewModel.stepCount.value

    // Define the step count range
    val minStepsForSurvival = 5000 // Example: Minimum steps required for Hunger Games survival
    val maxStepsForSafety = 25000  // Example: Beyond this, it may lead to fatigue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display current step count
            Text(
                text = stringResource(
                    R.string.current_step_count,
                    String.format(stringResource(R.string._1f), stepCount)
                ),
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Display health advice based on step count
            when {
                stepCount < minStepsForSurvival -> {
                    Text(
                        text = "You might need to be more active to survive the Hunger Games.",
                        color = Color.Red,
                        fontSize = 18.sp
                    )
                }
                stepCount > maxStepsForSafety -> {
                    Text(
                        text = "You already walked a lot today, be careful not to get too fatigued.",
                        color = Color.Yellow,
                        fontSize = 18.sp
                    )
                }
                else -> {
                    Text(
                        text = "You're within a healthy activity range for survival. Keep it up!",
                        color = Color.Green,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}