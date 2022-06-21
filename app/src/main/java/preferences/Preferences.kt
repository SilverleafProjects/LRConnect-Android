package preferences

import android.content.Context
import android.content.SharedPreferences

class Preferences(context: Context) {
    private val PREFERENCES = "WINNIE_PREFERENCES"
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun saveInt(key: String, value: Int) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putInt(key, value)
        editor.commit()
    }

    fun retrieveInt(key: String) : Int {
        return preferences.getInt(key, 0)
    }

    fun saveString(key: String, value: String) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putString(key, value)
        editor.commit()
    }

    fun retrieveString(key: String) : String? {
        return preferences.getString(key, null)
    }

    fun deleteValue(key: String) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.remove(key)
        editor.commit()
    }

    fun clearPreferences() {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.clear()
        editor.commit()
    }
}