package com.example.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionStatus {
    ONLINE_HIGH_SPEED,
    ONLINE_UNSTABLE,
    OFFLINE
}

data class NetworkState(
    val isOnline: Boolean = true,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val status: ConnectionStatus = ConnectionStatus.ONLINE_HIGH_SPEED,
    val simulatedOffline: Boolean = false
)

class NetworkConnectivityMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _networkState = MutableStateFlow(getCurrentState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        registerCallback()
    }

    private fun getCurrentState(): NetworkState {
        val cm = connectivityManager ?: return NetworkState(isOnline = false, status = ConnectionStatus.OFFLINE)
        val activeNetwork = cm.activeNetwork ?: return NetworkState(isOnline = false, status = ConnectionStatus.OFFLINE)
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkState(isOnline = false, status = ConnectionStatus.OFFLINE)

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        val status = if (hasInternet) {
            ConnectionStatus.ONLINE_HIGH_SPEED
        } else {
            ConnectionStatus.ONLINE_UNSTABLE
        }

        return NetworkState(
            isOnline = hasInternet,
            isWifi = isWifi,
            isCellular = isCellular,
            status = status
        )
    }

    private fun registerCallback() {
        if (connectivityManager == null) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    updateState()
                }

                override fun onLost(network: Network) {
                    updateState()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    updateState()
                }
            }

            networkCallback?.let {
                connectivityManager.registerNetworkCallback(request, it)
            }
        } catch (e: Exception) {
            // Fallback gracefully
        }
    }

    fun updateState() {
        val currentState = getCurrentState()
        if (_networkState.value.simulatedOffline) {
            _networkState.value = currentState.copy(
                isOnline = false,
                status = ConnectionStatus.OFFLINE,
                simulatedOffline = true
            )
        } else {
            _networkState.value = currentState
        }
    }

    fun isConnected(): Boolean {
        if (_networkState.value.simulatedOffline) return false
        return _networkState.value.isOnline
    }

    fun toggleSimulatedOffline(enabled: Boolean) {
        val current = getCurrentState()
        _networkState.value = if (enabled) {
            current.copy(isOnline = false, status = ConnectionStatus.OFFLINE, simulatedOffline = true)
        } else {
            current.copy(simulatedOffline = false)
        }
    }

    fun unregister() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
