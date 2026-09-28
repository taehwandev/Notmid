package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme

@Preview(
    name = "Feed proof stream",
    showBackground = true,
    backgroundColor = 0xFFF3F1EC,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun FeedContentPreview() {
    notmidTheme(darkTheme = false) {
        FeedContent(
            state = PreviewFeedState,
            listState = rememberLazyListState(),
            onAction = {},
        )
    }
}

private val PreviewFeedState = FeedUiState(
    title = "Feed",
    subtitle = "Short video receipts from places people say are not mid.",
    heroClip = FeedClipUi(
        id = "cafe-queue-check",
        title = "Cafe queue check",
        caption = "A 14 second proof clip showing wait time, table vibe, and drink texture.",
        creatorHandle = "min.zip",
        capturedAtLabel = "8m",
        palette = listOf(
            NotmidColorTokens.Ink,
            NotmidColorTokens.WarmClip,
            NotmidColorTokens.NightViolet,
        ),
        moodTags = listOf("line proof", "worth it", "quiet seats"),
        placeId = "millo-roasters",
    ),
    queue = listOf(
        FeedClipUi(
            id = "late-night-ramen",
            title = "Late night ramen",
            caption = "Creator-tagged receipt for taste, price, and the actual line outside.",
            creatorHandle = "receipt.han",
            capturedAtLabel = "22m",
            palette = listOf(
                Color(0xFF0F172A),
                NotmidColorTokens.SignalGreen,
                Color(0xFFCCFBF1),
            ),
            moodTags = listOf("price check", "line proof"),
            placeId = "basement-listening-bar",
        ),
        FeedClipUi(
            id = "gallery-opener",
            title = "Gallery opener",
            caption = "Muted clip stack with map-aware recommendations nearby.",
            creatorHandle = "nari.place",
            capturedAtLabel = "41m",
            palette = listOf(
                Color(0xFF312E81),
                NotmidColorTokens.NightViolet,
                Color(0xFFFDE68A),
            ),
            moodTags = listOf("exhibit", "date safe"),
            placeId = "han-river-steps",
        ),
    ),
    places = listOf(
        FeedPlaceUi(
            id = "millo-roasters",
            title = "Millo Roasters",
            subtitle = "Quiet seats, bright windows, and a verified second-floor view.",
            metric = "4.8",
            palette = listOf(
                NotmidColorTokens.WarmClip,
                NotmidColorTokens.NightViolet,
                NotmidColorTokens.RouteBlue,
            ),
        ),
        FeedPlaceUi(
            id = "basement-listening-bar",
            title = "Basement listening bar",
            subtitle = "Low light, crowded entrance, and sound level summarized from recent posts.",
            metric = "22m",
            palette = listOf(
                NotmidColorTokens.Ink,
                NotmidColorTokens.Slate,
                NotmidColorTokens.Mist,
            ),
        ),
        FeedPlaceUi(
            id = "han-river-steps",
            title = "Han River Steps",
            subtitle = "Sunset angle is checked by clips uploaded in the last hour.",
            metric = "hot",
            palette = listOf(
                NotmidColorTokens.RouteBlue,
                NotmidColorTokens.SignalGreen,
                Color(0xFFF7E37B),
            ),
        ),
    ),
)
