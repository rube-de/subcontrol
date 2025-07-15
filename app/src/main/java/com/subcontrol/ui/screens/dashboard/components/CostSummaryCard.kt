package com.subcontrol.ui.screens.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.subcontrol.R
import com.subcontrol.ui.theme.SubControlTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

/**
 * Card component that displays cost summary including monthly and yearly totals.
 */
@Composable
fun CostSummaryCard(
    monthlyCost: BigDecimal,
    yearlyCost: BigDecimal,
    currency: String = "USD",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cost_summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Monthly cost
            CostRow(
                label = stringResource(R.string.monthly_total),
                amount = monthlyCost,
                currency = currency,
                isLarge = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Yearly cost
            CostRow(
                label = stringResource(R.string.yearly_total),
                amount = yearlyCost,
                currency = currency,
                isLarge = false
            )
        }
    }
}

/**
 * Row component for displaying a cost label and amount.
 */
@Composable
private fun CostRow(
    label: String,
    amount: BigDecimal,
    currency: String,
    isLarge: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isLarge) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = formatCurrency(amount, currency),
            style = if (isLarge) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = if (isLarge) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Formats a BigDecimal amount as currency.
 */
private fun formatCurrency(amount: BigDecimal, currencyCode: String): String {
    return try {
        val formatter = NumberFormat.getCurrencyInstance()
        formatter.currency = Currency.getInstance(currencyCode)
        formatter.format(amount)
    } catch (e: Exception) {
        "$currencyCode ${amount.setScale(2)}"
    }
}

@Preview
@Composable
private fun CostSummaryCardPreview() {
    SubControlTheme {
        CostSummaryCard(
            monthlyCost = BigDecimal("49.97"),
            yearlyCost = BigDecimal("599.64"),
            currency = "USD"
        )
    }
}