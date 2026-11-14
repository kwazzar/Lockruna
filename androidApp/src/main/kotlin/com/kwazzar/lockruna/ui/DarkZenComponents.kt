@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber", "MatchingDeclarationName")

package com.kwazzar.lockruna.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlin.random.Random

object DarkZenPalette {
    val Background = Color(0xFF0F1424)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0x99FFFFFF)
    val Card = Color(0x0FFFFFFF)
    val Panel = Color(0x0DFFFFFF)
    val Toolbar = Color(0x1AFFFFFF)
    val Divider = Color(0x1AFFFFFF)
    val Accent = Color(0xFFDB34F2)
    val SecondaryAccent = Color(0xFFBF5AF2)
    val TertiaryAccent = Color(0xFF0A84FF)
    val Success = Color(0xFF30D158)
    val Warning = Color(0xBFFF9F0A)
    val Danger = Color(0xFFFF453A)
    val Muted = Color(0x33FFFFFF)
}

@Composable
fun DarkZenBackground(modifier: Modifier = Modifier) {
    val stars = remember { List(40) { Random.nextFloat() to Random.nextFloat() } }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkZenPalette.Background),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEach { (x, y) ->
                drawCircle(
                    color = Color(0x14FFFFFF),
                    radius = 1.dp.toPx(),
                    center = Offset(x * size.width, y * size.height),
                )
            }
        }
    }
}

@Composable
fun DarkZenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = DarkZenPalette.TextSecondary) },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = DarkZenPalette.TextPrimary,
            unfocusedTextColor = DarkZenPalette.TextPrimary,
            disabledTextColor = DarkZenPalette.TextSecondary,
            errorTextColor = DarkZenPalette.TextPrimary,
            focusedContainerColor = DarkZenPalette.Card,
            unfocusedContainerColor = DarkZenPalette.Card,
            disabledContainerColor = DarkZenPalette.Card,
            errorContainerColor = DarkZenPalette.Card,
            cursorColor = DarkZenPalette.Accent,
            errorCursorColor = DarkZenPalette.Danger,
            focusedBorderColor = DarkZenPalette.Accent,
            unfocusedBorderColor = DarkZenPalette.Divider,
            disabledBorderColor = DarkZenPalette.Divider,
            errorBorderColor = DarkZenPalette.Danger,
            focusedTrailingIconColor = DarkZenPalette.TextPrimary,
            unfocusedTrailingIconColor = DarkZenPalette.TextPrimary,
            disabledTrailingIconColor = DarkZenPalette.TextSecondary,
            errorTrailingIconColor = DarkZenPalette.Danger,
            focusedSupportingTextColor = DarkZenPalette.TextSecondary,
            unfocusedSupportingTextColor = DarkZenPalette.TextSecondary,
            disabledSupportingTextColor = DarkZenPalette.TextSecondary,
            errorSupportingTextColor = DarkZenPalette.Danger,
        ),
    )
}

@Composable
fun DarkZenCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DarkZenPalette.Card,
        contentColor = DarkZenPalette.TextPrimary,
        border = BorderStroke(1.dp, DarkZenPalette.Divider),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content,
        )
    }
}

@Composable
fun DarkZenPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkZenPalette.Accent,
            contentColor = DarkZenPalette.TextPrimary,
            disabledContainerColor = DarkZenPalette.Muted,
            disabledContentColor = DarkZenPalette.TextSecondary,
        ),
        content = {
            ProvideTextStyle(TextStyle(fontWeight = FontWeight.Bold)) {
                content()
            }
        },
    )
}

@Composable
fun DarkZenSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = DarkZenPalette.Panel,
            contentColor = DarkZenPalette.TextPrimary,
            disabledContainerColor = DarkZenPalette.Panel,
            disabledContentColor = DarkZenPalette.TextSecondary,
        ),
        border = BorderStroke(1.dp, DarkZenPalette.Divider),
        content = content,
    )
}

@Composable
fun DarkZenDangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = DarkZenPalette.Danger,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = DarkZenPalette.TextSecondary,
        ),
        content = content,
    )
}
