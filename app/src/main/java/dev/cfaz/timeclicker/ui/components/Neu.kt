package dev.cfaz.timeclicker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.cfaz.timeclicker.ui.theme.TimeClickerTheme
import dev.cfaz.timeclicker.ui.theme.pressedIn
import dev.cfaz.timeclicker.ui.theme.raised
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

/**
 * Animated 0..1 press amount for the neumorphic "sink in" effect.
 *
 * A quick tap is pressed and released in the same instant (in a scrolling list the press is only
 * reported on release), so a released press first finishes sinking in, then comes back up:
 * every tap shows the whole effect, not only a long hold.
 */
@Composable
fun rememberPressAmount(interaction: InteractionSource): () -> Float {
    val amount = remember { Animatable(0f) }
    LaunchedEffect(interaction) {
        val presses = mutableSetOf<PressInteraction.Press>()
        // Latest: a new press interrupts the release animation, and a release the press one.
        interaction.interactions.collectLatest { event ->
            when (event) {
                is PressInteraction.Press -> presses += event
                is PressInteraction.Release -> presses -= event.press
                // Cancelled (e.g. the list scrolled instead): no click, so no need to finish the press.
                is PressInteraction.Cancel -> {
                    presses -= event.press
                    if (presses.isEmpty()) amount.animateTo(0f, tween(PressOutMillis, easing = FastOutSlowInEasing))
                    return@collectLatest
                }
                else -> return@collectLatest
            }
            val remaining = (PressInMillis * (1f - amount.value)).roundToInt()
            amount.animateTo(1f, tween(remaining, easing = LinearOutSlowInEasing))
            if (presses.isEmpty()) amount.animateTo(0f, tween(PressOutMillis, easing = FastOutSlowInEasing))
        }
    }
    return { amount.value }
}

private const val PressInMillis = 90
private const val PressOutMillis = 220

/** A button raised off the ground that sinks in while pressed. */
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    background: Color = TimeClickerTheme.palette.ground,
    distance: Dp = 6.dp,
    blur: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val palette = TimeClickerTheme.palette
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressAmount(interaction)
    Box(
        modifier
            .graphicsLayer { val s = 1f - 0.03f * press(); scaleX = s; scaleY = s }
            .raised(shape, palette.shadow, palette.highlight, distance, blur, pressed = press)
            .clip(shape)
            .background(background)
            .pressedIn(shape, palette.shadow, palette.highlight, distance * 0.6f, blur * 0.7f, amount = press)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun NeuIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    shape: Shape = RoundedCornerShape(18.dp),
) {
    NeuButton(onClick = onClick, modifier = modifier.size(size), shape = shape) {
        Icon(icon, contentDescription, tint = TimeClickerTheme.palette.text, modifier = Modifier.size(22.dp))
    }
}

/** High-contrast filled pill, for the primary action of a screen or sheet. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = TimeClickerTheme.palette
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) palette.accent else palette.field)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) palette.onAccent else palette.muted)
    }
}

/** A single-line text field pressed into the surface. */
@Composable
fun NeuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
    /** Drawn at the start, e.g. a magnifier for a search field. */
    leadingIcon: ImageVector? = null,
    /** Drawn at the end, e.g. a button clearing the field. */
    trailing: (@Composable () -> Unit)? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    val palette = TimeClickerTheme.palette
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text, fontWeight = FontWeight.SemiBold),
        cursorBrush = SolidColor(palette.text),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .height(50.dp)
            .semantics { contentDescription = label }
            .clip(shape)
            .background(palette.field)
            .pressedIn(shape, palette.shadow, palette.highlight, distance = 3.dp, blur = 7.dp),
        decorationBox = { inner ->
            Row(
                Modifier.padding(start = if (leadingIcon != null) 14.dp else 16.dp, end = if (trailing != null) 4.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leadingIcon != null) Icon(leadingIcon, null, tint = palette.muted, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = palette.muted)
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

/** Mutually exclusive options in a pill track, e.g. Small / Wide / Tall. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = TimeClickerTheme.palette
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.field)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) palette.segment else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) palette.text else palette.muted,
                )
            }
        }
    }
}
