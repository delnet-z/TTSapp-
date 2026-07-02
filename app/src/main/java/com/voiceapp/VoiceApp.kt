package com.voiceapp

import android.app.Application

class VoiceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: VoiceApp
            private set
    }
}
