package ru.devandprod.chestniyznak.feature.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.devandprod.chestniyznak.R
import ru.devandprod.chestniyznak.core.designsystem.theme.CurrentAppDecorColors

@Composable
fun StatusCard(
    result: ScanResultCardUi,
    modifier: Modifier = Modifier,
) {
    val decor = CurrentAppDecorColors
    val (containerColor, textColor) = when (result.tone) {
        ScanResultTone.Success -> decor.successContainer to decor.success
        ScanResultTone.Error -> decor.dangerContainer to decor.danger
        ScanResultTone.Warning -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = containerColor,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.status_last_scan),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(textColor, androidx.compose.foundation.shape.CircleShape),
                )
            }
            Text(
                text = result.headline,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = textColor,
            )
            Text(
                text = result.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            )
        }
    }
}
