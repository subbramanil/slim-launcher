package com.sduduzog.slimlauncher.ui.main.setup


import android.app.AlertDialog
import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.ui.main.model.App
import com.sduduzog.slimlauncher.ui.main.model.HomeApp
import com.sduduzog.slimlauncher.ui.main.model.MainViewModel
import kotlinx.android.synthetic.main.setup_fragment.*
import java.util.*


class SetupFragment : Fragment(), DialogInteractionListener {

    private lateinit var viewModel: MainViewModel
    private var state = 0
    private val apps = mutableListOf<App>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.setup_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        setLoading()
        viewModel.homeApps.observe(this, ValueObserver())
        setupButton.setOnClickListener {
            when (state) {
                1 -> chooseApps()
                2 -> chooseLayout()
                3 -> chooseClock()
                else -> {
                    val settings = activity!!.getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
                    settings.edit {
                        putBoolean(getString(R.string.prefs_settings_key_fresh_install_setup), false)
                    }
                    finishSetup()
                }
            }
        }

    }

    private fun checkFreshInstall() {
        val settings = activity!!.getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
        if (settings.getBoolean(getString(R.string.prefs_settings_key_fresh_install_setup), true)) {
            val pm = activity!!.packageManager
            val main = Intent(Intent.ACTION_MAIN, null)

            main.addCategory(Intent.CATEGORY_LAUNCHER)

            val launchables = pm.queryIntentActivities(main, 0)
            Collections.sort(launchables,
                    ResolveInfo.DisplayNameComparator(pm))
            for (i in launchables.indices) {
                val item = launchables[i]
                val activity = item.activityInfo
                val app = App(launchables[i].loadLabel(pm).toString(), activity.applicationInfo.packageName, activity.name)
                apps.add(app)
            }
            revealUI(state)
        } else {
            finishSetup()
        }
    }

    private fun finishSetup() {
        val nav = Navigation.findNavController(setupContainer)
        nav.navigate(R.id.action_setupFragment_to_mainFragment2)
    }

    private fun setLoading() {
        progressBar.visibility = View.VISIBLE
        opt1Number.alpha = 0f
        opt1Text.alpha = 0f
        opt2Number.alpha = 0f
        opt2Text.alpha = 0f
        opt3Number.alpha = 0f
        opt3Text.alpha = 0f
        textViewLets.alpha = 0f
        setupButton.alpha = 0f
        tvWelcome.alpha = 0f
        cvIcon.alpha = 0f
    }

    private fun setupAnimation(viewElement: View,
                               startDelay: Long = 0,
                               duration: Long = 1000) {
        viewElement.animate()
                .setStartDelay(startDelay)
                .alpha(1f)
                .duration = duration
    }

    private fun setButtonText(stringSrc: Int) {
        setupButton.text = getString(stringSrc)
    }

    private fun setTickVisible(tickView: ImageView) {
        tickView.visibility = View.VISIBLE
    }

    private fun revealUI(level: Int) {
        progressBar.visibility = View.INVISIBLE
        setButtonText(R.string.setup_button_next)
        when (level) {
            0 -> doStepZero()
            1 -> setTickVisible(ivTick1)
            2 -> setTickVisible(ivTick2)
            3 -> {
                setButtonText(R.string.setup_button_finish)
                setTickVisible(ivTick3)
            }
        }
        state++
    }

    private fun doStepZero() {
        setupAnimation(tvWelcome)
        setupAnimation(cvIcon, 1500)
        setupAnimation(textViewLets)

        setupAnimation(opt1Text, 1000, 500)
        setupAnimation(opt1Number, 1000, 500)

        setupAnimation(opt2Text, 1500, 500)
        setupAnimation(opt2Number, 1500, 500)

        setupAnimation(opt3Text, 2000, 500)
        setupAnimation(opt3Number, 2000, 500)
        setupAnimation(setupButton, 2000, 500)
    }

    private fun chooseApps() {
        ChooseApps.newInstance(this, apps).show(fragmentManager, "APP_CHOOSER")
    }

    private fun chooseClock() {
        ChooseClockType.newInstance(this).show(fragmentManager, "CLOCK_CHOOSER")
    }

    private fun chooseLayout() {
        ChooseLayoutType.newInstance(this).show(fragmentManager, "LAYOUT_CHOOSER")
    }

    class ChooseApps : DialogFragment() {

        private lateinit var listener: DialogInteractionListener
        private lateinit var apps: List<App>
        private lateinit var checkedItems: BooleanArray

        companion object {
            fun newInstance(listener: DialogInteractionListener, apps: List<App>): ChooseApps {
                val chooser = ChooseApps()
                chooser.apps = apps
                chooser.listener = listener
                return chooser
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val appNames = arrayOfNulls<String>(apps.size)
            checkedItems = BooleanArray(apps.size)
            for (i in apps.indices) {
                appNames[i] = apps[i].appName
            }

            val builder = AlertDialog.Builder(context!!)
            builder.setMultiChoiceItems(appNames, checkedItems) { _, i, b ->
                checkedItems[i] = b
                if (checkedItems.filter { it }.size == 5) {
                    setApps()
                    dismiss()
                }

            }
            builder.setPositiveButton("Done") { _, _ ->
                if (checkedItems.none { it }) {
                    Toast.makeText(context, getString(R.string.no_app_selected_toast_msg), Toast.LENGTH_SHORT).show()
                } else {
                    setApps()
                }
            }
            builder.setTitle(getString(R.string.choose_apps_title))

            return builder.create()
        }

        private fun setApps() {
            val list = mutableListOf<App>()
            for (i in checkedItems.indices) {
                if (checkedItems[i]) {
                    list.add(apps[i])
                }
            }
            listener.onAppsChosen(list)
        }
    }

    class ChooseClockType : DialogFragment() {

        private lateinit var listener: DialogInteractionListener

        companion object {
            fun newInstance(listener: DialogInteractionListener): ChooseClockType {
                val chooser = ChooseClockType()
                chooser.listener = listener
                return chooser
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle("Choose clock type")
            builder.setSingleChoiceItems(R.array.clock_types, 0) { _, i ->
                val settings = context!!.getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
                if (i == 0)
                    settings.edit {
                        putBoolean(getString(R.string.prefs_settings_key_clock_type), false)
                    }
                else
                    settings.edit {
                        putBoolean(getString(R.string.prefs_settings_key_clock_type), true)
                    }
                listener.onClockChosen()
                dismiss()
            }
            return builder.create()
        }
    }

    class ChooseLayoutType : DialogFragment() {

        private lateinit var listener: DialogInteractionListener

        companion object {
            fun newInstance(listener: DialogInteractionListener): ChooseLayoutType {
                val chooser = ChooseLayoutType()
                chooser.listener = listener
                return chooser
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle("Choose Layout")
            builder.setSingleChoiceItems(R.array.layout_types, 0) { _, i ->
                val settings = context!!.getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
                if (i == 0)
                    settings.edit {
                        putString(getString(R.string.prefs_settings_key_layout_type), "List")
                    }
                else
                    settings.edit {
                        putString(getString(R.string.prefs_settings_key_layout_type), "Grid")
                    }
                listener.onLayoutChosen()
                dismiss()
            }
            return builder.create()
        }
    }

    inner class ValueObserver : Observer<List<HomeApp>> {
        override fun onChanged(it: List<HomeApp>?) {
            if (it != null && it.isEmpty()) {
                checkFreshInstall()
                viewModel.apps.removeObservers(this@SetupFragment)
            } else {
                if (state == 0)
                    finishSetup()
            }
        }
    }

    override fun onAppsChosen(apps: List<App>) {
        for (i in apps) {
            viewModel.addToHomeScreen(i)
        }
        revealUI(state)
    }

    override fun onClockChosen() {
        revealUI(state)
    }

    override fun onLayoutChosen() {
        revealUI(state)
    }
}
