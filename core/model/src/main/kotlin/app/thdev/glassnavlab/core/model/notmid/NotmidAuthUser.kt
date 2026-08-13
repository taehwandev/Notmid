package app.thdev.glassnavlab.core.model.notmid

data class NotmidAuthUser(
    val id: String,
    val handle: String,
    val displayName: String,
    val homeNeighborhood: String,
    val avatarImageUrl: String,
    val roles: List<String>,
)
