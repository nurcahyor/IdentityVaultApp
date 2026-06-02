package com.identityvault.app.root

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class RootShell {
    data class CommandResult(val exitCode: Int, val output: String, val error: String)

    fun run(command: String, timeoutSeconds: Long = 6): CommandResult {
        return runProcess(listOf("su", "-c", command), timeoutSeconds)
    }

    fun runPlain(command: String, timeoutSeconds: Long = 4): CommandResult {
        val parts = command.split(" ").filter { it.isNotBlank() }
        return runProcess(parts, timeoutSeconds)
    }

    fun hasRoot(): Boolean {
        val result = run("id", 3)
        return result.exitCode == 0 && result.output.contains("uid=0")
    }

    private fun runProcess(args: List<String>, timeoutSeconds: Long): CommandResult {
        return try {
            val process = ProcessBuilder(args).redirectErrorStream(false).start()
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return CommandResult(-1, "", "Command timed out")
            }
            CommandResult(
                exitCode = process.exitValue(),
                output = process.inputStream.readTextSafe(),
                error = process.errorStream.readTextSafe()
            )
        } catch (e: Exception) {
            CommandResult(-1, "", e.message ?: "Command failed")
        }
    }

    private fun java.io.InputStream.readTextSafe(): String {
        return BufferedReader(InputStreamReader(this)).use { reader ->
            val builder = StringBuilder()
            var line = reader.readLine()
            while (line != null) {
                builder.append(line).append('\n')
                line = reader.readLine()
            }
            builder.toString().trim()
        }
    }
}
