package net.arwix.location.data

//import org.threeten.bp.DateTimeUtils
//import org.threeten.bp.Instant
//import org.threeten.bp.ZoneId
//import org.threeten.bp.ZonedDateTime
//import org.threeten.bp.format.DateTimeFormatter
import android.content.Context
import android.icu.text.TimeZoneNames
import android.os.Build
import androidx.annotation.RequiresApi
import net.arwix.location.R
import org.xmlpull.v1.XmlPullParser
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern


class TimeZoneRepository(private val applicationContext: Context) {
    private val listTimeZones: List<ZoneId>

    init {
        listTimeZones = readTimezonesToDisplay()
    }

    fun getZonesList(instant: Instant = Instant.now()): List<TimeZoneDisplayEntry> = listTimeZones
        .map { getDisplayEntry(it, instant) }
        .sortedBy { it.zoneOffset.totalSeconds }

    private fun readTimezonesToDisplay(): List<ZoneId> =
        applicationContext.resources.runCatching {
            getXml(R.xml.timezones).use {
                val entries = mutableListOf<ZoneId>()
                while (it.next() != XmlPullParser.END_DOCUMENT) {
                    if (it.eventType != XmlPullParser.START_TAG) continue
                    if (it.name == "timezone")
                        it.getAttributeValue(0)
                            .runCatching(ZoneId::of)
                            .getOrNull()
                            ?.run(entries::add)
                }
                entries
            }
        }.getOrNull() ?: listOf()


    companion object {

        @Suppress("MemberVisibilityCanBePrivate")
        fun getDisplayEntry(zoneId: ZoneId, now: Instant): TimeZoneDisplayEntry {
            val offset = zoneId.rules.getOffset(now)
            val isLight = zoneId.rules.isDaylightSavings(now)
            val name = getName(zoneId)
            val longName = getLongName(zoneId, now, isLight)
            return TimeZoneDisplayEntry(
                zoneId,
                offset,
                name,
                longName,
                getGmtOffsetText(zoneId, now)
            )
        }

        fun getGmtOffsetText(
            zoneId: ZoneId,
            instant: Instant = Instant.now()
        ) = buildString {
            append("GMT")
            append(zoneId.rules.getOffset(instant).id.takeIf { it != "Z" } ?: "+00:00")
        }

        fun getName(zoneId: ZoneId): String {
            val name =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    getName(TimeZoneNames.getInstance(Locale.getDefault()), zoneId)
                        ?: getDefaultExemplarLocationName(zoneId.id)
                } else {
                    getDefaultExemplarLocationName(zoneId.id)
                }
            return name ?: ""
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getName(names: TimeZoneNames, zoneId: ZoneId): String? =
            names.getExemplarLocationName(getCanonicalID(zoneId))

        @Suppress("MemberVisibilityCanBePrivate")
        fun getLongName(
            zoneId: ZoneId,
            now: Instant,
            isLight: Boolean
        ): String {
            val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getLongName(TimeZoneNames.getInstance(Locale.getDefault()), zoneId, now, isLight)
                    ?: getLongName(zoneId, now)
            } else getLongName(zoneId, now)
            if (name.contains("GMT")) return getDefaultExemplarLocationName(zoneId.id) ?: ""
            return name
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun getLongName(
            names: TimeZoneNames,
            zoneId: ZoneId,
            now: Instant,
            isLight: Boolean
        ): String? {
            val nameType =
                if (isLight) TimeZoneNames.NameType.LONG_DAYLIGHT else TimeZoneNames.NameType.LONG_STANDARD
            return names.getDisplayName(zoneId.id, nameType, Date.from(now).time)
                ?: names.getDisplayName(
                    getCanonicalID(zoneId),
                    nameType,
                    Date.from(now).time
                )
        }

        fun getLongName(zoneId: ZoneId, now: Instant): String {
            val f = DateTimeFormatter.ofPattern("zzzz", Locale.getDefault())
            return ZonedDateTime.ofInstant(now, zoneId).format(f)
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun getCanonicalID(zoneId: ZoneId): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.icu.util.TimeZone.getCanonicalID(zoneId.id) ?: zoneId.id
            } else zoneId.id


        private val LOC_EXCLUSION_PATTERN = Pattern.compile("SystemV/.*|.*/Riyadh8[7-9]")

        /**
         * Default exemplar location name based on time zone ID.
         * For example, "America/New_York" -> "New York"
         * @param tzID the time zone ID
         * @return the exemplar location name or null if location is not available.
         */
        private fun getDefaultExemplarLocationName(tzID: String): String? {
            if (tzID.isEmpty() || LOC_EXCLUSION_PATTERN.matcher(tzID).matches()) {
                return null
            }
            var location: String? = null
            val sep = tzID.lastIndexOf('/')
            if (sep > 0 && sep + 1 < tzID.length) {
                location = tzID.substring(sep + 1).replace('_', ' ')
            }
            return location
        }
    }

}