package com.keeptrack.lasttime.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
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
import com.keeptrack.lasttime.ui.theme.LastTimeTheme
import com.keeptrack.lasttime.ui.theme.pressedIn
import com.keeptrack.lasttime.ui.theme.raised

/** Animated 0..1 press amount for the neumorphic "sink in" effect. */
@Composable
fun rememberPressAmount(interaction: MutableInteractionSource): () -> Float {
    val pressed by interaction.collectIsPressedAsState()
    val amount by animateFloatAsState(if (pressed) 1f else 0f, tween(120), label = "press")
    return { amount }
}

/** A button raised off the ground that sinks in while pressed. */
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    background: Color = LastTimeTheme.palette.ground,
    distance: Dp = 6.dp,
    blur: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val palette = LastTimeTheme.palette
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
        Icon(icon, contentDescription, tint = LastTimeTheme.palette.text, modifier = Modifier.size(22.dp))
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
    val palette = LastTimeTheme.palette
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) palette.text else palette.field)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) palette.ground else palette.muted)
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
) {
    val palette = LastTimeTheme.palette
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text, fontWeight = FontWeight.SemiBold),
        cursorBrush = SolidColor(palette.text),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .height(50.dp)
            .semantics { contentDescription = label }
            .clip(shape)
            .background(palette.field)
            .pressedIn(shape, palette.shadow, palette.highlight, distance = 3.dp, blur = 7.dp),
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = palette.muted)
                inner()
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
    val palette = LastTimeTheme.palette
    val selectedBackground = if (palette.isDark) Color(0xFF3A3F49) else Color.White
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
                    .background(if (isSelected) selectedBackground else Color.Transparent)
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
