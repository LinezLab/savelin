package com.pinfold.app.diagnostics

/**
 * Redaction shared by the in-memory log, the crash file and the clipboard. Only structural data is
 * kept: a URL is reduced to its host, and anything that looks like a credential is replaced. Titles
 * and other free text are never passed to the logger in the first place.
 */
private val Url = Regex("""\b(?:https?|ftp)://[^\s"'<>)]+""", RegexOption.IGNORE_CASE)
private val Email = Regex("""\b[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}\b""")
private val Bearer = Regex("""(?i)\bBearer\s+\S+""")
private val Credential = Regex("""(?i)\b(token|bearer|password|passphrase|secret|api[_-]?key|authorization|recovery|otp)\b(\s*[:=]\s*)\S+""")
private val LongSecret = Regex("""\b[A-Za-z0-9+/=_\-]{32,}\b""")

fun redact(input: String): String {
    var out = Url.replace(input) { match -> hostOnly(match.value) }
    out = Email.replace(out, "<email>")
    out = Bearer.replace(out, "Bearer <redacted>")
    out = Credential.replace(out) { match -> "${match.groupValues[1]}${match.groupValues[2]}<redacted>" }
    out = LongSecret.replace(out, "<redacted>")
    return out
}

private fun hostOnly(url: String): String {
    val scheme = url.substringBefore("://")
    val authority = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#').substringBefore('@')
    return if (authority.isEmpty()) "$scheme://<host>" else "$scheme://$authority"
}
