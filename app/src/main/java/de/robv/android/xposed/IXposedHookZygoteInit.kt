package de.robv.android.xposed

interface IXposedHookZygoteInit {
    fun initZygote(startupParam: StartupParam)

    class StartupParam {
        var modulePath: String? = null
        var startsSystemServer: Boolean = false
    }
}
