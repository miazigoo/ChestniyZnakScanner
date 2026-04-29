package ru.devandprod.chestniyznak.feature.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.devandprod.chestniyznak.core.designsystem.theme.Border
import ru.devandprod.chestniyznak.core.designsystem.theme.Error
import ru.devandprod.chestniyznak.core.designsystem.theme.ErrorContainer
import ru.devandprod.chestniyznak.core.designsystem.theme.Success
import ru.devandprod.chestniyznak.core.designsystem.theme.SuccessContainer

@Composable
fun StatusCard(
    result: ScanResultCardUi,
    modifier: Modifier = Modifier,
) {
    val (containerColor, textColor) = when (result.tone) {
        ScanResultTone.Success -> SuccessContainer to Success
        ScanResultTone.Error -> ErrorContainer to Error
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(28.dp))
            .border(1.dp, Border, RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = result.headline,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = textColor,
        )
        Text(
            text = result.message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black.copy(alpha = 0.8f),
        )
    }
}
