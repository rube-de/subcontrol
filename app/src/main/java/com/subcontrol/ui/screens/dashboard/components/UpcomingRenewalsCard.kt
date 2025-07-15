package com.subcontrol.ui.screens.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.subcontrol.R
import com.subcontrol.domain.usecase.analytics.UpcomingCost
import com.subcontrol.ui.theme.SubControlTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency

/**
 * Card component that displays upcoming subscription renewals.
 */
@Composable
fun UpcomingRenewalsCard(
    upcomingRenewals: List<UpcomingCost>,
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
                    text = stringResource(R.string.upcoming_renewals),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (upcomingRenewals.isEmpty()) {
                // Empty state
                EmptyRenewalsState()
            } else {
                // List of upcoming renewals
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(upcomingRenewals) { upcomingCost ->
                        UpcomingRenewalItem(
                            upcomingCost = upcomingCost,
                            currency = currency
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual item for an upcoming renewal.
 */
@Composable
private fun UpcomingRenewalItem(
    upcomingCost: UpcomingCost,
    currency: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = upcomingCost.subscriptionName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = formatRenewalDate(upcomingCost.billingDate, upcomingCost.daysUntilBilling),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatCurrency(upcomingCost.amount, currency),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = formatDaysUntilRenewal(upcomingCost.daysUntilBilling),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    upcomingCost.daysUntilBilling <= 1 -> MaterialTheme.colorScheme.error
                    upcomingCost.daysUntilBilling <= 3 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Empty state when there are no upcoming renewals.
 */
@Composable
private fun EmptyRenewalsState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.no_upcoming_renewals),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Formats the renewal date with relative time.
 */
private fun formatRenewalDate(date: LocalDate, daysUntil: Int): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd")
    return when (daysUntil) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> date.format(formatter)
    }
}

/**
 * Formats the days until renewal.
 */
private fun formatDaysUntilRenewal(daysUntil: Int): String {
    return when (daysUntil) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> "in $daysUntil days"
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
private fun UpcomingRenewalsCardPreview() {
    val sampleRenewals = listOf(
        UpcomingCost(
            subscriptionId = "1",
            subscriptionName = "Netflix",
            amount = BigDecimal("9.99"),
            currency = "USD",
            billingDate = LocalDate.now().plusDays(1),
            daysUntilBilling = 1
        ),
        UpcomingCost(
            subscriptionId = "2",
            subscriptionName = "Spotify Premium",
            amount = BigDecimal("11.99"),
            currency = "USD",
            billingDate = LocalDate.now().plusDays(3),
            daysUntilBilling = 3
        ),
        UpcomingCost(
            subscriptionId = "3",
            subscriptionName = "Adobe Creative Cloud",
            amount = BigDecimal("49.99"),
            currency = "USD",
            billingDate = LocalDate.now().plusDays(7),
            daysUntilBilling = 7
        )
    )
    
    SubControlTheme {
        UpcomingRenewalsCard(
            upcomingRenewals = sampleRenewals,
            currency = "USD"
        )
    }
}