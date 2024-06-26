import models.News
import newsproviders.NewsProvider
import utils.Logger
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class NewsNotifierServer(private val bot: NewsNotifierBot) {

    // constants:
    companion object {
        private val loggerId = "server"
        private val MAX_NEW_NEWS = 5
        private val ADMIN_PINGING_PERIOD: Long = 60 // minutes
        private val THREADS = 2
    }

    private val tpe = ScheduledThreadPoolExecutor(THREADS)

    private val dataProvider = NewsProvider()
    private var cache = mutableListOf<MutableSet<News>>()

    init {
        (1..utils.newsSources.size).forEach({ cache.add(mutableSetOf()) })
    }


    fun checkNewNews() {
        Logger.log(loggerId, "checkNewNews called")

        val allNewNews = dataProvider.getCurrentNews(utils.newsSources)

        // if cache is empty, let's initialize it
        if (cache.stream().allMatch({ it.isEmpty() })) {
            cache.forEachIndexed({ i, set -> set.addAll(allNewNews[i]) })
            Logger.log(loggerId, "Cache initialized and not announced")
            return
        }

        val newCache = mutableListOf<MutableSet<News>>()
        var newNews = mutableListOf<MutableList<News>>()

        for (i in 0 until allNewNews.size) {
            newNews.add(mutableListOf())
            newCache.add(mutableSetOf())
            allNewNews[i].forEach({
                newCache[i].add(it)
                if (!cache[i].contains(it)) {
                    newNews[i].add(it)
                }
            })
        }
        cache = newCache // update cache

        //don't send too much data
        newNews = reduceNewNewsSize(newNews)
        bot.announceNewData(newNews)
    }

    fun getLatestNews(): News? {
        return cache.flatMap { it }.maxBy { it.pubDate }
    }

    /**
     * Reduces size if needed
     */
    private fun reduceNewNewsSize(newNewsToCheck: MutableList<MutableList<News>>): MutableList<MutableList<News>> {
        var newNews = newNewsToCheck
        if (newNews.stream().map { it.size }.reduce({ a, b -> a + b }).get() > MAX_NEW_NEWS) {
            val lessNews = mutableListOf<MutableList<News>>()
            var curSize = 0
            outer@ for (l in newNews) {
                val newList = mutableListOf<News>()
                lessNews.add(newList)
                for (news in l) {
                    newList.add(news)
                    curSize++
                    if (curSize >= MAX_NEW_NEWS) {
                        break@outer
                    }
                }
            }
            newNews = lessNews
        }
        return newNews
    }

    fun informAboutBeingActive() {
        utils.admins.forEach {
            bot.subscribersDispatcher.directMessage(bot, it,
                    "Bot is still being active, don't worry. Users are: ${bot.subscribersDispatcher.getSubscribersString()}")
        }
    }

    /**
     * Start repeating calls to provide new news to bot
     */
    fun start(newsUpdatePeriodSeconds: Long) {
        fun scheduleTpe(f: () -> Unit, period: Long, timeUnit: TimeUnit) =
                tpe.scheduleAtFixedRate({
                    try {
                        f()
                    } catch (t: Throwable) {
                        Logger.logException(t)
                    }
                }, 0, period, timeUnit)

        Logger.log(loggerId, "server started with period: $newsUpdatePeriodSeconds")
        scheduleTpe({ checkNewNews() }, newsUpdatePeriodSeconds, TimeUnit.SECONDS)
        scheduleTpe({ informAboutBeingActive() }, ADMIN_PINGING_PERIOD, TimeUnit.MINUTES)
    }

    fun stop() {
        tpe.shutdown()
    }
}