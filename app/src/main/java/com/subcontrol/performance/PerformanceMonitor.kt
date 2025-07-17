package com.subcontrol.performance

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring system for SubControl application.
 * 
 * This class provides real-time performance monitoring capabilities
 * with focus on dashboard load times and critical user interactions.
 * 
 * Key Features:
 * - Dashboard load time tracking (<150ms target)
 * - Memory usage monitoring
 * - Frame timing analysis
 * - Performance regression detection
 * - Continuous monitoring hooks
 * 
 * Usage:
 * - Call startTiming() before operations
 * - Call endTiming() after operations
 * - Monitor performance metrics via StateFlow
 */
@Singleton
class PerformanceMonitor @Inject constructor() {

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    private val _dashboardLoadTime = MutableStateFlow(0L)
    val dashboardLoadTime: StateFlow<Long> = _dashboardLoadTime.asStateFlow()

    private val _isPerformanceMonitoringEnabled = MutableStateFlow(true)
    val isPerformanceMonitoringEnabled: StateFlow<Boolean> = _isPerformanceMonitoringEnabled.asStateFlow()

    private val activeTimers = mutableMapOf<String, Long>()
    private val performanceHistory = mutableListOf<PerformanceRecord>()
    private val scope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val DASHBOARD_LOAD_TARGET_MS = 150L
        private const val MAX_HISTORY_SIZE = 100
        
        // Performance timing keys
        const val DASHBOARD_LOAD = "dashboard_load"
        const val SUBSCRIPTION_FETCH = "subscription_fetch"
        const val ANALYTICS_CALCULATION = "analytics_calculation"
        const val UI_RENDERING = "ui_rendering"
        const val DATA_ENCRYPTION = "data_encryption"
    }

    /**
     * Start timing for a specific operation.
     * 
     * @param operationName Unique identifier for the operation
     */
    fun startTiming(operationName: String) {
        if (!_isPerformanceMonitoringEnabled.value) return
        
        val startTime = SystemClock.elapsedRealtime()
        activeTimers[operationName] = startTime
        
        Log.d(TAG, "Started timing: $operationName at $startTime")
    }

    /**
     * End timing for a specific operation and record the result.
     * 
     * @param operationName Unique identifier for the operation
     * @return Duration in milliseconds, or -1 if no matching start was found
     */
    fun endTiming(operationName: String): Long {
        if (!_isPerformanceMonitoringEnabled.value) return -1L
        
        val endTime = SystemClock.elapsedRealtime()
        val startTime = activeTimers.remove(operationName)
        
        if (startTime == null) {
            Log.w(TAG, "No start time found for operation: $operationName")
            return -1L
        }
        
        val duration = endTime - startTime
        recordPerformanceMetric(operationName, duration)
        
        Log.d(TAG, "Completed timing: $operationName - ${duration}ms")
        return duration
    }

    /**
     * Record a performance metric and update monitoring state.
     * 
     * @param operationName Name of the operation
     * @param durationMs Duration in milliseconds
     */
    private fun recordPerformanceMetric(operationName: String, durationMs: Long) {
        scope.launch {
            // Create performance record
            val record = PerformanceRecord(
                operationName = operationName,
                durationMs = durationMs,
                timestamp = System.currentTimeMillis(),
                isWithinTarget = when (operationName) {
                    DASHBOARD_LOAD -> durationMs <= DASHBOARD_LOAD_TARGET_MS
                    else -> true // Default to true for non-critical operations
                }
            )
            
            // Add to history
            performanceHistory.add(record)
            if (performanceHistory.size > MAX_HISTORY_SIZE) {
                performanceHistory.removeAt(0)
            }
            
            // Update specific metrics
            when (operationName) {
                DASHBOARD_LOAD -> {
                    _dashboardLoadTime.value = durationMs
                    if (durationMs > DASHBOARD_LOAD_TARGET_MS) {
                        Log.w(TAG, "Dashboard load time exceeded target: ${durationMs}ms > ${DASHBOARD_LOAD_TARGET_MS}ms")
                    }
                }
            }
            
            // Update overall metrics
            updatePerformanceMetrics()
        }
    }

    /**
     * Update overall performance metrics based on recent history.
     */
    private fun updatePerformanceMetrics() {
        val currentMetrics = _performanceMetrics.value
        
        // Calculate dashboard performance
        val dashboardRecords = performanceHistory.filter { it.operationName == DASHBOARD_LOAD }
        val dashboardPerformance = if (dashboardRecords.isNotEmpty()) {
            DashboardPerformance(
                averageLoadTime = dashboardRecords.map { it.durationMs }.average().toLong(),
                minLoadTime = dashboardRecords.minOfOrNull { it.durationMs } ?: 0L,
                maxLoadTime = dashboardRecords.maxOfOrNull { it.durationMs } ?: 0L,
                targetComplianceRate = dashboardRecords.count { it.isWithinTarget } / dashboardRecords.size.toFloat()
            )
        } else {
            currentMetrics.dashboardPerformance
        }
        
        // Calculate overall performance
        val allRecords = performanceHistory.takeLast(20) // Last 20 operations
        val overallPerformance = if (allRecords.isNotEmpty()) {
            OverallPerformance(
                totalOperations = allRecords.size.toLong(),
                averageResponseTime = allRecords.map { it.durationMs }.average().toLong(),
                slowOperationsCount = allRecords.count { it.durationMs > 500L }.toLong(),
                performanceScore = calculatePerformanceScore(allRecords)
            )
        } else {
            currentMetrics.overallPerformance
        }
        
        // Update state
        _performanceMetrics.value = PerformanceMetrics(
            dashboardPerformance = dashboardPerformance,
            overallPerformance = overallPerformance,
            isMonitoringActive = _isPerformanceMonitoringEnabled.value,
            lastUpdateTime = System.currentTimeMillis()
        )
    }

    /**
     * Calculate performance score based on operation history.
     * 
     * @param records List of performance records to analyze
     * @return Performance score from 0.0 to 1.0 (higher is better)
     */
    private fun calculatePerformanceScore(records: List<PerformanceRecord>): Float {
        if (records.isEmpty()) return 1.0f
        
        val targetCompliantCount = records.count { it.isWithinTarget }
        val baseScore = targetCompliantCount / records.size.toFloat()
        
        // Adjust score based on absolute performance
        val avgDuration = records.map { it.durationMs }.average()
        val durationPenalty = when {
            avgDuration > 1000 -> 0.3f
            avgDuration > 500 -> 0.15f
            avgDuration > 200 -> 0.05f
            else -> 0.0f
        }
        
        return (baseScore - durationPenalty).coerceIn(0.0f, 1.0f)
    }

    /**
     * Get performance history for analysis.
     * 
     * @param operationName Optional filter by operation name
     * @return List of performance records
     */
    fun getPerformanceHistory(operationName: String? = null): List<PerformanceRecord> {
        return if (operationName != null) {
            performanceHistory.filter { it.operationName == operationName }
        } else {
            performanceHistory.toList()
        }
    }

    /**
     * Check if dashboard performance meets target requirements.
     * 
     * @return True if dashboard load time is within target
     */
    fun isDashboardPerformanceOptimal(): Boolean {
        val currentLoadTime = _dashboardLoadTime.value
        return currentLoadTime > 0 && currentLoadTime <= DASHBOARD_LOAD_TARGET_MS
    }

    /**
     * Enable or disable performance monitoring.
     * 
     * @param enabled True to enable monitoring
     */
    fun setMonitoringEnabled(enabled: Boolean) {
        _isPerformanceMonitoringEnabled.value = enabled
        Log.d(TAG, "Performance monitoring ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Clear performance history and reset metrics.
     */
    fun clearHistory() {
        performanceHistory.clear()
        activeTimers.clear()
        _performanceMetrics.value = PerformanceMetrics()
        _dashboardLoadTime.value = 0L
        Log.d(TAG, "Performance history cleared")
    }

    /**
     * Get current performance summary for debugging.
     * 
     * @return Human-readable performance summary
     */
    fun getPerformanceSummary(): String {
        val metrics = _performanceMetrics.value
        return buildString {
            appendLine("Performance Summary:")
            appendLine("Dashboard Load Time: ${_dashboardLoadTime.value}ms (Target: ${DASHBOARD_LOAD_TARGET_MS}ms)")
            appendLine("Dashboard Average: ${metrics.dashboardPerformance.averageLoadTime}ms")
            appendLine("Target Compliance: ${(metrics.dashboardPerformance.targetComplianceRate * 100).toInt()}%")
            appendLine("Overall Score: ${(metrics.overallPerformance.performanceScore * 100).toInt()}%")
            appendLine("Total Operations: ${metrics.overallPerformance.totalOperations}")
            appendLine("Slow Operations: ${metrics.overallPerformance.slowOperationsCount}")
        }
    }
}

/**
 * Data class representing overall performance metrics.
 */
data class PerformanceMetrics(
    val dashboardPerformance: DashboardPerformance = DashboardPerformance(),
    val overallPerformance: OverallPerformance = OverallPerformance(),
    val isMonitoringActive: Boolean = true,
    val lastUpdateTime: Long = System.currentTimeMillis()
)

/**
 * Data class representing dashboard-specific performance metrics.
 */
data class DashboardPerformance(
    val averageLoadTime: Long = 0L,
    val minLoadTime: Long = 0L,
    val maxLoadTime: Long = 0L,
    val targetComplianceRate: Float = 1.0f
)

/**
 * Data class representing overall application performance metrics.
 */
data class OverallPerformance(
    val totalOperations: Long = 0L,
    val averageResponseTime: Long = 0L,
    val slowOperationsCount: Long = 0L,
    val performanceScore: Float = 1.0f
)

/**
 * Data class representing a single performance record.
 */
data class PerformanceRecord(
    val operationName: String,
    val durationMs: Long,
    val timestamp: Long,
    val isWithinTarget: Boolean
)