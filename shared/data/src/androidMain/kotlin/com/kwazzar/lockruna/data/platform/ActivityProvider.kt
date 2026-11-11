package com.kwazzar.lockruna.data.platform

import androidx.fragment.app.FragmentActivity

/** Gives the biometric prompt the activity that is currently in the foreground. */
interface ActivityProvider {
    fun current(): FragmentActivity?
}
