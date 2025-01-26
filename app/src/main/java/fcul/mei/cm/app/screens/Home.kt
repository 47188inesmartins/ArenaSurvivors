package fcul.mei.cm.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fcul.mei.cm.app.R


@Composable
fun Home(
    modifier: Modifier = Modifier,
    onClickChatButton: () -> Unit,
    onClickHealthButton: () -> Unit,
    onClickAlliancesList: () -> Unit,
    onClickUserButton: () -> Unit,
    onClickUserInfoButton: () -> Unit,
) {
    Column (
        modifier = modifier
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppButton(onClick = onClickChatButton, painter = painterResource(id = R.drawable.chat), contentDescription = "Chat")
            AppButton(onClick = onClickHealthButton, painter = painterResource(id = R.drawable.health), contentDescription = "Health")
            AppButton(onClick = onClickAlliancesList, painter = painterResource(id = R.drawable.alliance), contentDescription = "Alliances")
            AppButton(onClick = onClickUserButton, painter = painterResource(id = R.drawable.user), contentDescription = "User")
           // AppButton(onClick = onClickUserInfoButton, painter = painterResource(id = R.drawable.user), contentDescription = "info")
        }
    }
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    contentDescription: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
        ) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(86.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = contentDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

    }
}



