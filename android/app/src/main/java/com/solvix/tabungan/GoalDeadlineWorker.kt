package com.solvix.tabungan

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class GoalDeadlineWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val prefs = applicationContext.getSharedPreferences("tabungan_prefs", Context.MODE_PRIVATE)
    val username = prefs.getString("saved_username", "") ?: ""
    val password = prefs.getString("saved_password", "") ?: ""
    if (username.isBlank() || password.isBlank()) return Result.success()
    if (!canPostNotifications()) return Result.success()

    val language = if (prefs.getString("app_language", "EN") == "ID") AppLanguage.ID else AppLanguage.EN
    val strings = stringsFor(language)

    return try {
      val user = fetchUserByCredentials(username, password) ?: return Result.success()
      val moneyRows = SupabaseClient.client
        .from("money_entries")
        .select {
          filter { eq("user_id", user.id) }
        }
        .decodeList<SupabaseMoneyEntry>()
      val incomeTotal = moneyRows
        .filter { it.type == EntryType.Income.name }
        .sumOf { it.amount }

      val goals = SupabaseClient.client
        .from("dream_entries")
        .select {
          filter { eq("user_id", user.id) }
        }
        .decodeList<SupabaseDreamEntry>()

      if (goals.isEmpty()) return Result.success()

      val today = LocalDate.now(JAKARTA_ZONE)
      val notifyDays = setOf(30L, 7L, 1L, 0L)

      goals.forEach { goal ->
        if (goal.target <= 0) return@forEach
        val deadline = parseDeadlineLocalDate(goal.deadline) ?: return@forEach
        val daysLeft = ChronoUnit.DAYS.between(today, deadline)
        if (daysLeft !in notifyDays) return@forEach

        val markerKey = "goal_deadline_notified_${user.id}_${goal.id}_$daysLeft"
        val todayKey = today.toString()
        if (prefs.getString(markerKey, "") == todayKey) return@forEach

        val clampedProgress = incomeTotal.coerceAtMost(goal.target)
        val percent = if (goal.target > 0) {
          ((clampedProgress.toFloat() / goal.target.toFloat()) * 100f).roundToInt()
        } else {
          0
        }
        val dayLabel = if (daysLeft == 0L) strings["deadline_today"] else "H-$daysLeft"
        val body = strings["goal_deadline_body"]
          .replace("{title}", goal.title.ifBlank { strings["label_goal"] })
          .replace("{days}", dayLabel)
          .replace("{current}", formatRupiah(clampedProgress))
          .replace("{target}", formatRupiah(goal.target))
          .replace("{percent}", percent.toString())

        showNotification(
          notificationId = goal.id.hashCode(),
          title = strings["goal_deadline_title"],
          body = body,
        )
        prefs.edit { putString(markerKey, todayKey) }
      }

      Result.success()
    } catch (ex: Exception) {
      Result.retry()
    }
  }

  private suspend fun fetchUserByCredentials(username: String, password: String): SupabaseUser? {
    val response = SupabaseClient.client
      .from("users")
      .select {
        filter {
          eq("username", username)
          eq("password", password)
        }
        limit(1)
      }
      .decodeList<SupabaseUser>()
    return response.firstOrNull()
  }

  private fun parseDeadlineLocalDate(text: String): LocalDate? {
    if (text.isBlank()) return null
    val patterns = listOf("yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy")
    for (pattern in patterns) {
      try {
        return LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern))
      } catch (_: Exception) {
        // try next pattern
      }
    }
    return null
  }

  private fun canPostNotifications(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
  }

  private fun showNotification(notificationId: Int, title: String, body: String) {
    if (!canPostNotifications()) return
    ensureChannel()
    val notification = NotificationCompat.Builder(applicationContext, GOAL_DEADLINE_CHANNEL_ID)
      .setSmallIcon(R.drawable.logo2)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setAutoCancel(true)
      .build()
    try {
      NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Permission might be revoked while worker is running.
    }
  }

  private fun ensureChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val existing = manager.getNotificationChannel(GOAL_DEADLINE_CHANNEL_ID)
    if (existing != null) return
    val channel = NotificationChannel(
      GOAL_DEADLINE_CHANNEL_ID,
      "Goal Deadline",
      NotificationManager.IMPORTANCE_DEFAULT,
    )
    channel.description = "Goal deadline reminders"
    manager.createNotificationChannel(channel)
  }

  companion object {
    private const val GOAL_DEADLINE_CHANNEL_ID = "goal_deadline_channel"
    private val JAKARTA_ZONE = ZoneId.of("Asia/Jakarta")
  }
}
