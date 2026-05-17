package com.pandora.carlauncher

import androidx.multidex.MultiDexApplication

class PandaApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.instance.init(this)
    }
}
