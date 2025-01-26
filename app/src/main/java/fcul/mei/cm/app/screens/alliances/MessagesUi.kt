package fcul.mei.cm.app.screens.alliances

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import fcul.mei.cm.app.R


@Composable
fun MessageBubbleCurrentUser(sender: String, message: String) {
    MessageBubble(
        sender = sender,
        message = message,
        color = colorResource(R.color.blue_50),
        alignment = Alignment.End)
}

@Composable
fun MessageBubbleOtherUser(sender: String, message: String) {
    MessageBubble(
        sender = sender,
        message = message,
        color = colorResource(R.color.blue_400),
        alignment = Alignment.Start)
}

@Composable
fun MessageBubble(
    sender: String,
    message: String,
    color: Color,
    alignment: Alignment.Horizontal,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        horizontalAlignment = alignment
    ) {
        Text(
            text = sender,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color, shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(
                text = message,
                color = Color.Black,
            )
        }
    }
}