package com.identityvault.app.status

import android.content.Context
import com.identityvault.app.data.LsposedStatus
import com.identityvault.app.data.RootStatus
import com.identityvault.app.identity.IdentityProfileGenerator
import com.identityvault.app.identity.IdentityRepository
import com.identityvault.app.identity.IdentityValidator
import com.identityvault.app.log.LogRepository
import com.identityvault.app.root.MagiskChecker
import com.identityvault.app.root.RootChecker

class StatusViewModel(context: Context) {
    private val appContext = context.applicationContext
    val identityRepository = IdentityRepository(appContext)
    val logRepository = LogRepository(appContext)
    private val moduleStatusRepository = ModuleStatusRepository(appContext)
    private val lsposedChecker = LsposedChecker(appContext, moduleStatusRepository)
    private val rootChecker = RootChecker(appContext)
    private val magiskChecker = MagiskChecker(appContext)
    private val generator = IdentityProfileGenerator()
    private val validator = IdentityValidator()
    private val prefs = appContext.getSharedPreferences("profile_apply", Context.MODE_PRIVATE)

    var rootStatus: RootStatus = rootChecker.check()
        private set
    var lsposedStatus: LsposedStatus = lsposedChecker.check()
        private set
    var magiskStatus: String = magiskChecker.check()
        private set
    var profileApplied: Boolean = prefs.getBoolean("applied", false)
        private set

    fun refresh() {
        rootStatus = rootChecker.check()
        lsposedStatus = lsposedChecker.check()
        magiskStatus = magiskChecker.check()
        profileApplied = prefs.getBoolean("applied", false)
        logRepository.add("Status refreshed")
    }

    fun requestRoot() {
        rootStatus = rootChecker.requestRoot()
        logRepository.add("Root requested: ${rootStatus.detail}")
    }

    fun openLsposed(): Boolean {
        val opened = lsposedChecker.openManager()
        logRepository.add(if (opened) "Opened LSPosed manager" else "LSPosed manager not available")
        return opened
    }

    fun generateProfile() {
        val profile = generator.generate()
        identityRepository.saveProfile(profile)
        profileApplied = false
        prefs.edit().putBoolean("applied", false).apply()
        logRepository.add("Generated identity profile: ${profile.name}")
    }

    fun generateProfileObject() = generator.generate(identityRepository.getProfile().name)

    fun markProfileDirty() {
        profileApplied = false
        prefs.edit().putBoolean("applied", false).apply()
    }

    fun applyProfile() {
        profileApplied = true
        prefs.edit().putBoolean("applied", true).putLong("applied_at", System.currentTimeMillis()).apply()
        logRepository.add("Profile applied for hook provider")
    }

    fun validateCurrent(): Map<String, String> = validator.validate(identityRepository.getProfile())
}
