package fcul.mei.cm.app.screens.sensors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import fcul.mei.cm.app.R
import fcul.mei.cm.app.viewmodel.SensorsViewModel

@Composable
fun PlayerStatusUi(viewModel: SensorsViewModel) {
    val isPlayerDead = viewModel.isPlayerDead.value

    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isPlayerDead) {
                stringResource(R.string.player_is_dead)
            } else {
                stringResource(R.string.player_is_alive)
            },
            fontSize = 24.sp,
            color = if (isPlayerDead) Color.Red else Color.Green
        )
    }
}
