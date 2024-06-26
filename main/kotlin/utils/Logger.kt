package utils

import org.telegram.telegrambots.api.objects.Update
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Logger {
    companion object {
        private val logWriter = PrintWriter(OutputStreamWriter(FileOutputStream("botLog-${getStringTime()}.txt")))
        private val SEVERE_TAG = "~SEVERE"

        fun getStringTime() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd--kk-mm"))

        @Synchronized
        fun log(update: Update, text: String) = log(update.message.from.userName, text)

        @Synchronized
        fun log(text: String) = log("????????????????????", text)

        @Synchronized
        fun log(username: String, text: String) {
            val msg = "${getStringTime()} -- ${String.format("%-30s", username)} -- $text"
            logWriter.println(msg)
            logWriter.flush()
            println(msg)
        }

        @Synchronized
        fun logException(t: Throwable) {
            log(SEVERE_TAG, "$t. Stack trace: ${Arrays.toString(t.stackTrace)}")
        }
    }
}

