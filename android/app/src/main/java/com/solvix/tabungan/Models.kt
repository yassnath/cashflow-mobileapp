package com.solvix.tabungan

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

enum class EntryType { Income, Expense }

data class MoneyEntry(
  val id: String = UUID.randomUUID().toString(),
  val type: EntryType,
  val amount: Int,
  val date: String,
  val category: String,
  val note: String,
  val sourceOrMethod: String,
  val channelOrBank: String,
)

data class SavingEntry(
  val id: String = UUID.randomUUID().toString(),
  val amount: Int,
  val date: String,
  val goal: String,
  val note: String,
)

data class DreamEntry(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val target: Int,
  val current: Int,
  val deadline: String,
  val note: String,
)

data class UserProfile(
  val id: String = "",
  val name: String,
  val email: String,
  val country: String,
  val birthdate: String,
  val bio: String,
  val createdAt: String,
  val username: String,
  val password: String,
)

fun formatRupiah(value: Int): String {
  val text = value.toLong().toString()
  val builder = StringBuilder()
  var count = 0
  for (i in text.length - 1 downTo 0) {
    builder.append(text[i])
    count++
    if (count == 3 && i != 0) {
      builder.append('.')
      count = 0
    }
  }
  return "Rp " + builder.reverse().toString()
}

fun parseAmount(text: String): Int {
  return text.filter { it.isDigit() }.toIntOrNull() ?: 0
}

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

private fun startOfDay(millis: Long): Long {
  val cal = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  return cal.timeInMillis
}

fun parseDate(text: String): Long? {
  if (text.isBlank()) return null
  return try {
    val formats = listOf(
      SimpleDateFormat("yyyy-MM-dd", Locale.US),
      SimpleDateFormat("dd-MM-yyyy", Locale.US),
      SimpleDateFormat("dd/MM/yyyy", Locale.US),
    )
    val date = formats.firstNotNullOfOrNull { format ->
      try {
        format.isLenient = false
        format.parse(text)
      } catch (ex: ParseException) {
        null
      }
    } ?: return null
    startOfDay(date.time)
  } catch (ex: Exception) {
    null
  }
}

fun toDbDate(text: String): String {
  val millis = parseDate(text) ?: return text
  return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
}

fun toUiDate(text: String): String {
  val millis = parseDate(text) ?: return text
  return SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(millis))
}

private val JAKARTA_ZONE = ZoneId.of("Asia/Jakarta")
private val CREATED_AT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
private val CREATED_AT_PARSER = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).apply {
  isLenient = false
  timeZone = TimeZone.getTimeZone("Asia/Jakarta")
}

fun nowJakartaText(): String {
  return CREATED_AT_FORMATTER.format(Instant.now().atZone(JAKARTA_ZONE))
}

fun parseCreatedAtMillis(text: String): Long? {
  if (text.isBlank()) return null
  return try {
    Instant.parse(text).toEpochMilli()
  } catch (ex: Exception) {
    try {
      CREATED_AT_PARSER.parse(text)?.time
    } catch (ex: Exception) {
      null
    }
  }
}

fun formatCreatedAt(text: String): String {
  if (text.isBlank()) return "-"
  return try {
    val instant = Instant.parse(text)
    CREATED_AT_FORMATTER.format(instant.atZone(JAKARTA_ZONE))
  } catch (ex: Exception) {
    val millis = parseCreatedAtMillis(text) ?: return text
    CREATED_AT_FORMATTER.format(Instant.ofEpochMilli(millis).atZone(JAKARTA_ZONE))
  }
}

enum class SummaryRange(val label: String) {
  Month(""),
  Today(""),
  Week(""),
  Year(""),
  All(""),
}

fun summaryRangeLabel(range: SummaryRange, strings: AppStrings): String {
  return when (range) {
    SummaryRange.Month -> strings["summary_range_month"]
    SummaryRange.Today -> strings["summary_range_today"]
    SummaryRange.Week -> strings["summary_range_week"]
    SummaryRange.Year -> strings["summary_range_year"]
    SummaryRange.All -> strings["summary_range_all"]
  }
}

fun filterByRange(entries: List<MoneyEntry>, range: SummaryRange, todayMillis: Long = startOfDay(System.currentTimeMillis())): List<MoneyEntry> {
  if (range == SummaryRange.All) return entries
  val todayCal = Calendar.getInstance().apply { timeInMillis = todayMillis }
  return entries.filter { entry ->
    val dateMillis = parseDate(entry.date) ?: return@filter false
    when (range) {
      SummaryRange.Today -> dateMillis == todayMillis
      SummaryRange.Week -> dateMillis >= (todayMillis - (6 * DAY_MILLIS))
      SummaryRange.Month -> {
        val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        dateCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
          dateCal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH)
      }
      SummaryRange.Year -> {
        val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        dateCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
      }
      SummaryRange.All -> true
    }
  }
}
