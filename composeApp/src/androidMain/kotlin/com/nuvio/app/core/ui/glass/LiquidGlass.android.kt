package com.nuvio.app.core.ui.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.sign

actual typealias PlatformLayerBackdrop = LayerBackdrop

@Composable
actual fun isDeviceGlassCapable(): Boolean {
    val view = LocalView.current
    if (view.isInEditMode) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    if (!view.isHardwareAccelerated) return false
    val context = LocalContext.current
    val am = remember(context) {
        runCatching { context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager }.getOrNull()
    }
    if (am?.isLowRamDevice == true) return false
    return true
}

@Composable
actual fun isLiquidGlassBackdropSupported(): Boolean =
    LocalLiquidGlass.current && isDeviceGlassCapable()

@Composable
actual fun isLiquidGlassEnabled(): Boolean = LocalLiquidGlass.current

@Composable
actual fun rememberPlatformLayerBackdrop(color: Color): PlatformLayerBackdrop? =
    rememberLayerBackdrop {
        if (color.isSpecified && color != Color.Transparent) {
            drawRect(color)
        }
        drawContent()
    }

@Composable
actual fun Modifier.liquidGlassSource(backdrop: PlatformLayerBackdrop?): Modifier {
    if (backdrop == null) return this
    if (!isLiquidGlassBackdropSupported()) return this
    return this.layerBackdrop(backdrop)
}

@Composable
actual fun liquidGlassContainerColor(
    color: Color,
    enabled: Boolean,
    backdrop: PlatformLayerBackdrop?,
): Color = if (enabled && isLiquidGlassBackdropSupported() && backdrop != null) {
    if (color.isSpecified && color.alpha < 0.95f) color else Color.Transparent
} else color

internal object GlassRecipe {
    const val BLUR_BASE_DP = 12f
    const val BLUR_BRIGHT_DP = 20f
    const val BLUR_DARK_DP = 6f

    const val BRIGHTNESS = 0.05f
    const val CONTRAST = 1f
    const val SATURATION = 1.1f

    const val LENS_DIVISOR = 6f

    const val MIN_SCRIM = 0.18f
    const val MAX_SCRIM = 0.56f

    const val PRESS_BLUR_DP = 2f
}

class GlassInteraction(
    private val animationScope: CoroutineScope,
) {
    private val pressSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 300f, visibilityThreshold = 0.001f)
    private val pressAnimation = Animatable(0f, 0.001f)

    val pressProgress: Float get() = pressAnimation.value

    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    suspend fun detectPress(pointer: PointerInputScope) =
        with(pointer) {
            inspectDragGestures(
                onDragStart = { down ->
                    touchPosition = down.position
                    animationScope.launch { pressAnimation.animateTo(1f, pressSpec) }
                },
                onDragEnd = { animationScope.launch { pressAnimation.animateTo(0f, pressSpec) } },
                onDragCancel = { animationScope.launch { pressAnimation.animateTo(0f, pressSpec) } },
            ) { change, _ ->
                touchPosition = change.position
            }
        }
}

@Composable
fun rememberGlassInteraction(): GlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlassInteraction(scope) }
}

internal fun lensSafeGlassShape(shape: Shape): Shape =
    if (shape == RectangleShape) RoundedCornerShape(0.dp) else shape

fun Modifier.drawInteractiveGlass(
    isDark: Boolean,
    backdrop: LayerBackdrop,
    luminanceAnimation: Float,
    shape: Shape,
    interaction: GlassInteraction?,
    pressedScale: Float = 1.12f,
    highlight: Highlight = Highlight.Default,
    blurScale: Float = 1f,
    minScrim: Float = GlassRecipe.MIN_SCRIM,
    maxScrim: Float = GlassRecipe.MAX_SCRIM,
): Modifier =
    this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { lensSafeGlassShape(shape) },
            highlight = { highlight },
            effects = {
                val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
                val press = interaction?.pressProgress ?: 0f
                colorControls(
                    brightness = GlassRecipe.BRIGHTNESS,
                    contrast = GlassRecipe.CONTRAST,
                    saturation = GlassRecipe.SATURATION,
                )
                blur(
                    (
                        if (l > 0f) {
                            lerp(
                                GlassRecipe.BLUR_BASE_DP.dp.toPx(),
                                GlassRecipe.BLUR_BRIGHT_DP.dp.toPx(),
                                l,
                            )
                        } else {
                            lerp(
                                GlassRecipe.BLUR_BASE_DP.dp.toPx(),
                                GlassRecipe.BLUR_DARK_DP.dp.toPx(),
                                -l,
                            )
                        }
                    ) * blurScale + GlassRecipe.PRESS_BLUR_DP.dp.toPx() * press,
                )
                lens(
                    size.minDimension / GlassRecipe.LENS_DIVISOR +
                        GlassRecipe.PRESS_BLUR_DP.dp.toPx() * press,
                    size.minDimension / 2f,
                    false,
                )
            },
            onDrawBackdrop = { drawBackdrop ->
                drawBackdrop()
            },
            onDrawSurface = {
                val darken = lerp(minScrim, maxScrim, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
                drawRect((if (isDark) Color.Black else Color.White).copy(alpha = darken))
                val press = interaction?.pressProgress ?: 0f
                if (press > 0f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f * press),
                                Color.Transparent,
                            ),
                            center = interaction?.touchPosition ?: Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension * 1.2f,
                        ),
                        blendMode = BlendMode.Plus,
                    )
                }
            },
            layerBlock =
                if (interaction != null) {
                    {
                        val scale = lerp(1f, pressedScale, interaction.pressProgress)
                        scaleX = scale
                        scaleY = scale
                    }
                } else {
                    null
                },
        ).then(
            if (interaction != null) {
                Modifier.pointerInput(interaction) { interaction.detectPress(this) }
            } else {
                Modifier
            },
        )

@Composable
actual fun Modifier.liquidGlass(
    backdrop: PlatformLayerBackdrop?,
    shape: Shape,
    interactive: Boolean,
): Modifier {
    if (!LocalLiquidGlass.current || backdrop == null || !isLiquidGlassBackdropSupported()) {
        return this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
    }
    val interaction = if (interactive) rememberGlassInteraction() else null
    return this.drawInteractiveGlass(
        isDark = true,
        backdrop = backdrop,
        luminanceAnimation = 0.5f,
        shape = shape,
        interaction = interaction,
        highlight = Highlight.Default,
    )
}

@Composable
actual fun Modifier.liquidGlassChrome(
    shape: Shape,
    enabled: Boolean,
    preset: LiquidGlassPreset,
    backdrop: PlatformLayerBackdrop?,
): Modifier {
    if (!enabled || !preset.isGlassTarget() || backdrop == null || !isLiquidGlassBackdropSupported()) {
        return this
    }
    return this.liquidGlass(
        backdrop = backdrop,
        shape = shape,
        interactive = preset != LiquidGlassPreset.BottomNavigation && preset != LiquidGlassPreset.Header,
    )
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
    if (backdrop == null || !isLiquidGlassBackdropSupported()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)),
            contentAlignment = contentAlignment,
            content = content,
        )
        return
    }
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
    if (backdrop == null || !isLiquidGlassBackdropSupported()) {
        Row(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        return
    }
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

internal suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down)
        onDrag(down, Offset.Zero)
        val upEvent = drag(
            pointerId = down.id,
            onDrag = { onDrag(it, it.positionChange()) },
        )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) {
        return null
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) {
            return null
        }
        if (change.changedToUpIgnoreConsumed()) {
            return change
        }
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) {
                return dragEvent
            }
        }
    }
}
