package com.reminder.daily.data

import android.content.Context

object Prefs {
    private const val PREF_NAME = "reminder_prefs"
    private const val KEY_LIST_ID = "list_id"

    fun getListId(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LIST_ID, null)

    fun setListId(context: Context, listId: String) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LIST_ID, listId).apply()
}
