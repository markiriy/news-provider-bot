package models

data class News(val title: String,
                val description: String,
                val link: String,
                val pubDate: String,
                val providerUrl: String) {

    fun getMessage(): String {
        val sb = StringBuilder()
        sb.append(title)
        sb.append("\n\n")
        sb.append("Опубликовано $pubDate. RSS Лентой: $providerUrl")
        sb.append("\n\n")
        sb.append(link)
        sb.append("\n\n")
        sb.append("$description")
        return sb.toString()
    }

    fun getMessage(encounteredSubstring: String): String {
        val sb = StringBuilder()
        sb.append("Найдено слово: $encounteredSubstring\n")
        sb.append(getMessage())
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }

        if (other !is News) {
            return false
        }

        return title == other.title
    }

    override fun hashCode(): Int {
        return title.hashCode()
    }
}