package app.thdev.glassnavlab.feature.notmid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun LoginHelpButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(LoginPrimaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "?",
            color = LoginOnPrimaryContainer,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 24.sp,
        )
    }
}
