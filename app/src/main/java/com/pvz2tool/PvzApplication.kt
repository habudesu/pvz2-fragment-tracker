package com.pvz2tool

import android.app.Application

/** 应用入口：持有依赖容器。 */
class PvzApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
