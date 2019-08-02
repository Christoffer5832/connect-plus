package me.andreww7985.connectplus.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.core.content.getSystemService
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import me.andreww7985.connectplus.R
import me.andreww7985.connectplus.helpers.UIHelper
import java.io.BufferedReader
import java.io.InputStreamReader


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        findPreference<Preference>("copy_logs")!!.setOnPreferenceClickListener {
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            val log = StringBuilder()
            while (true) {
                log.appendln(bufferedReader.readLine() ?: break)
            }

            context!!.getSystemService<ClipboardManager>()!!.setPrimaryClip(ClipData.newPlainText("logs", log))

            UIHelper.showToast("Copied logs to clipboard")
            false
        }
    }
}