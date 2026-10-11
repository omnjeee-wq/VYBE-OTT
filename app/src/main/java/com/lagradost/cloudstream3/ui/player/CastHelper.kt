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

/** LG webOS TV casting via Connect SDK. Phone sirf URL bhejta hai, TV khud stream chalata hai. */
object CastHelper {
    private var started = false
    private var device: ConnectableDevice? = null
    private var mediaControl: MediaControl? = null

    val isCasting: Boolean get() = mediaControl != null

    /** Player khulte hi call karo, taaki button dabane tak TV discover ho chuka ho. */
    fun start(ctx: Context) {
        if (started) return
        try {
            DiscoveryManager.init(ctx.applicationContext)
            val dm = DiscoveryManager.getInstance()
            dm.registerDeviceService(WebOSTVService::class.java, SSDPDiscoveryProvider::class.java)
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
    
