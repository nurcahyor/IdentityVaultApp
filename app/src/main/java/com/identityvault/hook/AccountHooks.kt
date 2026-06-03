package com.identityvault.hook

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.os.Bundle
import android.os.Handler
import de.robv.android.xposed.XC_MethodHook

object AccountHooks {
    fun install(session: HookSession, profile: HookProfile) {
        HookCompat.safeHookMethod(session, "ACCOUNT", AccountManager::class.java, "getAccounts", hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val account = googleAccount(session, profile) ?: return
                param.result = arrayOf(account)
            }
        })
        HookCompat.safeHookMethod(session, "ACCOUNT", AccountManager::class.java, "getAccountsByType", String::class.java, hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val type = param.args.firstOrNull() as? String
                if (type != "com.google") {
                    session.skipped("SKIPPED", "ACCOUNT", "Skipped Google Account: type not com.google", "Google Account Email")
                    return
                }
                val account = googleAccount(session, profile) ?: return
                param.result = arrayOf(account)
            }
        })
        HookCompat.safeHookMethod(
            session,
            "ACCOUNT",
            AccountManager::class.java,
            "getAccountsByTypeAndFeatures",
            String::class.java,
            Array<String>::class.java,
            AccountManagerCallback::class.java,
            Handler::class.java,
            hook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val type = param.args.firstOrNull() as? String
                    if (type != "com.google") {
                        session.skipped("SKIPPED", "ACCOUNT", "Skipped Google Account: type not com.google", "Google Account Email")
                        return
                    }
                    val account = googleAccount(session, profile) ?: return
                    val future = accountFuture(arrayOf(account))
                    @Suppress("UNCHECKED_CAST")
                    (param.args.getOrNull(2) as? AccountManagerCallback<Array<Account>>)?.run(future)
                    param.result = future
                }
            }
        )
    }

    private fun googleAccount(session: HookSession, profile: HookProfile): Account? {
        val value = validField(session, "ACCOUNT", "Google Account Email", profile.googleAccountEmail, HookValidation::email) ?: return null
        session.applied("ACCOUNT", "Google Account Email", "Applied Google Account mock: $value")
        return Account(value, "com.google")
    }

    private fun accountFuture(accounts: Array<Account>): AccountManagerFuture<Array<Account>> {
        return object : AccountManagerFuture<Array<Account>> {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
            override fun isCancelled(): Boolean = false
            override fun isDone(): Boolean = true
            override fun getResult(): Array<Account> = accounts
            override fun getResult(timeout: Long, unit: java.util.concurrent.TimeUnit): Array<Account> = accounts
        }
    }
}
