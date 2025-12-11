package com.app.miklink.core.domain.logs

/**
 * LogFilter - placeholder for log filter rules
 *
 * Responsibilities:
 * - filter a list or stream of logs by severity/keywords per user prefs
 */
interface LogFilter {
    fun filter(logs: List<LogEntry>, prefs: FilterPreferences): List<LogEntry>
}

data class LogEntry(val timestamp: Long, val severity: String, val message: String)

data class FilterPreferences(val severities: Set<String>, val keywords: List<String>)
