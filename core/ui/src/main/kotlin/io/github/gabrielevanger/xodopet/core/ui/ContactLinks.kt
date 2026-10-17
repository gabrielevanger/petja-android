package io.github.gabrielevanger.xodopet.core.ui

/** Link do WhatsApp para um número com DDI e DDD (ex.: 5595900000001). Abre o app ou o site. */
fun whatsappLink(number: String): String = "https://wa.me/${number.filter(Char::isDigit)}"

/** Link de ligação: só os dígitos, para o discador aceitar "(95) 3000-0001". */
fun phoneLink(number: String): String = "tel:${number.filter(Char::isDigit)}"
