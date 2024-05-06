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

    fun saveLong(key: String, value: Long) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putLong(key, value)
        editor.commit()
    }

    fun retrieveLong(key: String) : Long {
        return preferences.getLong(key, 0)
    }

    fun saveString(key: String, value: String) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putString(key, value)
        editor.commit()
    }

    fun retrieveString(key: String) : String? {
        return preferences.getString(key, null)
    }

    fun saveBoolean(key: String, value: Boolean) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putBoolean(key, value)
        editor.commit()
    }

    fun retrieveBoolean(key: String): Boolean {
        return preferences.getBoolean(key, false)
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