/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.annotation.StringRes

/** String resources of the backup screen and the privacy dashboard. */
internal object BackupLabels {
    @StringRes
    fun failure(failure: BackupFailure): Int =
        when (failure) {
            BackupFailure.NOT_A_BACKUP -> R.string.backup_error_not_a_backup
            BackupFailure.TRUNCATED -> R.string.backup_error_truncated
            BackupFailure.UNSUPPORTED_FORMAT -> R.string.backup_error_unsupported
            BackupFailure.PASSPHRASE_REQUIRED -> R.string.backup_error_passphrase_required
            BackupFailure.WRONG_PASSPHRASE -> R.string.backup_error_wrong_passphrase
            BackupFailure.CORRUPTED -> R.string.backup_error_corrupted
            BackupFailure.INVALID_CONTENT -> R.string.backup_error_invalid
            BackupFailure.RESTORE_FAILED -> R.string.backup_error_restore_failed
            BackupFailure.FILE_UNREADABLE -> R.string.backup_error_unreadable
            BackupFailure.FILE_UNWRITABLE -> R.string.backup_error_unwritable
        }

    @StringRes
    fun table(table: BackupTableKind): Int =
        when (table) {
            BackupTableKind.PERSONAL_EVENTS -> R.string.backup_table_events
            BackupTableKind.EVENT_RECURRENCES -> R.string.backup_table_recurrences
            BackupTableKind.REMINDERS -> R.string.backup_table_reminders
            BackupTableKind.SHIFT_ROTATIONS -> R.string.backup_table_shift_rotations
            BackupTableKind.SHIFT_ROTATION_RECORDS -> R.string.backup_table_shift_records
            BackupTableKind.ICS_SUBSCRIPTIONS -> R.string.backup_table_subscriptions
            BackupTableKind.WORKDAY_PROFILES -> R.string.backup_table_workdays
            BackupTableKind.OFFICIAL_REMINDERS -> R.string.backup_table_official_reminders
        }

    @StringRes
    fun strength(strength: PassphraseStrength): Int =
        when (strength) {
            PassphraseStrength.EMPTY -> R.string.backup_strength_empty
            PassphraseStrength.WEAK -> R.string.backup_strength_weak
            PassphraseStrength.FAIR -> R.string.backup_strength_fair
            PassphraseStrength.STRONG -> R.string.backup_strength_strong
        }

    @StringRes
    fun problem(problem: PassphraseProblem): Int =
        when (problem) {
            PassphraseProblem.TOO_SHORT -> R.string.backup_problem_too_short
            PassphraseProblem.MISMATCH -> R.string.backup_problem_mismatch
        }

    @StringRes
    fun dataTitle(kind: StoredDataKind): Int =
        when (kind) {
            StoredDataKind.PERSONAL_EVENTS -> R.string.privacy_data_events
            StoredDataKind.REMINDERS -> R.string.privacy_data_reminders
            StoredDataKind.SUBSCRIPTIONS -> R.string.privacy_data_subscriptions
            StoredDataKind.LOCATION -> R.string.privacy_data_location
            StoredDataKind.DEVICE_CALENDAR_CACHE -> R.string.privacy_data_device_calendar
            StoredDataKind.DIAGNOSTICS -> R.string.privacy_data_diagnostics
            StoredDataKind.RECENT_SEARCHES -> R.string.privacy_data_searches
        }

    @StringRes
    fun dataWhere(kind: StoredDataKind): Int =
        when (kind) {
            StoredDataKind.PERSONAL_EVENTS,
            StoredDataKind.REMINDERS,
            StoredDataKind.DIAGNOSTICS,
            -> R.string.privacy_where_database

            StoredDataKind.SUBSCRIPTIONS -> R.string.privacy_where_subscriptions

            StoredDataKind.LOCATION, StoredDataKind.RECENT_SEARCHES -> R.string.privacy_where_settings

            StoredDataKind.DEVICE_CALENDAR_CACHE -> R.string.privacy_where_device_calendar
        }

    /** The clear action of [kind], or `null` when the dashboard cannot clear it. */
    @StringRes
    fun clearAction(kind: StoredDataKind): Int? =
        when (kind) {
            StoredDataKind.LOCATION -> R.string.privacy_clear_location
            StoredDataKind.DEVICE_CALENDAR_CACHE -> R.string.privacy_clear_device_calendar
            StoredDataKind.DIAGNOSTICS -> R.string.privacy_clear_diagnostics
            StoredDataKind.RECENT_SEARCHES -> R.string.privacy_clear_searches
            StoredDataKind.PERSONAL_EVENTS, StoredDataKind.REMINDERS, StoredDataKind.SUBSCRIPTIONS -> null
        }

    @StringRes
    fun permissionTitle(kind: PermissionKind): Int =
        when (kind) {
            PermissionKind.LOCATION -> R.string.privacy_permission_location
            PermissionKind.CALENDAR -> R.string.privacy_permission_calendar
            PermissionKind.NOTIFICATIONS -> R.string.privacy_permission_notifications
            PermissionKind.EXACT_ALARMS -> R.string.privacy_permission_exact_alarms
            PermissionKind.DO_NOT_DISTURB -> R.string.privacy_permission_dnd
        }

    @StringRes
    fun permissionWhy(kind: PermissionKind): Int =
        when (kind) {
            PermissionKind.LOCATION -> R.string.privacy_why_location
            PermissionKind.CALENDAR -> R.string.privacy_why_calendar
            PermissionKind.NOTIFICATIONS -> R.string.privacy_why_notifications
            PermissionKind.EXACT_ALARMS -> R.string.privacy_why_exact_alarms
            PermissionKind.DO_NOT_DISTURB -> R.string.privacy_why_dnd
        }
}
