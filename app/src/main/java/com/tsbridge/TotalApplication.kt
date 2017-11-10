package com.tsbridge

import android.app.Application
import android.support.multidex.MultiDex
import android.util.Log

/**
 * Description:
 * Copyright  : Copyright (c) 2017
 * Email      : jusenr@163.com
 * Author     : Jusenr
 * Date       : 2017/11/06
 * Time       : 10:59
 * Project    ：KotlinDemo.
 */
class TotalApplication : Application() {
    companion object {
        val TAG: String = TotalApplication::class.java.simpleName


    }

    override fun onCreate() {
        super.onCreate()

        MultiDex.install(applicationContext)

        Log.i("TotalApplication", "TotalApplication\'s onCreate()")
        Log.i("TotalApplication", "isDebug :" + BuildConfig.DEBUG)

    }
}