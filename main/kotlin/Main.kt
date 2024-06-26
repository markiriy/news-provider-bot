import org.telegram.telegrambots.exceptions.TelegramApiException
import org.telegram.telegrambots.TelegramBotsApi
import org.telegram.telegrambots.ApiContextInitializer

fun main(args: Array<String>) {
    ApiContextInitializer.init()
    val botsApi = TelegramBotsApi()

    val bot = NewsNotifierBot()

    try {
        botsApi.registerBot(bot)
    } catch (e: TelegramApiException) {
        e.printStackTrace()
    }

    val server = NewsNotifierServer(bot)
    server.start(30)
//    server.checkNewNews()
}
