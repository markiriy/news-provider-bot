import models.News
import utils.Logger.Companion.log
import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.objects.EntityType
import org.telegram.telegrambots.api.objects.Update
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import utils.Logger
import utils.Logger.Companion.logException
import utils.detailed_logging
import java.util.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import newsproviders.NewsProvider
import java.text.SimpleDateFormat

class NewsNotifierBot : TelegramLongPollingBot() {
    val subscribersDispatcher = SubscribersDispatcher()

    override fun getBotToken() = "6839272214:AAGu13Bv3CAC9cgwdTiDjQ7jGzvGzZwSIB8"
    override fun getBotUsername() = "Feedlyanalog_bot"

    private fun saveNewsToFile(username: String, newsTitle: String, newsLink: String) {
        val fileName = "$username.txt"
        val file = File(fileName)
        val isNewFile = !file.exists()

        val writer = BufferedWriter(FileWriter(file, true))
        if (isNewFile) {
            writer.write("$newsTitle $newsLink\n")
        } else {
            writer.append("$newsTitle $newsLink\n")
        }
        writer.close()
    }

    override fun onUpdateReceived(update: Update?) {
        try {
            if (update == null) {
                log("~WARNING: received onUpdate call with null")
                return
            }

            log(update, update.message.text)

            if (update.message.isCommand) {
                when (update.message.entities.first { it.type == EntityType.BOTCOMMAND }.text) {
                    "/s" -> subscribe(update)
                    "/u" -> unsubscribe(update)
                    "/lsu" -> listSubscribers(update)
                    "/help" -> help(update)
                    "/start" -> sendMessage(update, "NewsAgg_bot собирает новости из различных RSS-источников, " +
                            "предоставляя подборку заголовков и статей прямо в ваш Telegram-чат. " +
                            "Будьте в курсе текущих событий, технических новинок, новостей бизнеса " +
                            "и многого другого в одном месте \uD83C\uDF0D\n" +
                            "\n" +
                            "Подпишитесь на любимые темы и получайте своевременные обновления новостей \uD83D\uDCE8 \n" +
                            "Для инструкции по использованию введите команду /help")
                    "/ls" -> listSubstrings(update)
                    "/as" -> addSubstring(update)
                    "/rs" -> removeSubstring(update)
                    "/rmrf" -> removeAllSubstrings(update)
                    "/save" -> saveLastNews(update)
                    "/showsaved" -> showSavedNews(update)
                    "/delsaved" -> deleteSavedNews(update)
                }
            }
        } catch (t: Throwable) {
            logException(t)
        }
    }

    private fun saveLastNews(update: Update) {
        val newsProvider = NewsProvider()
        val allNews = newsProvider.getCurrentNews(utils.newsSources).flatten()

        if (allNews.isNotEmpty()) {
            // Sort news by publication date in descending order and get the most recent news
            val mostRecentNews = allNews.maxBy { parsePubDate(it.pubDate) }

            if (mostRecentNews != null) {
                val newsTitle = mostRecentNews.title
                val newsLink = mostRecentNews.link

                val username = update.message.from.userName ?: "UnknownUser"
                saveNewsToFile(username, newsTitle, newsLink)

                sendMessage(update, "Новость сохранена")
            } else {
                sendMessage(update, "Нет доступных новостей для сохранения")
            }
        } else {
            sendMessage(update, "Нет доступных новостей для сохранения")
        }
    }

    // Function to parse the publication date
    private fun parsePubDate(pubDate: String): Date {
        val dateFormats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",  // Example: "Thu, 16 May 2024 18:26:00 GMT"
            "EEE, dd MMM yyyy HH:mm:ss ZZZZ",  // Example: "Thu, 16 May 2024 18:26:00 +0300"
            "yyyy-MM-dd'T'HH:mm:ss'Z'"  // Example: "2024-05-16T18:26:00Z"
        )

        for (format in dateFormats) {
            try {
                return SimpleDateFormat(format, Locale.ENGLISH).parse(pubDate)
            } catch (e: Exception) {
                // Ignore and try the next format
            }
        }

        return Date(0)  // Return epoch time if parsing fails
    }

    // /showsaved command
    private fun showSavedNews(update: Update) {
        val username = update.message.from.userName ?: "UnknownUser"
        val fileName = "$username.txt"
        val file = File(fileName)

        val savedNews = if (file.exists()) {
            file.readLines().mapIndexed { index, line -> "$index. $line" }
        } else {
            listOf("No saved news")
        }

        sendMessage(update, ("Сохраненные новости:\n" + savedNews.joinToString("\n")))
    }

    // /delsaved command
    private fun deleteSavedNews(update: Update) {
        val username = update.message.from.userName ?: "UnknownUser"
        val fileName = "$username.txt"
        val file = File(fileName)

        val indexToDelete = update.message.text.substringAfter(" ").toIntOrNull() ?: return
        val savedNews = file.readLines().toMutableList()

        if (indexToDelete in savedNews.indices) {
            savedNews.removeAt(indexToDelete)
            file.writeText(savedNews.mapIndexed { index, news -> "$index. ${news.substringAfter(". ")}" }.joinToString("\n"))
            sendMessage(update, "Новость удалена")
        } else {
            sendMessage(update, "Неверный номер новости")
        }
    }

    private fun subscribe(update: Update) {
        val subscribed = subscribersDispatcher.addSubscriber(update.message.chatId.toString(), update.message.from.userName)
        log(update.message.from.userName, "subscribed ${if (!subscribed) "though was already" else ""}")
        sendMessage(update, "Вы подписались на рассылку")
    }

    private fun unsubscribe(update: Update) {
        val unsubscribed = subscribersDispatcher.removeSubscriber(update.message.chat.id.toString())
        log(update.message.from.userName, "unsubscribed ${if (!unsubscribed) "though wasn't subscribed" else ""}")
        sendMessage(update, "Вы отписались от рассылки")
    }

    private fun listSubscribers(update: Update) {
        sendMessage(update, subscribersDispatcher.getSubscribersString())
    }

    @Synchronized
    fun announceNewData(newNews: List<List<News>>) {
        log("For clients ${subscribersDispatcher.getSubscribersString()} ${newNews.stream().map { it.size }.reduce({ a, b -> a + b }).get()} new news are dispatched")
        subscribersDispatcher.sendAll(this, newNews)
    }

    private fun help(update: Update) = sendMessage(update, utils.helpMessage)
    private fun listSubstrings(update: Update) = subscribersDispatcher.listSubstrings(this, update)

    /**
     * exception
     */
    private fun extractSubstring(update: Update): String {
        val msg = update.message.text
        if (msg.count { it == '\"' } != 2) {
            throw IllegalArgumentException("В ключевом слове должно быть только две кавычки - в начале и в конце")
        }

        val suffix = msg.subSequence(msg.indexOfFirst { it == '\"' } + 1, msg.length)
        return suffix.subSequence(0, suffix.indexOfFirst { it == '\"' }).toString().toLowerCase()
    }

    private fun addSubstring(update: Update) {
        try {
            val ss = extractSubstring(update)
            subscribersDispatcher.addSubstring(update.message.chatId.toString(), ss)
        } catch (e: IllegalArgumentException) {
            sendMessage(update, "Ошибка: " + e.message)
        }
    }

    private fun removeSubstring(update: Update) {
        try {
            val ss = extractSubstring(update)
            subscribersDispatcher.removeSubstring(update.message.chatId.toString(), ss)
        } catch (e: IllegalArgumentException) {
            sendMessage(update, "Ошибка: " + e.message)
        }
    }

    private fun removeAllSubstrings(update: Update) = subscribersDispatcher.removeAllSubstrings(update.message.chatId.toString())

    fun sendMessage(update: Update, text: String) {
        sendMessage(update.message.chatId.toString(), update.message.from.userName, text)
    }

    // logging
    fun sendMessage(chatId: String, username: String, text: String) {
        if (username in detailed_logging) {
            log(username, "Message sent: $text")
        }
        sendMessageWithoutLogging(chatId, text)
    }


    private fun sendMessageWithoutLogging(chatId: String, text: String) {
        sendMessage(SendMessage(chatId, text))
    }
}