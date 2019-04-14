package com.sduduzog.slimlauncher.ui.main

import android.animation.ObjectAnimator
import android.content.*
import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
import com.sduduzog.slimlauncher.MainActivity
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.ui.main.model.HomeApp
import com.sduduzog.slimlauncher.ui.main.model.MainViewModel
import kotlinx.android.synthetic.main.main_bottom_sheet.*
import kotlinx.android.synthetic.main.main_content.*
import java.text.SimpleDateFormat
import java.util.*


class MainFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var receiver: BroadcastReceiver
    private lateinit var adapter: MainAppsAdapter
    private lateinit var sheetBehavior: BottomSheetBehavior<FrameLayout>

    @Suppress("PropertyName")
    val TAG: String = "MainFragment"

    private val LIST_LAYOUT_TYPE = 1
    private val GRID_LAYOUT_TYPE = 2

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sheetBehavior = BottomSheetBehavior.from(bottomSheet)
        optionsView.alpha = 0.0f
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        val layoutPref = context?.getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
                ?.getString(getString(R.string.prefs_settings_key_layout_type), "List")
        var layoutType = LIST_LAYOUT_TYPE
        if (layoutPref == "List") {
            mainAppsList.layoutManager = LinearLayoutManager(context)
        } else {
            layoutType = GRID_LAYOUT_TYPE
            mainAppsList.layoutManager = GridLayoutManager(context, 2)
        }

        adapter = MainAppsAdapter(mutableSetOf(), context, layoutType, InteractionHandler())
        mainAppsList.adapter = adapter
        viewModel.homeApps.observe(this, Observer {
            if (it != null) {
                adapter.setApps(it)
            }
        })
        setEventListeners()
    }

    override fun onStart() {
        super.onStart()
        receiver = ClockReceiver()
        activity?.registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))
        doBounceAnimation(ivExpand)
        sheetBehavior.state = STATE_COLLAPSED
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(receiver)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        with(context as MainActivity) {
            this.onBackPressedListener = object : MainActivity.OnBackPressedListener {
                override fun onBackPressed() {
                    sheetBehavior.state = STATE_COLLAPSED
                }
            }
        }
    }

    inner class ClockReceiver : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            updateUi()
            doBounceAnimation(ivExpand)
        }
    }

    fun updateUi() {
        val twenty4Hour = context?.getSharedPreferences(getString(R.string.prefs_settings), MODE_PRIVATE)
                ?.getBoolean(getString(R.string.prefs_settings_key_clock_type), false)
        val date = Date()
        if (twenty4Hour as Boolean) {
            val fWatchTime = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            clockTextView.text = fWatchTime.format(date)
            clockAmPm.visibility = View.GONE
        } else {
            val fWatchTime = SimpleDateFormat("hh:mm", Locale.ENGLISH)
            val fWatchTimeAP = SimpleDateFormat("aa", Locale.ENGLISH)
            clockTextView.text = fWatchTime.format(date)
            clockAmPm.text = fWatchTimeAP.format(date)
            clockAmPm.visibility = View.VISIBLE
        }
        val fWatchDate = SimpleDateFormat("EEE, MMM dd", Locale.ENGLISH)
        dateTextView.text = fWatchDate.format(date)
    }

    inner class InteractionHandler : OnListFragmentInteractionListener {
        override fun onLaunch(item: HomeApp) {
            val name = ComponentName(item.packageName, item.activityName)
            val intent = Intent()
            intent.action = Intent.ACTION_MAIN
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            intent.component = name
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, e.message)
                Toast.makeText(activity, "${item.appName} seems to be uninstalled, removing from list", Toast.LENGTH_LONG).show()
                viewModel.deleteApp(item)
            }
        }
    }

    interface OnListFragmentInteractionListener {
        fun onLaunch(item: HomeApp)
    }

    private fun doBounceAnimation(targetView: View) {
        val animator = ObjectAnimator.ofFloat(targetView, "translationY", 0f, -20f, 0f)
        animator.interpolator = EasingInterpolator(Ease.QUINT_OUT)
        animator.startDelay = 1000
        animator.duration = 1000
        animator.repeatCount = 1
        animator.start()
    }

    private fun rateApp() {
        val uri = Uri.parse("market://details?id=" + context?.packageName)
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        try {
            startActivity(goToMarket)
            Log.d(TAG, goToMarket.data?.query)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + context?.packageName)))
        }
    }

    private fun openSettings() {
        startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
    }

    private fun setEventListeners() {
        setClockEventListener()

        bottomSheet.setOnClickListener {
            // dummy listener to listen to the touch and override the default behavior
        }

        setBottomSheetCallback()

        setHomeOptionsListeners()

        setCallClickListener()

        setCameraClickListener()

        showHomeOptions()
    }

    private fun setBottomSheetCallback() {
        sheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(p0: View, p1: Float) {
                val multi = 3 * p1
                optionsView.alpha = multi
                optionsView.cardElevation = p1 * 8
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    optionsView.elevation = p1 * 8
                } else {
                    // Not available Pre-Lollipop
                }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                iconTray.visibility = View.GONE
                if (newState == STATE_COLLAPSED) {
                    iconTray.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setHomeOptionsListeners() {
        settingsText.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_openSettingsFragment))
        deviceSettingsText.setOnClickListener { openSettings() }
        rateAppText.setOnClickListener { rateApp() }
        aboutText.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_openAboutFragment))
        showLauncherChange()
    }

    private fun showHomeOptions() {
        ivExpand.setOnClickListener {
            if (sheetBehavior.state == STATE_COLLAPSED) {
                sheetBehavior.state = STATE_HALF_EXPANDED
            }
        }
    }

    private fun showLauncherChange() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            changeLauncherText.setOnClickListener {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        } else {
            changeLauncherText.visibility = View.GONE
        }
    }

    private fun setCallClickListener() {
        ivCall.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_DIAL)
                val title = resources.getString(R.string.chooser_title)
                val chooser = Intent.createChooser(intent, title)
                if (intent.resolveActivity(activity!!.packageManager) != null) {
                    startActivity(chooser)
                }
            } catch (e: Exception) {
                Log.e(TAG, e.message)
            }
        }
    }

    private fun setCameraClickListener() {
        ivCamera.setOnClickListener {
            try {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, e.message)
            }
        }
    }

    private fun setClockEventListener() {
        clockTextView.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } else {
                // Not required if the version is below kitkat
            }
        }
    }
}
