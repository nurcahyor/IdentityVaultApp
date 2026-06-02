package de.robv.android.xposed

open class XC_MethodHook {
    open fun beforeHookedMethod(param: MethodHookParam) = Unit
    open fun afterHookedMethod(param: MethodHookParam) = Unit

    open class MethodHookParam {
        var thisObject: Any? = null
        var args: Array<Any?> = emptyArray()
        var result: Any? = null
        var throwable: Throwable? = null
    }
}
