package com.identityvault.app.identity

import android.content.Context
import com.identityvault.app.data.IdentityProfile
import org.json.JSONObject

class IdentityRepository(context: Context) {
    private val prefs = context.getSharedPreferences("identity_profile", Context.MODE_PRIVATE)
    private val generator = IdentityProfileGenerator()

    fun getProfile(): IdentityProfile {
        val raw = prefs.getString("profile", null)
        return if (raw.isNullOrBlank()) generator.generate("Default Profile") else IdentityProfile.fromJson(JSONObject(raw))
    }

    fun saveProfile(profile: IdentityProfile) {
        prefs.edit().putString("profile", profile.toJson().toString()).apply()
    }

    fun exportJson(): JSONObject = JSONObject().put("profile", getProfile().toJson())

    fun importJson(json: JSONObject) {
        val profileJson = json.optJSONObject("profile") ?: return
        saveProfile(IdentityProfile.fromJson(profileJson))
    }
}
