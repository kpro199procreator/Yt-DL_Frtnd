package com.ytmusicdl.app.data.api

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

object PythonBridge {
    @Volatile private var initialized = false
    @Volatile private var initError: String? = null

    fun initialize(context: Context) {
        if (initialized) return
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
            }
            initialized = true
            initError = null
        } catch (e: Exception) {
            initialized = false
            initError = e.message ?: "No se pudo iniciar runtime Python"
        }
    }

    fun isReady(): Boolean = initialized && Python.isStarted() && initError == null

    fun getInitError(): String? = initError

    fun call(module: String, fn: String, vararg args: Any?): PyObject {
        check(isReady()) { initError ?: "Python runtime no está listo" }
        val py = Python.getInstance()
        val pyModule = py.getModule(module)
        return pyModule.callAttr(fn, *args)
    }
}
