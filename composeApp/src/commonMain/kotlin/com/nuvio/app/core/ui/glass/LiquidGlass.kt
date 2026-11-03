package com.nuvio.app.core.ui.glass

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Shared opt-in flag for Settings > Appearance > Liquid Glass UI. */
val LocalLiquidGlass = staticCompositionLocalOf { false }

/** Content-brightness hint. */
val LocalLiquidGlassContentBrightness = compositionLocalOf { 0f }

/** Presets matching LastWave Native. */
enum class LiquidGlassPreset {
    BottomNavigation,
    MiniPlayer,
    PlayerControls,
    FloatingControls,
    Header,
    ModalSheet,
    ContextMenu,
    Overlay,
    Card,
}

internal fun LiquidGlassPreset.isGlassTarget(): Boolean = when (this) {
    LiquidGlassPreset.BottomNavigation,
    LiquidGlassPreset.MiniPlayer,
    LiquidGlassPreset.PlayerControls,
    LiquidGlassPreset.FloatingControls,
    LiquidGlassPreset.Header -> true
    LiquidGlassPreset.ModalSheet,
    LiquidGlassPreset.ContextMenu,
    LiquidGlassPreset.Overlay,
    LiquidGlassPreset.Card -> false
}

expect class PlatformLayerBackdrop

@Composable
expect fun isDeviceGlassCapable(): Boolean

@Composable
expect fun isLiquidGlassBackdropSupported(): Boolean

@Composable
expect fun isLiquidGlassEnabled(): Boolean

@Composable
expect fun rememberPlatformLayerBackdrop(color: Color = Color.Transparent): PlatformLayerBackdrop?

@Composable
expect fun Modifier.liquidGlassSource(backdrop: PlatformLayerBackdrop?): Modifier

@Composable
expect fun liquidGlassContainerColor(
    color: Color,
    enabled: Boolean = LocalLiquidGlass.current,
    backdrop: PlatformLayerBackdrop? = null,
): Color

@Composable
expect fun Modifier.liquidGlass(
    backdrop: PlatformLayerBackdrop?,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
): Modifier

@Composable
expect fun Modifier.liquidGlassChrome(
    shape: Shape,
    enabled: Boolean = true,
    preset: LiquidGlassPreset = LiquidGlassPreset.Card,
    backdrop: PlatformLayerBackdrop? = null,
): Modifier

@Composable
expect fun LiquidGlassContainer(
    backdrop: PlatformLayerBackdrop?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
)

@Composable
expect fun LiquidGlassActionPill(
    backdrop: PlatformLayerBackdrop?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable RowScope.() -> Unit,
)

@Composable
expect fun LiquidGlassIconButton(
    backdrop: PlatformLayerBackdrop?,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    tint: Color = Color.White,
    interactive: Boolean = true,
)
