package com.identityvault.hook

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        XposedBridge.log("IdentityVault zygote initialized")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName ?: return
        if (packageName == "com.identityvault.app") return
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.args.firstOrNull() as? Context ?: return
                        sendMarker(context, packageName)
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("IdentityVault marker failed for $packageName: ${t.message}")
        }
    }

    private fun sendMarker(context: Context, packageName: String) {
        try {
            val intent = Intent("com.identityvault.app.HOOK_ACTIVE")
                .setComponent(ComponentName("com.identityvault.app", "com.identityvault.app.status.HookMarkerReceiver"))
                .putExtra("package", packageName)
            context.sendBroadcast(intent)
            XposedBridge.log("IdentityVault active for $packageName")
        } catch (t: Throwable) {
            XposedBridge.log("IdentityVault broadcast marker failed for $packageName: ${t.message}")
        }
    }
}
