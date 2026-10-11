package com.lagradost.cloudstream3.ui.player

import android.content.Context
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.device.ConnectableDeviceListener
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.connectsdk.service.DeviceService
import com.connectsdk.service.WebOSTVService
import com.connectsdk.service.capability.MediaControl
import com.connectsdk.service.capability.MediaPlayer
import com.connectsdk.service.command.ServiceCommandError
import com.connectsdk.service.DLNAService

object CastHelper {
    private var started = false
    private var device: ConnectableDevice? = null
    private var mediaControl: MediaControl? = null

    val isCasting: Boolean get() = mediaControl != null

    fun start(ctx: Context) {
        if (started) return
        try {
            DiscoveryManager.init(ctx.applicationContext)
            val dm = DiscoveryManager.getInstance()
            dm.registerDeviceService(DLNAService::class.java, SSDPDiscoveryProvider::class.java)
            dm.pairingLevel = DiscoveryManager.PairingLevel.ON
            dm.start()
            started = true
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun devices(): List<ConnectableDevice> {
        if (!started) return emptyList()
        return DiscoveryManager.getInstance().compatibleDevices.values.toList()
    }

    fun cast(target: ConnectableDevice, url: String, mime: String, title: String, onResult: (Boolean, String?) -> Unit) {
        if (target.isConnected) {
            device = target
            launch(target, url, mime, title, onResult)
            return
        }
        target.addListener(object : ConnectableDeviceListener {
            override fun onDeviceReady(d: ConnectableDevice) {
                device = d
                launch(d, url, mime, title, onResult)
            }

            override fun onDeviceDisconnected(d: ConnectableDevice) {
                if (device == d) {
                    device = null
                    mediaControl = null
                }
            }

            override fun onPairingRequired(
                d: ConnectableDevice,
                s: DeviceService,
                t: DeviceService.PairingType
            ) {
            }

            override fun onCapabilityUpdated(
                d: ConnectableDevice,
                added: List<String>,
                removed: List<String>
            ) {
            }

            override fun onConnectionFailed(d: ConnectableDevice, e: ServiceCommandError) {
                onResult(false, e.message)
            }
        })
        target.connect()
    }

    private fun launch(d: ConnectableDevice, url: String, mime: String, title: String, onResult: (Boolean, String?) -> Unit) {
        val mp = d.getCapability(MediaPlayer::class.java)
        if (mp == null) {
            onResult(false, "Is TV me media player support nahi mila")
            return
        }
        mp.playMedia(url, mime, title, "", "", false, object : MediaPlayer.LaunchListener {
            override fun onSuccess(obj: MediaPlayer.MediaLaunchObject) {
                mediaControl = obj.mediaControl
                onResult(true, null)
            }

            override fun onError(e: ServiceCommandError) {
                onResult(false, e.message)
            }
        })
    }

    fun pause() { mediaControl?.pause(null) }
    fun resume() { mediaControl?.play(null) }
    fun seek(ms: Long) { mediaControl?.seek(ms, null) }

    fun stop() {
        mediaControl?.stop(null)
        mediaControl = null
        device?.disconnect()
        device = null
    }
}
