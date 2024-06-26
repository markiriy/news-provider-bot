package utils

val newsSources = listOf(
        "https://habr.com/ru/rss/articles/top/?fl=ru",
        "https://feeds.a.dj.com/rss/RSSWSJD.xml",
        "https://www.nasa.gov/feeds/iotd-feed",
        "https://www.nasa.gov/feed/",
        "https://www.nasa.gov/missions/artemis/feed/",
        "https://www.nasa.gov/missions/station/feed/",
        "https://feeds.a.dj.com/rss/RSSLifestyle.xml",
        "https://news.rambler.ru/rss/world/",
        "https://news.rambler.ru/rss/tech/",
        "https://lenta.ru/rss/news"
)

val helpMessage = """
        Бот будет уведомлять вас о новых новостях, которые публикуются в списке источников.
        Текущий список источников:
        $newsSources.

        Вы можете выбрать интересующие вас новости, предоставив боту список ключевых слов. Вам будут отправлены только новости, содержащие любое из них.
        Если ключевые слова не указаны, вам будут отправлены все новости.

        Команды:
        /s = подписаться на рассылку
        /u = отписаться от рассылки
        /ls = список текущих ключевых слов
        /as "СЛОВО" = добавить ключевое слово
        /rs = удалить ключевое слово
        /rmrf = очистить список ключевых слов
        /help = показать инструкцию по использованию
        /save = сохранить последнюю новость
        /showsaved = показать все сохраненные новости
        /delsaved НОМЕР = удалить новость по ее порядковому номеру в сохраненных, начинается с 0

        Все ключевые слова должны быть в кавычках. Регистр ключевых слов не учитывается.
        Пример:
        /as "Россия"
        /as "YNDX"
        тогда
        /ls выведет
        [россия, yndx]

    """.trimIndent()

val onboardingMessage = """
        Спасибо за использование бота!

        Вы подписаны на этот список новостных источников:
        $newsSources

        Начиная с этого сообщения и до тех пор, пока вы не откажетесь от подписки, вы будете получать сообщения о появлении новых новостей, соответствующих вашим интересам
    """.trimIndent()

val admins = listOf(
        "admin"
)

val detailed_logging = listOf(
        "chatid"
)