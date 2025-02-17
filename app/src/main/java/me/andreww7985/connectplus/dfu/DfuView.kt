package me.andreww7985.connectplus.dfu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_dfu.*
import kotlinx.coroutines.launch
import me.andreww7985.connectplus.App
import me.andreww7985.connectplus.R
import me.andreww7985.connectplus.helpers.HexHelper.toHexString
import me.andreww7985.connectplus.helpers.UIHelper
import me.andreww7985.connectplus.manager.PresenterManager
import me.andreww7985.connectplus.mvp.BaseView

class DfuView : Fragment(), BaseView {

    private val presenter = PresenterManager.getPresenter(DfuPresenter::class.java) as DfuPresenter

    fun showFileBrowserError() {
        lifecycleScope.launch {
            UIHelper.showToast(this@DfuView.requireView(), getString(R.string.dfu_error_file_manager), Toast.LENGTH_LONG)
        }
    }

    fun showWrongFile() {
        lifecycleScope.launch {
            UIHelper.showToast(this@DfuView.requireView(), getString(R.string.dfu_error_wrong_file), Toast.LENGTH_LONG)
        }
    }

    fun updateUi(dfu: DfuModel, isSpeakerCharging: Boolean) {
        lifecycleScope.launch {
            dfu_battery_warning_title.text =
                if (!isSpeakerCharging)
                    getString(R.string.dfu_connect_charger_title)
                else
                    getString(R.string.dfu_connect_charger_title_connected)

            when (dfu.state) {
                DfuModel.State.FILE_NOT_SELECTED -> {
                    dfu_file_selector_card.visibility = View.VISIBLE
                    dfu_file_selector_text.text = getString(R.string.dfu_state_file_not_selected)
                    dfu_file_selector_button.isEnabled = true

                    dfu_file_info_card.visibility = View.GONE

                    dfu_flash_card.visibility = View.GONE
                }

                DfuModel.State.LOADING_FILE -> {
                    dfu_file_selector_card.visibility = View.VISIBLE
                    dfu_file_selector_text.text = getString(R.string.dfu_state_loading_file)
                    dfu_file_selector_button.isEnabled = false

                    dfu_file_info_card.visibility = View.GONE

                    dfu_flash_card.visibility = View.GONE
                }

                DfuModel.State.READY -> {
                    dfu_file_selector_text.text = dfu.filename!!
                    dfu_file_selector_button.isEnabled = true

                    dfu_file_info_card.visibility = View.VISIBLE
                    dfu_file_info_size_value.text = dfu.fileSize.toString()
                    dfu_file_info_checksum_value.text = dfu.checksum!!.toHexString()

                    dfu_flash_card.visibility = if (isSpeakerCharging) View.VISIBLE else View.GONE
                    if (isSpeakerCharging) {
                        dfu_flash_card.visibility = View.VISIBLE

                        dfu_flash_progressbar.visibility = View.GONE

                        dfu_flash_button.isEnabled = true
                        dfu_flash_button.text = getString(R.string.dfu_button_flash)

                        dfu_flash_text.text = getString(R.string.dfu_state_ready)

                        dfu_flash_button.setOnClickListener {
                            presenter.startDfu()
                        }
                    }
                }

                DfuModel.State.INITIALIZING_DFU -> {
                    dfu_file_selector_text.text = dfu.filename!!
                    dfu_file_selector_button.isEnabled = false

                    dfu_file_info_card.visibility = View.VISIBLE
                    dfu_file_info_size_value.text = dfu.fileSize.toString()
                    dfu_file_info_checksum_value.text = dfu.checksum!!.toHexString()

                    dfu_flash_card.visibility = View.VISIBLE

                    dfu_flash_progressbar.isIndeterminate = true
                    dfu_flash_progressbar.visibility = View.VISIBLE

                    dfu_flash_button.isEnabled = false
                    dfu_flash_button.text = getString(R.string.dfu_button_flash)

                    dfu_flash_text.text = getString(R.string.dfu_state_initializing_dfu)
                }

                DfuModel.State.FLASHING_DFU -> {
                    dfu_file_selector_text.text = dfu.filename!!
                    dfu_file_selector_button.isEnabled = false

                    dfu_file_info_card.visibility = View.VISIBLE
                    dfu_file_info_size_value.text = dfu.fileSize.toString()
                    dfu_file_info_checksum_value.text = dfu.checksum!!.toHexString()

                    dfu_flash_card.visibility = View.VISIBLE

                    val progress = dfu.getProgress()
                    dfu_flash_progressbar.visibility = View.VISIBLE
                    dfu_flash_progressbar.isIndeterminate = false
                    dfu_flash_progressbar.progress = (progress * 1e5f).toInt()

                    dfu_flash_button.isEnabled = true
                    dfu_flash_button.text = getString(R.string.dfu_button_cancel)

                    dfu_flash_text.text = getString(R.string.dfu_state_flashing, progress * 100f)

                    dfu_flash_button.setOnClickListener {
                        presenter.cancelDfu()
                    }
                }

                DfuModel.State.WAITING_REBOOT -> {
                    dfu_file_selector_text.text = dfu.filename!!
                    dfu_file_selector_button.isEnabled = false

                    dfu_file_info_card.visibility = View.VISIBLE
                    dfu_file_info_size_value.text = dfu.fileSize.toString()
                    dfu_file_info_checksum_value.text = dfu.checksum!!.toHexString()

                    dfu_flash_card.visibility = View.VISIBLE

                    /* Cannot switch to indeterminate mode while the progress indicator is visible. */
                    dfu_flash_progressbar.visibility = View.GONE
                    dfu_flash_progressbar.isIndeterminate = true
                    dfu_flash_progressbar.visibility = View.VISIBLE

                    dfu_flash_button.isEnabled = false
                    dfu_flash_button.text = getString(R.string.dfu_button_cancel)

                    dfu_flash_text.text = when (dfu.status) {
                        DfuModel.Status.SUCCESS -> getString(R.string.dfu_state_done)
                        DfuModel.Status.ERROR -> getString(R.string.dfu_state_error)
                        DfuModel.Status.CANCELED -> getString(R.string.dfu_state_canceled)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dfu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!App.sharedPreferences.getBoolean("dfu_warning_accepted", false)) {
            val dialogView = View.inflate(context, R.layout.dialog_dfu_warning, null)
            val checkBox =
                dialogView.findViewById<MaterialCheckBox>(R.id.dfu_warning_dont_show_checkbox)

            MaterialAlertDialogBuilder(
                requireContext(),
                R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            ).setCancelable(false)
                .setIcon(R.drawable.ic_warning)
                .setTitle(getString(R.string.dialog_dfu_warning_title))
                .setView(dialogView)
                .setMessage(getString(R.string.dialog_dfu_warning_text))
                .setPositiveButton(R.string.dialog_dfu_warning_agree) { _, _ ->
                    App.sharedPreferences.edit {
                        putBoolean("dfu_warning_accepted", checkBox.isChecked)
                    }
                }.show()
        }

        dfu_file_selector_button.setOnClickListener {
            val intent = Intent()
            intent.type = "*/*"
            intent.action = Intent.ACTION_GET_CONTENT

            startActivityForResult(intent, 1)
        }

        presenter.attachView(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data ?: return
        lifecycleScope.launch {
            presenter.loadFile(data.data!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.detachView()

        if (isRemoving) {
            PresenterManager.destroyPresenter(DfuPresenter::class.java)
        }
    }
}
