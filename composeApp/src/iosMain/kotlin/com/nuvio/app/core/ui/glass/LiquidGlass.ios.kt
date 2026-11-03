package com.nuvio.app.core.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

actual class PlatformLayerBackdrop

@Composable
actual fun isDeviceGlassCapable(): Boolean = true

@Composable
actual fun isLiquidGlassBackdropSupported(): Boolean = false

@Composable
actual fun isLiquidGlassEnabled(): Boolean = LocalLiquidGlass.current

@Composable
actual fun rememberPlatformLayerBackdrop(color: Color): PlatformLayerBackdrop? = null

@Composable
actual fun Modifier.liquidGlassSource(backdrop: PlatformLayerBackdrop?): Modifier = this

@Composable
actual fun liquidGlassContainerColor(
    color: Color,
    enabled: Boolean,
    backdrop: PlatformLayerBackdrop?,
): Color = color

@Composable
actual fun Modifier.liquidGlass(
    backdrop: PlatformLayerBackdrop?,
    shape: Shape,
    interactive: Boolean,
): Modifier {
    if (!LocalLiquidGlass.current) return this
    return this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
}

@Composable
actual fun Modifier.liquidGlassChrome(
    shape: Shape,
    enabled: Boolean,
    preset: LiquidGlassPreset,
    backdrop: PlatformLayerBackdrop?,
): Modifier {
    if (!enabled) return this
    return this.liquidGlass(backdrop = backdrop, shape = shape, interactive = false)
}

@Composable
actual fun LiquidGlassContainer(
    backdrop: PlatformLayerBackdrop?,
    modifier: Modifier,
    shape: Shape,
    interactive: Boolean,
    contentAlignment: Alignment,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.liquidGlass(backdrop, shape, interactive),
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
actual fun LiquidGlassActionPill(
    backdrop: PlatformLayerBackdrop?,
    modifier: Modifier,
    shape: Shape,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.liquidGlass(backdrop, shape, true),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
actual fun LiquidGlassIconButton(
    backdrop: PlatformLayerBackdrop?,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    shape: Shape,
    tint: Color,
    interactive: Boolean,
) {
    LiquidGlassContainer(
        backdrop = backdrop,
        modifier = modifier,
        shape = shape,
        interactive = interactive,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp).clickable(onClick = onClick),
        )
    }
}
