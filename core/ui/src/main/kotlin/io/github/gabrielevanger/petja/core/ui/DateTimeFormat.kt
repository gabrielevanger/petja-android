package io.github.gabrielevanger.petja.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Data e hora no fuso do aparelho (Boa Vista fica uma hora atrás de Brasília). */
fun formatDateTime(instant: Instant, pattern: String, zone: ZoneId, locale: Locale): String =
    DateTimeFormatter.ofPattern(pattern, locale).withZone(zone).format(instant)

/** "18/10 às 11:15", com o padrão do strings.xml e o fuso do aparelho. */
@Composable
fun formatDateTime(instant: Instant): String {
    val pattern = stringResource(R.string.date_time_pattern)
    val zone = remember { ZoneId.systemDefault() }
    return formatDateTime(instant, pattern, zone, Locale.forLanguageTag(stringResource(R.string.date_time_locale)))
}
