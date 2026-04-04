/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.folklore25.ghosthand.server

import com.folklore25.ghosthand.R
import com.folklore25.ghosthand.capability.*
import com.folklore25.ghosthand.catalog.*
import com.folklore25.ghosthand.integration.github.*
import com.folklore25.ghosthand.integration.projection.*
import com.folklore25.ghosthand.interaction.accessibility.*
import com.folklore25.ghosthand.interaction.clipboard.*
import com.folklore25.ghosthand.interaction.effects.*
import com.folklore25.ghosthand.interaction.execution.*
import com.folklore25.ghosthand.notification.*
import com.folklore25.ghosthand.payload.*
import com.folklore25.ghosthand.preview.*
import com.folklore25.ghosthand.screen.find.*
import com.folklore25.ghosthand.screen.ocr.*
import com.folklore25.ghosthand.screen.read.*
import com.folklore25.ghosthand.screen.summary.*
import com.folklore25.ghosthand.server.*
import com.folklore25.ghosthand.server.http.*
import com.folklore25.ghosthand.service.accessibility.*
import com.folklore25.ghosthand.service.notification.*
import com.folklore25.ghosthand.service.runtime.*
import com.folklore25.ghosthand.state.*
import com.folklore25.ghosthand.state.device.*
import com.folklore25.ghosthand.state.diagnostics.*
import com.folklore25.ghosthand.state.health.*
import com.folklore25.ghosthand.state.read.*
import com.folklore25.ghosthand.state.runtime.*
import com.folklore25.ghosthand.state.summary.*
import com.folklore25.ghosthand.ui.common.dialog.*
import com.folklore25.ghosthand.ui.common.model.*
import com.folklore25.ghosthand.ui.diagnostics.*
import com.folklore25.ghosthand.ui.main.*
import com.folklore25.ghosthand.ui.permissions.*
import com.folklore25.ghosthand.wait.*

import android.util.Log
import com.folklore25.ghosthand.observation.GhosthandObservationLog
import com.folklore25.ghosthand.observation.GhosthandObservationPublisher
import com.folklore25.ghosthand.routes.action.ActionRouteHandlers
import com.folklore25.ghosthand.routes.GhosthandRoutePolicies
import com.folklore25.ghosthand.routes.input.InputRouteHandlers
import com.folklore25.ghosthand.routes.observation.ObservationRouteHandlers
import com.folklore25.ghosthand.routes.read.ReadRouteHandlers
import com.folklore25.ghosthand.routes.system.SystemRouteHandlers
import com.folklore25.ghosthand.routes.wait.WaitRouteHandlers
import com.folklore25.ghosthand.state.StateCoordinator
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class LocalApiServerRequestException(
    val statusCode: Int,
    val errorCode: String,
    override val message: String
) : IllegalArgumentException(message)

class LocalApiServer(
    context: android.content.Context,
    runtimeStateProvider: () -> RuntimeState
) {
    private val stateCoordinator = StateCoordinator(
        context = context.applicationContext,
        runtimeStateProvider = runtimeStateProvider
    )
    private val running = AtomicBoolean(false)
    private val observationLog = GhosthandObservationLog()
    private val observationPublisher = GhosthandObservationPublisher(observationLog)

    @Volatile
    private var resources = createResources()

    private val routeRegistry by lazy(::createRouteRegistry)

    fun setMediaProjection(projection: android.media.projection.MediaProjection) {
        stateCoordinator.setMediaProjection(projection)
    }

    fun hasMediaProjection(): Boolean = stateCoordinator.hasMediaProjection()

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }

        if (resources.serverExecutor.isShutdown || resources.clientExecutor.isShutdown) {
            resources = createResources()
        }

        val localResources = resources
        localResources.serverExecutor.execute {
            try {
                val socket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(InetAddress.getByName(HOST), PORT))
                }
                localResources.attachServerSocket(socket)
                RuntimeStateStore.markLocalApiServerStarted()
                Log.i(LOG_TAG, "Listening on $HOST:$PORT")

                while (running.get()) {
                    val client = socket.accept()
                    client.soTimeout = CLIENT_READ_TIMEOUT_MS
                    localResources.registerClient(client)
                    try {
                        localResources.clientExecutor.execute {
                            handleClient(client, localResources)
                        }
                    } catch (error: RejectedExecutionException) {
                        localResources.unregisterClient(client)
                        rejectBusyClient(client)
                    }
                }
            } catch (error: SocketException) {
                if (running.get()) {
                    RuntimeStateStore.markLocalApiServerFailed(error.message ?: "socket error")
                    Log.e(LOG_TAG, "Socket failure", error)
                }
            } catch (error: Exception) {
                if (running.get()) {
                    RuntimeStateStore.markLocalApiServerFailed(error.message ?: "unknown error")
                    Log.e(LOG_TAG, "Startup failure", error)
                }
            } finally {
                localResources.stopAll()
                running.set(false)
                RuntimeStateStore.markLocalApiServerStopped()
                Log.i(LOG_TAG, "Stopped")
            }
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) {
            return
        }
        resources.stopAll()
        if (!resources.awaitStopped(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            Log.w(LOG_TAG, "Timed out waiting for LocalApiServer executors to stop")
        }
    }

    private fun handleClient(socket: Socket, localResources: LocalApiServerResources) {
        socket.use { client ->
            try {
                val request = LocalApiServerProtocol.readRequest(client.getInputStream())
                val response = CapabilityRoutePolicy.policyDeniedResponse(
                    path = request.path,
                    capabilityAccess = stateCoordinator.capabilityAccessSnapshot()
                )?.let { denied ->
                    LocalApiServerEnvelope.httpResponse(
                        statusCode = 403,
                        body = LocalApiServerEnvelope.error(
                            code = "CAPABILITY_POLICY_DENIED",
                            message = denied
                        )
                    )
                } ?: routeRegistry.dispatch(
                    method = request.method,
                    path = request.path,
                    queryParameters = request.queryParameters,
                    requestBody = request.body
                )

                OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8).use { writer ->
                    writer.write(response)
                    writer.flush()
                }
            } catch (error: LocalApiServerRequestException) {
                Log.w(
                    LOG_TAG,
                    "component=LocalApiServer operation=handleClient status=${error.statusCode} code=${error.errorCode} failure=${error.javaClass.simpleName} message=${error.message}"
                )
                writeResponse(
                    client,
                    LocalApiServerEnvelope.httpResponse(
                        statusCode = error.statusCode,
                        body = LocalApiServerEnvelope.error(
                            code = error.errorCode,
                            message = error.message
                        )
                    )
                )
            } catch (error: SocketTimeoutException) {
                Log.w(
                    LOG_TAG,
                    "component=LocalApiServer operation=handleClient failure=${error.javaClass.simpleName} message=Client read timed out"
                )
                writeResponse(
                    client,
                    LocalApiServerEnvelope.httpResponse(
                        statusCode = 408,
                        body = LocalApiServerEnvelope.error(
                            code = "REQUEST_TIMEOUT",
                            message = "Timed out waiting for the full HTTP request."
                        )
                    )
                )
            } catch (error: Exception) {
                Log.e(
                    LOG_TAG,
                    "component=LocalApiServer operation=handleClient failure=${error.javaClass.simpleName}",
                    error
                )
                writeResponse(
                    client,
                    LocalApiServerEnvelope.httpResponse(
                        statusCode = 500,
                        body = LocalApiServerEnvelope.error(
                            code = "INTERNAL_ERROR",
                            message = "Failed to handle request."
                        )
                    )
                )
            } finally {
                localResources.unregisterClient(client)
            }
        }
    }

    private fun rejectBusyClient(client: Socket) {
        client.use {
            writeResponse(
                it,
                LocalApiServerEnvelope.httpResponse(
                    statusCode = 503,
                    body = LocalApiServerEnvelope.error(
                        code = "SERVER_BUSY",
                        message = "Local API server is at capacity. Retry the request."
                    )
                )
            )
        }
    }

    private fun writeResponse(client: Socket, response: String) {
        try {
            OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8).use { writer ->
                writer.write(response)
                writer.flush()
            }
        } catch (error: Exception) {
            Log.w(
                LOG_TAG,
                "component=LocalApiServer operation=writeResponse failure=${error.javaClass.simpleName}",
                error
            )
        }
    }

    private fun createResources(): LocalApiServerResources {
        return LocalApiServerResources(
            serverExecutor = Executors.newSingleThreadExecutor(serverThreadFactory("ghosthand-local-api")),
            clientExecutor = ThreadPoolExecutor(
                CLIENT_POOL_SIZE,
                CLIENT_POOL_SIZE,
                0L,
                TimeUnit.MILLISECONDS,
                ArrayBlockingQueue(CLIENT_QUEUE_CAPACITY),
                serverThreadFactory("ghosthand-local-client"),
                ThreadPoolExecutor.AbortPolicy()
            )
        )
    }

    private fun serverThreadFactory(namePrefix: String): ThreadFactory {
        val count = AtomicInteger(1)
        return ThreadFactory { runnable ->
            Thread(runnable, "$namePrefix-${count.getAndIncrement()}")
        }
    }

    private fun createRouteRegistry(): LocalApiServerRouteRegistry {
        val routes = buildList {
            addAll(SystemRouteHandlers(stateCoordinator).routes())
            addAll(ObservationRouteHandlers(observationLog).routes())
            addAll(ReadRouteHandlers(stateCoordinator, observationPublisher).routes())
            addAll(ActionRouteHandlers(stateCoordinator, observationPublisher).routes())
            addAll(InputRouteHandlers(stateCoordinator, observationPublisher).routes())
            addAll(WaitRouteHandlers(stateCoordinator).routes())
        }
        val pathPolicies = buildMap {
            routes.map { it.path }.distinct().forEach { path ->
                GhosthandRoutePolicies.policyFor(path)?.let { put(path, it) }
            }
        }
        return LocalApiServerRouteRegistry(routes = routes, pathPolicies = pathPolicies)
    }

    companion object {
        const val HOST = "127.0.0.1"
        const val PORT = 5583
        const val LOG_TAG = "LocalApiServer"
        const val MAX_REQUEST_LINE_BYTES = 4096
        const val MAX_HEADER_LINE_BYTES = 8192
        const val MAX_HEADER_COUNT = 40
        const val MAX_HEADER_BYTES = 16 * 1024
        const val MAX_BODY_BYTES = 256 * 1024
        const val CLIENT_READ_TIMEOUT_MS = 5_000
        const val CLIENT_POOL_SIZE = 4
        const val CLIENT_QUEUE_CAPACITY = 16
        const val SHUTDOWN_TIMEOUT_MS = 2_000L
    }
}

internal object CapabilityRoutePolicy {
    internal val accessibilityPaths = setOf(
        "/tree",
        "/find",
        "/tap",
        "/swipe",
        "/type",
        "/screen",
        "/focused",
        "/click",
        "/input",
        "/setText",
        "/scroll",
        "/longpress",
        "/gesture",
        "/back",
        "/home",
        "/recents",
        "/wait"
    )
    private val screenshotPaths = setOf("/screenshot")

    fun routeCapability(path: String): GhosthandCapability? {
        return when {
            path in accessibilityPaths -> GhosthandCapability.Accessibility
            path in screenshotPaths -> GhosthandCapability.Screenshot
            else -> null
        }
    }

    fun policyDeniedResponse(path: String, capabilityAccess: CapabilityAccessSnapshot): String? {
        val capability = routeCapability(path) ?: return null
        val gateState = capabilityAccess.gateStateFor(capability)
        if (gateState.policyAllowed) {
            return null
        }
        return denialMessage(capability)
    }

    fun denialMessage(capability: GhosthandCapability): String {
        return when (capability) {
            GhosthandCapability.Accessibility ->
                "Accessibility control is disabled by app policy. Enable it on the Permissions page before using accessibility-backed Ghosthand routes."
            GhosthandCapability.Screenshot ->
                "Screenshot capture is disabled by app policy. Enable it on the Permissions page before using screenshot routes."
        }
    }
}
