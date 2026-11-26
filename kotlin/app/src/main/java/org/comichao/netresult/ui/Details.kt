package org.comichao.netresult.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.comichao.netresult.R
import org.comichao.netresult.data.Game
import org.comichao.netresult.isDoubles

@Composable
fun surfaceLabel(key: String) = stringResource(when (key) {
    "hard" -> R.string.surface_hard; "clay" -> R.string.surface_clay; "grass" -> R.string.surface_grass
    "carpet" -> R.string.surface_carpet; else -> R.string.surface_other })

@Composable
fun settingLabel(key: String) = stringResource(if (key == "indoor") R.string.setting_indoor else R.string.setting_outdoor)

@Composable
fun weatherLabel(key: String) = stringResource(when (key) {
    "sunny" -> R.string.weather_sunny; "cloudy" -> R.string.weather_cloudy; "rain" -> R.string.weather_rain
    "wind" -> R.string.weather_wind; "hot" -> R.string.weather_hot; else -> R.string.weather_cold })

/** One line with what is known about a match besides its score: "Clay · Outdoor · 18:30 · 90 min · Sunny 22°C · Central courts". */
@Composable
fun detailsLine(g: Game): String {
    val parts = listOfNotNull(
        g.surface?.let { surfaceLabel(it) },
        g.setting?.let { settingLabel(it) },
        g.startTime,
        g.duration?.let { stringResource(R.string.minutes_short, it) },
        listOfNotNull(g.weather?.let { weatherLabel(it) }, g.temp?.let { "$it°C" }).joinToString(" ").ifEmpty { null },
        g.venue?.takeIf { it.isNotBlank() },
    )
    return parts.joinToString(" · ")
}

/** "with Marco" under the opponents of a doubles match; null for singles or when no partner was entered. */
@Composable
fun partnerLine(g: Game): String? = if (g.isDoubles && !g.partner.isNullOrBlank()) stringResource(R.string.with_partner, g.partner) else null
