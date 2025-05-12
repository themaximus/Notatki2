package com.example.notatki2.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.notatki2.R
import com.example.notatki2.data.database.AppDatabase
import com.example.notatki2.data.database.NoteEntity
import com.example.notatki2.data.repository.NotatkiRepository
import com.example.notatki2.widget.WidgetConfigurationActivity // <<< УБЕДИСЬ, ЧТО ЭТОТ ИМПОРТ ЕСТЬ И ПРАВИЛЬНЫЙ
import kotlinx.coroutines.runBlocking

class WidgetNotesRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var widgetBoardId: String? = null
    private var notesList: List<NoteEntity> = emptyList()
    private lateinit var repository: NotatkiRepository

    override fun onCreate() {
        Log.d("WidgetFactory", "onCreate for widget ID: $appWidgetId")
        val noteDao = AppDatabase.getDatabase(context).noteDao()
        val boardDao = AppDatabase.getDatabase(context).boardDao()
        repository = NotatkiRepository(noteDao, boardDao)
    }

    override fun onDataSetChanged() {
        Log.d("WidgetFactory", "onDataSetChanged for widget ID: $appWidgetId. Загрузка данных...")

        // Используем константы из WidgetConfigurationActivity
        val prefs = context.getSharedPreferences(
            WidgetConfigurationActivity.PREFS_NAME, // <<< Проверь, что WidgetConfigurationActivity.PREFS_NAME доступна
            Context.MODE_PRIVATE
        )
        val boardIdFromPrefs = prefs.getString(
            "${WidgetConfigurationActivity.PREF_PREFIX_KEY}$appWidgetId", // <<< Проверь, что WidgetConfigurationActivity.PREF_PREFIX_KEY доступна
            null
        )

        val boardIdFromIntent = intent.getStringExtra("WIDGET_BOARD_ID")
        widgetBoardId = boardIdFromPrefs ?: boardIdFromIntent

        if (widgetBoardId == null) {
            Log.e("WidgetFactory", "widgetBoardId is null for widget ID: $appWidgetId. Невозможно загрузить заметки.")
            notesList = emptyList()
            return
        }

        Log.d("WidgetFactory", "Используемый widgetBoardId: $widgetBoardId для widget ID: $appWidgetId")

        runBlocking {
            try {
                val board = repository.getBoardById(widgetBoardId!!)
                if (board != null) {
                    Log.d("WidgetFactory", "Найдена доска виджета: ${board.name} (ID: ${board.id})")
                    notesList = repository.getNotesForBoardList(widgetBoardId!!)
                    Log.d("WidgetFactory", "Загружено заметок для виджета: ${notesList.size}")
                } else {
                    Log.w("WidgetFactory", "Доска с ID $widgetBoardId не найдена.")
                    notesList = emptyList()
                }
            } catch (e: Exception) {
                Log.e("WidgetFactory", "Ошибка при загрузке данных: ${e.message}", e)
                notesList = emptyList()
            }
        }
    }

    override fun onDestroy() {
        notesList = emptyList()
    }

    override fun getCount(): Int {
        return notesList.size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= notesList.size) {
            Log.w("WidgetFactory", "Invalid position: $position, notesList size: ${notesList.size}")
            return getLoadingView()
        }

        val note = notesList[position]
        Log.d("WidgetFactory", "getViewAt position $position, note content: ${note.content}")

        val views = RemoteViews(context.packageName, R.layout.widget_list_item)
        views.setTextViewText(R.id.widget_note_content, note.content) // Предполагается, что content не null

        val fillInIntent = Intent()
        fillInIntent.putExtra(YourWidgetProvider.EXTRA_ITEM_POSITION, position)
        fillInIntent.putExtra("SELECTED_NOTE_ID", note.id)
        fillInIntent.putExtra("SELECTED_BOARD_ID", note.boardId)
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        Log.d("WidgetFactory", "RemoteViews created for position $position with note ID ${note.id}")
        return views
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_loading_item)
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemId(position: Int): Long {
        return notesList.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
