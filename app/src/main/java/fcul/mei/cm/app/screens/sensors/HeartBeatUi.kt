package fcul.mei.cm.app.screens.sensors

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fcul.mei.cm.app.R
import fcul.mei.cm.app.viewmodel.SensorsViewModel

@Composable
fun HeartBeatUi(viewModel: SensorsViewModel) {
    val heartRate = viewModel.humidity.value

    if (heartRate == null ){
        Text("Cant access heartBeat")
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (heartRate != null && heartRate > 0) 1.2f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = ""
        )

        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                scale(scale) {
                    drawIntoCanvas { canvas ->
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width / 2, size.height * 0.75f)
                            cubicTo(
                                size.width * 0.1f, size.height * 0.2f,
                                size.width * 0.4f, size.height * -0.1f,
                                size.width / 2, size.height * 0.25f
                            )
                            cubicTo(
                                size.width * 0.6f, size.height * -0.1f,
                                size.width * 0.9f, size.height * 0.2f,
                                size.width / 2, size.height * 0.75f
                            )
                            close()
                        }
                        canvas.drawPath(
                            path,
                            androidx.compose.ui.graphics.Paint()
                                .apply { color = androidx.compose.ui.graphics.Color.Red })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (heartRate != null) {
                Text(
                    text = stringResource(
                        R.string.heartbeat_bpm,
                        if (heartRate > 0) heartRate.toInt() else "--"
                    ),
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    color = androidx.compose.ui.graphics.Color.Black
                )
            }
        }
    }
}