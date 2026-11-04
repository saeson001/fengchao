package com.saeson.fengchao

import android.app.Application
import com.saeson.fengchao.data.Api
import com.saeson.fengchao.data.Config

class App : Application() {
    companion object {
        @Volatile
        var unlocked = false
    }

    override fun onCreate() {
        super.onCreate()
        Config.init(this)
        Api.init()
    }
}
