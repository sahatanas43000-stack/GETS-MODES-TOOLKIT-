package com.getsmodes.toolkit

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

/** Home screen widget: four buttons that open a tool directly. */
class GetsWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_toolkit)
            views.setOnClickPendingIntent(R.id.btn_qr, open(context, "QR Code Scanner"))
            views.setOnClickPendingIntent(R.id.btn_notes, open(context, "Offline Notes"))
            views.setOnClickPendingIntent(R.id.btn_pdf, open(context, "Image to PDF"))
            views.setOnClickPendingIntent(R.id.btn_rates, open(context, "Currency Converter"))
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun open(context: Context, tool: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_TOOL, tool)
            // A distinct data URI keeps the four PendingIntents separate.
            data = Uri.parse("getsmodes://tool/" + Uri.encode(tool))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context, tool.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
