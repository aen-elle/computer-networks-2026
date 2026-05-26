package data

import kotlinx.serialization.Serializable

@Serializable
data class UpdateTableMessage(
    val ip: String,
    val data: Map<String, RouterTableEntry>
)

@Serializable
data class NetworkConfig(
    val routers: List<RouterInfo>
)

@Serializable
data class RouterInfo(
    val routerIp: String,
    val factualIp: String = "127.0.0.1",
    val port: Int,
    val neighbours: List<NeighbourEntry>
)

@Serializable
data class RouterTableEntry(
    val nextHop: String,
    val distance: Int
)

@Serializable
data class NeighbourEntry(
    val routerIp: String,
    val factualIp: String = "127.0.0.1",
    val port: Int,
)