package com.solvix.tabungan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

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
  modifier: Modifier = Modifier,
  shape: RoundedCornerShape = RoundedCornerShape(AppDimens.radiusLg),
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = LocalAppColors.current
  val shadowColor = Color.Black.copy(alpha = 0.18f)
  val cardModifier = if (modifier == Modifier) Modifier.fillMaxWidth() else modifier
  CompositionLocalProvider(LocalContentColor provides colors.text) {
    Column(
      modifier = cardModifier
        .shadow(AppDimens.shadow, shape, ambientColor = shadowColor, spotColor = shadowColor)
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
  val shadowColor = Color.Black.copy(alpha = 0.18f)
  CompositionLocalProvider(LocalContentColor provides colors.text) {
    Column(
      modifier = modifier
        .shadow(10.dp, RoundedCornerShape(AppDimens.radiusMd), ambientColor = shadowColor, spotColor = shadowColor)
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
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  val colors = LocalAppColors.current
  val background = if (enabled) {
    Brush.linearGradient(listOf(colors.accent, colors.accent2))
  } else {
    Brush.linearGradient(listOf(colors.muted, colors.muted))
  }
  Box(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 44.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(background)
      .clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    val textColor = if (enabled) Color.White else colors.text.copy(alpha = 0.6f)
    Text(text = text, color = textColor, fontWeight = FontWeight.Bold)
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
  val shadowColor = Color.Black.copy(alpha = 0.18f)
  val borderColor = colors.accent
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(999.dp))
      .background(colors.card)
      .border(1.dp, borderColor, RoundedCornerShape(999.dp))
      .shadow(10.dp, RoundedCornerShape(999.dp), ambientColor = shadowColor, spotColor = shadowColor)
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
  var expanded by remember { mutableStateOf(false) }
  var fieldWidthPx by remember { mutableIntStateOf(0) }
  var anchorBounds by remember { mutableStateOf(IntRect.Zero) }
  val density = LocalDensity.current
  val extraYOffsetPx = with(density) { 10.dp.roundToPx() }
  val dropdownModifier = if (modifier == Modifier) Modifier.fillMaxWidth() else modifier

  Column(modifier = dropdownModifier) {
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
          fieldWidthPx = coordinates.size.width
          val rect = coordinates.boundsInWindow()
          anchorBounds = IntRect(
            rect.left.roundToInt(),
            rect.top.roundToInt(),
            rect.right.roundToInt(),
            rect.bottom.roundToInt(),
          )
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
    val menuScroll = rememberScrollState()
    if (expanded) {
      val positionProvider = remember(anchorBounds, extraYOffsetPx) {
        object : PopupPositionProvider {
          override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
          ): IntOffset {
            val bounds = anchorBounds
            var x = if (layoutDirection == LayoutDirection.Rtl) {
              bounds.right - popupContentSize.width
            } else {
              bounds.left
            }
            val downY = bounds.bottom + extraYOffsetPx
            val upY = bounds.top - popupContentSize.height - extraYOffsetPx
            var y = if (downY + popupContentSize.height <= windowSize.height) downY else upY
            if (x + popupContentSize.width > windowSize.width) {
              x = windowSize.width - popupContentSize.width
            }
            if (x < 0) x = 0
            if (y < 0) y = 0
            return IntOffset(x, y)
          }
        }
      }
      val menuWidth = with(density) { fieldWidthPx.toDp() }
      Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = true),
        onDismissRequest = { expanded = false },
      ) {
        Column(
          modifier = Modifier
            .width(menuWidth)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .border(1.dp, colors.fieldBorder, RoundedCornerShape(18.dp))
            .padding(12.dp)
            .heightIn(max = 360.dp)
            .verticalScroll(menuScroll),
          verticalArrangement = Arrangement.spacedBy(10.dp),
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
                .height(34.dp)
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
  val shadowColor = Color.Black.copy(alpha = 0.18f)
  val borderColor = if (active) colors.accent else colors.cardBorder
  val elevation by animateFloatAsState(if (active) 12f else 6f, label = "theme-elevation")
  Row(
    modifier = modifier
      .fillMaxWidth()
      .shadow(elevation.dp, RoundedCornerShape(14.dp), ambientColor = shadowColor, spotColor = shadowColor)
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
  val alphaAnim = remember { Animatable(0f) }
  LaunchedEffect(key) {
    alphaAnim.snapTo(0f)
    alphaAnim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
    )
  }
  Box(modifier = Modifier.graphicsLayer { alpha = alphaAnim.value }) {
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
  anchorBounds: IntRect = IntRect.Zero,
  alignRight: Boolean = true,
  xOffsetDp: Dp = 0.dp,
  yOffsetDp: Dp = 10.dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  if (!expanded) return
  val colors = LocalAppColors.current
  val density = LocalDensity.current
  val extraYOffsetPx = with(density) { yOffsetDp.roundToPx() }
  val extraXOffsetPx = with(density) { xOffsetDp.roundToPx() }
  val positionProvider = remember(anchorBounds, alignRight, extraYOffsetPx, extraXOffsetPx) {
    object : PopupPositionProvider {
      override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
      ): IntOffset {
        val bounds = anchorBounds
        var x = if (alignRight) {
          bounds.right - popupContentSize.width
        } else if (layoutDirection == LayoutDirection.Rtl) {
          bounds.right - popupContentSize.width
        } else {
          bounds.left
        }
        x += extraXOffsetPx
        val downY = bounds.bottom + extraYOffsetPx
        val upY = bounds.top - popupContentSize.height - extraYOffsetPx
        var y = if (downY + popupContentSize.height <= windowSize.height) downY else upY
        if (x + popupContentSize.width > windowSize.width) {
          x = windowSize.width - popupContentSize.width
        }
        if (x < 0) x = 0
        if (y < 0) y = 0
        return IntOffset(x, y)
      }
    }
  }
  Popup(
    popupPositionProvider = positionProvider,
    properties = PopupProperties(focusable = true),
    onDismissRequest = onDismiss,
  ) {
    Column(
      modifier = modifier
        .clip(RoundedCornerShape(18.dp))
        .background(colors.card, RoundedCornerShape(18.dp))
        .border(2.dp, colors.accent, RoundedCornerShape(18.dp))
        .padding(12.dp),
      content = content,
    )
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
