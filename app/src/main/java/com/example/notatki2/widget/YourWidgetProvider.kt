package com.example.notatki2.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.example.notatki2.MainActivity
import com.example.notatki2.R // Важно, чтобы этот импорт был правильным

class YourWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "YourWidgetProvider"
        // Константы теперь в WidgetConstants, если ты их создал, или здесь, если нет.
        // Для данного исправления предположим, что они определены здесь или в WidgetConfigurationActivity.Companion
        const val ACTION_ITEM_CLICK = "com.example.notatki2.widget.ACTION_ITEM_CLICK"
        const val EXTRA_ITEM_POSITION = "com.example.notatki2.widget.EXTRA_ITEM_POSITION"
        const val ACTION_REFRESH_WIDGET = "com.example.notatki2.widget.ACTION_REFRESH_WIDGET"
        const val ACTION_CONFIGURE_WIDGET = "com.example.notatki2.widget.ACTION_CONFIGURE_WIDGET"

        // Ключи для SharedPreferences, которые используются в WidgetConfigurationActivity
        // Они должны быть идентичны тем, что в WidgetConfigurationActivity.Companion
        const val PREFS_NAME = "com.example.notatki2.widget.YourWidgetProvider"
        const val PREF_PREFIX_KEY = "appwidget_board_id_" // Используем тот же ключ, что и в WidgetConfigurationActivity
        const val PREF_WIDGET_TITLE_PREFIX_KEY = "appwidget_title_" // Используем тот же ключ


        internal fun updateAppWidgetInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "updateAppWidgetInternal для ID: $appWidgetId")

            val prefs = context.getSharedPreferences(WidgetConfigurationActivity.PREFS_NAME, Context.MODE_PRIVATE)
            val widgetBoardId = prefs.getString(WidgetConfigurationActivity.PREF_PREFIX_KEY + appWidgetId, null)

            if (widgetBoardId == null) {
                Log.w(TAG, "widgetBoardId не найден для виджета ID $appWidgetId. Виджет не настроен.")
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                // Убедись, что R.string.widget_needs_configuration и R.id.widget_header_title существуют
                views.setTextViewText(R.id.widget_header_title, context.getString(R.string.widget_needs_configuration))
                views.setOnClickPendingIntent(R.id.widget_header_title, getConfigurePendingIntent(context, appWidgetId))
                views.setViewVisibility(R.id.widget_list_view, android.view.View.GONE)
                // Убедись, что R.id.empty_view и R.string.widget_tap_to_configure существуют
                views.setViewVisibility(R.id.empty_view, android.view.View.VISIBLE)
                views.setTextViewText(R.id.empty_view, context.getString(R.string.widget_tap_to_configure))
                views.setOnClickPendingIntent(R.id.empty_view, getConfigurePendingIntent(context, appWidgetId))
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            Log.d(TAG, "Board ID для виджета $appWidgetId: $widgetBoardId")

            val serviceIntent = Intent(context, WidgetNotesService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("WIDGET_BOARD_ID", widgetBoardId)
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
            }

            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setRemoteAdapter(R.id.widget_list_view, serviceIntent)
                // Убедись, что R.id.empty_view существует
                setEmptyView(R.id.widget_list_view, R.id.empty_view)
                // Убедись, что R.id.widget_header_title и R.string.widget_default_title существуют
                setTextViewText(R.id.widget_header_title, prefs.getString(WidgetConfigurationActivity.PREF_WIDGET_TITLE_PREFIX_KEY + appWidgetId, context.getString(R.string.widget_default_title)))
            }

            val itemClickIntent = Intent(context, YourWidgetProvider::class.java).apply {
                action = ACTION_ITEM_CLICK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("widget://$appWidgetId/itemclick")
            }
            val itemClickPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, itemClickPendingIntent)

            val refreshIntent = Intent(context, YourWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("widget://$appWidgetId/refresh")
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId + 1000,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            // Убедись, что R.id.widget_refresh_button существует
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
            // Убедись, что R.id.widget_header_title существует
            views.setOnClickPendingIntent(R.id.widget_header_title, getConfigurePendingIntent(context, appWidgetId))

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
            Log.d(TAG, "Виджет ID $appWidgetId обновлен, notifyAppWidgetViewDataChanged вызван.")
        }

        private fun getConfigurePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val configureIntent = Intent(context, WidgetConfigurationActivity::class.java).apply {
                action = ACTION_CONFIGURE_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("widget://$appWidgetId/configure")
            }
            return PendingIntent.getActivity(
                context,
                appWidgetId + 2000,
                configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate. IDs: ${appWidgetIds.joinToString()}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidgetInternal(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        Log.d(TAG, "onReceive: action = $action, extras = ${intent.extrasToStringSafely()}")

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        when (action) {
            ACTION_ITEM_CLICK -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val position = intent.getIntExtra(EXTRA_ITEM_POSITION, -1)
                    val selectedNoteId = intent.getStringExtra("SELECTED_NOTE_ID")
                    val selectedBoardId = intent.getStringExtra("SELECTED_BOARD_ID")

                    if (position != -1) {
                        Toast.makeText(context, "Нажат элемент $position. Note ID: $selectedNoteId", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Нажат элемент $position для виджета ID $appWidgetId. Note ID: $selectedNoteId, Board ID: $selectedBoardId")

                        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            this.action = "OPEN_NOTE_FROM_WIDGET"
                            putExtra("NOTE_ID_TO_OPEN", selectedNoteId)
                            putExtra("BOARD_ID_TO_OPEN", selectedBoardId)
                            val prefs = context.getSharedPreferences(WidgetConfigurationActivity.PREFS_NAME, Context.MODE_PRIVATE)
                            val boardIdForMain = prefs.getString(WidgetConfigurationActivity.PREF_PREFIX_KEY + appWidgetId, selectedBoardId)
                            putExtra("WIDGET_BOARD_ID_FOR_MAIN", boardIdForMain)
                        }
                        context.startActivity(mainActivityIntent)
                    } else {
                        Log.w(TAG, "Неверная позиция элемента в интенте клика.")
                    }
                } else {
                    Log.w(TAG, "ACTION_ITEM_CLICK получен без валидного appWidgetId.")
                }
            }
            ACTION_REFRESH_WIDGET -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    Log.d(TAG, "Получено действие ACTION_REFRESH_WIDGET для ID: $appWidgetId")
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
                    Toast.makeText(context, "Виджет обновляется...", Toast.LENGTH_SHORT).show()
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_DELETED -> {
                val deletedAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (deletedAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    Log.d(TAG, "Виджет ID $deletedAppWidgetId удален. Очистка SharedPreferences.")
                    // ИСПРАВЛЕНО: Используем deleteWidgetPreferences
                    WidgetConfigurationActivity.deleteWidgetPreferences(context, deletedAppWidgetId)
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetIdsFromIntent = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (appWidgetIdsFromIntent != null) {
                    Log.d(TAG, "onReceive (из ACTION_APPWIDGET_UPDATE): обновляем данные для IDs: ${appWidgetIdsFromIntent.joinToString()}")
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            Log.d(TAG, "onDeleted: Удаление настроек для виджета ID $appWidgetId")
            // ИСПРАВЛЕНО: Используем deleteWidgetPreferences
            WidgetConfigurationActivity.deleteWidgetPreferences(context, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        Log.d(TAG, "onEnabled: Первый экземпляр виджета создан.")
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "onDisabled: Последний экземпляр виджета удален.")
    }

    private fun Intent.extrasToStringSafely(): String {
        val extras = this.extras ?: return "null"
        val stringBuilder = StringBuilder("Bundle[")
        for (key in extras.keySet()) {
            stringBuilder.append(" $key=${extras.get(key)};")
        }
        stringBuilder.append(" ]")
        return stringBuilder.toString()
    }
}
