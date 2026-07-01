package com.textvision.alistclient.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkMonitorContract {
    val isOnline: StateFlow<Boolean>
}

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : NetworkMonitorContract {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        // Recompute the online state from the current active network's
        // capabilities. NetworkCallback only fires on change, so without a
        // re-query the state can be stuck at a stale value when the system
        // already has a network on app launch. We deliberately do NOT require
        // NET_CAPABILITY_VALIDATED: that flag is set by the system only after a
        // probe to a hard-coded validation server (e.g. Google) succeeds. On
        // networks that can't reach that server — captive portals, mobile
        // carriers that block it, or the user being on a private LAN that
        // proxies to their alist server — VALIDATED is never set, even though
        // the user can clearly reach the services they care about. We only
        // require INTERNET, which is true whenever any usable data path exists.
        fun recomputeFromActive() {
            val activeCaps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            _isOnline.value = activeCaps != null &&
                activeCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        recomputeFromActive()
        cm.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { recomputeFromActive() }
                override fun onLost(network: Network) { recomputeFromActive() }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            }
        )
    }
}
