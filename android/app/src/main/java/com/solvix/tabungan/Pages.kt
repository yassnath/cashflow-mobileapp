package com.solvix.tabungan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

@Composable
fun HeroSummary(
  range: SummaryRange,
  onRangeChange: (SummaryRange) -> Unit,
  incomeTotal: Int,
  expenseTotal: Int,
  strings: AppStrings,
) {
  val colors = LocalAppColors.current
  val options = SummaryRange.values().toList()
  AppCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${strings["summary_title"]} ${summaryRangeLabel(range, strings)}",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = colors.text,
      )
      AppDropdown(
        label = "",
        placeholder = summaryRangeLabel(range, strings),
        options = options.map { summaryRangeLabel(it, strings) },
        selected = summaryRangeLabel(range, strings),
        onSelected = { label ->
          val selected = options.firstOrNull { summaryRangeLabel(it, strings) == label }
          if (selected != null) {
            onRangeChange(selected)
          }
        },
        modifier = Modifier
          .widthIn(max = 130.dp)
          .weight(0.45f, fill = false),
      )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      StatRow(icon = "⬆️", label = strings["summary_income"], value = formatRupiah(incomeTotal))
      StatRow(icon = "⬇️", label = strings["summary_expense"], value = formatRupiah(expenseTotal))
      StatRow(icon = "✨", label = strings["summary_balance"], value = formatRupiah(incomeTotal - expenseTotal))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
) {
  val colors = LocalAppColors.current
  val strings = LocalStrings.current
  var showPicker by remember { mutableStateOf(false) }
  val initialMillis = parseDate(value) ?: System.currentTimeMillis()
  val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
  val formatter = remember { SimpleDateFormat("dd-MM-yyyy", Locale.US) }

  AppTextField(
    label = label,
    value = value,
    onValueChange = onValueChange,
    placeholder = placeholder,
    textFontSize = 12.sp,
    placeholderFontSize = 11.sp,
    trailing = {
      Text(
        text = "📅",
        color = colors.muted,
        modifier = Modifier.clickable { showPicker = true },
      )
    },
  )

  if (showPicker) {
    DatePickerDialog(
      onDismissRequest = { showPicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val selected = datePickerState.selectedDateMillis
            if (selected != null) {
              onValueChange(formatter.format(Date(selected)))
            }
            showPicker = false
          },
        ) {
          Text(text = strings["ok"], color = colors.text)
        }
      },
      dismissButton = {
        TextButton(onClick = { showPicker = false }) {
          Text(text = strings["confirm_cancel"], color = colors.muted)
        }
      },
      colors = DatePickerDefaults.colors(
        containerColor = colors.card,
        titleContentColor = colors.text,
        headlineContentColor = colors.text,
        weekdayContentColor = colors.muted,
        subheadContentColor = colors.muted,
        dayContentColor = colors.text,
        selectedDayContainerColor = colors.accent,
        selectedDayContentColor = Color.White,
        todayDateBorderColor = colors.accent2,
        todayContentColor = colors.text,
      ),
    ) {
      DatePicker(
        state = datePickerState,
        title = null,
        headline = null,
        showModeToggle = false,
      )
    }
  }
}

@Composable
fun IncomePage(
  entries: List<MoneyEntry>,
  onSave: (MoneyEntry) -> Unit,
  onUpdate: (MoneyEntry) -> Unit,
  onDelete: (MoneyEntry) -> Unit,
  editEntry: MoneyEntry?,
  onEditConsumed: () -> Unit,
  strings: AppStrings,
) {
  val language = LocalLanguage.current
  var amount by rememberSaveable { mutableStateOf("") }
  var date by rememberSaveable { mutableStateOf("") }
  var category by rememberSaveable { mutableStateOf("") }
  var source by rememberSaveable { mutableStateOf("") }
  var channel by rememberSaveable { mutableStateOf("") }
  var note by rememberSaveable { mutableStateOf("") }
  var editingId by rememberSaveable { mutableStateOf<String?>(null) }

  LaunchedEffect(editEntry?.id) {
    val entry = editEntry
    if (entry != null && entry.type == EntryType.Income) {
      editingId = entry.id
      amount = entry.amount.toString()
      date = entry.date
      category = entry.category
      source = entry.sourceOrMethod
      channel = entry.channelOrBank
      note = entry.note
      onEditConsumed()
    }
  }

  Column {
    SectionTitle(icon = themePageIcon(LocalThemeName.current, Page.Income), title = strings["section_income_title"], subtitle = strings["section_income_subtitle"])
    AppCard {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(
          label = strings["label_amount"],
          value = amount,
          onValueChange = { amount = it },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        DateField(strings["label_date"], value = date, onValueChange = { date = it }, placeholder = strings["placeholder_date"])
        AppTextField(strings["label_category"], value = category, onValueChange = { category = it }, placeholder = strings["placeholder_category_income"])
        AppDropdown(
          label = strings["label_source"],
          placeholder = strings["placeholder_source"],
          options = optionList(language, OptionList.IncomeSources),
          selected = source,
          onSelected = { source = it },
        )
        AppDropdown(
          label = strings["label_channel"],
          placeholder = strings["placeholder_channel"],
          options = optionList(language, OptionList.IncomeChannels),
          selected = channel,
          onSelected = { channel = it },
        )
        AppTextField(strings["label_note"], value = note, onValueChange = { note = it }, placeholder = strings["placeholder_note"], minLines = 2)
        GradientButton(
          text = if (editingId == null) strings["save_income"] else strings["update_income"],
          onClick = {
            val entry = MoneyEntry(
              id = editingId ?: UUIDString(),
              type = EntryType.Income,
              amount = parseAmount(amount),
              date = date,
              category = category,
              note = note,
              sourceOrMethod = source,
              channelOrBank = channel,
            )
            if (editingId == null) {
              onSave(entry)
            } else {
              onUpdate(entry)
            }
            amount = ""
            date = ""
            category = ""
            source = ""
            channel = ""
            note = ""
            editingId = null
          },
        )
      }
    }
  }
}

@Composable
fun ExpensePage(
  entries: List<MoneyEntry>,
  onSave: (MoneyEntry) -> Unit,
  onUpdate: (MoneyEntry) -> Unit,
  onDelete: (MoneyEntry) -> Unit,
  editEntry: MoneyEntry?,
  onEditConsumed: () -> Unit,
  strings: AppStrings,
) {
  val language = LocalLanguage.current
  var amount by rememberSaveable { mutableStateOf("") }
  var date by rememberSaveable { mutableStateOf("") }
  var category by rememberSaveable { mutableStateOf("") }
  var method by rememberSaveable { mutableStateOf("") }
  var bank by rememberSaveable { mutableStateOf("") }
  var note by rememberSaveable { mutableStateOf("") }
  var editingId by rememberSaveable { mutableStateOf<String?>(null) }

  LaunchedEffect(editEntry?.id) {
    val entry = editEntry
    if (entry != null && entry.type == EntryType.Expense) {
      editingId = entry.id
      amount = entry.amount.toString()
      date = entry.date
      category = entry.category
      method = entry.sourceOrMethod
      bank = entry.channelOrBank
      note = entry.note
      onEditConsumed()
    }
  }

  Column {
    SectionTitle(icon = themePageIcon(LocalThemeName.current, Page.Expense), title = strings["section_expense_title"], subtitle = strings["section_expense_subtitle"])
    AppCard {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(
          label = strings["label_amount"],
          value = amount,
          onValueChange = { amount = it },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        DateField(strings["label_date"], value = date, onValueChange = { date = it }, placeholder = strings["placeholder_date"])
        AppTextField(strings["label_category"], value = category, onValueChange = { category = it }, placeholder = strings["placeholder_category_expense"])
        AppDropdown(
          label = strings["label_method"],
          placeholder = strings["placeholder_method"],
          options = optionList(language, OptionList.ExpenseMethods),
          selected = method,
          onSelected = { method = it },
        )
        AppDropdown(
          label = strings["label_bank"],
          placeholder = strings["placeholder_bank"],
          options = optionList(language, OptionList.ExpenseBanks),
          selected = bank,
          onSelected = { bank = it },
        )
        AppTextField(strings["label_note"], value = note, onValueChange = { note = it }, placeholder = strings["placeholder_note"], minLines = 2)
        GradientButton(
          text = if (editingId == null) strings["save_expense"] else strings["update_expense"],
          onClick = {
            val entry = MoneyEntry(
              id = editingId ?: UUIDString(),
              type = EntryType.Expense,
              amount = parseAmount(amount),
              date = date,
              category = category,
              note = note,
              sourceOrMethod = method,
              channelOrBank = bank,
            )
            if (editingId == null) {
              onSave(entry)
            } else {
              onUpdate(entry)
            }
            amount = ""
            date = ""
            category = ""
            method = ""
            bank = ""
            note = ""
            editingId = null
          },
        )
      }
    }
  }
}

@Composable
fun SavingPage(
  entries: List<SavingEntry>,
  onSave: (SavingEntry) -> Unit,
  onUpdate: (SavingEntry) -> Unit,
  onDelete: (SavingEntry) -> Unit,
  strings: AppStrings,
) {
  var amount by rememberSaveable { mutableStateOf("") }
  var date by rememberSaveable { mutableStateOf("") }
  var goal by rememberSaveable { mutableStateOf("") }
  var note by rememberSaveable { mutableStateOf("") }
  var editingId by rememberSaveable { mutableStateOf<String?>(null) }

  Column {
    SectionTitle(icon = themePageIcon(LocalThemeName.current, Page.Saving), title = strings["section_saving_title"], subtitle = strings["section_saving_subtitle"])
    AppCard {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(strings["label_amount"], value = amount, onValueChange = { amount = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        DateField(strings["label_date"], value = date, onValueChange = { date = it }, placeholder = strings["placeholder_date"])
        AppTextField(strings["label_goal"], value = goal, onValueChange = { goal = it }, placeholder = strings["placeholder_goal"])
        AppTextField(strings["label_note"], value = note, onValueChange = { note = it }, placeholder = strings["placeholder_note"], minLines = 2)
        GradientButton(
          text = if (editingId == null) strings["save_saving"] else strings["update_saving"],
          onClick = {
            val entry = SavingEntry(
              id = editingId ?: UUIDString(),
              amount = parseAmount(amount),
              date = date,
              goal = goal,
              note = note,
            )
            if (editingId == null) onSave(entry) else onUpdate(entry)
            amount = ""
            date = ""
            goal = ""
            note = ""
            editingId = null
          },
        )
      }
    }

    SavingList(
      entries = entries,
      onEdit = { entry ->
        editingId = entry.id
        amount = entry.amount.toString()
        date = entry.date
        goal = entry.goal
        note = entry.note
      },
      onDelete = onDelete,
    )
  }
}

@Composable
fun DreamsPage(
  entries: List<DreamEntry>,
  onSave: (DreamEntry) -> Unit,
  onUpdate: (DreamEntry) -> Unit,
  onDelete: (DreamEntry) -> Unit,
  strings: AppStrings,
) {
  var title by rememberSaveable { mutableStateOf("") }
  var target by rememberSaveable { mutableStateOf("") }
  var current by rememberSaveable { mutableStateOf("") }
  var deadline by rememberSaveable { mutableStateOf("") }
  var note by rememberSaveable { mutableStateOf("") }
  var editingId by rememberSaveable { mutableStateOf<String?>(null) }

  Column {
    SectionTitle(icon = themePageIcon(LocalThemeName.current, Page.Dreams), title = strings["section_dreams_title"], subtitle = strings["section_dreams_subtitle"])
    AppCard {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(strings["label_target_name"], value = title, onValueChange = { title = it }, placeholder = strings["placeholder_target"])
        AppTextField(strings["label_target_amount"], value = target, onValueChange = { target = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        AppTextField(strings["label_target_current"], value = current, onValueChange = { current = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        DateField(strings["label_deadline"], value = deadline, onValueChange = { deadline = it }, placeholder = strings["placeholder_date"])
        AppTextField(strings["label_note"], value = note, onValueChange = { note = it }, placeholder = strings["placeholder_strategy"], minLines = 2)
        GradientButton(
          text = if (editingId == null) strings["save_dream"] else strings["update_dream"],
          onClick = {
            val entry = DreamEntry(
              id = editingId ?: UUIDString(),
              title = title,
              target = parseAmount(target),
              current = parseAmount(current),
              deadline = deadline,
              note = note,
            )
            if (editingId == null) onSave(entry) else onUpdate(entry)
            title = ""
            target = ""
            current = ""
            deadline = ""
            note = ""
            editingId = null
          },
        )
      }
    }
  }
}

@Composable
fun CalculatorPage() {
  var display by rememberSaveable { mutableStateOf("0") }
  val strings = LocalStrings.current
  val colors = LocalAppColors.current
  Column {
    SectionTitle(icon = themePageIcon(LocalThemeName.current, Page.Calculator), title = strings["calculator_title"], subtitle = strings["calculator_subtitle"])
    AppCard {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusSm))
            .background(colors.bg2)
            .padding(12.dp),
          contentAlignment = Alignment.CenterEnd,
        ) {
          Text(text = display, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.text)
        }

        val keys = listOf(
          "7", "8", "9", "÷",
          "4", "5", "6", "×",
          "1", "2", "3", "−",
          "0", "⌫", "C", "+",
        )
        val rows: List<List<String>> = listOf(
          keys.subList(0, 4),
          keys.subList(4, 8),
          keys.subList(8, 12),
          keys.subList(12, 16),
        )
        rows.forEach { row ->
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { key ->
              CalculatorKey(
                label = key,
                onPress = { display = calculate(display, key) },
              )
            }
          }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
          CalculatorKey(
            label = "=",
            isWide = true,
            isPrimary = true,
            onPress = { display = calculate(display, "=") },
          )
        }
      }
    }
  }
}

@Composable
private fun RowScope.CalculatorKey(label: String, onPress: () -> Unit, isWide: Boolean = false, isPrimary: Boolean = false) {
  val colors = LocalAppColors.current
  val backgroundColor = when {
    label == "C" || label == "⌫" -> colors.danger.copy(alpha = 0.2f)
    label == "÷" || label == "×" || label == "−" || label == "+" -> colors.accent.copy(alpha = 0.2f)
    else -> colors.bg2
  }
  val backgroundModifier = if (isPrimary) {
    Modifier.background(brush = Brush.linearGradient(listOf(colors.accent, colors.accent2)))
  } else {
    Modifier.background(color = backgroundColor)
  }
  val textColor = when {
    isPrimary -> Color.White
    label == "C" || label == "⌫" -> Color(0xFFB92643)
    label == "÷" || label == "×" || label == "−" || label == "+" -> Color(0xFFA55B00)
    else -> colors.text
  }

  Box(
    modifier = Modifier
      .weight(if (isWide) 1f else 0.25f)
      .height(48.dp)
      .clip(RoundedCornerShape(14.dp))
      .then(backgroundModifier)
      .shadow(0.dp, RoundedCornerShape(14.dp))
      .clickable { onPress() }
      .padding(vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = textColor)
  }
}

@Composable
fun ReportPage(
  income: List<MoneyEntry>,
  expense: List<MoneyEntry>,
  strings: AppStrings,
  language: AppLanguage,
  startYear: Int,
  onToast: (String) -> Unit,
) {
  val colors = LocalAppColors.current
  val theme = LocalThemeName.current
  val context = LocalContext.current
  val incomeTotal = income.sumOf { it.amount }
  val expenseTotal = expense.sumOf { it.amount }
  var modeIndex by rememberSaveable { mutableStateOf(0) }
  val monthly = buildMonthlySeries(income, expense, language)
  val yearly = buildYearlySeries(income, expense, startYear)
  val series = if (modeIndex == 0) monthly else yearly
  val totals = if (modeIndex == 0) totalsFromSeries(monthly) else totalsFromSeries(yearly)
  val modeLabel = if (modeIndex == 0) strings["report_monthly"] else strings["report_yearly"]
  val dateTag = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) }
  val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    try {
      writeReportPdf(
        context = context,
        uri = uri,
        title = "${strings["section_report_title"]} - $modeLabel",
        series = series,
        totals = totals,
      )
      openExportedFile(context, uri, "application/pdf")
      onToast(strings["export_success_pdf"])
    } catch (ex: Exception) {
      onToast(strings["export_failed"])
    }
  }
  val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    try {
      writeReportCsv(
        context = context,
        uri = uri,
        title = "${strings["section_report_title"]} - $modeLabel",
        series = series,
        totals = totals,
      )
      openExportedFile(context, uri, "text/csv")
      onToast(strings["export_success_csv"])
    } catch (ex: Exception) {
      onToast(strings["export_failed"])
    }
  }
  val chartBackground = if (isDarkTheme(theme)) {
    Brush.linearGradient(listOf(Color(0xFF2A2F4D), Color(0xFF3A4270)))
  } else {
    Brush.linearGradient(listOf(Color(0xFF0F1220), Color(0xFF1B2034)))
  }
  Column {
    SectionTitle(icon = themePageIcon(LocalThemeName.current, Page.Report), title = strings["section_report_title"])
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(12.dp, RoundedCornerShape(AppDimens.radiusLg))
        .clip(RoundedCornerShape(AppDimens.radiusLg))
        .background(chartBackground)
        .padding(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val inactiveBrush = Brush.linearGradient(
          listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(999.dp))
              .background(if (modeIndex == 0) Brush.linearGradient(listOf(colors.accent, colors.accent2)) else inactiveBrush)
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = strings["report_monthly"],
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.clickable { modeIndex = 0 },
            )
          }
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(999.dp))
              .background(if (modeIndex == 1) Brush.linearGradient(listOf(colors.accent, colors.accent2)) else inactiveBrush)
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = strings["report_yearly"],
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.clickable { modeIndex = 1 },
            )
          }
        }

        LineChart(series = series)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          ReportLine(label = strings["report_total_income"], value = formatRupiah(totals.first), positive = true)
          ReportLine(label = strings["report_total_expense"], value = formatRupiah(totals.second), positive = false)
          ReportLine(label = strings["report_total_balance"], value = formatRupiah(totals.first - totals.second), positive = true)
        }
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      GradientButton(
        text = strings["export_pdf"],
        onClick = { pdfLauncher.launch("CashFlow_${modeLabel.lowercase()}_$dateTag.pdf") },
      )
      GradientButton(
        text = strings["export_csv"],
        onClick = { csvLauncher.launch("CashFlow_${modeLabel.lowercase()}_$dateTag.csv") },
      )
    }
  }
}

@Composable
private fun ReportLine(label: String, value: String, positive: Boolean) {
  val colors = LocalAppColors.current
  val theme = LocalThemeName.current
  val labelColor = if (isDarkTheme(theme)) colors.text else Color.White.copy(alpha = 0.8f)
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(text = label, color = labelColor, fontSize = 13.sp)
    Text(
      text = value,
      fontWeight = FontWeight.Bold,
      color = if (positive) colors.accent2 else colors.danger,
      fontSize = 13.sp,
    )
  }
}

private fun openExportedFile(context: Context, uri: Uri, mimeType: String) {
  val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, mimeType)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
    // Viewer not available; export still succeeded.
  }
}

private fun writeReportPdf(
  context: Context,
  uri: Uri,
  title: String,
  series: ChartSeries,
  totals: Pair<Int, Int>,
) {
  val document = PdfDocument()
  val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
  val page = document.startPage(pageInfo)
  val canvas = page.canvas
  val paint = Paint().apply {
    color = Color.Black.toArgb()
    textSize = 18f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  var y = 40f
  canvas.drawText(title, 40f, y, paint)
  y += 22f
  paint.textSize = 11f
  paint.typeface = Typeface.DEFAULT
  val generated = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
  canvas.drawText("Generated: $generated", 40f, y, paint)
  y += 22f

  paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  canvas.drawText("Label", 40f, y, paint)
  canvas.drawText("Income", 240f, y, paint)
  canvas.drawText("Expense", 360f, y, paint)
  canvas.drawText("Balance", 480f, y, paint)
  y += 16f
  paint.typeface = Typeface.DEFAULT

  for (i in series.labels.indices) {
    val label = series.labels[i]
    val income = series.income.getOrNull(i) ?: 0
    val expense = series.expense.getOrNull(i) ?: 0
    val balance = income - expense
    canvas.drawText(label, 40f, y, paint)
    canvas.drawText(formatRupiah(income), 240f, y, paint)
    canvas.drawText(formatRupiah(expense), 360f, y, paint)
    canvas.drawText(formatRupiah(balance), 480f, y, paint)
    y += 16f
  }

  y += 12f
  paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  canvas.drawText("Total Income: ${formatRupiah(totals.first)}", 40f, y, paint)
  y += 16f
  canvas.drawText("Total Expense: ${formatRupiah(totals.second)}", 40f, y, paint)
  y += 16f
  canvas.drawText("Total Balance: ${formatRupiah(totals.first - totals.second)}", 40f, y, paint)

  document.finishPage(page)
  context.contentResolver.openOutputStream(uri)?.use { output ->
    document.writeTo(output)
  }
  document.close()
}

private fun writeReportCsv(
  context: Context,
  uri: Uri,
  title: String,
  series: ChartSeries,
  totals: Pair<Int, Int>,
) {
  context.contentResolver.openOutputStream(uri)?.use { output ->
    OutputStreamWriter(output).use { writer ->
      writer.appendLine(title)
      writer.appendLine("Label,Income,Expense,Balance")
      for (i in series.labels.indices) {
        val label = series.labels[i]
        val income = series.income.getOrNull(i) ?: 0
        val expense = series.expense.getOrNull(i) ?: 0
        val balance = income - expense
        writer.appendLine("$label,${formatRupiah(income)},${formatRupiah(expense)},${formatRupiah(balance)}")
      }
      writer.appendLine()
      writer.appendLine("Total Income,${formatRupiah(totals.first)}")
      writer.appendLine("Total Expense,${formatRupiah(totals.second)}")
      writer.appendLine("Total Balance,${formatRupiah(totals.first - totals.second)}")
      writer.flush()
    }
  }
}

@Composable
fun HistoryPage(
  entries: List<MoneyEntry>,
  strings: AppStrings,
  onEdit: (MoneyEntry) -> Unit,
  onDelete: (MoneyEntry) -> Unit,
) {
  val colors = LocalAppColors.current
  val defaultRange = remember {
    val now = Calendar.getInstance()
    val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    val start = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_MONTH, 1)
    }.time
    val end = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_MONTH, now.getActualMaximum(Calendar.DAY_OF_MONTH))
    }.time
    formatter.format(start) to formatter.format(end)
  }
  var fromDate by rememberSaveable { mutableStateOf(defaultRange.first) }
  var toDate by rememberSaveable { mutableStateOf(defaultRange.second) }
  val filtered = entries.filter { entry ->
    val date = parseDate(entry.date) ?: return@filter false
    val from = parseDate(fromDate)
    val to = parseDate(toDate)
    val afterFrom = from == null || date >= from
    val beforeTo = to == null || date <= to
    afterFrom && beforeTo
  }
  val sorted = filtered.sortedByDescending { entry -> parseDate(entry.date) ?: 0L }
  val incomeTotal = filtered.filter { it.type == EntryType.Income }.sumOf { it.amount }
  val expenseTotal = filtered.filter { it.type == EntryType.Expense }.sumOf { it.amount }

  Column {
    SectionTitle(icon = themePageIcon(LocalThemeName.current, Page.History), title = strings["section_history_title"], subtitle = strings["section_history_subtitle"])
    AppCard {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = strings["from"], fontSize = 11.sp, color = colors.text)
          Spacer(modifier = Modifier.height(4.dp))
          DateField(label = "", value = fromDate, onValueChange = { fromDate = it }, placeholder = strings["placeholder_date"])
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(text = strings["to"], fontSize = 11.sp, color = colors.text)
          Spacer(modifier = Modifier.height(4.dp))
          DateField(label = "", value = toDate, onValueChange = { toDate = it }, placeholder = strings["placeholder_date"])
        }
      }
    }
    Spacer(modifier = Modifier.height(10.dp))
    AppCard {
      Text(text = strings["summary_period"], fontWeight = FontWeight.Bold, fontSize = 14.sp)
      Spacer(modifier = Modifier.height(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatRow(icon = "⬆️", label = strings["summary_income"], value = formatRupiah(incomeTotal))
        StatRow(icon = "⬇️", label = strings["summary_expense"], value = formatRupiah(expenseTotal))
        StatRow(icon = "✨", label = strings["summary_balance"], value = formatRupiah(incomeTotal - expenseTotal))
      }
    }

    EntryList(
      title = strings["section_history_title"],
      entries = sorted,
      onEdit = onEdit,
      onDelete = onDelete,
    )
    if (filtered.isEmpty()) {
      Spacer(modifier = Modifier.height(10.dp))
      AppCard {
        Text(text = strings["no_transactions"], color = colors.muted, fontSize = 12.sp)
      }
    }
  }
}

@Composable
fun ProfilePage(
  user: UserProfile?,
  strings: AppStrings,
  onSave: (UserProfile) -> Unit,
  onLogout: () -> Unit,
) {
  val language = LocalLanguage.current
  var name by rememberSaveable { mutableStateOf(user?.name.orEmpty()) }
  var email by rememberSaveable { mutableStateOf(user?.email.orEmpty()) }
  var country by rememberSaveable { mutableStateOf(user?.country.orEmpty()) }
  var birthdate by rememberSaveable { mutableStateOf(user?.birthdate.orEmpty()) }
  var bio by rememberSaveable { mutableStateOf(user?.bio.orEmpty()) }
  var username by rememberSaveable { mutableStateOf(user?.username.orEmpty()) }
  var password by rememberSaveable { mutableStateOf(user?.password.orEmpty()) }

  LaunchedEffect(user) {
    name = user?.name.orEmpty()
    email = user?.email.orEmpty()
    country = user?.country.orEmpty()
    birthdate = user?.birthdate.orEmpty()
    bio = user?.bio.orEmpty()
    username = user?.username.orEmpty()
    password = user?.password.orEmpty()
  }

  Column {
    SectionTitle(icon = themePageIcon(LocalThemeName.current, Page.Profile), title = strings["section_profile_title"], subtitle = strings["section_profile_subtitle"])
    AppCard {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileLine(label = strings["label_name"], value = name.ifBlank { strings["guest"] })
        ProfileLine(label = strings["label_email"], value = email.ifBlank { "-" })
        ProfileLine(label = strings["label_country"], value = country.ifBlank { "-" })
        ProfileLine(label = strings["label_birthdate"], value = birthdate.ifBlank { "-" })
        ProfileLine(label = strings["label_bio"], value = bio.ifBlank { "-" })
        ProfileLine(label = strings["label_username"], value = username.ifBlank { "-" })
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    AppCard {
      Text(text = strings["profile_update"], fontWeight = FontWeight.Bold, fontSize = 16.sp)
      Spacer(modifier = Modifier.height(12.dp))
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(strings["label_name"], value = name, onValueChange = { name = it })
        AppTextField(strings["label_email"], value = email, onValueChange = { email = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        AppDropdown(
          label = strings["label_country"],
          placeholder = strings["placeholder_country"],
          options = optionList(language, OptionList.Countries),
          selected = country,
          onSelected = { country = it },
        )
        DateField(
          label = strings["label_birthdate"],
          value = birthdate,
          onValueChange = { birthdate = it },
          placeholder = strings["placeholder_date"],
        )
        AppTextField(strings["label_bio"], value = bio, onValueChange = { bio = it }, minLines = 2)
        AppTextField(strings["label_username"], value = username, onValueChange = { username = it })
        AppTextField(strings["password_new"], value = password, onValueChange = { password = it }, isPassword = true)
          GradientButton(text = strings["save_profile"]) {
            onSave(
              UserProfile(
                id = user?.id.orEmpty(),
                name = name,
                email = email,
                country = country,
                birthdate = birthdate,
                bio = bio,
                createdAt = user?.createdAt.orEmpty(),
                username = username,
                password = password,
            ),
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    GhostButton(text = strings["menu_logout"], onClick = onLogout, modifier = Modifier.padding(bottom = 10.dp))
  }
}

@Composable
private fun ProfileLine(label: String, value: String) {
  val colors = LocalAppColors.current
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(text = label, color = colors.text, fontSize = 12.sp)
    Text(text = value, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
  }
}

@Composable
fun SettingsPage(
  fingerprintEnabled: Boolean,
  onFingerprintToggle: (Boolean) -> Unit,
  faceUnlockEnabled: Boolean,
  onFaceUnlockToggle: (Boolean) -> Unit,
  language: AppLanguage,
  onLanguageChange: (AppLanguage) -> Unit,
  strings: AppStrings,
  onToast: (String) -> Unit,
) {
  val colors = LocalAppColors.current
  var langIndex by rememberSaveable { mutableStateOf(0) }
  var currentPassword by rememberSaveable { mutableStateOf("") }
  var newPassword by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }

  Column {
    LaunchedEffect(language) {
      langIndex = if (language == AppLanguage.ID) 0 else 1
    }
    SectionTitle(icon = themePageIcon(LocalThemeName.current, Page.Settings), title = strings["section_settings_title"], subtitle = strings["section_settings_subtitle"])
    AppCard {
      Text(text = strings["settings_security"], fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = strings["settings_fingerprint"], fontSize = 12.sp, color = colors.text)
          Text(
            text = strings["settings_fingerprint_desc"],
            color = colors.muted,
            fontSize = 11.sp,
          )
        }
        Switch(
          checked = fingerprintEnabled,
          onCheckedChange = onFingerprintToggle,
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = strings["settings_face"], fontSize = 12.sp, color = colors.text)
          Text(
            text = strings["settings_face_desc"],
            color = colors.muted,
            fontSize = 11.sp,
          )
        }
        Switch(
          checked = faceUnlockEnabled,
          onCheckedChange = onFaceUnlockToggle,
        )
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    AppCard {
      Text(text = strings["settings_language"], fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = strings["settings_language"], fontSize = 12.sp, color = colors.text)
        ChipToggle(
          options = listOf("IN", "EN"),
          selectedIndex = langIndex,
          onSelect = {
            langIndex = it
            onLanguageChange(if (it == 0) AppLanguage.ID else AppLanguage.EN)
          },
        )
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    AppCard {
      Text(text = strings["settings_change_password"], fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
      Spacer(modifier = Modifier.height(12.dp))
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(strings["password_current"], value = currentPassword, onValueChange = { currentPassword = it }, isPassword = true)
        AppTextField(strings["password_new"], value = newPassword, onValueChange = { newPassword = it }, isPassword = true)
        AppTextField(strings["password_confirm"], value = confirmPassword, onValueChange = { confirmPassword = it }, isPassword = true)
        GradientButton(text = strings["save_password"], onClick = {
          onToast(strings["save_password"])
        })
      }
    }
  }
}

@Composable
fun ThemesPage(currentTheme: ThemeName, onThemeSelected: (ThemeName) -> Unit) {
  val strings = LocalStrings.current
  Column {
    SectionTitle(icon = themePageIcon(LocalThemeName.current, Page.Themes), title = strings["section_themes_title"], subtitle = strings["section_themes_subtitle"])
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      ThemeCard(themeAppIcon(ThemeName.StandardLight), strings["theme_standard_light"], currentTheme == ThemeName.StandardLight, Color(0xFFFFF6E7)) {
        onThemeSelected(ThemeName.StandardLight)
      }
      ThemeCard(themeAppIcon(ThemeName.StandardDark), strings["theme_standard_dark"], currentTheme == ThemeName.StandardDark, Color(0xFFE6EBFF)) {
        onThemeSelected(ThemeName.StandardDark)
      }
      ThemeCard(themeAppIcon(ThemeName.CartoonFood), strings["theme_food"], currentTheme == ThemeName.CartoonFood, Color(0xFFFFF0DC)) {
        onThemeSelected(ThemeName.CartoonFood)
      }
      ThemeCard(themeAppIcon(ThemeName.CartoonSpace), strings["theme_space"], currentTheme == ThemeName.CartoonSpace, Color(0xFFE3E9FF)) {
        onThemeSelected(ThemeName.CartoonSpace)
      }
      ThemeCard(themeAppIcon(ThemeName.CartoonMonster), strings["theme_monster"], currentTheme == ThemeName.CartoonMonster, Color(0xFFE9FFF2)) {
        onThemeSelected(ThemeName.CartoonMonster)
      }
      ThemeCard(themeAppIcon(ThemeName.CartoonHero), strings["theme_hero"], currentTheme == ThemeName.CartoonHero, Color(0xFFFFE9EE)) {
        onThemeSelected(ThemeName.CartoonHero)
      }
      ThemeCard(themeAppIcon(ThemeName.CartoonSea), strings["theme_sea"], currentTheme == ThemeName.CartoonSea, Color(0xFFE6F6FF)) {
        onThemeSelected(ThemeName.CartoonSea)
      }
      ThemeCard(themeAppIcon(ThemeName.CartoonPlant), strings["theme_plant"], currentTheme == ThemeName.CartoonPlant, Color(0xFFEFFFE5)) {
        onThemeSelected(ThemeName.CartoonPlant)
      }
      ThemeCard(themeAppIcon(ThemeName.CartoonPinky), strings["theme_pinky"], currentTheme == ThemeName.CartoonPinky, Color(0xFFFFE6F2)) {
        onThemeSelected(ThemeName.CartoonPinky)
      }
      ThemeCard(themeAppIcon(ThemeName.CartoonColorful), strings["theme_colorful"], currentTheme == ThemeName.CartoonColorful, Color(0xFFEEF3FF)) {
        onThemeSelected(ThemeName.CartoonColorful)
      }
    }
  }
}

@Composable
private fun EntryList(
  title: String,
  entries: List<MoneyEntry>,
  onEdit: (MoneyEntry) -> Unit,
  onDelete: (MoneyEntry) -> Unit,
  readonly: Boolean = false,
) {
  if (entries.isEmpty()) return
  val colors = LocalAppColors.current
  Spacer(modifier = Modifier.height(12.dp))
  Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.text)
  Spacer(modifier = Modifier.height(8.dp))
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    entries.forEach { entry ->
      EntryCard(entry = entry, onEdit = onEdit, onDelete = onDelete, readonly = readonly)
    }
  }
}

@Composable
private fun EntryCard(
  entry: MoneyEntry,
  onEdit: (MoneyEntry) -> Unit,
  onDelete: (MoneyEntry) -> Unit,
  readonly: Boolean,
) {
  val colors = LocalAppColors.current
  val isDark = isDarkTheme(LocalThemeName.current)
  val amountColor = when (entry.type) {
    EntryType.Income -> if (isDark) Color(0xFF7BE27A) else Color(0xFF2E7D32)
    EntryType.Expense -> if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
  }
  val strings = LocalStrings.current
  val typeLabel = if (entry.type == EntryType.Income) strings["page_income"] else strings["page_expense"]
  val amountText = if (entry.type == EntryType.Income) "+ ${formatRupiah(entry.amount)}" else "- ${formatRupiah(entry.amount)}"
  val sourceText = entry.sourceOrMethod.ifBlank { "-" }
  val bankText = entry.channelOrBank.ifBlank { "-" }
  val sourceLabel = if (entry.type == EntryType.Income) strings["label_source"] else strings["label_method"]
  val bankLabel = if (entry.type == EntryType.Income) strings["label_channel"] else strings["label_bank"]
  val noteText = entry.note.ifBlank { "-" }
  AppCard(shape = RoundedCornerShape(AppDimens.radiusMd)) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(text = typeLabel, fontWeight = FontWeight.Bold, color = colors.text)
        Text(text = amountText, fontWeight = FontWeight.Bold, color = amountColor)
      }
      Text(text = entry.date, color = colors.muted, fontSize = 12.sp)
      Text(text = "${strings["label_category"]}: ${entry.category}", color = colors.muted, fontSize = 12.sp)
      Text(text = "$sourceLabel: $sourceText", color = colors.muted, fontSize = 12.sp)
      Text(text = "$bankLabel: $bankText", color = colors.muted, fontSize = 12.sp)
      Text(text = "${strings["label_note"]}: $noteText", color = colors.muted, fontSize = 12.sp)
      if (!readonly) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 40.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Brush.linearGradient(listOf(colors.accent, colors.accent2)))
              .clickable { onEdit(entry) },
            contentAlignment = Alignment.Center,
          ) {
            Text(text = strings["edit"], color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
          }
          Box(
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 40.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFFD32F2F))
              .clickable { onDelete(entry) },
            contentAlignment = Alignment.Center,
          ) {
            Text(text = strings["delete"], color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
          }
        }
      }
    }
  }
}

private data class ChartSeries(
  val labels: List<String>,
  val income: List<Int>,
  val expense: List<Int>,
)

private fun buildMonthlySeries(income: List<MoneyEntry>, expense: List<MoneyEntry>, language: AppLanguage): ChartSeries {
  val now = Calendar.getInstance()
  val currentYear = now.get(Calendar.YEAR)
  val labels = mutableListOf<String>()
  val incomeTotals = mutableListOf<Int>()
  val expenseTotals = mutableListOf<Int>()
  val locale = if (language == AppLanguage.ID) Locale("id", "ID") else Locale.US
  val fmt = SimpleDateFormat("MMM", locale)
  for (month in 0..11) {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, currentYear)
      set(Calendar.MONTH, month)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    labels.add(fmt.format(cal.time))
    incomeTotals.add(income.sumOf { entry ->
      val date = parseDate(entry.date) ?: return@sumOf 0
      val dCal = Calendar.getInstance().apply { timeInMillis = date }
      if (dCal.get(Calendar.YEAR) == currentYear && dCal.get(Calendar.MONTH) == month) entry.amount else 0
    })
    expenseTotals.add(expense.sumOf { entry ->
      val date = parseDate(entry.date) ?: return@sumOf 0
      val dCal = Calendar.getInstance().apply { timeInMillis = date }
      if (dCal.get(Calendar.YEAR) == currentYear && dCal.get(Calendar.MONTH) == month) entry.amount else 0
    })
  }
  return ChartSeries(labels, incomeTotals, expenseTotals)
}

private fun buildYearlySeries(income: List<MoneyEntry>, expense: List<MoneyEntry>, startYear: Int): ChartSeries {
  val now = Calendar.getInstance()
  val currentYear = now.get(Calendar.YEAR)
  val baseEnd = startYear + 4
  val shift = (currentYear - baseEnd).coerceAtLeast(0)
  val windowStart = startYear + shift
  val windowEnd = windowStart + 4

  val labels = mutableListOf<String>()
  val incomeTotals = mutableListOf<Int>()
  val expenseTotals = mutableListOf<Int>()
  for (year in windowStart..windowEnd) {
    labels.add(year.toString())
    incomeTotals.add(income.sumOf { entry ->
      val date = parseDate(entry.date) ?: return@sumOf 0
      val dCal = Calendar.getInstance().apply { timeInMillis = date }
      if (dCal.get(Calendar.YEAR) == year) entry.amount else 0
    })
    expenseTotals.add(expense.sumOf { entry ->
      val date = parseDate(entry.date) ?: return@sumOf 0
      val dCal = Calendar.getInstance().apply { timeInMillis = date }
      if (dCal.get(Calendar.YEAR) == year) entry.amount else 0
    })
  }
  return ChartSeries(labels, incomeTotals, expenseTotals)
}

private fun totalsFromSeries(series: ChartSeries): Pair<Int, Int> {
  return Pair(series.income.sum(), series.expense.sum())
}

@Composable
private fun LineChart(series: ChartSeries) {
  val colors = LocalAppColors.current
  val incomeValues = series.income.map { it.toFloat() }
  val expenseValues = series.expense.map { it.toFloat() }
  val allValues = incomeValues + expenseValues
  val maxVal = (allValues.maxOrNull() ?: 1f).coerceAtLeast(1f)
  val minVal = (allValues.minOrNull() ?: 0f).coerceAtMost(0f)
  val range = (maxVal - minVal).coerceAtLeast(1f)

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White.copy(alpha = 0.05f))
        .padding(10.dp),
    ) {
      val width = size.width
      val height = size.height
      if (incomeValues.isEmpty()) return@Canvas
      val stepX = if (incomeValues.size == 1) width else width / (incomeValues.size - 1)
      fun buildPoints(values: List<Float>): List<Offset> {
        return values.mapIndexed { index, value ->
          val x = index * stepX
          val y = height - ((value - minVal) / range) * height
          Offset(x, y)
        }
      }
      val incomePoints = buildPoints(incomeValues)
      val expensePoints = buildPoints(expenseValues)
      for (i in 0 until incomePoints.size - 1) {
        drawLine(
          color = colors.accent2,
          start = incomePoints[i],
          end = incomePoints[i + 1],
          strokeWidth = 6f,
          cap = StrokeCap.Round,
        )
      }
      for (i in 0 until expensePoints.size - 1) {
        drawLine(
          color = colors.danger,
          start = expensePoints[i],
          end = expensePoints[i + 1],
          strokeWidth = 6f,
          cap = StrokeCap.Round,
        )
      }
      drawPoints(
        points = incomePoints,
        pointMode = androidx.compose.ui.graphics.PointMode.Points,
        color = colors.accent,
        strokeWidth = 12f,
      )
      drawPoints(
        points = expensePoints,
        pointMode = androidx.compose.ui.graphics.PointMode.Points,
        color = colors.danger,
        strokeWidth = 12f,
      )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      series.labels.forEach { label ->
        Text(text = label, color = colors.muted, fontSize = 11.sp)
      }
    }
  }
}

@Composable
private fun SavingList(
  entries: List<SavingEntry>,
  onEdit: (SavingEntry) -> Unit,
  onDelete: (SavingEntry) -> Unit,
) {
  val strings = LocalStrings.current
  if (entries.isEmpty()) return
  Spacer(modifier = Modifier.height(12.dp))
  Text(text = strings["list_saving"], fontWeight = FontWeight.Bold, fontSize = 14.sp)
  Spacer(modifier = Modifier.height(8.dp))
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    entries.forEach { entry ->
      AppCard(shape = RoundedCornerShape(AppDimens.radiusMd)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text(text = entry.goal, fontWeight = FontWeight.Bold)
          Text(text = formatRupiah(entry.amount), fontWeight = FontWeight.Bold)
        }
        Text(text = entry.date, color = LocalAppColors.current.muted, fontSize = 12.sp)
        if (entry.note.isNotBlank()) {
          Text(text = entry.note, color = LocalAppColors.current.muted, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          GhostButton(text = strings["edit"], fillMaxWidth = false, modifier = Modifier.weight(1f), onClick = { onEdit(entry) })
          GhostButton(text = strings["delete"], fillMaxWidth = false, modifier = Modifier.weight(1f), onClick = { onDelete(entry) })
        }
      }
    }
  }
}

@Composable
private fun DreamsList(
  entries: List<DreamEntry>,
  onEdit: (DreamEntry) -> Unit,
  onDelete: (DreamEntry) -> Unit,
) {
  val strings = LocalStrings.current
  if (entries.isEmpty()) return
  Spacer(modifier = Modifier.height(12.dp))
  Text(text = strings["list_dreams"], fontWeight = FontWeight.Bold, fontSize = 14.sp)
  Spacer(modifier = Modifier.height(8.dp))
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    entries.forEach { entry ->
      AppCard(shape = RoundedCornerShape(AppDimens.radiusMd)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text(text = entry.title, fontWeight = FontWeight.Bold)
          Text(text = formatRupiah(entry.target), fontWeight = FontWeight.Bold)
        }
        Text(text = "${strings["label_target_current"]}: ${formatRupiah(entry.current)}", color = LocalAppColors.current.muted, fontSize = 12.sp)
        Text(text = "${strings["label_deadline"]}: ${entry.deadline}", color = LocalAppColors.current.muted, fontSize = 12.sp)
        if (entry.note.isNotBlank()) {
          Text(text = entry.note, color = LocalAppColors.current.muted, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          GhostButton(text = strings["edit"], fillMaxWidth = false, modifier = Modifier.weight(1f), onClick = { onEdit(entry) })
          GhostButton(text = strings["delete"], fillMaxWidth = false, modifier = Modifier.weight(1f), onClick = { onDelete(entry) })
        }
      }
    }
  }
}

private fun UUIDString(): String = java.util.UUID.randomUUID().toString()

private fun calculate(current: String, key: String): String {
  if (key == "C") return "0"
  if (key == "⌫") {
    val trimmed = current.dropLast(1)
    return if (trimmed.isBlank()) "0" else trimmed
  }
  if (key == "=") return current
  if (current == "0" && key.all { it.isDigit() }) return key
  return current + key
}
