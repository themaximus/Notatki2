package com.example.notatki2

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.notatki2.data.database.BoardEntity
import com.example.notatki2.data.database.NoteEntity
import com.example.notatki2.viewmodel.NotatkiViewModel
import com.example.notatki2.widget.YourWidgetProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext // <<<--- УБЕДИТЕСЬ, ЧТО ЭТОТ ИМПОРТ ЕСТЬ

import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var notatkiViewModel: NotatkiViewModel
    private lateinit var webAppInterface: WebAppInterface

    private var widgetBoardIdForNative: String? = null
    private var appWidgetIdForMain: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notatkiViewModel = ViewModelProvider(this)[NotatkiViewModel::class.java]
        webView = findViewById(R.id.webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        WebView.setWebContentsDebuggingEnabled(true)


        webAppInterface = WebAppInterface(this, notatkiViewModel)
        webView.addJavascriptInterface(webAppInterface, "AndroidNative")

        handleIntent(intent)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("MainActivity", "Страница загружена: $url")
                Log.d("MainActivity", "ID доски виджета для JS (из onPageFinished): $widgetBoardIdForNative")
                widgetBoardIdForNative?.let { boardId ->
                    webView.evaluateJavascript("javascript:if(window.controller && typeof window.controller.handleBoardIdChange === 'function'){ controller.handleBoardIdChange('$boardId'); } else { console.error('JS controller.handleBoardIdChange not found'); }", null)
                }
            }
        }
        webView.loadUrl("file:///android_asset/notatki.html")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent вызван")
        handleIntent(intent)
        if (::webView.isInitialized && webView.url != null) {
            Log.d("MainActivity", "onNewIntent: страница уже загружена, пытаемся обновить JS с widgetBoardId: $widgetBoardIdForNative")
            widgetBoardIdForNative?.let { boardId ->
                webView.evaluateJavascript("javascript:if(window.controller && typeof window.controller.handleBoardIdChange === 'function'){ controller.handleBoardIdChange('$boardId'); } else { console.error('JS controller.handleBoardIdChange not found'); }", null)
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        Log.d("MainActivity", "Обработка интента: action=${intent.action}, extras=${intent.extrasToStringSafely()}")
        val newWidgetBoardId = intent.getStringExtra("WIDGET_BOARD_ID_FOR_MAIN")
        val newAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (intent.action == "OPEN_NOTE_FROM_WIDGET" && newWidgetBoardId != null) {
            widgetBoardIdForNative = newWidgetBoardId
            if (newAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                appWidgetIdForMain = newAppWidgetId
            }
            Log.d("MainActivity", "Intent от виджета (OPEN_NOTE_FROM_WIDGET): widgetBoardIdForNative установлен в '$widgetBoardIdForNative', appWidgetIdForMain в '$appWidgetIdForMain'")
        } else if (intent.hasExtra("WIDGET_BOARD_ID_FOR_MAIN")) {
            widgetBoardIdForNative = newWidgetBoardId
            if (newAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                appWidgetIdForMain = newAppWidgetId
            }
            Log.d("MainActivity", "Intent с WIDGET_BOARD_ID_FOR_MAIN: widgetBoardIdForNative установлен в '$widgetBoardIdForNative', appWidgetIdForMain в '$appWidgetIdForMain'")
        }
        else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE || newAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (newAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                appWidgetIdForMain = newAppWidgetId
            }
            Log.d("MainActivity", "Intent для конфигурации или связанный с виджетом: appWidgetIdForMain установлен в '$appWidgetIdForMain'")
        } else {
            Log.d("MainActivity", "Обычный запуск приложения или интент без ID доски виджета.")
        }
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


    inner class WebAppInterface(
        private val context: Context,
        private val viewModel: NotatkiViewModel
    ) {

        @JavascriptInterface
        fun getWidgetBoardId(): String? {
            Log.d("WebAppInterface", "JS вызвал getWidgetBoardId. Текущий widgetBoardIdForNative: ${this@MainActivity.widgetBoardIdForNative}, appWidgetIdForMain: ${this@MainActivity.appWidgetIdForMain}")
            if (this@MainActivity.widgetBoardIdForNative != null) {
                Log.d("WebAppInterface", "Возвращаем ID доски (из MainActivity.widgetBoardIdForNative): ${this@MainActivity.widgetBoardIdForNative}")
                return this@MainActivity.widgetBoardIdForNative
            }
            val currentAppWidgetId = this@MainActivity.appWidgetIdForMain
            if (currentAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val prefs = context.getSharedPreferences(YourWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
                val boardIdFromPrefs = prefs.getString(YourWidgetProvider.PREF_PREFIX_KEY + currentAppWidgetId, null)
                if (boardIdFromPrefs != null) {
                    Log.d("WebAppInterface", "Возвращаем ID доски (из SharedPreferences для appWidgetId $currentAppWidgetId): $boardIdFromPrefs")
                    this@MainActivity.widgetBoardIdForNative = boardIdFromPrefs
                    return boardIdFromPrefs
                } else {
                    Log.d("WebAppInterface", "ID доски не найден в SharedPreferences для appWidgetId $currentAppWidgetId")
                }
            } else {
                Log.d("WebAppInterface", "appWidgetIdForMain не установлен, не могу проверить SharedPreferences.")
            }
            Log.w("WebAppInterface", "ID доски виджета не определен (ни через Intent, ни из prefs), возвращаем null.")
            return null
        }

        @JavascriptInterface
        fun saveBoard(id: String?, name: String): String {
            val boardIdToSave = id ?: UUID.randomUUID().toString()
            runBlocking(Dispatchers.IO) {
                val board = BoardEntity(
                    id = boardIdToSave,
                    name = name,
                    createdAt = System.currentTimeMillis(),
                    order = 0
                )
                if (id == null) {
                    viewModel.insertBoard(board)
                    Log.d("WebAppInterface", "Доска сохранена (новая): $name, ID: $boardIdToSave")
                } else {
                    viewModel.updateBoard(board)
                    Log.d("WebAppInterface", "Доска обновлена: $name, ID: $boardIdToSave")
                }
            }
            return boardIdToSave
        }

        @JavascriptInterface
        fun deleteBoard(boardId: String) {
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.getBoardById(boardId)?.let { board ->
                    viewModel.deleteBoard(board)
                    Log.d("WebAppInterface", "Доска удалена: ID $boardId")
                }
            }
        }

        @JavascriptInterface
        fun getAllBoards(): String {
            return runBlocking(Dispatchers.IO) {
                try {
                    // Если getBoardsListFromRepository возвращает Flow, используйте .first()
                    // val boards = viewModel.getBoardsListFromRepository().first()
                    // Если возвращает List напрямую:
                    val boards = viewModel.getBoardsListFromRepository()
                    val json = Gson().toJson(boards)
                    Log.d("WebAppInterface", "getAllBoards: Возвращено досок (JSON): ${json.take(200)}")
                    json
                } catch (e: Exception) {
                    Log.e("WebAppInterface", "Ошибка в getAllBoards: ${e.message}", e)
                    "[]"
                }
            }
        }

        @JavascriptInterface
        fun getNotesForBoard(boardId: String): String {
            return runBlocking(Dispatchers.IO) {
                try {
                    // Если getNotesListForBoard возвращает Flow, используйте .first()
                    // val notes = viewModel.getNotesListForBoard(boardId).first()
                    // Если возвращает List напрямую:
                    val notes = viewModel.getNotesListForBoard(boardId)
                    val json = Gson().toJson(notes)
                    Log.d("WebAppInterface", "getNotesForBoard для ID $boardId: Возвращено заметок (JSON): ${json.take(200)}")
                    json
                } catch (e: Exception) {
                    Log.e("WebAppInterface", "Ошибка в getNotesForBoard для ID $boardId: ${e.message}", e)
                    "[]"
                }
            }
        }

        @JavascriptInterface
        fun saveNote(
            boardId: String,
            noteId: String?,
            content: String,
            isDone: Boolean,
            priority: String?,
            orderInList: Int?
        ) {
            val actualNoteId = noteId ?: "task-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
            lifecycleScope.launch(Dispatchers.IO) {
                val note = NoteEntity(
                    id = actualNoteId,
                    boardId = boardId,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    isDone = isDone,
                    priority = priority ?: "medium",
                    orderInList = orderInList ?: 0
                )
                viewModel.insertNote(note)
                Log.d("WebAppInterface", "Заметка сохранена/обновлена: ID $actualNoteId для доски $boardId")
                // Переключаемся на Main для вызова notifyWidgetIfNecessary, если он делает UI операции
                withContext(Dispatchers.Main) {
                    notifyWidgetIfNecessary(boardId)
                }
            }
        }

        @JavascriptInterface
        fun deleteNote(noteId: String, boardId: String) {
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.getNoteById(noteId)?.let { note ->
                    viewModel.deleteNote(note)
                    Log.d("WebAppInterface", "Заметка удалена: ID $noteId")
                    // Переключаемся на Main для вызова notifyWidgetIfNecessary
                    withContext(Dispatchers.Main) {
                        notifyWidgetIfNecessary(boardId)
                    }
                }
            }
        }

        @JavascriptInterface
        fun requestWidgetUpdate(boardIdToUpdate: String?) {
            boardIdToUpdate?.let {
                Log.d("WebAppInterface", "JS запросил обновление виджета для доски: $it")
                // notifyWidgetIfNecessary уже должен быть потокобезопасен или вызываться из Main
                notifyWidgetIfNecessary(it)
            }
        }

        private fun notifyWidgetIfNecessary(changedBoardId: String) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, YourWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            val prefs = context.getSharedPreferences(YourWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)

            Log.d("WebAppInterface", "notifyWidgetIfNecessary: Проверка виджетов для доски $changedBoardId. Всего виджетов: ${appWidgetIds.size}")

            var widgetUpdated = false
            for (appWidgetId in appWidgetIds) {
                val widgetConfiguredBoardId = prefs.getString(YourWidgetProvider.PREF_PREFIX_KEY + appWidgetId, null)
                Log.d("WebAppInterface", "Проверка виджета ID $appWidgetId: связан с доской $widgetConfiguredBoardId")
                if (changedBoardId == widgetConfiguredBoardId) {
                    Log.d("WebAppInterface", "Доска $changedBoardId связана с виджетом $appWidgetId. Обновление данных виджета.")
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
                    widgetUpdated = true
                }
            }
            if (!widgetUpdated) {
                Log.d("WebAppInterface", "notifyWidgetIfNecessary: Не найдено активных виджетов, связанных с доской $changedBoardId, для обновления.")
            }
        }
    }
}
