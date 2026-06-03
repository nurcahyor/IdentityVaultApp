package com.identityvault.hook

fun validField(
    session: HookSession,
    category: String,
    label: String,
    field: HookField,
    validator: (String) -> Boolean
): String? {
    if (!field.enabled) {
        session.skipped("WARNING", category, "$label disabled, using original value", label)
        return null
    }
    val value = field.value.trim()
    if (!validator(value)) {
        session.skipped("SKIPPED", category, "$label invalid, using original value", label)
        return null
    }
    return value
}
