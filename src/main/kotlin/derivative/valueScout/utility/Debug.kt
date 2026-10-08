package derivative.valueScout.utility

import java.io.File
import java.time.LocalTime

class Debug {
    companion object {
        private const val CYAN = "\u001B[36m"
        private const val MAGENTA = "\u001B[35m"
        private const val BLUE = "\u001B[34m"
        private const val YELLOW = "\u001B[33m"
        private const val GREEN = "\u001B[32m"
        private const val RED = "\u001B[31m"


        private val file = File("debugLog.txt")
        init {
            file.writeText("Session start - ${LocalTime.now()}\n")
        }

        fun log(msg: String) {
            println(CYAN + msg)
            file.appendText("${LocalTime.now()} - LOG: $msg")
        }

        fun error(msg: String) {
            println(RED + msg)
            file.appendText("${LocalTime.now()} - ERROR: $msg")
        }
    }
}