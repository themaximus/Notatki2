package com.example.notatki2.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context // Добавлен импорт для Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notatki2.R // Предполагается, что у тебя есть макет activity_widget_configuration
import com.example.notatki2.data.database.AppDatabase
import com.example.notatki2.data.database.BoardEntity
import com.example.notatki2.data.repository.NotatkiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigurationActivity : AppCompatActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var boardSpinner: Spinner // Добавлено для выбора доски
    private lateinit var saveButton: Button     // Добавлено для сохранения
    private lateinit var repository: NotatkiRepository
    private var availableBoards: List<BoardEntity> = emptyList()


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Убедись, что у тебя есть макет R.layout.activity_widget_configuration
        // Если его нет, Activity будет пустой или вызовет ошибку.
        // Для простоты, если макета нет, можно пока закомментировать setContentView
        // и логику UI, оставив только сохранение дефолтного значения.
        // Но для полноценной конфигурации макет нужен.
        setContentView(R.layout.activity_widget_configuration)
        Log.d("WidgetConfig", "onCreate конфигурационной Activity")

        setResult(Activity.RESULT_CANCELED) // По умолчанию результат отменен

        val noteDao = AppDatabase.getDatabase(applicationContext).noteDao()
        val boardDao = AppDatabase.getDatabase(applicationContext).boardDao()
        repository = NotatkiRepository(noteDao, boardDao)

        // Инициализация UI элементов (если они есть в макете)
        boardSpinner = findViewById(R.id.board_spinner) // Убедись, что ID правильный
        saveButton = findViewById(R.id.save_config_button) // Убедись, что ID правильный

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e("WidgetConfig", "Неверный appWidgetId, завершение Activity")
            finish()
            return
        }

        loadBoards() // Загружаем список досок для выбора

        saveButton.setOnClickListener {
            handleSaveConfiguration()
        }
    }

    private fun loadBoards() {
        CoroutineScope(Dispatchers.IO).launch {
            availableBoards = repository.getAllBoards() // Используем метод, возвращающий List<BoardEntity>
            withContext(Dispatchers.Main) {
                if (availableBoards.isNotEmpty()) {
                    val boardNames = availableBoards.map { it.name }
                    val adapter = ArrayAdapter(
                        this@WidgetConfigurationActivity,
                        android.R.layout.simple_spinner_item,
                        boardNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    boardSpinner.adapter = adapter
                } else {
                    // Если досок нет, можно сообщить пользователю или создать доску по умолчанию
                    Toast.makeText(this@WidgetConfigurationActivity, "Нет доступных досок. Создайте доску в приложении.", Toast.LENGTH_LONG).show()
                    saveButton.isEnabled = false // Например, блокируем кнопку сохранения
                }
            }
        }
    }

    private fun handleSaveConfiguration() {
        val context: Context = this@WidgetConfigurationActivity

        if (availableBoards.isEmpty() || boardSpinner.selectedItemPosition < 0) {
            Toast.makeText(context, "Пожалуйста, выберите доску.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedBoard = availableBoards[boardSpinner.selectedItemPosition]
        val selectedBoardId = selectedBoard.id
        val selectedBoardName = selectedBoard.name

        Log.d("WidgetConfig", "Сохранение конфигурации для widget ID $appWidgetId. Выбрана доска: ID $selectedBoardId, Имя: $selectedBoardName")

        // Сохраняем выбранный ID доски и ее имя (как заголовок виджета)
        saveWidgetPreferences(context, appWidgetId, selectedBoardId, selectedBoardName)

        // Обновляем виджет
        val appWidgetManager = AppWidgetManager.getInstance(context)
        YourWidgetProvider.updateAppWidgetInternal(context, appWidgetManager, appWidgetId)

        // Устанавливаем результат и завершаем Activity
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        Log.d("WidgetConfig", "Конфигурация сохранена, результат OK, завершаем активность.")
        finish()
    }

    // !!!!! COMPANION OBJECT С КОНСТАНТАМИ И МЕТОДАМИ ДЛЯ PREFERENCES !!!!!
    companion object {
        // Ключи для SharedPreferences
        const val PREFS_NAME = "com.example.notatki2.widget.YourWidgetProvider" // Уникальное имя для SharedPreferences
        const val PREF_PREFIX_KEY = "appwidget_board_id_" // Префикс для ключа ID доски
        const val PREF_WIDGET_TITLE_PREFIX_KEY = "appwidget_title_" // Префикс для ключа заголовка виджета

        // Сохраняет ID выбранной доски и ее имя (как заголовок виджета)
        internal fun saveWidgetPreferences(context: Context, appWidgetId: Int, boardId: String, boardName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            prefs.putString(PREF_PREFIX_KEY + appWidgetId, boardId)
            prefs.putString(PREF_WIDGET_TITLE_PREFIX_KEY + appWidgetId, boardName) // Сохраняем имя доски как заголовок
            prefs.apply()
            Log.d("WidgetConfig", "Сохранены: boardId '$boardId' и title '$boardName' для widgetId $appWidgetId")
        }

        // Загружает ID доски
        internal fun loadBoardIdPref(context: Context, appWidgetId: Int): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
        }

        // Загружает сохраненное имя доски (заголовок виджета)
        internal fun loadWidgetTitlePref(context: Context, appWidgetId: Int): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(PREF_WIDGET_TITLE_PREFIX_KEY + appWidgetId, null)
        }

        // Удаляет настройки для конкретного виджета
        internal fun deleteWidgetPreferences(context: Context, appWidgetId: Int) {
            Log.d("WidgetConfig", "Удаление настроек для widget ID: $appWidgetId")
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            prefs.remove(PREF_PREFIX_KEY + appWidgetId)
            prefs.remove(PREF_WIDGET_TITLE_PREFIX_KEY + appWidgetId)
            prefs.apply()
            Log.d("WidgetConfig", "Настройки для widgetId $appWidgetId удалены.")
        }
    }
}
