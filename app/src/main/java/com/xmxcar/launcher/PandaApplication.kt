package com.xmxcar.launcher

import androidx.multidex.MultiDexApplication

class PandaApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.instance.init(this)
    }
}
