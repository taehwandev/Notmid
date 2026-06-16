package app.thdev.glassnavlab.core.base.activity

import android.graphics.Color
import androidx.activity.SystemBarStyle

data class EdgeToEdgeConfig(
    val statusBarStyle: SystemBarStyle,
    val navigationBarStyle: SystemBarStyle,
)

object EdgeToEdgeDefaults {
    fun lightTransparent(): EdgeToEdgeConfig {
        return EdgeToEdgeConfig(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
    }
}
