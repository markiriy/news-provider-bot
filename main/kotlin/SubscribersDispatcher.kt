import models.News
import models.Subscriber
import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.objects.Update
import utils.Logger
import java.io.*
import java.util.regex.Pattern


class SubscribersDispatcher {

    private val subscribers: MutableMap<String, Subscriber> // chatId -> subscriber
    private val onboardingSubscribers = mutableMapOf<String, Subscriber>() // chatId -> subscriber

    init {
        var subs: MutableMap<String, Subscriber> = mutableMapOf()
        try {
            ObjectInputStream(FileInputStream("stored-subscribers"))
                .use { subs = it.readObject() as MutableMap<String, Subscriber> }
        } catch (e: Exception) {
            Logger.logException(e)
        }
        subscribers = subs
    }

    private fun storeSubscribers() {
        try {
            ObjectOutputStream(FileOutputStream("stored-subscribers")).use { it.writeObject(subscribers) }
        } catch (e: Exception) {
            Logger.logException(e)
        }
    }

    /**
     * Add new subscriber and return true if it was added
     */
    @Synchronized
    fun addSubscriber(chatId: String, name: String): Boolean {
        if (subscribers.containsKey(chatId)) {
            return false
        }

        val sub = Subscriber(chatId, name)
        subscribers[chatId] = sub
        onboardingSubscribers[chatId] = sub
        storeSubscribers()
        return true
    }

    /**
     * Return true if subscriber was removed
     */
    @Synchronized
    fun removeSubscriber(chatId: String): Boolean {
        if (!subscribers.containsKey(chatId)) {
            return false
        }
        onboardingSubscribers.remove(chatId)
        subscribers.remove(chatId)
        storeSubscribers()
        return true
    }

    @Synchronized
    fun sendAll(bot: NewsNotifierBot, text: String) {
        subscribers.forEach { (_, sub) -> bot.sendMessage(sub.chatId, sub.username, text) }
    }

    @Synchronized
    fun sendAll(bot: NewsNotifierBot, newNews: List<List<News>>) {
        for ((_, subscriber) in subscribers) {
            if (subscriber.chatId in onboardingSubscribers) {
                bot.sendMessage(subscriber.chatId, subscriber.username, utils.onboardingMessage)
            }

            for (newsFromOneAddress in newNews) {
                if (subscriber.substrings.isEmpty()) {
                    newsFromOneAddress.forEach {
                        bot.sendMessage(subscriber.chatId, subscriber.username, it.getMessage())
                    }
                } else {
                    val metSubstrings = mutableMapOf<News, String>() // новость -> первое встреченное ключевое слово
                    newsFromOneAddress.forEach { news ->
                        subscriber.substrings.forEach { substring ->
                            val regex = "\\b${Pattern.quote(substring)}\\b".toRegex(RegexOption.IGNORE_CASE)
                            if (regex.containsMatchIn(news.title) || regex.containsMatchIn(news.description)) {
                                metSubstrings[news] = substring
                            }
                        }
                    }
                    // все отфильтрованные новости и соответствующие ключевые слова
                    for ((news, substring) in metSubstrings) {
                        bot.sendMessage(subscriber.chatId, subscriber.username, news.getMessage(substring))
                    }
                }
            }
        }
        onboardingSubscribers.clear()
    }

    @Synchronized
    fun getSubscribersString(): String {
        val sb = StringBuilder()
        sb.append("[")
        for ((_, sub) in subscribers) {
            sb.append("${sub.username}, ")
        }
        if (sb.length > 2) {
            sb.delete(sb.length - 2, sb.length)
        }
        sb.append("]")
        return sb.toString()
    }

    @Synchronized
    fun listSubstrings(bot: NewsNotifierBot, update: Update) {
        val substrings = subscribers[update.message.chatId.toString()]?.substrings?.toString() ?: return
        bot.sendMessage(update, "Ваши ключевые слова:\n$substrings")
    }

    @Synchronized
    fun addSubstring(chatId: String, str: String) {
        subscribers[chatId]?.substrings?.add(str)
        storeSubscribers()
    }

    @Synchronized
    fun removeSubstring(chatId: String, str: String) {
        subscribers[chatId]?.substrings?.remove(str)
        storeSubscribers()
    }

    @Synchronized
    fun removeAllSubstrings(chatId: String) {
        subscribers[chatId]?.substrings?.clear()
        storeSubscribers()
    }

    /**
     * Find chat for user with provided username and send text to it
     */
    @Synchronized
    fun directMessage(bot: NewsNotifierBot, username: String, text: String) {
        var neededChatId: String? = null
        for ((chatId, sub) in subscribers) {
            if (sub.username == username) {
                neededChatId = chatId
            }
        }
        if (neededChatId == null) {
            return
        }
        bot.sendMessage(neededChatId, username, text)
    }
}