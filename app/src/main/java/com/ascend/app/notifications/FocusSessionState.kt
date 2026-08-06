package com.ascend.app.notifications

/** Set true while a Focus Session is on screen so the morning/evening
 * notification workers skip firing during that window (Flow theory —
 * interruptions end a flow state and cost far more than they save). */
object FocusSessionState {
    @Volatile
    var isActive: Boolean = false
}
