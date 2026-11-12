package com.smarttranslator.lectures.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

class MicDeviceHelper(private val context: Context) {
    fun inputs(): List<AudioDeviceInfo> {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getDevices(AudioManager.GET_DEVICES_INPUTS).toList()
    }

    fun label(dev: AudioDeviceInfo): String = when (dev.type) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Встроенный микрофон"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth (SCO)"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB-микрофон"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Гарнитура"
        else -> "Устройство #${dev.id}"
    }
}