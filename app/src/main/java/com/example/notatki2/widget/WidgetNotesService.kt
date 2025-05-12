package com.example.notatki2.widget

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * Service that provides the RemoteViewsFactory for the ListView in the widget.
 */
class WidgetNotesService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        // Передаем applicationContext и intent в наш RemoteViewsFactory
        return WidgetNotesRemoteViewsFactory(this.applicationContext, intent)
    }
}
