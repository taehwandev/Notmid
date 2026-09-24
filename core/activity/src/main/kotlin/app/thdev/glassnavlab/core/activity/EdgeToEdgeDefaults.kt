package app.thdev.glassnavlab.core.activity

import android.graphics.Color
import androidx.activity.SystemBarStyle

object EdgeToEdgeDefaults {
    fun lightTransparent(): EdgeToEdgeConfig {
        return EdgeToEdgeConfig(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
    }
}
