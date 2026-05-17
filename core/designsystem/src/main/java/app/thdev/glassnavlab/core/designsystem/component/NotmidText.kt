package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

enum class NotmidTextVariant {
    Display,
    Title,
    Headline,
    Body,
    BodySmall,
    Label,
    Caption,
}

@Composable
fun NotmidText(
    text: String,
    modifier: Modifier = Modifier,
    variant: NotmidTextVariant = NotmidTextVariant.Body,
    color: Color = NotmidTheme.colors.content,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = variant.notmidTextStyle(),
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        style = style,
    )
}

@Composable
fun NotmidTextVariant.notmidTextStyle(): TextStyle {
    return when (this) {
        NotmidTextVariant.Display -> NotmidTheme.typography.display
        NotmidTextVariant.Title -> NotmidTheme.typography.title
        NotmidTextVariant.Headline -> NotmidTheme.typography.headline
        NotmidTextVariant.Body -> NotmidTheme.typography.body
        NotmidTextVariant.BodySmall -> NotmidTheme.typography.bodySmall
        NotmidTextVariant.Label -> NotmidTheme.typography.label
        NotmidTextVariant.Caption -> NotmidTheme.typography.caption
    }
}
