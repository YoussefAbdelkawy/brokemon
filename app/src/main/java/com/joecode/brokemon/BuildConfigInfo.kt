package com.joecode.brokemon

import android.content.Context

object BuildConfigInfo {
    fun versionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "?"

    @Suppress("DEPRECATION")
    fun versionCode(context: Context): Int =
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else info.versionCode
        }.getOrDefault(0)

    /** A short, non-personal description of the app and phone, for bug reports the user chooses to send. */
    fun deviceSummary(context: Context): String =
        "Brokemon ${versionName(context)} (${versionCode(context)})\n" +
            "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n" +
            "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
}
