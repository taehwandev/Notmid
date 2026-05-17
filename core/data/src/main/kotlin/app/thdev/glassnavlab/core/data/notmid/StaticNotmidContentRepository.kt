package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidColor
import app.thdev.glassnavlab.core.model.notmid.NotmidColors
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace

class StaticNotmidContentRepository : NotmidContentRepository {
    override fun destinations(): List<NotmidDestination> = NotmidDestinations
}

private fun color(argb: Long) = NotmidColor(argb)

private val NotmidDestinations = listOf(
    NotmidDestination(
        id = "feed",
        title = "Feed",
        subtitle = "Short video receipts from places people say are not mid.",
        icon = NotmidNavigationIcon.Feed,
        clips = listOf(
            NotmidClip(
                title = "Cafe queue check",
                description = "A 14 second proof clip showing wait time, table vibe, and drink texture.",
                badge = "live rn",
                palette = listOf(color(0xFF111827), color(0xFFFF4D6D), color(0xFFFFD166)),
            ),
            NotmidClip(
                title = "Late night ramen",
                description = "Creator-tagged receipt for taste, price, and the actual line outside.",
                badge = "receipt",
                palette = listOf(color(0xFF0F172A), color(0xFF22C55E), color(0xFFCCFBF1)),
            ),
            NotmidClip(
                title = "Gallery opener",
                description = "Muted clip stack with map-aware recommendations nearby.",
                badge = "near you",
                palette = listOf(color(0xFF312E81), color(0xFF7C3AED), color(0xFFFDE68A)),
            ),
        ),
        places = listOf(
            NotmidPlace(
                title = "Millo Roasters",
                description = "Quiet seats, bright windows, and a verified second-floor view.",
                metric = "4.8",
                palette = listOf(color(0xFFFA705A), color(0xFF7A4EF3), color(0xFF232A7C)),
                heightDp = 180,
            ),
            NotmidPlace(
                title = "Han River Steps",
                description = "Sunset angle is checked by clips uploaded in the last hour.",
                metric = "hot",
                palette = listOf(color(0xFF005E7C), color(0xFF5AD7CF), color(0xFFF7E37B)),
                heightDp = 138,
                contentColor = NotmidColors.DarkCardContent,
            ),
            NotmidPlace(
                title = "Basement listening bar",
                description = "Low light, crowded entrance, and sound level summarized from recent posts.",
                metric = "22m",
                palette = listOf(color(0xFF101820), color(0xFF31495E), color(0xFFC8D7E0)),
                heightDp = 154,
            ),
        ),
    ),
    NotmidDestination(
        id = "map",
        title = "Map",
        subtitle = "A place-first discovery view where every pin has recent video proof.",
        icon = NotmidNavigationIcon.Map,
        clips = listOf(
            NotmidClip(
                title = "Cluster around Seongsu",
                description = "Pins rank by fresh clips, chat activity, and save velocity.",
                badge = "map pulse",
                palette = listOf(color(0xFF262626), color(0xFF8D8D8D), color(0xFFF3F3F3)),
            ),
            NotmidClip(
                title = "Avoid the bait",
                description = "Places with old media fall behind live creator receipts.",
                badge = "freshness",
                palette = listOf(color(0xFF005E7C), color(0xFF5AD7CF), color(0xFFF7E37B)),
            ),
            NotmidClip(
                title = "Route-ready saves",
                description = "Saved spots can become a quick crawl without leaving the app.",
                badge = "plan",
                palette = listOf(color(0xFFF65868), color(0xFFB15FF4), color(0xFF593AE8)),
            ),
        ),
        places = listOf(
            NotmidPlace(
                title = "Three pins rising",
                description = "The map can later swap this card for Google Maps or Compose Maps.",
                metric = "3",
                palette = listOf(color(0xFF262626), color(0xFF8D8D8D), color(0xFFF3F3F3)),
                heightDp = 156,
            ),
            NotmidPlace(
                title = "Creator heat",
                description = "A place earns a stronger marker when trusted accounts post in sequence.",
                metric = "live",
                palette = listOf(color(0xFF0E7C66), color(0xFF32D6A2), color(0xFFF6D66B)),
                heightDp = 180,
            ),
            NotmidPlace(
                title = "Neighborhood filter",
                description = "Food, coffee, pop-up, night, and date filters should be domain data.",
                metric = "soon",
                palette = listOf(color(0xFF111827), color(0xFF3B4A6B), color(0xFFB9C7D6)),
                heightDp = 124,
            ),
        ),
    ),
    NotmidDestination(
        id = "capture",
        title = "Capture",
        subtitle = "Record a short place clip, attach proof, and publish without leaking keys.",
        icon = NotmidNavigationIcon.Capture,
        clips = listOf(
            NotmidClip(
                title = "Shoot the receipt",
                description = "Video-first composer with place, mood, wait time, and price notes.",
                badge = "camera",
                palette = listOf(color(0xFFEB4965), color(0xFFF7A35C), color(0xFFFFE4AA)),
            ),
            NotmidClip(
                title = "Add context",
                description = "Creator tags should stay structured so feed and map can share ranking.",
                badge = "tags",
                palette = listOf(color(0xFF2D5BE3), color(0xFF26D0CE), color(0xFFF1F7B5)),
            ),
            NotmidClip(
                title = "Draft safely",
                description = "Uploads can later use Firebase Storage with emulator-first setup.",
                badge = "draft",
                palette = listOf(color(0xFF0D3B2E), color(0xFF1B9C85), color(0xFFCCE8CC)),
            ),
        ),
        places = listOf(
            NotmidPlace(
                title = "Clip composer",
                description = "Camera permissions, draft state, and upload policy belong in impl modules.",
                metric = "01",
                palette = listOf(color(0xFFEB4965), color(0xFFF7A35C), color(0xFFFFE4AA)),
                heightDp = 150,
            ),
            NotmidPlace(
                title = "Place attach",
                description = "Existing place search can become feature:places before Firebase indexing.",
                metric = "02",
                palette = listOf(color(0xFF2D5BE3), color(0xFF26D0CE), color(0xFFF1F7B5)),
                heightDp = 190,
                contentColor = NotmidColors.DarkCardContent,
            ),
            NotmidPlace(
                title = "Publish gate",
                description = "Open-source builds must read Firebase config from ignored local files.",
                metric = "safe",
                palette = listOf(color(0xFF313B72), color(0xFF9C5FD5), color(0xFFE9B5CA)),
                heightDp = 132,
            ),
        ),
    ),
    NotmidDestination(
        id = "inbox",
        title = "Inbox",
        subtitle = "Chat around clips, places, and plans without leaving discovery context.",
        icon = NotmidNavigationIcon.Inbox,
        clips = listOf(
            NotmidClip(
                title = "Clip thread",
                description = "A shared video can open a direct message with the place attached.",
                badge = "chat",
                palette = listOf(color(0xFF172A3A), color(0xFF2E8BC0), color(0xFFB1D4E0)),
            ),
            NotmidClip(
                title = "Plan together",
                description = "Friends vote on spots and turn saved pins into a route.",
                badge = "crew",
                palette = listOf(color(0xFF6930C3), color(0xFF64DFDF), color(0xFFFFF3B0)),
            ),
            NotmidClip(
                title = "Creator reply",
                description = "Public comments and private replies share a conversation domain later.",
                badge = "reply",
                palette = listOf(color(0xFF111111), color(0xFF555555), color(0xFFEDEDED)),
            ),
        ),
        places = listOf(
            NotmidPlace(
                title = "No dead-end share",
                description = "Every shared place keeps the clip, map pin, and chat thread connected.",
                metric = "dm",
                palette = listOf(color(0xFF172A3A), color(0xFF2E8BC0), color(0xFFB1D4E0)),
                heightDp = 154,
            ),
            NotmidPlace(
                title = "Group signal",
                description = "Votes and reactions should feed recommendation signals carefully.",
                metric = "5",
                palette = listOf(color(0xFF6930C3), color(0xFF64DFDF), color(0xFFFFF3B0)),
                heightDp = 130,
                contentColor = NotmidColors.DarkCardContent,
            ),
            NotmidPlace(
                title = "Moderation queue",
                description = "Chats need report states before this becomes a real public service.",
                metric = "mod",
                palette = listOf(color(0xFF111111), color(0xFF555555), color(0xFFEDEDED)),
                heightDp = 176,
            ),
        ),
    ),
    NotmidDestination(
        id = "profile",
        title = "Profile",
        subtitle = "Creator identity, saves, posted receipts, and settings under one route.",
        icon = NotmidNavigationIcon.Profile,
        clips = listOf(
            NotmidClip(
                title = "Creator receipts",
                description = "Your posted clips become credibility, not just content count.",
                badge = "profile",
                palette = listOf(color(0xFF111111), color(0xFF555555), color(0xFFEDEDED)),
            ),
            NotmidClip(
                title = "Saved places",
                description = "Keep spots for later and send them into chat planning.",
                badge = "saves",
                palette = listOf(color(0xFF3D4D57), color(0xFF99AAB5), color(0xFFE5ECEF)),
            ),
            NotmidClip(
                title = "Firebase auth",
                description = "Login is required, but local secrets and production keys stay out of git.",
                badge = "auth",
                palette = listOf(color(0xFF172A3A), color(0xFF2E8BC0), color(0xFFB1D4E0)),
            ),
        ),
        places = listOf(
            NotmidPlace(
                title = "Identity card",
                description = "Display name, handle, avatar, and linked provider will come from auth.",
                metric = "@you",
                palette = listOf(color(0xFF111111), color(0xFF555555), color(0xFFEDEDED)),
                heightDp = 154,
            ),
            NotmidPlace(
                title = "Settings route",
                description = "https://thdev.app/notmid/profile/settings opens Profile then Settings.",
                metric = "web",
                palette = listOf(color(0xFF3D4D57), color(0xFF99AAB5), color(0xFFE5ECEF)),
                heightDp = 130,
                contentColor = NotmidColors.DarkCardContent,
            ),
            NotmidPlace(
                title = "Open-source guardrails",
                description = "Production Firebase config is documented, ignored, and emulator-first.",
                metric = "safe",
                palette = listOf(color(0xFF172A3A), color(0xFF2E8BC0), color(0xFFB1D4E0)),
                heightDp = 144,
            ),
        ),
    ),
)
