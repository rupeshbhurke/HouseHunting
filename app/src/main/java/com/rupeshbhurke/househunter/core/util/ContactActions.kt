package com.rupeshbhurke.househunter.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object ContactActions {
    /**
     * Digits for a wa.me link. Ten-digit numbers are assumed to be Indian and get the 91 prefix;
     * anything already carrying a country code is left as typed.
     */
    fun whatsAppNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length == 10) "91$digits" else digits.trimStart('0')
    }

    fun dial(context: Context, phone: String) =
        launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}")))

    fun whatsApp(context: Context, phone: String) =
        launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${whatsAppNumber(phone)}")))

    private fun launch(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // No app can handle it; nothing useful to do.
        }
    }
}
