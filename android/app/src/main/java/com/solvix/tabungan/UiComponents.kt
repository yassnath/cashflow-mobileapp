package com.solvix.tabungan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.PopupProperties

object AppDimens {
  val radiusLg = 24.dp
  val radiusMd = 16.dp
  val radiusSm = 12.dp
  val pagePadding = 18.dp
  val cardPadding = 16.dp
  val shadow = 16.dp
}

@Composable
fun AppCard(
  modifier: Modifier = Modifier.fillMaxWidth(),
  shape: RoundedCornerShape = RoundedCornerShape(AppDimens.radiusLg),
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = LocalAppColors.current
  CompositionLocalProvider(LocalContentColor provides colors.text) {
    Column(
      modifier = modifier
        .shadow(AppDimens.shadow, shape)
        .border(1.dp, colors.cardBorder, shape)
        .background(colors.card, shape)
        .padding(AppDimens.cardPadding),
      content = content,
    )
  }
}

@Composable
fun MiniCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = LocalAppColors.current
  CompositionLocalProvider(LocalContentColor provides colors.text) {
    Column(
      modifier = modifier
        .shadow(10.dp, RoundedCornerShape(AppDimens.radiusMd))
        .border(1.dp, colors.cardBorder, RoundedCornerShape(AppDimens.radiusMd))
        .background(colors.card, RoundedCornerShape(AppDimens.radiusMd))
        .padding(12.dp),
      content = content,
    )
  }
}

@Composable
fun GradientButton(
  text: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val colors = LocalAppColors.current
  Box(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 44.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(Brush.linearGradient(listOf(colors.accent, colors.accent2)))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun GhostButton(
  text: String,
  modifier: Modifier = Modifier,
  fillMaxWidth: Boolean = true,
  onClick: () -> Unit,
) {
  val colors = LocalAppColors.current
  val baseModifier = if (fillMaxWidth) modifier.fillMaxWidth() else modifier
  Box(
    modifier = baseModifier
      .heightIn(min = 44.dp)
      .drawBehind {
        drawRoundRect(
          color = colors.cardBorder.copy(alpha = 0.7f),
          style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
          ),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
        )
      }
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(text = text, color = colors.text, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
fun ChipButton(
  text: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val colors = LocalAppColors.current
  val borderColor = colors.accent
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(999.dp))
      .background(colors.card)
      .border(1.dp, borderColor, RoundedCornerShape(999.dp))
      .shadow(10.dp, RoundedCornerShape(999.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.text)
  }
}

@Composable
fun SectionTitle(icon: String, title: String, subtitle: String? = null) {
  val colors = LocalAppColors.current
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = icon, fontSize = 20.sp)
      Spacer(modifier = Modifier.width(6.dp))
      Text(text = title, style = MaterialTheme.typography.titleLarge, color = colors.text)
    }
    if (subtitle != null) {
      Text(
        text = subtitle,
        color = colors.muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
      )
    } else {
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
fun AppTextField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String = "",
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  minLines: Int = 1,
  trailing: (@Composable () -> Unit)? = null,
  isPassword: Boolean = false,
  textFontSize: TextUnit = TextUnit.Unspecified,
  placeholderFontSize: TextUnit = TextUnit.Unspecified,
) {
  val colors = LocalAppColors.current
  var showPassword by remember { mutableStateOf(false) }
  val trailingIcon = trailing ?: if (isPassword) {
    {
      Icon(
        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
        contentDescription = if (showPassword) "Hide password" else "Show password",
        tint = colors.muted,
        modifier = Modifier.clickable { showPassword = !showPassword },
      )
    }
  } else {
    null
  }
  Column(modifier = Modifier.fillMaxWidth()) {
    if (label.isNotBlank()) {
      Text(text = label, fontSize = 13.sp, color = colors.text)
      Spacer(modifier = Modifier.height(6.dp))
    }
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = {
        if (placeholder.isNotBlank()) {
          Text(
            text = placeholder,
            color = colors.placeholder,
            fontSize = if (placeholderFontSize == TextUnit.Unspecified) 14.sp else placeholderFontSize,
          )
        }
      },
      keyboardOptions = keyboardOptions,
      singleLine = minLines == 1,
      minLines = minLines,
      trailingIcon = trailingIcon,
      visualTransformation = if (isPassword && !showPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else VisualTransformation.None,
      textStyle = if (textFontSize == TextUnit.Unspecified) {
        MaterialTheme.typography.bodyMedium
      } else {
        TextStyle(
          fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
          fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
          fontSize = textFontSize,
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.fieldBorder,
        focusedTextColor = colors.text,
        unfocusedTextColor = colors.text,
        focusedContainerColor = colors.card,
        unfocusedContainerColor = colors.card,
      ),
      shape = RoundedCornerShape(AppDimens.radiusSm),
    )
  }
}

@Composable
fun AppDropdown(
  label: String,
  placeholder: String,
  options: List<String>,
  selected: String,
  onSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalAppColors.current
  val theme = LocalThemeName.current
  val menuBorderColor = if (isDarkTheme(theme)) {
    Color.White.copy(alpha = 0.7f)
  } else {
    colors.text.copy(alpha = 0.35f)
  }
  var expanded by remember { mutableStateOf(false) }
  var fieldWidth by remember { mutableStateOf(0.dp) }
  val density = LocalDensity.current

  Column(modifier = modifier.fillMaxWidth()) {
    if (label.isNotBlank()) {
      Text(text = label, fontSize = 13.sp, color = colors.text)
      Spacer(modifier = Modifier.height(6.dp))
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(AppDimens.radiusSm))
        .border(1.dp, colors.fieldBorder, RoundedCornerShape(AppDimens.radiusSm))
        .background(colors.card)
        .clickable { expanded = true }
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .onGloballyPositioned { coordinates ->
          fieldWidth = with(density) { coordinates.size.width.toDp() }
        },
      contentAlignment = Alignment.CenterStart,
    ) {
      Text(
        text = if (selected.isBlank()) placeholder else selected,
        color = if (selected.isBlank()) colors.placeholder else colors.text,
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(text = "▾", color = colors.muted, modifier = Modifier.align(Alignment.CenterEnd))
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier
        .width(fieldWidth)
        .heightIn(max = 520.dp)
        .background(Color.Transparent),
      properties = PopupProperties(focusable = true),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(colors.card)
          .border(1.dp, menuBorderColor, RoundedCornerShape(18.dp))
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        options.forEach { option ->
          val isSelected = option == selected
          val itemShape = RoundedCornerShape(10.dp)
          val itemBackground = if (isSelected) {
            Brush.linearGradient(listOf(colors.accent, colors.accent2))
          } else {
            Brush.linearGradient(listOf(colors.bg2, colors.bg2))
          }
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(32.dp)
              .clip(itemShape)
              .background(itemBackground)
              .clickable {
                onSelected(option)
                expanded = false
              }
              .padding(10.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            Text(
              text = option,
              color = if (isSelected) Color.White else colors.text,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
              fontSize = 12.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
fun StatRow(
  icon: String,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  accentColor: Color? = null,
) {
  val colors = LocalAppColors.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(AppDimens.radiusMd))
      .background(colors.bg2)
      .border(1.dp, colors.cardBorder, RoundedCornerShape(AppDimens.radiusMd))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = icon, fontSize = 22.sp)
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(text = label, color = colors.muted, fontSize = 12.sp)
      Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor ?: colors.text)
    }
  }
}

@Composable
fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
  val colors = LocalAppColors.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(AppDimens.radiusSm))
      .background(colors.bg2)
      .border(1.dp, colors.cardBorder, RoundedCornerShape(AppDimens.radiusSm))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, color = colors.muted, fontSize = 12.sp)
    Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
  }
}

@Composable
fun ThemeCard(
  emoji: String,
  label: String,
  active: Boolean,
  background: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val colors = LocalAppColors.current
  val borderColor = if (active) colors.accent else colors.cardBorder
  val elevation by animateFloatAsState(if (active) 12f else 6f, label = "theme-elevation")
  Row(
    modifier = modifier
      .fillMaxWidth()
      .shadow(elevation.dp, RoundedCornerShape(14.dp))
      .border(1.dp, borderColor, RoundedCornerShape(14.dp))
      .background(background, RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = emoji, fontSize = 16.sp)
    Spacer(modifier = Modifier.width(12.dp))
    Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
  }
}

@Composable
fun ChipToggle(
  options: List<String>,
  selectedIndex: Int,
  modifier: Modifier = Modifier,
  onSelect: (Int) -> Unit,
) {
  val colors = LocalAppColors.current
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(999.dp))
      .background(colors.bg2)
      .padding(4.dp),
  ) {
    options.forEachIndexed { index, option ->
      val active = index == selectedIndex
      val chipModifier = if (active) {
        Modifier.background(Brush.linearGradient(listOf(colors.accent, colors.accent2)))
      } else {
        Modifier.background(Color.Transparent)
      }
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .then(chipModifier)
          .clickable { onSelect(index) }
          .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = option,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = if (active) Color.White else colors.text,
        )
      }
      if (index != options.lastIndex) {
        Spacer(modifier = Modifier.width(4.dp))
      }
    }
  }
}

@Composable
fun ToastMessage(text: String, visible: Boolean, modifier: Modifier = Modifier) {
  val colors = LocalAppColors.current
  AnimatedVisibility(visible = visible) {
    Box(
      modifier = modifier
        .clip(RoundedCornerShape(14.dp))
        .background(colors.card)
        .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
        .padding(horizontal = 18.dp, vertical = 12.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(text = text, fontWeight = FontWeight.Bold, color = colors.text)
    }
  }
}

@Composable
fun FadeInPage(key: Any?, content: @Composable () -> Unit) {
  var visible by remember(key) { mutableStateOf(false) }
  LaunchedEffect(key) { visible = true }
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = tween(durationMillis = 220)),
  ) {
    content()
  }
}

@Composable
fun IconCircle(emoji: String, size: Dp = 28.dp) {
  val colors = LocalAppColors.current
  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(colors.bg2),
    contentAlignment = Alignment.Center,
  ) {
    Text(text = emoji)
  }
}

@Composable
fun DropDownMenuCard(
  expanded: Boolean,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = LocalAppColors.current
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismiss,
    modifier = modifier
      .background(colors.card)
      .border(2.dp, colors.accent, RoundedCornerShape(18.dp))
      .padding(12.dp),
    properties = PopupProperties(focusable = true),
  ) {
    Column(content = content)
  }
}

@Composable
fun MenuItem(text: String, emoji: String, color: Color? = null, onClick: () -> Unit) {
  val colors = LocalAppColors.current
  val itemColor = color ?: colors.text
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(colors.bg2)
      .clickable(onClick = onClick)
      .padding(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = emoji, fontSize = 18.sp)
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = text, color = itemColor, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
fun ModalCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = LocalAppColors.current
  Column(
    modifier = modifier
      .shadow(20.dp, RoundedCornerShape(24.dp))
      .background(colors.card, RoundedCornerShape(24.dp))
      .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    content = content,
  )
}
