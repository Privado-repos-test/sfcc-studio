package com.binarysushi.studio.debugger

import com.binarysushi.studio.configuration.projectSettings.StudioConfigurationProvider
import com.binarysushi.studio.debugger.breakpoint.StudioDebuggerBreakpointHandler
import com.binarysushi.studio.debugger.client.SDAPIClient
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.util.ArrayUtil
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import java.nio.file.Paths

class StudioDebuggerProcess(session: XDebugSession) : XDebugProcess(session) {
    private val config = session.project.service<StudioConfigurationProvider>()
    val debuggerClient = SDAPIClient(config.hostname, config.username, config.password)
    private val breakpointHandler = StudioDebuggerBreakpointHandler(this)
    private val debugger = SDAPIDebugger(session, this)
    private val idKey: Key<Int> = Key.create("STUDIO_BP_ID")
    private val awaitingBreakpoints = mutableListOf<XLineBreakpoint<XBreakpointProperties<*>?>>()

    override fun sessionInitialized() {
        debugger.connect(awaitingBreakpoints)
    }

    override fun stop() {
        debugger.disconnect()
        session.reportMessage("Debug session stopped", MessageType.INFO)
    }

    override fun getEditorsProvider(): XDebuggerEditorsProvider {
        return StudioDebuggerEditorsProvider()
    }

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>?> {
        val breakpointHandlers = super.getBreakpointHandlers()
        return ArrayUtil.append(breakpointHandlers, breakpointHandler)
    }

    fun addBreakpoint(xLineBreakpoint: XLineBreakpoint<XBreakpointProperties<*>?>) {
        val line = xLineBreakpoint.line;
        val path = xLineBreakpoint.presentableFilePath.substring(
            Paths.get(session.project.basePath.toString(), "cartridges").toString().length
        )

        if (debugger.connectionState === DebuggerConnectionState.CONNECTED) {
            debuggerClient.createBreakpoint(line + 1, path, onSuccess = { breakpoint ->
                xLineBreakpoint.putUserData(idKey, breakpoint.id!!)
                session.updateBreakpointPresentation(xLineBreakpoint, AllIcons.Debugger.Db_verified_breakpoint, null)
            })
        } else {
            awaitingBreakpoints.add(xLineBreakpoint)
        }
    }

    fun removeBreakpoint(xLineBreakpoint: XLineBreakpoint<XBreakpointProperties<*>?>) {
        if (debugger.connectionState === DebuggerConnectionState.CONNECTED) {
            debuggerClient.deleteBreakpoint(xLineBreakpoint.getUserData(idKey)!!, onSuccess = {
                println("Success!")
            })
        } else {
            awaitingBreakpoints.add(xLineBreakpoint)
        }
    }

    override fun resume(context: XSuspendContext?) {
        if (context != null) {
            val activeExecutionStack = context.activeExecutionStack as StudioDebuggerExecutionStack
            debugger.resume(activeExecutionStack.scriptThread)
        }
    }
}
