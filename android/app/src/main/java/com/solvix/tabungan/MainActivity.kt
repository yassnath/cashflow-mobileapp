package com.solvix.tabungan

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlin.math.roundToInt
import kotlinx.serialization.json.put
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      TabunganApp()
    }
  }
}

enum class Page(val label: String, val icon: String) {
  Income("Pemasukkan", "💸"),
  Expense("Pengeluaran", "🧾"),
  Dreams("Target", "🌟"),
  History("History", "📒"),
  Calculator("Kalkulator", "🧮"),
  Report("Laporan", "📈"),
  Profile("Profile", "👤"),
  Settings("Pengaturan", "⚙️"),
  Themes("Tema", "🎨"),
}

private enum class AuthTab { SignIn, SignUp }
private enum class LoadingTarget { Startup, Logout }

private const val GOAL_DEADLINE_WORK = "goal_deadline_worker"
private const val GOAL_REACHED_CHANNEL_ID = "goal_reached_channel"
private const val PREF_SELECTED_THEME = "selected_theme"

private data class GoalReachEvent(
  val goalId: String,
  val sourceType: String,
)

@Composable
fun TabunganApp() {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("tabungan_prefs", Context.MODE_PRIVATE) }
  val persistedTheme = remember {
    val storedTheme = prefs.getString(PREF_SELECTED_THEME, ThemeName.StandardLight.name)
    runCatching { ThemeName.valueOf(storedTheme ?: ThemeName.StandardLight.name) }.getOrDefault(ThemeName.StandardLight)
  }
  var currentTheme by rememberSaveable { mutableStateOf(persistedTheme) }
  var currentPage by rememberSaveable { mutableStateOf(Page.Income) }
  var summaryRange by rememberSaveable { mutableStateOf(SummaryRange.Month) }
  var showProfileMenu by remember { mutableStateOf(false) }
  var showSplash by rememberSaveable { mutableStateOf(false) }
  var showLoading by rememberSaveable { mutableStateOf(true) }
  var loadingFadeOut by rememberSaveable { mutableStateOf(false) }
  var loadingTarget by rememberSaveable { mutableStateOf(LoadingTarget.Startup) }
  var showAuth by rememberSaveable { mutableStateOf(false) }
  var authTab by rememberSaveable { mutableStateOf(AuthTab.SignIn) }
  var adminLoggedIn by rememberSaveable { mutableStateOf(false) }
  val adminUsers = remember { mutableStateListOf<SupabaseUser>() }
  var showConfirm by remember { mutableStateOf(false) }
  var confirmMessage by remember { mutableStateOf("") }
  var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var showAlert by remember { mutableStateOf(false) }
  var alertMessage by remember { mutableStateOf("") }
  var pendingEdit by remember { mutableStateOf<MoneyEntry?>(null) }
  var goalReachEvent by remember { mutableStateOf<GoalReachEvent?>(null) }
  LaunchedEffect(Unit) {
    prefs.edit { remove("face_unlock_enabled") }
  }
  val lifecycleOwner = LocalLifecycleOwner.current
  var fadeSeed by remember { mutableIntStateOf(0) }
  var pageFadeSeed by remember { mutableIntStateOf(0) }
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_START) {
        fadeSeed += 1
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  fun navigateTo(page: Page) {
    if (currentPage == page) {
      pageFadeSeed += 1
      return
    }
    currentPage = page
  }
  var toastVisible by remember { mutableStateOf(false) }
  var toastMessage by remember { mutableStateOf("") }
  val defaultLang = prefs.getString("app_language", "EN") ?: "EN"
  var currentLang by rememberSaveable {
    mutableStateOf(if (defaultLang == "ID") AppLanguage.ID else AppLanguage.EN)
  }
  var currentUser by remember { mutableStateOf<UserProfile?>(null) }
  val scope = rememberCoroutineScope()
  var isLoggedIn by remember { mutableStateOf(false) }
  LaunchedEffect(currentTheme) {
    prefs.edit { putString(PREF_SELECTED_THEME, currentTheme.name) }
  }
  LaunchedEffect(currentPage) {
    pageFadeSeed += 1
  }
  LaunchedEffect(showAuth, showSplash, isLoggedIn) {
    if (!showAuth && !showSplash && isLoggedIn) {
      pageFadeSeed += 1
    }
  }
  var hasSeenWelcome by remember { mutableStateOf(prefs.getBoolean("has_seen_welcome", false)) }
  var fingerprintEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("fingerprint_enabled", false)) }
  var biometricAllowed by rememberSaveable { mutableStateOf(prefs.getBoolean("biometric_allowed", false)) }
  var hasRegistered by rememberSaveable { mutableStateOf(prefs.getBoolean("has_registered", false)) }
  var savedUsername by rememberSaveable { mutableStateOf(prefs.getString("saved_username", "") ?: "") }
  var savedPassword by rememberSaveable { mutableStateOf(prefs.getString("saved_password", "") ?: "") }
  var biometricPrompted by rememberSaveable { mutableStateOf(false) }
  val biometricManager = remember { BiometricManager.from(context) }
  val keyguardManager = remember {
    context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
  }
  val executor = remember { ContextCompat.getMainExecutor(context) }

  val incomeEntries = remember { mutableStateListOf<MoneyEntry>() }
  val expenseEntries = remember { mutableStateListOf<MoneyEntry>() }
  val dreamEntries = remember { mutableStateListOf<DreamEntry>() }

  var signInUsername by rememberSaveable { mutableStateOf("") }
  var signInPassword by rememberSaveable { mutableStateOf("") }
  var signUpName by rememberSaveable { mutableStateOf("") }
  var signUpEmail by rememberSaveable { mutableStateOf("") }
  var signUpCountry by rememberSaveable { mutableStateOf("") }
  var signUpBirthdate by rememberSaveable { mutableStateOf("") }
  var signUpBio by rememberSaveable { mutableStateOf("") }
  var signUpUsername by rememberSaveable { mutableStateOf("") }
  var signUpPassword by rememberSaveable { mutableStateOf("") }
  val achievedGoalIds = remember { mutableStateListOf<String>() }
  var lastIncomeTotal by rememberSaveable { mutableIntStateOf(0) }
  var lastExpenseTotal by rememberSaveable { mutableIntStateOf(0) }
  var lastBalanceTotal by rememberSaveable { mutableIntStateOf(0) }

  val strings = stringsFor(currentLang)
  fun resolveStartYear(): Int {
    val fallback = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val createdAt = currentUser?.createdAt.orEmpty()
    if (createdAt.isBlank()) return fallback
    val millis = parseCreatedAtMillis(createdAt) ?: return fallback
    return Instant.ofEpochMilli(millis).atZone(ZoneId.of("Asia/Jakarta")).year
  }

  suspend fun fetchUserByCredentials(username: String, password: String): UserProfile? {
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
    val user = response.firstOrNull() ?: return null
    return UserProfile(
      id = user.id,
      name = user.name,
      email = user.email,
      country = user.country,
      birthdate = toUiDate(user.birthdate),
      bio = user.bio.orEmpty(),
      createdAt = user.createdAt,
      username = user.username,
      password = user.password,
    )
  }

  suspend fun userExists(username: String, email: String): Boolean {
    val usernameHit = SupabaseClient.client
      .from("users")
      .select {
        filter { eq("username", username) }
        limit(1)
      }
      .decodeList<SupabaseUser>()
      .isNotEmpty()
    if (usernameHit) return true
    if (email.isBlank()) return false
    val emailHit = SupabaseClient.client
      .from("users")
      .select {
        filter { eq("email", email) }
        limit(1)
      }
      .decodeList<SupabaseUser>()
      .isNotEmpty()
    return emailHit
  }

  suspend fun insertUser(user: SupabaseUser) {
    SupabaseClient.client
      .from("users")
      .insert(
        buildJsonObject {
          put("name", user.name)
          put("email", user.email)
          put("country", user.country)
          put("bio", user.bio)
          put("birthdate", toUiDate(user.birthdate))
          if (user.createdAt.isNotBlank()) {
            put("created_at", user.createdAt)
          }
          put("username", user.username)
          put("password", user.password)
        },
      )
  }

  suspend fun updateUserProfile(user: UserProfile) {
    SupabaseClient.client
      .from("users")
      .update(
        buildJsonObject {
          put("name", user.name)
          put("email", user.email)
          put("country", user.country)
          put("bio", user.bio)
          put("birthdate", toUiDate(user.birthdate))
          put("username", user.username)
          put("password", user.password)
        },
      ) {
        filter { eq("id", user.id) }
      }
  }

  fun persistCredentials(username: String, password: String) {
    savedUsername = username
    savedPassword = password
    hasRegistered = true
    prefs.edit {
      putBoolean("has_registered", true)
      putString("saved_username", username)
      putString("saved_password", password)
    }
  }

  fun showGoalReachedNotification(goal: DreamEntry) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        GOAL_REACHED_CHANNEL_ID,
        strings["goal_reached_notification_title"],
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = strings["goal_reached_notification_desc"]
      }
      notificationManager.createNotificationChannel(channel)
    }
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }
    val body = strings["goal_reached_notification_body"]
      .replace("{title}", goal.title.ifBlank { strings["label_goal"] })
      .replace(
        "{message}",
        if (goal.sourceType == "expense") {
          strings["goal_reached_popup_expense"]
        } else {
          strings["goal_reached_popup_income_balance"]
        },
      )
    val notification = NotificationCompat.Builder(context, GOAL_REACHED_CHANNEL_ID)
      .setSmallIcon(R.drawable.logo2)
      .setContentTitle(strings["goal_reached_notification_title"])
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .build()
    NotificationManagerCompat.from(context).notify(goal.id.hashCode(), notification)
  }

  fun updateGoalMilestones(notify: Boolean = true) {
    val totalIncome = incomeEntries.sumOf { it.amount }
    val totalExpense = expenseEntries.sumOf { it.amount }
    val totalBalance = totalIncome - totalExpense
    val previousIncome = lastIncomeTotal
    val previousExpense = lastExpenseTotal
    val previousBalance = lastBalanceTotal
    lastIncomeTotal = totalIncome
    lastExpenseTotal = totalExpense
    lastBalanceTotal = totalBalance
    achievedGoalIds.removeAll { id -> dreamEntries.none { it.id == id } }
    val newlyReached = mutableListOf<DreamEntry>()
    dreamEntries.forEach { goal ->
      val progress = when (goal.sourceType) {
        "balance" -> totalBalance
        "expense" -> totalExpense
        else -> totalIncome
      }
      val previousProgress = when (goal.sourceType) {
        "balance" -> previousBalance
        "expense" -> previousExpense
        else -> previousIncome
      }
      val reached = goal.target > 0 && progress >= goal.target
      val hasReached = achievedGoalIds.contains(goal.id)
      val justReached = previousProgress < goal.target && progress >= goal.target
      if (reached && !hasReached) {
        achievedGoalIds.add(goal.id)
        if (notify && justReached) {
          newlyReached.add(goal)
        }
      }
      if (!reached && hasReached) {
        achievedGoalIds.remove(goal.id)
      }
    }
    if (notify && newlyReached.isNotEmpty()) {
      toastMessage = if (newlyReached.size == 1) {
        strings["goal_reached_single"].replace("{title}", newlyReached.first().title)
      } else {
        strings["goal_reached_multi"].replace("{count}", newlyReached.size.toString())
      }
      toastVisible = true
      val firstReached = newlyReached.first()
      goalReachEvent = GoalReachEvent(
        goalId = firstReached.id,
        sourceType = firstReached.sourceType.ifBlank { "income" },
      )
      newlyReached.forEach { goal ->
        showGoalReachedNotification(goal)
      }
    }
  }

  fun scheduleGoalDeadlineWorker() {
    val zone = ZoneId.of("Asia/Jakarta")
    val now = ZonedDateTime.now(zone)
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
    val initialDelay = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(0)
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()
    val request = PeriodicWorkRequestBuilder<GoalDeadlineWorker>(24, TimeUnit.HOURS)
      .setConstraints(constraints)
      .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
      .build()
    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(
        GOAL_DEADLINE_WORK,
        ExistingPeriodicWorkPolicy.UPDATE,
        request,
      )
  }

  fun cancelGoalDeadlineWorker() {
    WorkManager.getInstance(context).cancelUniqueWork(GOAL_DEADLINE_WORK)
  }

  suspend fun fetchAllUsers(): List<SupabaseUser> {
    val users = SupabaseClient.client
      .from("users")
      .select()
      .decodeList<SupabaseUser>()
    return users.sortedByDescending { parseCreatedAtMillis(it.createdAt) ?: 0L }
  }

  suspend fun updateMoneyEntryDate(entryId: String, date: String) {
    SupabaseClient.client
      .from("money_entries")
      .update(
        buildJsonObject {
          put("date", date)
        },
      ) {
        filter { eq("id", entryId) }
      }
  }

  suspend fun loadUserData(userId: String) {
    val moneyRows = SupabaseClient.client
      .from("money_entries")
      .select {
        filter { eq("user_id", userId) }
      }
      .decodeList<SupabaseMoneyEntry>()

    val pendingDateUpdates = mutableListOf<Pair<String, String>>()
    val income = moneyRows.filter { it.type == EntryType.Income.name }.map { row ->
      val fallbackTime = formatTimeFromCreatedAt(row.createdAt)
      val normalizedDate = if (fallbackTime != null) {
        ensureDateHasTime(row.date, fallbackTime)
      } else {
        row.date
      }
      if (normalizedDate != row.date && formatTimeFromCreatedAt(row.createdAt) != null) {
        pendingDateUpdates.add(row.id to normalizedDate)
      }
      MoneyEntry(
        id = row.id,
        type = EntryType.Income,
        amount = row.amount,
        date = normalizedDate,
        category = row.category,
        note = row.note,
        sourceOrMethod = row.sourceOrMethod,
        channelOrBank = row.channelOrBank,
        createdAt = row.createdAt,
      )
    }
    val expense = moneyRows.filter { it.type == EntryType.Expense.name }.map { row ->
      val fallbackTime = formatTimeFromCreatedAt(row.createdAt)
      val normalizedDate = if (fallbackTime != null) {
        ensureDateHasTime(row.date, fallbackTime)
      } else {
        row.date
      }
      if (normalizedDate != row.date && formatTimeFromCreatedAt(row.createdAt) != null) {
        pendingDateUpdates.add(row.id to normalizedDate)
      }
      MoneyEntry(
        id = row.id,
        type = EntryType.Expense,
        amount = row.amount,
        date = normalizedDate,
        category = row.category,
        note = row.note,
        sourceOrMethod = row.sourceOrMethod,
        channelOrBank = row.channelOrBank,
        createdAt = row.createdAt,
      )
    }

    val dreamRows = SupabaseClient.client
      .from("dream_entries")
      .select {
        filter { eq("user_id", userId) }
      }
      .decodeList<SupabaseDreamEntry>()
    val dreams = dreamRows.map { row ->
      DreamEntry(
        id = row.id,
        title = row.title,
        target = row.target,
        current = row.current,
        deadline = row.deadline,
        note = row.note,
        sourceType = row.sourceType.ifBlank { "income" },
      )
    }

    if (pendingDateUpdates.isNotEmpty()) {
      pendingDateUpdates.distinctBy { it.first }.forEach { (entryId, dateText) ->
        try {
          updateMoneyEntryDate(entryId, dateText)
        } catch (_: Exception) {
          // Ignore failed backfill updates.
        }
      }
    }

    withContext(Dispatchers.Main) {
      incomeEntries.clear()
      incomeEntries.addAll(income)
      expenseEntries.clear()
      expenseEntries.addAll(expense)
      dreamEntries.clear()
      dreamEntries.addAll(dreams)
      updateGoalMilestones(notify = false)
    }
  }

  fun clearUserData() {
    currentUser = null
    isLoggedIn = false
    pendingEdit = null
    goalReachEvent = null
    achievedGoalIds.clear()
    lastIncomeTotal = 0
    lastExpenseTotal = 0
    lastBalanceTotal = 0
    cancelGoalDeadlineWorker()
    incomeEntries.clear()
    expenseEntries.clear()
    dreamEntries.clear()
  }

  fun enterAdminMode() {
    clearUserData()
    adminLoggedIn = true
    adminUsers.clear()
    showAuth = false
    showSplash = false
    showProfileMenu = false
  }

  fun exitAdminMode() {
    adminLoggedIn = false
    adminUsers.clear()
    showProfileMenu = false
    showAuth = true
    authTab = AuthTab.SignIn
  }

  fun signInWithCredentials(username: String, password: String) {
    if (username.isBlank() || password.isBlank()) {
      alertMessage = strings["signin_missing"]
      showAlert = true
      return
    }
    if (username.trim() == "admin" && password == "adminsolvixstudio") {
      enterAdminMode()
      scope.launch(Dispatchers.IO) {
        val users = fetchAllUsers()
        withContext(Dispatchers.Main) {
          adminUsers.clear()
          adminUsers.addAll(users)
        }
      }
      return
    }
    scope.launch(Dispatchers.IO) {
      try {
        val matchedUser = fetchUserByCredentials(username.trim(), password)
        withContext(Dispatchers.Main) {
          if (matchedUser == null) {
            alertMessage = strings["signin_failed"]
            showAlert = true
          } else {
            currentUser = matchedUser
            isLoggedIn = true
            showAuth = false
            biometricAllowed = true
            prefs.edit { putBoolean("biometric_allowed", true) }
            persistCredentials(matchedUser.username, matchedUser.password)
            scheduleGoalDeadlineWorker()
            toastMessage = strings["login_success"]
            toastVisible = true
            scope.launch(Dispatchers.IO) {
              loadUserData(matchedUser.id)
            }
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          alertMessage = "Gagal login: ${e.localizedMessage ?: "Cek koneksi internet"}"
          showAlert = true
        }
      }
    }
  }

  suspend fun insertMoneyEntry(userId: String, entry: MoneyEntry) {
    SupabaseClient.client
      .from("money_entries")
      .insert(
        buildJsonObject {
          put("id", entry.id)
          put("user_id", userId)
          put("type", entry.type.name)
          put("amount", entry.amount)
          put("date", entry.date)
          if (entry.createdAt.isNotBlank()) {
            put("created_at", entry.createdAt)
          }
          put("category", entry.category)
          put("note", entry.note)
          put("source_method", entry.sourceOrMethod)
          put("channel_bank", entry.channelOrBank)
        },
      )
  }

  suspend fun updateMoneyEntry(entry: MoneyEntry) {
    SupabaseClient.client
      .from("money_entries")
      .update(
        buildJsonObject {
          put("amount", entry.amount)
          put("date", entry.date)
          put("category", entry.category)
          put("note", entry.note)
          put("source_method", entry.sourceOrMethod)
          put("channel_bank", entry.channelOrBank)
        },
      ) {
        filter { eq("id", entry.id) }
      }
  }

  suspend fun deleteMoneyEntry(entryId: String) {
    SupabaseClient.client
      .from("money_entries")
      .delete {
        filter { eq("id", entryId) }
      }
  }

  suspend fun insertDreamEntry(userId: String, entry: DreamEntry) {
    SupabaseClient.client
      .from("dream_entries")
      .insert(
        buildJsonObject {
          put("id", entry.id)
          put("user_id", userId)
          put("title", entry.title)
          put("target", entry.target)
          put("current", entry.current)
          put("deadline", entry.deadline)
          put("note", entry.note)
          put("source_type", entry.sourceType)
        },
      )
  }

  suspend fun updateDreamEntry(entry: DreamEntry) {
    SupabaseClient.client
      .from("dream_entries")
      .update(
        buildJsonObject {
          put("title", entry.title)
          put("target", entry.target)
          put("current", entry.current)
          put("deadline", entry.deadline)
          put("note", entry.note)
          put("source_type", entry.sourceType)
        },
      ) {
        filter { eq("id", entry.id) }
      }
  }

  suspend fun deleteDreamEntry(entryId: String) {
    SupabaseClient.client
      .from("dream_entries")
      .delete {
        filter { eq("id", entryId) }
      }
  }

  fun requestConfirm(message: String, onConfirm: () -> Unit) {
    confirmMessage = message
    confirmAction = onConfirm
    showConfirm = true
  }

  fun canUseFingerprint(): Boolean {
    val strong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    return strong == BiometricManager.BIOMETRIC_SUCCESS && keyguardManager.isDeviceSecure
  }

  fun launchBiometricAuth(
    title: String,
    subtitle: String,
    allowedAuthenticators: Int,
    onSuccess: () -> Unit,
  ) {
    val activity = context as? FragmentActivity
    if (activity == null) {
      alertMessage = strings["fingerprint_activity_required"]
      showAlert = true
      return
    }
    val usable = canUseFingerprint()
    if (!usable) {
      alertMessage = strings["fingerprint_required"]
      showAlert = true
      return
    }
    val prompt = BiometricPrompt(
      activity,
      executor,
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
          onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
          alertMessage = errString.toString()
          showAlert = true
        }
      },
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setNegativeButtonText(strings["confirm_cancel"])
      .setAllowedAuthenticators(allowedAuthenticators)
      .build()
    prompt.authenticate(promptInfo)
  }

  fun launchFingerprintAuth(onSuccess: () -> Unit) {
    launchBiometricAuth(
      title = strings["fingerprint_prompt_title"],
      subtitle = strings["fingerprint_prompt_subtitle"],
      allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG,
      onSuccess = onSuccess,
    )
  }

  TabunganTheme(theme = currentTheme) {
    CompositionLocalProvider(
      LocalStrings provides strings,
      LocalLanguage provides currentLang,
    ) {
      val colors = LocalAppColors.current
      val modalOpen = showLoading || showSplash || showAuth || showConfirm || showAlert
      val displayName = if (adminLoggedIn) strings["menu_admin"] else (currentUser?.name ?: strings["guest"])
      val displayUsername = if (adminLoggedIn) "admin" else (currentUser?.username ?: strings["guest"])
      val loadingAlpha by animateFloatAsState(
        targetValue = if (showLoading && !loadingFadeOut) 1f else 0f,
        animationSpec = tween(700),
        label = "loading-alpha",
      )

      LaunchedEffect(toastVisible) {
        if (toastVisible) {
          delay(2000)
          toastVisible = false
        }
      }
      LaunchedEffect(showAuth, authTab) {
        if (showAuth && authTab == AuthTab.SignIn) {
          biometricPrompted = false
        }
      }
      LaunchedEffect(showAuth, authTab, fingerprintEnabled, hasRegistered, biometricAllowed) {
        if (!showAuth || authTab != AuthTab.SignIn) return@LaunchedEffect
        if (!hasRegistered || !biometricAllowed || biometricPrompted) return@LaunchedEffect
        if (!fingerprintEnabled || !canUseFingerprint()) return@LaunchedEffect
        biometricPrompted = true
        launchFingerprintAuth {
          if (savedUsername.isBlank() || savedPassword.isBlank()) {
            alertMessage = strings["signin_missing"]
            showAlert = true
          } else {
            signInWithCredentials(savedUsername, savedPassword)
          }
        }
      }
      LaunchedEffect(showLoading) {
        if (!showLoading) return@LaunchedEffect
        loadingFadeOut = false
        delay(5000)
        loadingFadeOut = true
        delay(700)
        showLoading = false
        when (loadingTarget) {
          LoadingTarget.Startup -> {
            if (!hasSeenWelcome) {
              showSplash = true
            } else {
              showAuth = true
              authTab = AuthTab.SignIn
            }
          }
          LoadingTarget.Logout -> {
            showAuth = true
            authTab = AuthTab.SignIn
          }
        }
      }

      Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        AmbientBackground()
        Scaffold(
          containerColor = Color.Transparent,
          bottomBar = {
            if (!adminLoggedIn) {
              val navBlur = showSplash || showAuth
              Box {
                BottomNav(
                  current = currentPage,
                  onSelect = { page -> navigateTo(page) },
                  strings = strings,
                  theme = currentTheme,
                  modifier = Modifier.then(if (navBlur) Modifier.blur(24.dp) else Modifier),
                )
                if (navBlur) {
                  Box(
                    modifier = Modifier
                      .matchParentSize()
                      .background(Color(0xCCFFFFFF))
                      .clickable(enabled = true, onClick = {}),
                  )
                }
              }
            }
          },
        ) { padding ->
          val blurRadius = when {
            showAlert || showConfirm -> 0.dp
            showSplash || showAuth -> 72.dp
            modalOpen -> 20.dp
            else -> 0.dp
          }
          val activeUserId = currentUser?.id.orEmpty()
          Box(modifier = Modifier.fillMaxSize()) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (blurRadius.value > 0f) Modifier.blur(blurRadius) else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(
                  start = AppDimens.pagePadding,
                  end = AppDimens.pagePadding,
                  bottom = 24.dp,
                  top = 0.dp,
                ),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              FadeInPage(key = "${currentPage}_${fadeSeed}_${pageFadeSeed}_page") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                  TopBar(
                    onProfileClick = { showProfileMenu = true },
                    showMenu = showProfileMenu,
                    onDismissMenu = { showProfileMenu = false },
                    onNavigate = { page ->
                      if (!adminLoggedIn) {
                        navigateTo(page)
                      }
                      showProfileMenu = false
                    },
                    onLogout = {
                      showProfileMenu = false
                      requestConfirm(strings["logout_confirm"]) {
                        if (adminLoggedIn) {
                          exitAdminMode()
                          clearAuthFields(
                            onSignInUsername = { signInUsername = it },
                            onSignInPassword = { signInPassword = it },
                            onSignUpName = { signUpName = it },
                            onSignUpEmail = { signUpEmail = it },
                            onSignUpCountry = { signUpCountry = it },
                            onSignUpBirthdate = { signUpBirthdate = it },
                            onSignUpBio = { signUpBio = it },
                            onSignUpUsername = { signUpUsername = it },
                            onSignUpPassword = { signUpPassword = it },
                          )
                          toastMessage = strings["logout_success"]
                          toastVisible = true
                        } else {
                          clearUserData()
                          biometricAllowed = false
                          prefs.edit { putBoolean("biometric_allowed", false) }
                          showSplash = false
                          showAuth = false
                          loadingTarget = LoadingTarget.Logout
                          showLoading = true
                          clearAuthFields(
                            onSignInUsername = { signInUsername = it },
                            onSignInPassword = { signInPassword = it },
                            onSignUpName = { signUpName = it },
                            onSignUpEmail = { signUpEmail = it },
                            onSignUpCountry = { signUpCountry = it },
                            onSignUpBirthdate = { signUpBirthdate = it },
                            onSignUpBio = { signUpBio = it },
                            onSignUpUsername = { signUpUsername = it },
                            onSignUpPassword = { signUpPassword = it },
                          )
                          toastMessage = strings["logout_success"]
                          toastVisible = true
                        }
                      }
                    },
                    displayName = displayName,
                    displayUsername = displayUsername,
                    strings = strings,
                    theme = currentTheme,
                    currentPage = currentPage,
                    isAdmin = adminLoggedIn,
                  )

                  if (adminLoggedIn) {
                    AdminDashboard(users = adminUsers, strings = strings)
                  } else {
                    if (currentPage.showHero()) {
                      val filtered = filterByRange(incomeEntries + expenseEntries, summaryRange)
                      val incomeTotal = filtered.filter { it.type == EntryType.Income }.sumOf { it.amount }
                      val expenseTotal = filtered.filter { it.type == EntryType.Expense }.sumOf { it.amount }
                      HeroSummary(
                        range = summaryRange,
                        onRangeChange = { summaryRange = it },
                        incomeTotal = incomeTotal,
                        expenseTotal = expenseTotal,
                        strings = strings,
                      )
                    }

                    when (currentPage) {
                      Page.Income -> IncomePage(
                        onSave = { entry ->
                          incomeEntries.add(entry)
                          updateGoalMilestones()
                          if (activeUserId.isNotBlank()) {
                            scope.launch(Dispatchers.IO) { insertMoneyEntry(activeUserId, entry) }
                          }
                          toastMessage = strings["income_added"]
                          toastVisible = true
                        },
                        onUpdate = { entry ->
                          val index = incomeEntries.indexOfFirst { it.id == entry.id }
                          if (index >= 0) incomeEntries[index] = entry
                          updateGoalMilestones()
                          if (activeUserId.isNotBlank()) {
                            scope.launch(Dispatchers.IO) { updateMoneyEntry(entry) }
                          }
                          toastMessage = strings["income_updated"]
                          toastVisible = true
                        },
                        editEntry = pendingEdit,
                        onEditConsumed = { pendingEdit = null },
                        strings = strings,
                      )
                      Page.Expense -> ExpensePage(
                        onSave = { entry ->
                          expenseEntries.add(entry)
                          updateGoalMilestones()
                          if (activeUserId.isNotBlank()) {
                            scope.launch(Dispatchers.IO) { insertMoneyEntry(activeUserId, entry) }
                          }
                          toastMessage = strings["expense_added"]
                          toastVisible = true
                        },
                        onUpdate = { entry ->
                          val index = expenseEntries.indexOfFirst { it.id == entry.id }
                          if (index >= 0) expenseEntries[index] = entry
                          updateGoalMilestones()
                          if (activeUserId.isNotBlank()) {
                            scope.launch(Dispatchers.IO) { updateMoneyEntry(entry) }
                          }
                          toastMessage = strings["expense_updated"]
                          toastVisible = true
                        },
                        editEntry = pendingEdit,
                        onEditConsumed = { pendingEdit = null },
                        strings = strings,
                      )
                      Page.Dreams -> DreamsPage(
                        entries = dreamEntries,
                        incomeTotal = incomeEntries.sumOf { it.amount },
                        expenseTotal = expenseEntries.sumOf { it.amount },
                        balanceTotal = incomeEntries.sumOf { it.amount } - expenseEntries.sumOf { it.amount },
                        onInvalid = {
                          alertMessage = strings["goal_missing"]
                          showAlert = true
                        },
                        onSave = { entry ->
                          dreamEntries.add(entry)
                          updateGoalMilestones()
                          if (activeUserId.isNotBlank()) {
                            scope.launch(Dispatchers.IO) { insertDreamEntry(activeUserId, entry) }
                          }
                          toastMessage = strings["dream_added"]
                          toastVisible = true
                        },
                        onUpdate = { entry ->
                          val index = dreamEntries.indexOfFirst { it.id == entry.id }
                          if (index >= 0) dreamEntries[index] = entry
                          updateGoalMilestones()
                          if (activeUserId.isNotBlank()) {
                            scope.launch(Dispatchers.IO) { updateDreamEntry(entry) }
                          }
                          toastMessage = strings["dream_updated"]
                          toastVisible = true
                        },
                        onDelete = { entry ->
                          requestConfirm(strings["confirm_delete_dream"]) {
                            dreamEntries.removeAll { it.id == entry.id }
                            updateGoalMilestones()
                            if (activeUserId.isNotBlank()) {
                              scope.launch(Dispatchers.IO) { deleteDreamEntry(entry.id) }
                            }
                            toastMessage = strings["dream_deleted"]
                            toastVisible = true
                          }
                        },
                        goalReachSourceType = if (currentPage == Page.Dreams) goalReachEvent?.sourceType else null,
                        onGoalReachDismiss = { goalReachEvent = null },
                        strings = strings,
                      )
                      Page.History -> HistoryPage(
                        entries = incomeEntries + expenseEntries,
                        strings = strings,
                        onEdit = { entry ->
                          pendingEdit = entry
                          navigateTo(if (entry.type == EntryType.Income) Page.Income else Page.Expense)
                        },
                        onDelete = { entry ->
                          val confirmKey = if (entry.type == EntryType.Income) "confirm_delete_income" else "confirm_delete_expense"
                          requestConfirm(strings[confirmKey]) {
                            when (entry.type) {
                              EntryType.Income -> {
                                incomeEntries.removeAll { it.id == entry.id }
                                updateGoalMilestones()
                                toastMessage = strings["income_deleted"]
                                toastVisible = true
                              }
                              EntryType.Expense -> {
                                expenseEntries.removeAll { it.id == entry.id }
                                updateGoalMilestones()
                                toastMessage = strings["expense_deleted"]
                                toastVisible = true
                              }
                            }
                            if (activeUserId.isNotBlank()) {
                              scope.launch(Dispatchers.IO) { deleteMoneyEntry(entry.id) }
                            }
                          }
                        },
                      )
                      Page.Calculator -> CalculatorPage()
                      Page.Report -> ReportPage(
                        incomeEntries,
                        expenseEntries,
                        strings = strings,
                        language = currentLang,
                        startYear = resolveStartYear(),
                      ) {
                        toastMessage = it
                        toastVisible = true
                      }
                      Page.Profile -> ProfilePage(
                        user = currentUser,
                        strings = strings,
                        onSave = { updated ->
                          currentUser = updated
                          if (updated.id.isNotBlank()) {
                            scope.launch {
                              try {
                                withContext(Dispatchers.IO) { updateUserProfile(updated) }
                                toastMessage = strings["save_profile"]
                                toastVisible = true
                              } catch (ex: Exception) {
                                toastMessage = strings["save_profile_failed"]
                                toastVisible = true
                              }
                            }
                          } else {
                            toastMessage = strings["save_profile_failed"]
                            toastVisible = true
                          }
                        },
                        onLogout = {
                          clearUserData()
                          biometricAllowed = false
                          prefs.edit { putBoolean("biometric_allowed", false) }
                          showSplash = false
                          showAuth = false
                          loadingTarget = LoadingTarget.Logout
                          showLoading = true
                          clearAuthFields(
                            onSignInUsername = { signInUsername = it },
                            onSignInPassword = { signInPassword = it },
                            onSignUpName = { signUpName = it },
                            onSignUpEmail = { signUpEmail = it },
                            onSignUpCountry = { signUpCountry = it },
                            onSignUpBirthdate = { signUpBirthdate = it },
                            onSignUpBio = { signUpBio = it },
                            onSignUpUsername = { signUpUsername = it },
                            onSignUpPassword = { signUpPassword = it },
                          )
                          toastMessage = strings["logout_success"]
                          toastVisible = true
                        },
                      )
                      Page.Settings -> SettingsPage(
                        fingerprintEnabled = fingerprintEnabled,
                        onFingerprintToggle = { enabled ->
                          if (enabled) {
                            if (canUseFingerprint()) {
                              fingerprintEnabled = true
                              prefs.edit { putBoolean("fingerprint_enabled", true) }
                              alertMessage = strings["fingerprint_enabled"]
                              showAlert = true
                            } else {
                              fingerprintEnabled = false
                              prefs.edit { putBoolean("fingerprint_enabled", false) }
                              alertMessage = strings["fingerprint_required"]
                              showAlert = true
                            }
                          } else {
                            fingerprintEnabled = false
                            prefs.edit { putBoolean("fingerprint_enabled", false) }
                          }
                        },
                        language = currentLang,
                        onLanguageChange = {
                          currentLang = it
                          prefs.edit { putString("app_language", if (it == AppLanguage.ID) "ID" else "EN") }
                        },
                        strings = strings,
                        onToast = {
                          toastMessage = it
                          toastVisible = true
                        },
                      )
                      Page.Themes -> ThemesPage(currentTheme) { selected ->
                        requestConfirm("${strings["confirm_theme_change"]} ${themeLabel(selected, strings)}?") {
                          currentTheme = selected
                        }
                      }
                    }
                  }
                }
              }
              Text(
                text = strings["footer"],
                color = colors.muted,
                fontSize = 11.sp,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 0.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              )
            }
            if (showSplash || showAuth) {
              Box(
                modifier = Modifier
                  .matchParentSize()
                  .background(Color(0xEFFFFFFF)),
              )
            }
          }
        }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 72.dp)
          .align(Alignment.BottomCenter),
        contentAlignment = Alignment.Center,
      ) {
        ToastMessage(text = toastMessage, visible = toastVisible)
      }

        if (showSplash) {
          ModalOverlay {
            ModalCard(
              modifier = Modifier.fillMaxWidth(0.92f),
            ) {
              Text(
                text = "x",
                modifier = Modifier
                  .align(Alignment.End)
                  .clickable {
                    hasSeenWelcome = true
                    prefs.edit { putBoolean("has_seen_welcome", true) }
                    showSplash = false
                    showAuth = true
                    authTab = AuthTab.SignIn
                  },
              )
              Spacer(modifier = Modifier.height(8.dp))
              LogoCircle()
              Spacer(modifier = Modifier.height(12.dp))
              Text(text = strings["welcome_title"], fontWeight = FontWeight.Bold, fontSize = 18.sp)
              Spacer(modifier = Modifier.height(8.dp))
              Text(text = strings["welcome_subtitle"], color = colors.muted, fontSize = 13.sp)
              Spacer(modifier = Modifier.height(16.dp))
              GradientButton(text = strings["welcome_action"]) {
                hasSeenWelcome = true
                prefs.edit { putBoolean("has_seen_welcome", true) }
                if (fingerprintEnabled && biometricAllowed && canUseFingerprint()) {
                  launchFingerprintAuth {
                    showSplash = false
                    isLoggedIn = true
                    toastMessage = strings["login_success"]
                    toastVisible = true
                  }
                } else {
                  showSplash = false
                  showAuth = true
                }
              }
            }
          }
        }

        if (showAuth) {
          ModalOverlay {
            ModalCard(
              modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 520.dp),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.bg2),
                ) {
                  AuthTabButton(
                    text = strings["auth_sign_in"],
                    active = authTab == AuthTab.SignIn,
                    onClick = { authTab = AuthTab.SignIn },
                  )
                  AuthTabButton(
                    text = strings["auth_sign_up"],
                    active = authTab == AuthTab.SignUp,
                    onClick = { authTab = AuthTab.SignUp },
                  )
                }
                if (authTab == AuthTab.SignIn) {
                  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(strings["label_username"], value = signInUsername, onValueChange = { signInUsername = it })
                    AppTextField(strings["password"], value = signInPassword, onValueChange = { signInPassword = it }, isPassword = true)
                    if (fingerprintEnabled && hasRegistered && biometricAllowed) {
                      GhostButton(text = strings["auth_fingerprint"]) {
                        launchFingerprintAuth {
                          if (savedUsername.isBlank() || savedPassword.isBlank()) {
                            alertMessage = strings["signin_missing"]
                            showAlert = true
                          } else {
                            signInWithCredentials(savedUsername, savedPassword)
                          }
                        }
                      }
                    }
                    GradientButton(text = strings["auth_login"]) {
                      signInWithCredentials(signInUsername, signInPassword)
                    }
              }
                } else {
                  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(strings["label_name"], value = signUpName, onValueChange = { signUpName = it })
                    AppTextField(strings["label_email"], value = signUpEmail, onValueChange = { signUpEmail = it })
                    AppDropdown(
                      label = strings["label_country"],
                      placeholder = strings["placeholder_country"],
                      options = optionList(currentLang, OptionList.Countries),
                      selected = signUpCountry,
                      onSelected = { signUpCountry = it },
                    )
                    DateField(
                      label = strings["label_birthdate"],
                      value = signUpBirthdate,
                      onValueChange = { signUpBirthdate = it },
                      placeholder = strings["placeholder_date"],
                    )
                    AppTextField(strings["label_bio"], value = signUpBio, onValueChange = { signUpBio = it }, minLines = 2)
                    AppTextField(strings["label_username"], value = signUpUsername, onValueChange = { signUpUsername = it })
                    AppTextField(strings["password"], value = signUpPassword, onValueChange = { signUpPassword = it }, isPassword = true)
                    GradientButton(text = strings["auth_register"]) {
                      if (signUpName.isBlank() || signUpUsername.isBlank() || signUpPassword.isBlank()) {
                        alertMessage = strings["signup_missing"]
                        showAlert = true
                        return@GradientButton
                      }
                      val normalizedUsername = signUpUsername.trim()
                      val normalizedEmail = signUpEmail.trim()
                      scope.launch(Dispatchers.IO) {
                        try {
                          val exists = userExists(normalizedUsername, normalizedEmail)
                          if (exists) {
                            withContext(Dispatchers.Main) {
                              alertMessage = strings["signup_exists"]
                              showAlert = true
                            }
                            return@launch
                          }
                          val newUser = SupabaseUser(
                            name = signUpName,
                            email = normalizedEmail,
                            country = signUpCountry,
                            bio = signUpBio,
                            birthdate = signUpBirthdate,
                            createdAt = nowJakartaText(),
                            username = normalizedUsername,
                            password = signUpPassword,
                          )
                          insertUser(newUser)
                          withContext(Dispatchers.Main) {
                            isLoggedIn = false
                            showAuth = true
                            authTab = AuthTab.SignIn
                            signInUsername = normalizedUsername
                            signInPassword = ""
                            persistCredentials(normalizedUsername, signUpPassword)
                            toastMessage = strings["signup_success"]
                            toastVisible = true
                          }
                        } catch (e: Exception) {
                          withContext(Dispatchers.Main) {
                            alertMessage = "Gagal daftar: ${e.localizedMessage ?: "Cek koneksi internet"}"
                            showAlert = true
                          }
                        }
                      }
                    }
                }
              }
                GhostButton(text = strings["close"]) {
                  requestConfirm(strings["exit_confirm"]) {
                    (context as? android.app.Activity)?.finish()
                  }
                }
              }
            }
          }
        }

        if (showLoading) {
        AnimatedVisibility(
          visible = showLoading,
          enter = fadeIn(tween(600)),
          exit = fadeOut(tween(600)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.White)
              .alpha(loadingAlpha),
            contentAlignment = Alignment.Center,
          ) {
            LoadingLogo()
          }
        }
      }

      if (showConfirm) {
        ModalOverlay {
          ModalCard {
            Text(text = strings["confirm_title"], fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = confirmMessage, color = colors.muted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              GhostButton(text = strings["confirm_cancel"]) { showConfirm = false }
              GradientButton(text = strings["confirm_ok"]) {
                showConfirm = false
                confirmAction?.invoke()
              }
            }
          }
        }
      }

        if (showAlert) {
          ModalOverlay {
            ModalCard {
              Text(text = strings["alert_title"], fontWeight = FontWeight.Bold, fontSize = 18.sp)
              Spacer(modifier = Modifier.height(8.dp))
              Text(text = alertMessage, color = colors.muted, fontSize = 13.sp)
              Spacer(modifier = Modifier.height(12.dp))
              GradientButton(text = strings["ok"]) { showAlert = false }
            }
          }
        }
      }
    }
  }
}

private fun Page.showHero(): Boolean {
  return this == Page.Income ||
    this == Page.Expense
}

private fun clearAuthFields(
  onSignInUsername: (String) -> Unit,
  onSignInPassword: (String) -> Unit,
  onSignUpName: (String) -> Unit,
  onSignUpEmail: (String) -> Unit,
  onSignUpCountry: (String) -> Unit,
  onSignUpBirthdate: (String) -> Unit,
  onSignUpBio: (String) -> Unit,
  onSignUpUsername: (String) -> Unit,
  onSignUpPassword: (String) -> Unit,
) {
  onSignInUsername("")
  onSignInPassword("")
  onSignUpName("")
  onSignUpEmail("")
  onSignUpCountry("")
  onSignUpBirthdate("")
  onSignUpBio("")
  onSignUpUsername("")
  onSignUpPassword("")
}

@Composable
private fun TopBar(
  onProfileClick: () -> Unit,
  showMenu: Boolean,
  onDismissMenu: () -> Unit,
  onNavigate: (Page) -> Unit,
  onLogout: () -> Unit,
  displayName: String,
  displayUsername: String,
  strings: AppStrings,
  theme: ThemeName,
  currentPage: Page,
  isAdmin: Boolean,
) {
  val colors = LocalAppColors.current
  var menuAnchorBounds by remember { mutableStateOf(IntRect.Zero) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 20.dp, bottom = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(colors.card)
          .shadow(10.dp, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Text(text = themeAppIcon(theme), fontSize = 26.sp)
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(text = strings["app_name"], fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.text)
        Text(text = strings["app_tagline"], color = colors.muted, fontSize = 13.sp)
      }
    }

    Box {
      ChipButton(
        text = if (displayUsername.isBlank()) strings["guest"] else displayUsername,
        modifier = Modifier.onGloballyPositioned { coordinates ->
          val rect = coordinates.boundsInWindow()
          menuAnchorBounds = IntRect(
            rect.left.roundToInt(),
            rect.top.roundToInt(),
            rect.right.roundToInt(),
            rect.bottom.roundToInt(),
          )
        },
        onClick = onProfileClick,
      )
      DropDownMenuCard(
        expanded = showMenu,
        onDismiss = onDismissMenu,
        modifier = Modifier.width(220.dp),
        anchorBounds = menuAnchorBounds,
        alignRight = true,
        xOffsetDp = (-2).dp,
        yOffsetDp = 10.dp,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(text = displayName, fontWeight = FontWeight.Bold, color = colors.text)
              Text(text = "@$displayUsername", color = colors.muted, fontSize = 12.sp)
            }
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.bg2)
                .padding(4.dp)
                .clickable { onDismissMenu() },
              contentAlignment = Alignment.Center,
            ) {
              Text(text = "x", fontWeight = FontWeight.Bold, color = colors.text)
            }
          }
          if (isAdmin) {
            MenuItem(text = strings["admin_logout"], emoji = "🚪", color = colors.danger) { onLogout() }
          } else {
            MenuItem(
              text = strings["menu_theme"],
              emoji = themePageIcon(theme, Page.Themes),
              active = currentPage == Page.Themes,
            ) { onNavigate(Page.Themes) }
            MenuItem(
              text = strings["menu_calculator"],
              emoji = themePageIcon(theme, Page.Calculator),
              active = currentPage == Page.Calculator,
            ) { onNavigate(Page.Calculator) }
            MenuItem(
              text = strings["menu_report"],
              emoji = themePageIcon(theme, Page.Report),
              active = currentPage == Page.Report,
            ) { onNavigate(Page.Report) }
            MenuItem(
              text = strings["menu_profile"],
              emoji = themePageIcon(theme, Page.Profile),
              active = currentPage == Page.Profile,
            ) { onNavigate(Page.Profile) }
            MenuItem(
              text = strings["menu_settings"],
              emoji = themePageIcon(theme, Page.Settings),
              active = currentPage == Page.Settings,
            ) { onNavigate(Page.Settings) }
            MenuItem(text = strings["menu_logout"], emoji = "🚪", color = colors.danger) { onLogout() }
          }
        }
      }
    }
  }
}

@Composable
private fun BottomNav(
  current: Page,
  onSelect: (Page) -> Unit,
  strings: AppStrings,
  theme: ThemeName,
  modifier: Modifier = Modifier,
) {
  val colors = LocalAppColors.current
  val items = listOf(Page.Income, Page.Expense, Page.Dreams, Page.History)
  val navShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 88.dp)
      .clip(navShape)
      .background(colors.card)
      .border(2.dp, colors.accent, navShape)
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    items.forEach { item ->
      val active = current == item
      val indicatorProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "bottom-nav-indicator-${item.name}",
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 3.dp, vertical = 4.dp)
          .offset(y = 5.dp)
          .clickable { onSelect(item) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(text = themePageIcon(theme, item), fontSize = 26.sp, color = if (active) colors.text else colors.muted)
        Spacer(modifier = Modifier.height(1.dp))
        Text(
          text = pageLabel(item, strings),
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (active) colors.text else colors.muted,
        )
        Box(
          modifier = Modifier
            .padding(top = 5.dp)
            .width(32.dp)
            .height(3.dp)
            .graphicsLayer {
              scaleX = indicatorProgress
              alpha = indicatorProgress
              transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.linearGradient(listOf(colors.accent, colors.accent2))),
        )
      }
    }
  }
}

@Composable
private fun AmbientBackground() {
  val colors = LocalAppColors.current
  val transition = rememberInfiniteTransition(label = "ambient")
  val float1 by transition.animateFloat(
    initialValue = 0f,
    targetValue = 20f,
    animationSpec = infiniteRepeatable(tween(8000), RepeatMode.Reverse),
    label = "float1",
  )
  val float2 by transition.animateFloat(
    initialValue = 0f,
    targetValue = -20f,
    animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse),
    label = "float2",
  )
  val sparkle by transition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
    label = "sparkle",
  )

  Box(modifier = Modifier.fillMaxSize()) {
    Blob(
      modifier = Modifier
        .size(280.dp)
        .offset {
          IntOffset(
            x = (-60).dp.roundToPx(),
            y = (-80 + float1).dp.roundToPx(),
          )
        },
      brush = Brush.radialGradient(
        listOf(colors.accent, Color.Transparent),
      ),
    )
    Blob(
      modifier = Modifier
        .size(280.dp)
        .align(Alignment.BottomEnd)
        .offset {
          IntOffset(
            x = 80.dp.roundToPx(),
            y = (120 + float2).dp.roundToPx(),
          )
        },
      brush = Brush.radialGradient(
        listOf(colors.accent2, Color.Transparent),
      ),
    )
    Blob(
      modifier = Modifier
        .size(280.dp)
        .align(Alignment.CenterEnd)
        .offset {
          IntOffset(
            x = 120.dp.roundToPx(),
            y = float1.dp.roundToPx(),
          )
        },
      brush = Brush.radialGradient(
        listOf(Color(0xFF8AA7FF), Color.Transparent),
      ),
    )
    Text(
      text = "✦",
      modifier = Modifier
        .offset(x = 48.dp, y = 140.dp)
        .alpha(sparkle),
      color = Color.White.copy(alpha = 0.7f),
      fontSize = 18.sp,
    )
    Text(
      text = "✦",
      modifier = Modifier
        .offset(x = 260.dp, y = 360.dp)
        .alpha(sparkle),
      color = Color.White.copy(alpha = 0.7f),
      fontSize = 18.sp,
    )
    Text(
      text = "✦",
      modifier = Modifier
        .offset(x = 160.dp, y = 640.dp)
        .alpha(sparkle),
      color = Color.White.copy(alpha = 0.7f),
      fontSize = 18.sp,
    )
  }
}

@Composable
private fun Blob(modifier: Modifier, brush: Brush) {
  Box(
    modifier = modifier
      .clip(CircleShape)
      .blur(12.dp)
      .background(brush)
      .alpha(0.3f),
  )
}

@Composable
private fun ModalOverlay(content: @Composable () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xB20A0A14))
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .matchParentSize()
        .clickable(enabled = true, onClick = {}),
    )
    content()
  }
}

@Composable
private fun LoadingLogo() {
  val strings = LocalStrings.current
  val transition = rememberInfiniteTransition(label = "loading")
  val scale by transition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
    label = "loading-scale",
  )
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(id = R.drawable.logo2),
        contentDescription = strings["logo_alt"],
        modifier = Modifier
          .size(96.dp)
          .graphicsLayer(scaleX = scale, scaleY = scale),
        contentScale = ContentScale.Fit,
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "CashFlow by Solvix Studio",
        color = Color(0xFF9CA3AF),
        fontSize = 12.sp,
      )
    }
    Text(
      text = strings["footer"],
      color = Color(0xFF9CA3AF),
      fontSize = 11.sp,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 18.dp),
    )
  }
}

private fun pageLabel(page: Page, strings: AppStrings): String {
  return when (page) {
    Page.Income -> strings["page_income"]
    Page.Expense -> strings["page_expense"]
    Page.Dreams -> strings["page_dreams"]
    Page.History -> strings["page_history"]
    Page.Calculator -> strings["page_calculator"]
    Page.Report -> strings["page_report"]
    Page.Profile -> strings["page_profile"]
    Page.Settings -> strings["page_settings"]
    Page.Themes -> strings["page_themes"]
  }
}

private fun themeLabel(theme: ThemeName, strings: AppStrings): String {
  return when (theme) {
    ThemeName.StandardLight -> strings["theme_standard_light"]
    ThemeName.StandardDark -> strings["theme_standard_dark"]
    ThemeName.CartoonFood -> strings["theme_food"]
    ThemeName.CartoonSpace -> strings["theme_space"]
    ThemeName.CartoonMonster -> strings["theme_monster"]
    ThemeName.CartoonHero -> strings["theme_hero"]
    ThemeName.CartoonSea -> strings["theme_sea"]
    ThemeName.CartoonPlant -> strings["theme_plant"]
    ThemeName.CartoonPinky -> strings["theme_pinky"]
    ThemeName.CartoonColorful -> strings["theme_colorful"]
  }
}

@Composable
private fun RowScope.AuthTabButton(text: String, active: Boolean, onClick: () -> Unit) {
  val colors = LocalAppColors.current
  val background = if (active) {
    Brush.linearGradient(listOf(colors.accent, colors.accent2))
  } else {
    Brush.linearGradient(listOf(colors.bg2, colors.bg2))
  }
  Box(
    modifier = Modifier
      .weight(1f)
      .clip(RoundedCornerShape(12.dp))
      .background(background)
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      fontWeight = FontWeight.Bold,
      color = if (active) Color.White else colors.text,
    )
  }
}

@Composable
private fun LogoCircle() {
  val strings = LocalStrings.current
  val colors = LocalAppColors.current
  Box(
    modifier = Modifier
      .size(120.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(colors.card)
      .shadow(16.dp, RoundedCornerShape(20.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.logo2),
      contentDescription = strings["logo_alt"],
      modifier = Modifier.size(96.dp),
      contentScale = ContentScale.Fit,
    )
  }
}
