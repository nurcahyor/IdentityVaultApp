package de.robv.android.xposed

object XposedHelpers {
    @JvmStatic
    fun findAndHookMethod(clazz: Class<*>, methodName: String, vararg parameterTypesAndCallback: Any): Any? {
        XposedBridge.log("Stub hook request: ${clazz.name}#$methodName")
        return null
    }
}
