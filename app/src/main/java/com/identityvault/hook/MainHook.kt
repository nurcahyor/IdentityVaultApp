package com.identityvault.hook

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaDrm
import android.net.Uri
import android.net.wifi.WifiInfo
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        XposedBridge.log("IdentityVault zygote initialized")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName ?: return
        if (packageName == MODULE_PACKAGE) return
        safe("install attach hook for $packageName") {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.args.firstOrNull() as? Context ?: return
                        val profile = loadProfile(context)
                        sendMarker(context, packageName, emptySet())
                        if (profile == null) {
                            XposedBridge.log("IdentityVault skipped $packageName because profile is empty")
                            return
                        }
                        applyBuildFields(context, packageName, profile)
                        installIdentityHooks(context, packageName, profile)
                    }
                }
            )
        }
    }

    private fun installIdentityHooks(context: Context, packageName: String, profile: HookProfile) {
        hookAndroidId(context, packageName, profile)
        hookTelephony(context, packageName, profile)
        hookWifi(context, packageName, profile)
        hookBluetooth(context, packageName, profile)
        hookMediaDrm(context, packageName, profile)
        hookSystemProperties(context, packageName, profile)
    }

    private fun hookAndroidId(context: Context, packageName: String, profile: HookProfile) {
        val callback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val name = param.args.getOrNull(1) as? String ?: return
                if (name != Settings.Secure.ANDROID_ID) return
                val value = profile.androidId.validHex(16, "Android ID", packageName) ?: return logOff(profile.androidId, "Android ID", packageName)
                param.result = value
                applied(context, packageName, "Android ID")
            }
        }
        hookMethod("Settings.Secure.getString", Settings.Secure::class.java, "getString", ContentResolver::class.java, String::class.java, callback)
        hookMethod("Settings.Secure.getStringForUser", Settings.Secure::class.java, "getStringForUser", ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType!!, callback)
    }

    private fun hookTelephony(context: Context, packageName: String, profile: HookProfile) {
        hookReturn("IMEI", packageName, context, profile.imei, { it.isImei() }, TelephonyManager::class.java, "getDeviceId")
        hookReturn("IMEI", packageName, context, profile.imei, { it.isImei() }, TelephonyManager::class.java, "getDeviceId", Int::class.javaPrimitiveType!!)
        hookReturn("IMEI", packageName, context, profile.imei, { it.isImei() }, TelephonyManager::class.java, "getImei")
        hookReturn("IMEI", packageName, context, profile.imei, { it.isImei() }, TelephonyManager::class.java, "getImei", Int::class.javaPrimitiveType!!)
        hookReturn("SIM Sub IDs", packageName, context, profile.simSubIds, { it.isNotBlank() }, TelephonyManager::class.java, "getSubscriberId")
        hookReturn("SIM Sub IDs", packageName, context, profile.simSubIds, { it.isNotBlank() }, TelephonyManager::class.java, "getSubscriberId", Int::class.javaPrimitiveType!!)
        hookReturn("SIM Serial ID", packageName, context, profile.simSerialId, { it.matches(Regex("[0-9]{19,20}")) }, TelephonyManager::class.java, "getSimSerialNumber")
        hookReturn("Mobile No", packageName, context, profile.mobileNo, { it.matches(Regex("\\+?[0-9]{8,15}")) }, TelephonyManager::class.java, "getLine1Number")
        hookReturn("SIM Operator", packageName, context, profile.simOperator, { it.matches(Regex("[0-9]{5,6}")) }, TelephonyManager::class.java, "getSimOperator")
        hookReturn("SIM Operator Name", packageName, context, profile.simOperator, { it.matches(Regex("[0-9]{5,6}")) }, TelephonyManager::class.java, "getSimOperatorName")
    }

    private fun hookWifi(context: Context, packageName: String, profile: HookProfile) {
        hookReturn("MAC Address", packageName, context, profile.macAddress, { it.isMac() }, WifiInfo::class.java, "getMacAddress")
        hookReturn("MAC SSID", packageName, context, profile.macSsid, { it.isNotBlank() }, WifiInfo::class.java, "getSSID")
        hookReturn("MAC BSSID", packageName, context, profile.macBssid, { it.isMac() }, WifiInfo::class.java, "getBSSID")
    }

    private fun hookBluetooth(context: Context, packageName: String, profile: HookProfile) {
        hookReturn("Bluetooth MAC", packageName, context, profile.bluetoothMac, { it.isMac() }, BluetoothAdapter::class.java, "getAddress")
        hookReturn("Bluetooth MAC", packageName, context, profile.bluetoothMac, { it.isMac() }, BluetoothDevice::class.java, "getAddress")
    }

    private fun hookMediaDrm(context: Context, packageName: String, profile: HookProfile) {
        hookMethod("MediaDrm.getPropertyByteArray", MediaDrm::class.java, "getPropertyByteArray", String::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val property = param.args.firstOrNull() as? String ?: return
                if (property != "deviceUniqueId") return
                if (!profile.mediaDrmId.enabled) return logOff(profile.mediaDrmId, "MediaDrm ID", packageName)
                val bytes = profile.mediaDrmId.value.hexBytes()
                if (bytes == null || bytes.isEmpty()) {
                    XposedBridge.log("IdentityVault skipped MediaDrm ID for $packageName because value invalid")
                    return
                }
                param.result = bytes
                applied(context, packageName, "MediaDrm ID")
            }
        })
    }

    private fun applyBuildFields(context: Context, packageName: String, profile: HookProfile) {
        if (!profile.buildPropEnabled) {
            XposedBridge.log("IdentityVault skipped Build Prop because field OFF")
            return
        }
        val build = profile.build
        safeSetBuild(context, packageName, "MODEL", "ro.product.model", build["ro.product.model"])
        safeSetBuild(context, packageName, "BRAND", "ro.product.brand", build["ro.product.brand"])
        safeSetBuild(context, packageName, "MANUFACTURER", "ro.product.manufacturer", build["ro.product.manufacturer"])
        safeSetBuild(context, packageName, "DEVICE", "ro.product.device", build["ro.product.device"])
        safeSetBuild(context, packageName, "PRODUCT", "ro.product.name", build["ro.product.name"])
        safeSetBuild(context, packageName, "BOARD", "ro.product.board", build["ro.product.board"])
        safeSetBuild(context, packageName, "HARDWARE", "ro.hardware", build["ro.hardware"])
        safeSetBuild(context, packageName, "FINGERPRINT", "ro.build.fingerprint", build["ro.build.fingerprint"])
        safeSetBuild(context, packageName, "ID", "ro.build.id", build["ro.build.id"])
        safeSetBuild(context, packageName, "DISPLAY", "ro.build.display.id", build["ro.build.display.id"])
        safe("Build.VERSION.RELEASE") {
            build["ro.build.version.release"]?.takeIf { it.isNotBlank() }?.let {
                XposedHelpers.setStaticObjectField(Build.VERSION::class.java, "RELEASE", it)
                applied(context, packageName, "Build Prop")
            }
        }
        safe("Build.VERSION.SDK_INT") {
            build["ro.build.version.sdk"]?.toIntOrNull()?.takeIf { it in 28..33 }?.let {
                XposedHelpers.setStaticIntField(Build.VERSION::class.java, "SDK_INT", it)
                applied(context, packageName, "Build Prop")
            }
        }
    }

    private fun hookSystemProperties(context: Context, packageName: String, profile: HookProfile) {
        if (!profile.buildPropEnabled) return
        val clazz = runCatching { Class.forName("android.os.SystemProperties") }.getOrNull() ?: return
        val callback = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val key = param.args.firstOrNull() as? String ?: return
                val value = profile.build[key]?.takeIf { it.isNotBlank() } ?: return
                param.result = value
                applied(context, packageName, "SystemProperties:$key")
            }
        }
        hookMethod("SystemProperties.get", clazz, "get", String::class.java, callback)
        hookMethod("SystemProperties.get(default)", clazz, "get", String::class.java, String::class.java, callback)
    }

    private fun hookReturn(
        label: String,
        packageName: String,
        context: Context,
        field: Field,
        validator: (String) -> Boolean,
        clazz: Class<*>,
        methodName: String,
        vararg args: Class<*>
    ) {
        hookMethod("$clazz.$methodName", clazz, methodName, *args, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!field.enabled) return logOff(field, label, packageName)
                if (!validator(field.value)) {
                    XposedBridge.log("IdentityVault skipped $label for $packageName because value invalid")
                    return
                }
                param.result = field.value
                applied(context, packageName, label)
            }
        })
    }

    private fun hookMethod(label: String, clazz: Class<*>, methodName: String, vararg argsAndCallback: Any) {
        safe(label) {
            XposedHelpers.findAndHookMethod(clazz, methodName, *argsAndCallback)
        }
    }

    private fun safeSetBuild(context: Context, packageName: String, fieldName: String, propKey: String, value: String?) {
        safe("Build.$fieldName") {
            if (value.isNullOrBlank()) {
                XposedBridge.log("IdentityVault skipped $propKey for $packageName because value invalid")
                return@safe
            }
            XposedHelpers.setStaticObjectField(Build::class.java, fieldName, value)
            applied(context, packageName, "Build Prop")
        }
    }

    private fun loadProfile(context: Context): HookProfile? {
        return runCatching {
            val bundle = context.contentResolver.call(PROFILE_URI, "getProfile", null, null) ?: return null
            val raw = bundle.getString("profileJson").orEmpty()
            val profileJson = JSONObject(raw).optJSONObject("profile") ?: return null
            HookProfile.fromJson(profileJson)
        }.onFailure {
            XposedBridge.log("IdentityVault profile load failed: ${it.message}")
        }.getOrNull()
    }

    private fun sendMarker(context: Context, packageName: String, fields: Set<String>) {
        try {
            val intent = Intent("com.identityvault.app.HOOK_ACTIVE")
                .setComponent(ComponentName(MODULE_PACKAGE, "com.identityvault.app.status.HookMarkerReceiver"))
                .putExtra("package", packageName)
                .putStringArrayListExtra("fields", ArrayList(fields))
            context.sendBroadcast(intent)
            XposedBridge.log("IdentityVault active for $packageName")
        } catch (t: Throwable) {
            XposedBridge.log("IdentityVault broadcast marker failed for $packageName: ${t.message}")
        }
    }

    private fun applied(context: Context, packageName: String, field: String) {
        sendMarker(context, packageName, setOf(field))
        XposedBridge.log("IdentityVault applied $field for $packageName")
    }

    private fun logOff(field: Field, label: String, packageName: String) {
        if (!field.enabled) XposedBridge.log("IdentityVault skipped $label because field OFF")
    }

    private fun safe(label: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            XposedBridge.log("IdentityVault hook failed at $label: ${t.message}")
        }
    }

    private data class Field(val value: String, val enabled: Boolean)

    private data class HookProfile(
        val imei: Field,
        val macAddress: Field,
        val macBssid: Field,
        val macSsid: Field,
        val bluetoothMac: Field,
        val androidId: Field,
        val simSerialId: Field,
        val simSubIds: Field,
        val mobileNo: Field,
        val mediaDrmId: Field,
        val simOperator: Field,
        val buildPropEnabled: Boolean,
        val build: Map<String, String>
    ) {
        companion object {
            fun fromJson(json: JSONObject): HookProfile = HookProfile(
                imei = field(json, "imei"),
                macAddress = field(json, "macAddress"),
                macBssid = field(json, "macBssid"),
                macSsid = field(json, "macSsid"),
                bluetoothMac = field(json, "bluetoothMac"),
                androidId = field(json, "androidId"),
                simSerialId = field(json, "simSerialId"),
                simSubIds = field(json, "simSubIds"),
                mobileNo = field(json, "mobileNo"),
                mediaDrmId = field(json, "mediaDrmId"),
                simOperator = field(json, "simOperator"),
                buildPropEnabled = json.optBoolean("buildPropEnabled", true),
                build = buildMap(json.optJSONObject("buildProp") ?: JSONObject())
            )

            private fun field(json: JSONObject, key: String): Field {
                val obj = json.optJSONObject(key) ?: JSONObject()
                return Field(obj.optString("value"), obj.optBoolean("enabled", true))
            }

            private fun buildMap(json: JSONObject): Map<String, String> {
                val map = linkedMapOf<String, String>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.optString(key)
                }
                return map
            }
        }
    }

    private fun Field.validHex(length: Int, label: String, packageName: String): String? {
        if (!enabled) return null
        if (!value.matches(Regex("[0-9a-fA-F]{$length}"))) {
            XposedBridge.log("IdentityVault skipped $label for $packageName because value invalid")
            return null
        }
        return value
    }

    private fun String.isImei(): Boolean = matches(Regex("[0-9]{15}")) && luhnValid()
    private fun String.isMac(): Boolean = matches(Regex("(?i)^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))

    private fun String.luhnValid(): Boolean {
        var sum = 0
        var alternate = false
        for (i in length - 1 downTo 0) {
            var n = this[i].digitToIntOrNull() ?: return false
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private fun String.hexBytes(): ByteArray? {
        val clean = trim()
        if (clean.length % 2 != 0 || !clean.matches(Regex("[0-9a-fA-F]+"))) return null
        return ByteArray(clean.length / 2) { index ->
            clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    companion object {
        private const val MODULE_PACKAGE = "com.identityvault.app"
        private val PROFILE_URI: Uri = Uri.parse("content://com.identityvault.app.profile")
    }
}
