package models

import java.io.Serializable

data class Subscriber(val chatId: String,
                      val username: String,
                      val substrings: MutableSet<String> = mutableSetOf()) : Serializable