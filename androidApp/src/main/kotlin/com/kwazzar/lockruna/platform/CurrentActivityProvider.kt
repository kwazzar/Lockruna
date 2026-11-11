package com.kwazzar.lockruna.platform

import androidx.fragment.app.FragmentActivity
import com.kwazzar.lockruna.data.platform.ActivityProvider

class CurrentActivityProvider : ActivityProvider {
    @Volatile
    private var current: FragmentActivity? = null

    fun set(activity: FragmentActivity?) {
        current = activity
    }

    override fun current(): FragmentActivity? = current
}
