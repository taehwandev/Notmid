package app.thdev.glassnavlab.feature.notmid.common.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

data class NotmidPlace(
    val id: String,
    val title: String,
    val description: String,
    val metric: String,
    val palette: List<Color>,
    val height: Dp,
    val contentColor: Color,
    val category: String = "",
    val address: String = "",
    val coordinate: NotmidGeoPoint? = null,
    val openNow: Boolean = true,
    val receiptCount: Int = 0,
)
