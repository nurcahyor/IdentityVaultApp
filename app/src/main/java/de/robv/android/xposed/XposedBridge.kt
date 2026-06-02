package de.robv.android.xposed

object XposedBridge {
    @JvmStatic
    fun log(message: String) {
        android.util.Log.i("IdentityVault-Xposed", message)
    }

    @JvmStatic
    fun log(throwable: Throwable) {
        android.util.Log.e("IdentityVault-Xposed", throwable.message ?: "Throwable", throwable)
    }
}
