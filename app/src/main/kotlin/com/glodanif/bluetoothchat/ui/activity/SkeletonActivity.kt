package com.glodanif.bluetoothchat.ui.activity

import android.arch.lifecycle.Lifecycle
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.glodanif.bluetoothchat.R

open class
SkeletonActivity : AppCompatActivity() {

    protected enum class ActivityType { CHILD_ACTIVITY, CUSTOM_TOOLBAR_ACTIVITY }

    protected var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    protected fun setContentView(@LayoutRes layoutId: Int, type: ActivityType) {

        when (type) {
            ActivityType.CUSTOM_TOOLBAR_ACTIVITY -> super.setContentView(layoutId)
            ActivityType.CHILD_ACTIVITY -> super.setContentView(R.layout.activity_skeleton)
        }

        toolbar = findViewById(R.id.tb_toolbar)
        setSupportActionBar(toolbar)

        val rootView = findViewById<ViewGroup>(R.id.fl_content_container)

        if (type == ActivityType.CHILD_ACTIVITY) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            LayoutInflater.from(this).inflate(layoutId, rootView, true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    inline fun doIfStarted(action: () -> Unit) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            action.invoke()
        }
    }

    fun hideKeyboard() {
        val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            inputManager.hideSoftInputFromWindow(it.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }
}
