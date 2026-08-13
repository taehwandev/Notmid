package app.thdev.glassnavlab.core.model.notmid

data class NotmidPlace(
    val title: String,
    val description: String,
    val metric: String,
    val palette: List<NotmidColor>,
    val heightDp: Int,
    val contentColor: NotmidColor = NotmidColors.White,
    val id: String = title.toStableRouteId(),
    val category: String = "",
    val address: String = "",
    val coordinate: NotmidGeoPoint? = null,
    val openNow: Boolean = true,
    val receiptCount: Int = 0,
)
