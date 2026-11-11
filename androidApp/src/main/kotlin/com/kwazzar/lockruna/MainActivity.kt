package com.kwazzar.lockruna

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.kwazzar.lockruna.ui.AppRoot
import com.kwazzar.lockruna.ui.Theme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        (application as LockrunaApplication).activityProvider.set(this)
        setContent {
            Theme { AppRoot() }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as LockrunaApplication).activityProvider.set(this)
    }

    override fun onPause() {
        super.onPause()
        (application as LockrunaApplication).activityProvider.set(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as LockrunaApplication).activityProvider.set(null)
    }
}
