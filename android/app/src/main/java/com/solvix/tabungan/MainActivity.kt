package com.solvix.tabungan

import android.os.Bundle
import android.content.Context
import android.app.DatePickerDialog
import android.app.KeyguardManager
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

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
  Saving("Tabungan", "🏦"),
  Calculator("Kalkulator", "🧮"),
  Report("Laporan", "📈"),
  Profile("Profile", "👤"),
  Settings("Pengaturan", "⚙️"),
  Themes("Tema", "🎨"),
}

private enum class AuthTab { SignIn, SignUp }
private enum class LoadingTarget { Startup, Logout }

@Composable
fun TabunganApp() {
  var currentTheme by rememberSaveable { mutableStateOf(ThemeName.StandardLight) }
  var currentPage by rememberSaveable { mutableStateOf(Page.Income) }
  var summaryRange by rememberSaveable { mutableStateOf(SummaryRange.Month) }
  var showProfileMenu by remember { mutableStateOf(false) }
  var showSplash by rememberSaveable { mutableStateOf(false) }
  var showLoading by rememberSaveable { mutableStateOf(true) }
  var loadingFadeOut by rememberSaveable { mutableStateOf(false) }
  var loadingTarget by rememberSaveable { mutableStateOf(LoadingTarget.Startup) }
  var showAuth by rememberSaveable { mutableStateOf(false) }
  var authTab by rememberSaveable { mutableStateOf(AuthTab.SignIn) }
  var showAdmin by rememberSaveable { mutableStateOf(false) }
  var adminLoggedIn by rememberSaveable { mutableStateOf(false) }
  var adminUsername by rememberSaveable { mutableStateOf("") }
  var adminPassword by rememberSaveable { mutableStateOf("") }
  val adminUsers = remember { mutableStateListOf<SupabaseUser>() }
  var showConfirm by remember { mutableStateOf(false) }
  var confirmMessage by remember { mutableStateOf("") }
  var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var showAlert by remember { mutableStateOf(false) }
  var alertMessage by remember { mutableStateOf("") }
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("tabungan_prefs", Context.MODE_PRIVATE) }
  var toastVisible by remember { mutableStateOf(false) }
  var toastMessage by remember { mutableStateOf("") }
  val defaultLang = prefs.getString("app_language", "EN") ?: "EN"
  var currentLang by rememberSaveable {
    mutableStateOf(if (defaultLang == "ID") AppLanguage.ID else AppLanguage.EN)
  }
  val registeredUsers = remember { mutableStateListOf<UserProfile>() }
  var currentUser by remember { mutableStateOf<UserProfile?>(null) }
  val scope = rememberCoroutineScope()
  var isLoggedIn by remember { mutableStateOf(false) }
  var hasSeenWelcome by remember { mutableStateOf(prefs.getBoolean("has_seen_welcome", false)) }
  var fingerprintEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("fingerprint_enabled", false)) }
  var hasRegistered by rememberSaveable { mutableStateOf(prefs.getBoolean("has_registered", false)) }
  var savedUsername by rememberSaveable { mutableStateOf(prefs.getString("saved_username", "") ?: "") }
  var savedPassword by rememberSaveable { mutableStateOf(prefs.getString("saved_password", "") ?: "") }
  var fingerprintPrompted by rememberSaveable { mutableStateOf(false) }
  val biometricManager = remember { BiometricManager.from(context) }
  val keyguardManager = remember {
    context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
  }
  val executor = remember { ContextCompat.getMainExecutor(context) }

  val incomeEntries = remember { mutableStateListOf<MoneyEntry>() }
  val expenseEntries = remember { mutableStateListOf<MoneyEntry>() }
  val savingEntries = remember { mutableStateListOf<SavingEntry>() }
  val dreamEntries = remember { mutableStateListOf<DreamEntry>() }

  var signInUsername by rememberSaveable { mutableStateOf("") }
  var signInPassword by rememberSaveable { mutableStateOf("") }
  var signUpName by rememberSaveable { mutableStateOf("") }
  var signUpEmail by rememberSaveable { mutableStateOf("") }
  var signUpPhone by rememberSaveable { mutableStateOf("") }
  var signUpCountry by rememberSaveable { mutableStateOf("") }
  var signUpBirthdate by rememberSaveable { mutableStateOf("") }
  var signUpBio by rememberSaveable { mutableStateOf("") }
  var signUpUsername by rememberSaveable { mutableStateOf("") }
  var signUpPassword by rememberSaveable { mutableStateOf("") }

  val strings = stringsFor(currentLang)

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
      phone = user.phone,
      country = user.country,
      birthdate = user.birthdate,
      bio = user.bio,
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
          put("phone", user.phone)
          put("country", user.country)
          put("bio", user.bio)
          put("birthdate", user.birthdate)
          put("created_at", user.createdAt)
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
          put("phone", user.phone)
          put("country", user.country)
          put("bio", user.bio)
          put("birthdate", user.birthdate)
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
    prefs.edit()
      .putBoolean("has_registered", true)
      .putString("saved_username", username)
      .putString("saved_password", password)
      .apply()
  }

  suspend fun fetchAllUsers(): List<SupabaseUser> {
    val users = SupabaseClient.client
      .from("users")
      .select()
      .decodeList<SupabaseUser>()
    return users.sortedByDescending { it.createdAt }
  }

  suspend fun loadUserData(userId: String) {
    val moneyRows = SupabaseClient.client
      .from("money_entries")
      .select {
        filter { eq("user_id", userId) }
      }
      .decodeList<SupabaseMoneyEntry>()

    val income = moneyRows.filter { it.type == EntryType.Income.name }.map { row ->
      MoneyEntry(
        id = row.id,
        type = EntryType.Income,
        amount = row.amount,
        date = row.date,
        category = row.category,
        note = row.note,
        sourceOrMethod = row.sourceOrMethod,
        channelOrBank = row.channelOrBank,
      )
    }
    val expense = moneyRows.filter { it.type == EntryType.Expense.name }.map { row ->
      MoneyEntry(
        id = row.id,
        type = EntryType.Expense,
        amount = row.amount,
        date = row.date,
        category = row.category,
        note = row.note,
        sourceOrMethod = row.sourceOrMethod,
        channelOrBank = row.channelOrBank,
      )
    }

    val savingRows = SupabaseClient.client
      .from("saving_entries")
      .select {
        filter { eq("user_id", userId) }
      }
      .decodeList<SupabaseSavingEntry>()
    val saving = savingRows.map { row ->
      SavingEntry(
        id = row.id,
        amount = row.amount,
        date = row.date,
        goal = row.goal,
        note = row.note,
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
      )
    }

    withContext(Dispatchers.Main) {
      incomeEntries.clear()
      incomeEntries.addAll(income)
      expenseEntries.clear()
      expenseEntries.addAll(expense)
      savingEntries.clear()
      savingEntries.addAll(saving)
      dreamEntries.clear()
      dreamEntries.addAll(dreams)
    }
  }

  fun signInWithCredentials(username: String, password: String) {
    if (username.isBlank() || password.isBlank()) {
      alertMessage = strings["signin_missing"]
      showAlert = true
      return
    }
    if (username.trim() == "admin" && password == "adminsolvixstudio") {
      showAuth = false
      showAdmin = true
      adminLoggedIn = true
      adminUsername = "admin"
      adminPassword = ""
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
            persistCredentials(matchedUser.username, matchedUser.password)
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

  suspend fun insertSavingEntry(userId: String, entry: SavingEntry) {
    SupabaseClient.client
      .from("saving_entries")
      .insert(
        buildJsonObject {
          put("id", entry.id)
          put("user_id", userId)
          put("amount", entry.amount)
          put("date", entry.date)
          put("goal", entry.goal)
          put("note", entry.note)
        },
      )
  }

  suspend fun updateSavingEntry(entry: SavingEntry) {
    SupabaseClient.client
      .from("saving_entries")
      .update(
        buildJsonObject {
          put("amount", entry.amount)
          put("date", entry.date)
          put("goal", entry.goal)
          put("note", entry.note)
        },
      ) {
        filter { eq("id", entry.id) }
      }
  }

  suspend fun deleteSavingEntry(entryId: String) {
    SupabaseClient.client
      .from("saving_entries")
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
    val weak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    val biometricOk = strong == BiometricManager.BIOMETRIC_SUCCESS || weak == BiometricManager.BIOMETRIC_SUCCESS
    return biometricOk && keyguardManager.isDeviceSecure
  }

  fun launchFingerprintAuth(onSuccess: () -> Unit) {
    val activity = context as? FragmentActivity
    if (activity == null) {
      alertMessage = strings["fingerprint_activity_required"]
      showAlert = true
      return
    }
    if (!canUseFingerprint()) {
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
      .setTitle(strings["fingerprint_prompt_title"])
      .setSubtitle(strings["fingerprint_prompt_subtitle"])
      .setNegativeButtonText(strings["confirm_cancel"])
      .build()
    prompt.authenticate(promptInfo)
  }

  TabunganTheme(theme = currentTheme) {
    CompositionLocalProvider(
      LocalStrings provides strings,
      LocalLanguage provides currentLang,
    ) {
      val colors = LocalAppColors.current
      val modalOpen = showLoading || showSplash || showAuth || showConfirm || showAlert
      val displayName = currentUser?.name ?: strings["guest"]
      val displayUsername = currentUser?.username ?: strings["guest"]
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
          fingerprintPrompted = false
        }
      }
      LaunchedEffect(showAuth, authTab, fingerprintEnabled, hasRegistered) {
        if (!showAuth || authTab != AuthTab.SignIn) return@LaunchedEffect
        if (!fingerprintEnabled || !hasRegistered || fingerprintPrompted) return@LaunchedEffect
        if (!canUseFingerprint()) return@LaunchedEffect
        fingerprintPrompted = true
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
            val navBlur = showSplash || showAuth
            Box {
              BottomNav(
                current = currentPage,
                onSelect = { page -> currentPage = page },
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
            LazyColumn(
              modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (blurRadius.value > 0f) Modifier.blur(blurRadius) else Modifier),
              contentPadding = PaddingValues(
                start = AppDimens.pagePadding,
                end = AppDimens.pagePadding,
                bottom = 24.dp,
                top = 0.dp,
              ),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              item {
                TopBar(
                  onProfileClick = { showProfileMenu = true },
                  showMenu = showProfileMenu,
                  onDismissMenu = { showProfileMenu = false },
                  onNavigate = { page ->
                    currentPage = page
                    showProfileMenu = false
                  },
                  onAdmin = { showAdmin = true },
                  onLogout = {
                    showProfileMenu = false
                    requestConfirm(strings["logout_confirm"]) {
                      isLoggedIn = false
                      currentUser = null
                      showSplash = false
                      showAuth = false
                      loadingTarget = LoadingTarget.Logout
                      showLoading = true
                      clearAuthFields(
                        onSignInUsername = { signInUsername = it },
                        onSignInPassword = { signInPassword = it },
                        onSignUpName = { signUpName = it },
                        onSignUpEmail = { signUpEmail = it },
                        onSignUpPhone = { signUpPhone = it },
                        onSignUpCountry = { signUpCountry = it },
                        onSignUpBirthdate = { signUpBirthdate = it },
                        onSignUpBio = { signUpBio = it },
                        onSignUpUsername = { signUpUsername = it },
                        onSignUpPassword = { signUpPassword = it },
                      )
                      toastMessage = strings["logout_success"]
                      toastVisible = true
                    }
                  },
                  displayName = displayName,
                  displayUsername = displayUsername,
                  strings = strings,
                  theme = currentTheme,
                )
              }

              if (currentPage.showHero()) {
                val filtered = filterByRange(incomeEntries + expenseEntries, summaryRange)
                val incomeTotal = filtered.filter { it.type == EntryType.Income }.sumOf { it.amount }
                val expenseTotal = filtered.filter { it.type == EntryType.Expense }.sumOf { it.amount }
                item {
                  HeroSummary(
                    range = summaryRange,
                    onRangeChange = { summaryRange = it },
                    incomeTotal = incomeTotal,
                    expenseTotal = expenseTotal,
                    strings = strings,
                  )
                }
              }

              item {
                when (currentPage) {
                  Page.Income -> IncomePage(
                    entries = incomeEntries,
                    onSave = { entry ->
                      incomeEntries.add(entry)
                      if (activeUserId.isNotBlank()) {
                        scope.launch(Dispatchers.IO) { insertMoneyEntry(activeUserId, entry) }
                      }
                    },
                    onUpdate = { entry ->
                      val index = incomeEntries.indexOfFirst { it.id == entry.id }
                      if (index >= 0) incomeEntries[index] = entry
                      if (activeUserId.isNotBlank()) {
                        scope.launch(Dispatchers.IO) { updateMoneyEntry(entry) }
                      }
                    },
                    onDelete = { entry ->
                      requestConfirm(strings["confirm_delete_income"]) {
                        incomeEntries.removeAll { it.id == entry.id }
                        if (activeUserId.isNotBlank()) {
                          scope.launch(Dispatchers.IO) { deleteMoneyEntry(entry.id) }
                        }
                      }
                    },
                    strings = strings,
                  )
                  Page.Expense -> ExpensePage(
                    entries = expenseEntries,
                    onSave = { entry ->
                      expenseEntries.add(entry)
                      if (activeUserId.isNotBlank()) {
                        scope.launch(Dispatchers.IO) { insertMoneyEntry(activeUserId, entry) }
                      }
                    },
                    onUpdate = { entry ->
                      val index = expenseEntries.indexOfFirst { it.id == entry.id }
                      if (index >= 0) expenseEntries[index] = entry
                      if (activeUserId.isNotBlank()) {
                        scope.launch(Dispatchers.IO) { updateMoneyEntry(entry) }
                      }
                    },
                    onDelete = { entry ->
                      requestConfirm(strings["confirm_delete_expense"]) {
                        expenseEntries.removeAll { it.id == entry.id }
                        if (activeUserId.isNotBlank()) {
                          scope.launch(Dispatchers.IO) { deleteMoneyEntry(entry.id) }
                        }
                      }
                    },
                    strings = strings,
                  )
                  Page.Dreams -> DreamsPage(
                    entries = dreamEntries,
                    onSave = { entry ->
                      dreamEntries.add(entry)
                      if (activeUserId.isNotBlank()) {
                        scope.launch(Dispatchers.IO) { insertDreamEntry(activeUserId, entry) }
                      }
                    },
                    onUpdate = { entry ->
                      val index = dreamEntries.indexOfFirst { it.id == entry.id }
                      if (index >= 0) dreamEntries[index] = entry
                      if (activeUserId.isNotBlank()) {
                        scope.launch(Dispatchers.IO) { updateDreamEntry(entry) }
                      }
                    },
                    onDelete = { entry ->
                      requestConfirm(strings["confirm_delete_dream"]) {
                        dreamEntries.removeAll { it.id == entry.id }
                        if (activeUserId.isNotBlank()) {
                          scope.launch(Dispatchers.IO) { deleteDreamEntry(entry.id) }
                        }
                      }
                    },
                    strings = strings,
                  )
                  Page.History -> HistoryPage(entries = incomeEntries + expenseEntries, strings = strings)
                  Page.Saving -> SavingPage(
                    entries = savingEntries,
                    onSave = { entry ->
                      savingEntries.add(entry)
                      if (activeUserId.isNotBlank()) {
                        scope.launch(Dispatchers.IO) { insertSavingEntry(activeUserId, entry) }
                      }
                    },
                    onUpdate = { entry ->
                      val index = savingEntries.indexOfFirst { it.id == entry.id }
                      if (index >= 0) savingEntries[index] = entry
                      if (activeUserId.isNotBlank()) {
                        scope.launch(Dispatchers.IO) { updateSavingEntry(entry) }
                      }
                    },
                    onDelete = { entry ->
                      requestConfirm(strings["confirm_delete_saving"]) {
                        savingEntries.removeAll { it.id == entry.id }
                        if (activeUserId.isNotBlank()) {
                          scope.launch(Dispatchers.IO) { deleteSavingEntry(entry.id) }
                        }
                      }
                    },
                    strings = strings,
                  )
                  Page.Calculator -> CalculatorPage()
                  Page.Report -> ReportPage(
                    incomeEntries,
                    expenseEntries,
                    strings = strings,
                    language = currentLang,
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
                        scope.launch(Dispatchers.IO) { updateUserProfile(updated) }
                      }
                      toastMessage = strings["save_profile"]
                      toastVisible = true
                    },
                    onLogout = {
                      isLoggedIn = false
                      currentUser = null
                      showSplash = false
                      showAuth = false
                      loadingTarget = LoadingTarget.Logout
                      showLoading = true
                      clearAuthFields(
                        onSignInUsername = { signInUsername = it },
                        onSignInPassword = { signInPassword = it },
                        onSignUpName = { signUpName = it },
                        onSignUpEmail = { signUpEmail = it },
                        onSignUpPhone = { signUpPhone = it },
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
                          prefs.edit().putBoolean("fingerprint_enabled", true).apply()
                          alertMessage = strings["fingerprint_enabled"]
                          showAlert = true
                        } else {
                          fingerprintEnabled = false
                          prefs.edit().putBoolean("fingerprint_enabled", false).apply()
                          alertMessage = strings["fingerprint_required"]
                          showAlert = true
                        }
                      } else {
                        fingerprintEnabled = false
                        prefs.edit().putBoolean("fingerprint_enabled", false).apply()
                      }
                    },
                    language = currentLang,
                    onLanguageChange = {
                      currentLang = it
                      prefs.edit().putString("app_language", if (it == AppLanguage.ID) "ID" else "EN").apply()
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

              item {
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
                    prefs.edit().putBoolean("has_seen_welcome", true).apply()
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
                prefs.edit().putBoolean("has_seen_welcome", true).apply()
                if (fingerprintEnabled && canUseFingerprint()) {
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
                    if (fingerprintEnabled && hasRegistered) {
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
                    val calendar = java.util.Calendar.getInstance()
                    AppTextField(strings["label_name"], value = signUpName, onValueChange = { signUpName = it })
                    AppTextField(strings["label_email"], value = signUpEmail, onValueChange = { signUpEmail = it })
                    AppTextField(strings["label_phone"], value = signUpPhone, onValueChange = { signUpPhone = it })
                    AppDropdown(
                      label = strings["label_country"],
                      placeholder = strings["placeholder_country"],
                      options = optionList(currentLang, OptionList.Countries),
                      selected = signUpCountry,
                      onSelected = { signUpCountry = it },
                    )
                    AppTextField(
                      strings["label_birthdate"],
                      value = signUpBirthdate,
                      onValueChange = { signUpBirthdate = it },
                      placeholder = strings["placeholder_date"],
                      trailing = {
                        Text(
                          text = "📅",
                          modifier = Modifier.clickable {
                            val dialog = DatePickerDialog(
                              context,
                              { _, year, month, dayOfMonth ->
                                signUpBirthdate = String.format("%02d-%02d-%04d", dayOfMonth, month + 1, year)
                              },
                              calendar.get(java.util.Calendar.YEAR),
                              calendar.get(java.util.Calendar.MONTH),
                              calendar.get(java.util.Calendar.DAY_OF_MONTH),
                            )
                            dialog.show()
                          },
                        )
                      },
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
                            phone = signUpPhone,
                            country = signUpCountry,
                            bio = signUpBio,
                            birthdate = signUpBirthdate,
                            createdAt = Instant.now().toString(),
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
                GhostButton(text = strings["close"]) { showAuth = false }
              }
            }
          }
        }

        if (showAdmin) {
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
                Text(text = strings["admin_title"], fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (!adminLoggedIn) {
                  AppTextField(strings["admin_username"], value = adminUsername, onValueChange = { adminUsername = it })
                  AppTextField(
                    strings["admin_password"],
                    value = adminPassword,
                    onValueChange = { adminPassword = it },
                    isPassword = true,
                  )
                  GradientButton(text = strings["admin_login"]) {
                    if (adminUsername == "admin" && adminPassword == "adminsolvixstudio") {
                      adminLoggedIn = true
                      scope.launch(Dispatchers.IO) {
                        val users = fetchAllUsers()
                        withContext(Dispatchers.Main) {
                          adminUsers.clear()
                          adminUsers.addAll(users)
                        }
                      }
                    } else {
                      alertMessage = strings["admin_login_failed"]
                      showAlert = true
                    }
                  }
                } else {
                  Text(text = strings["admin_users_title"], color = colors.muted, fontSize = 12.sp)
                  if (adminUsers.isEmpty()) {
                    Text(text = strings["admin_no_users"], color = colors.muted, fontSize = 12.sp)
                  } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                      adminUsers.forEach { user ->
                        Column(
                          modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.bg2)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        ) {
                          Text(text = user.name, fontWeight = FontWeight.Bold)
                          Text(text = user.email, color = colors.muted, fontSize = 12.sp)
                          Text(text = user.phone, color = colors.muted, fontSize = 12.sp)
                          Text(text = user.country, color = colors.muted, fontSize = 12.sp)
                          Text(text = user.createdAt, color = colors.muted, fontSize = 12.sp)
                        }
                      }
                    }
                  }
                  GhostButton(text = strings["admin_logout"]) {
                    adminLoggedIn = false
                    adminPassword = ""
                    adminUsers.clear()
                  }
                }
                GhostButton(text = strings["close"]) { showAdmin = false }
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
            LoadingLogo(text = strings["loading"])
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
    this == Page.Expense ||
    this == Page.Saving ||
    this == Page.Dreams
}

private fun clearAuthFields(
  onSignInUsername: (String) -> Unit,
  onSignInPassword: (String) -> Unit,
  onSignUpName: (String) -> Unit,
  onSignUpEmail: (String) -> Unit,
  onSignUpPhone: (String) -> Unit,
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
  onSignUpPhone("")
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
  onAdmin: () -> Unit,
  onLogout: () -> Unit,
  displayName: String,
  displayUsername: String,
  strings: AppStrings,
  theme: ThemeName,
) {
  val colors = LocalAppColors.current
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
      ChipButton(text = if (displayUsername.isBlank()) strings["guest"] else displayUsername, onClick = onProfileClick)
      DropDownMenuCard(
        expanded = showMenu,
        onDismiss = onDismissMenu,
        modifier = Modifier.width(220.dp),
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
          MenuItem(text = strings["menu_theme"], emoji = themePageIcon(theme, Page.Themes)) { onNavigate(Page.Themes) }
          MenuItem(text = strings["menu_calculator"], emoji = themePageIcon(theme, Page.Calculator)) { onNavigate(Page.Calculator) }
          MenuItem(text = strings["menu_report"], emoji = themePageIcon(theme, Page.Report)) { onNavigate(Page.Report) }
          MenuItem(text = strings["menu_profile"], emoji = themePageIcon(theme, Page.Profile)) { onNavigate(Page.Profile) }
          MenuItem(text = strings["menu_settings"], emoji = themePageIcon(theme, Page.Settings)) { onNavigate(Page.Settings) }
          MenuItem(text = strings["menu_logout"], emoji = "🚪", color = colors.danger) { onLogout() }
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
  Row(
    modifier = modifier
      .fillMaxWidth()
      .shadow(20.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
      .background(colors.card, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
      .padding(horizontal = 10.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    items.forEach { item ->
      val active = current == item
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 4.dp)
          .clickable { onSelect(item) },
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(text = themePageIcon(theme, item), fontSize = 26.sp, color = if (active) colors.text else colors.muted)
        Text(
          text = pageLabel(item, strings),
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (active) colors.text else colors.muted,
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
        .offset(x = (-60).dp, y = (-80 + float1).dp),
      brush = Brush.radialGradient(
        listOf(colors.accent, Color.Transparent),
      ),
    )
    Blob(
      modifier = Modifier
        .size(280.dp)
        .align(Alignment.BottomEnd)
        .offset(x = 80.dp, y = (120 + float2).dp),
      brush = Brush.radialGradient(
        listOf(colors.accent2, Color.Transparent),
      ),
    )
    Blob(
      modifier = Modifier
        .size(280.dp)
        .align(Alignment.CenterEnd)
        .offset(x = 120.dp, y = float1.dp),
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
private fun LoadingLogo(text: String) {
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
    Page.Saving -> strings["page_saving"]
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
