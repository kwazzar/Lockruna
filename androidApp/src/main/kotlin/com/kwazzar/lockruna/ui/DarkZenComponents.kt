import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    Box(modifier = modifier.fillMaxSize())
}

@Composable
fun DarkZenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        modifier = modifier,
    )
}

@Composable
fun DarkZenCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.padding(14.dp),
    ) {
        Divider(
            color = DarkZenPalette.Divider,
            thickness = 1.dp,
        )
        content()
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
            .fillMaxWidth(),
        content = content
    )
}

@Composable
fun DarkZenSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        content = content
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
        modifier = modifier
            .fillMaxWidth(),
        content = content
    )
}