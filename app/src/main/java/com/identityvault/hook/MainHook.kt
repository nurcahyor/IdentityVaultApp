package com.identityvault.hook

import android.app.Application
import android.content.Context
import android.os.Build
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
        val processName = lpparam.processName ?: ""
        XposedBridge.log("IdentityVault MainHook called: package=$packageName, process=$processName, sdk=${Build.VERSION.SDK_INT}")
        if (packageName == HookConstants.MODULE_PACKAGE) {
            XposedBridge.log("IdentityVault Skipped $packageName: module package")
            return
        }
        HookCompat.safeCall("install Application.attach for $packageName") {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.args.firstOrNull() as? Context
                        if (context == null) {
                            XposedBridge.log("IdentityVault Skipped $packageName: Application.attach context missing")
                            return
                        }
                        HookInstaller.installAll(context, lpparam)
                    }
                }
            )
        }
    }
}
