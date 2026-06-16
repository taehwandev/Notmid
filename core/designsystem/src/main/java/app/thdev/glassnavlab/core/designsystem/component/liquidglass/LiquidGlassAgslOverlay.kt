package app.thdev.glassnavlab.core.designsystem.component.liquidglass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope

internal fun DrawScope.drawLiquidGlassAgslOverlay(
    shape: Shape,
    surfaceColor: Color,
    renderMode: LiquidGlassRenderMode,
    progress: Float,
) {
    if (!shouldUseAgslOverlay(renderMode)) return

    LiquidGlassAgslRenderer.draw(
        drawScope = this,
        shape = shape,
        surfaceColor = surfaceColor,
        progress = progress,
    )
}

private fun shouldUseAgslOverlay(renderMode: LiquidGlassRenderMode): Boolean {
    return renderMode != LiquidGlassRenderMode.Legacy &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object LiquidGlassAgslRenderer {
    private val shader = RuntimeShader(LiquidGlassAgslSource)
    private val brush = ShaderBrush(shader)

    fun draw(
        drawScope: DrawScope,
        shape: Shape,
        surfaceColor: Color,
        progress: Float,
    ) = with(drawScope) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform(
            "surfaceColor",
            surfaceColor.red,
            surfaceColor.green,
            surfaceColor.blue,
            surfaceColor.alpha,
        )
        shader.setFloatUniform("progress", progress.coerceIn(0f, 1f))

        drawOutline(
            outline = shape.createOutline(
                size = size,
                layoutDirection = layoutDirection,
                density = this,
            ),
            brush = brush,
        )
    }
}

private const val LiquidGlassAgslSource = """
uniform float2 resolution;
uniform float4 surfaceColor;
uniform float progress;

half4 main(float2 coord) {
    float2 uv = float2(
        coord.x / max(resolution.x, 1.0),
        coord.y / max(resolution.y, 1.0)
    );

    float edgeX = min(uv.x, 1.0 - uv.x);
    float edgeY = min(uv.y, 1.0 - uv.y);
    float rim = 1.0 - smoothstep(0.0, 0.18, min(edgeX, edgeY));
    rim = pow(rim, 2.35);

    float upperSheen = 1.0 - smoothstep(0.0, 0.42, uv.y);
    float lowerShade = smoothstep(0.58, 1.0, uv.y);
    float diagonal = 1.0 - smoothstep(0.0, 0.54, abs((uv.x * 0.82 + uv.y) - 0.42));

    float waveA = sin((uv.x * 13.0 + uv.y * 4.5) + progress * 2.8);
    float waveB = cos((uv.y * 15.0 - uv.x * 3.0) - progress * 2.1);
    float liquid = (waveA * waveB) * 0.5 + 0.5;

    float active = progress;
    float highlight = rim * 0.120
        + upperSheen * 0.075
        + diagonal * 0.045
        + liquid * active * 0.052;
    float shade = lowerShade * (0.035 + active * 0.035);

    float3 color = surfaceColor.rgb;
    color += float3(highlight);
    color -= float3(shade);
    color = clamp(color, 0.0, 1.0);

    float alpha = surfaceColor.a * 0.10
        + rim * (0.048 + active * 0.025)
        + upperSheen * 0.034
        + liquid * active * 0.020;
    alpha = clamp(alpha, 0.0, 0.20);

    return half4(color.r, color.g, color.b, alpha);
}
"""
