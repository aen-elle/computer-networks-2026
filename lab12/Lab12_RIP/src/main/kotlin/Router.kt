import data.RouterInfo
import data.RouterTableEntry
import data.UpdateTableMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.iterator
import kotlin.time.Duration.Companion.seconds

class Router(
    private val ownInfo: RouterInfo,
    private val scope: CoroutineScope,
) {
    private val ip = ownInfo.routerIp
    private val routerTable = mutableMapOf<String, RouterTableEntry>()
    private val tableLock = Mutex()
    private val neighbourTimeouts = ConcurrentHashMap<String, Job>()
    private var periodicSendTimeout: Job? = null

    //using UDP here
    private val socket = DatagramSocket(ownInfo.port)
    private val recBuffer = ByteArray(65600)

    init {
        routerTable[ip] = RouterTableEntry(ip, 0)
        initRouterTable()
    }

    private fun initRouterTable() {
        ownInfo.neighbours.forEach {
            routerTable[it.routerIp] = RouterTableEntry(it.routerIp, 1)
        }
    }

    fun start() {
        logger.info {" Router ${ownInfo.routerIp} is starting up... "}

        ownInfo.neighbours.forEach { neighbour ->
            setNeighborTimeout(neighbour.routerIp)
        }

        logger.info { "Router ${ownInfo.routerIp} is listening on port ${ownInfo.port}" }
        scope.launch(Dispatchers.IO) {
            while(isActive) {
                acceptUpdate()
            }
        }

        logger.info {"Router ${ownInfo.routerIp} begins periodical sent of its routing table... "}
        periodicSendTimeout = scope.launch {
            while (isActive) {
                delay(30.seconds)
                sendUpdates()
            }
        }
    }

    fun stop() {
        periodicSendTimeout?.cancel()
        neighbourTimeouts.values.forEach { it.cancel() }
        neighbourTimeouts.clear()
        socket.close()
    }

    private suspend fun acceptUpdate() {
        val packet = DatagramPacket(recBuffer, recBuffer.size)
        socket.receive(packet)

        logger.info { "Router ${ownInfo.routerIp} received an update message!" }
        var update: UpdateTableMessage
        try {
            val data = String(packet.data, 0, packet.length)
            update = Json.Default.decodeFromString<UpdateTableMessage>(data)
        } catch (e: Exception) {
            logger.error { "The packet for ${ownInfo.routerIp} seems malformed. Not proceeding with the updates." }
            return
        }
        var hasUpdated = false
        setNeighborTimeout(update.ip)

        tableLock.withLock {
            for ((dest, entry) in update.data) {
                val newMetric = entry.distance + 1
                if (newMetric > 15) continue

                val currEntry = routerTable[dest]
                when {
                    currEntry == null -> {
                        logger.info { "Router ${ownInfo.routerIp} updating their entry table for $dest: it is a new entry for them." }
                        hasUpdated = true
                        routerTable[dest] = RouterTableEntry(update.ip, newMetric)
                    }
                    currEntry.nextHop == update.ip -> {
                        if (currEntry.distance != newMetric) {
                            logger.info { "Router ${ownInfo.routerIp} updating their entry table for $dest: it is a news about an old route." }
                            routerTable[dest] = RouterTableEntry(update.ip, newMetric)
                            hasUpdated = true
                        }
                    }
                    newMetric < currEntry.distance -> {
                        logger.info { "Router ${ownInfo.routerIp} updating their entry table for $dest: it has found a better way to it." }
                        hasUpdated = true
                        routerTable[dest] = RouterTableEntry(update.ip, newMetric)
                    }
                }
            }
        }

        if (hasUpdated) {
            printRoutingTable()
            sendUpdates()
        }
    }

    private suspend fun sendUpdates() {
        val snapshot = tableLock.withLock { routerTable.toMap() }
        ownInfo.neighbours.forEach { neighbourEntry ->
            val prSnapshot = snapshot.mapValues { (_, entry) ->
                if (entry.nextHop == neighbourEntry.routerIp) {
                    RouterTableEntry("", 16)   // poison reverse
                } else {
                    entry
                }
            }
            val neighborAddress = InetSocketAddress(neighbourEntry.factualIp, neighbourEntry.port)
            val data = Json.Default.encodeToString(UpdateTableMessage(ownInfo.routerIp, prSnapshot)).encodeToByteArray()
            val packet = DatagramPacket(data, data.size, neighborAddress)
            socket.send(packet)
        }
    }

    private fun setNeighborTimeout(neighbourIp: String) {
        neighbourTimeouts[neighbourIp]?.cancel()
        neighbourTimeouts[neighbourIp] = scope.launch {
            delay(180.seconds)
            handleNeighbourTimeout(neighbourIp)
        }

    }

    private suspend fun handleNeighbourTimeout(neighbourIp: String) {
        logger.info { " Neighbour $neighbourIp has timeouted. Considering it unachievable..." }
        tableLock.withLock {
            val toRm = routerTable.filter { it.value.nextHop == neighbourIp }.keys
            toRm.forEach {
                routerTable[it] = RouterTableEntry("", 16)
            }
        }
        sendUpdates()
        neighbourTimeouts.remove(neighbourIp)
    }

    suspend fun printRoutingTable() {
        val output = tableLock.withLock {
            buildString {
                appendLine("=== Router ${ownInfo.routerIp} routing table ===")
                appendLine("[Destination IP]\t[Next Hop]\t[Metric]")
                for ((dest, entry) in routerTable) {
                    appendLine("$dest\t\t${entry.nextHop}\t\t${entry.distance}")
                }
                appendLine()
            }
        }
        println(output)
    }
}