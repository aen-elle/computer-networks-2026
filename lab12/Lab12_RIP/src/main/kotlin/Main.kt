import data.NetworkConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File

val logger = KotlinLogging.logger {}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun main() = runBlocking {
    val simulationDurationSeconds = 120L
    val config = loadNetworkConfigFromFile("./config.json")

    val dispatchers = mutableListOf<ExecutorCoroutineDispatcher>()
    val routers = mutableListOf<Router>()

    for (routerInfo in config.routers) {
        val dispatcher = newSingleThreadContext("Router-${routerInfo.routerIp}")
        dispatchers.add(dispatcher)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val router = Router(routerInfo, scope)
        routers.add(router)
    }

    routers.forEach { it.start() }

    delay(simulationDurationSeconds * 1000)

    routers.forEach { it.stop() }
    dispatchers.forEach { it.close() }

    println("\n========== FINAL ROUTING TABLES ==========")
    routers.forEach { router ->
        router.printRoutingTable()
    }
}


fun loadNetworkConfigFromFile(path: String): NetworkConfig {
    val jsonString = File(path).readText()
    return Json.decodeFromString<NetworkConfig>(jsonString)
}