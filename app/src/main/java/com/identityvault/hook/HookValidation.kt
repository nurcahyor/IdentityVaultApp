package com.identityvault.hook

object HookValidation {
    fun imei(value: String): Boolean = value.matches(Regex("[0-9]{15}")) && value.luhnValid()
    fun hex16(value: String): Boolean = value.matches(Regex("[0-9a-fA-F]{16}"))
    fun mac(value: String): Boolean = value.matches(Regex("(?i)^([0-9A-F]{2}:){5}[0-9A-F]{2}$"))
    fun simSerial(value: String): Boolean = value.matches(Regex("[0-9]{19,20}"))
    fun mobile(value: String): Boolean = value.matches(Regex("\\+?[0-9]{8,15}"))
    fun simOperator(value: String): Boolean = value.matches(Regex("[0-9]{5,6}"))
    fun notBlank(value: String): Boolean = value.isNotBlank()
    fun mediaDrm(value: String): Boolean = value.isNotBlank() && value.length in 8..128
    fun buildProp(value: String): Boolean = value.isNotBlank() && value.contains("/") && value.contains(":")

    fun hexBytes(value: String): ByteArray? {
        val clean = value.trim()
        if (clean.length % 2 != 0 || !clean.matches(Regex("[0-9a-fA-F]+"))) return null
        return ByteArray(clean.length / 2) { index ->
            clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

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
}
