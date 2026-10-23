package com.example.pos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Surface
import com.example.R
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PosButtonGradients {
    val Emerald = Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))
    val Primary = Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6)))
    val Amber = Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFF59E0B)))
    val Rose = Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFEF4444)))
    val Dark = Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF334155)))
}

@Composable
fun PosGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = PosButtonGradients.Emerald,
    icon: ImageVector? = null,
    iconBadge: Boolean = true,
    subtitle: String? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
    fontSize: TextUnit = 14.sp,
    testTag: String? = null
) {
    val isEffectivelyActive = enabled || isLoading
    val disabledBrush = Brush.horizontalGradient(
        listOf(
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0)
        )
    )

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .shadow(if (enabled && !isLoading) 2.dp else 0.dp, shape = shape, clip = false)
            .defaultMinSize(minHeight = height)
            .clip(shape)
            .background(if (isEffectivelyActive) gradient else disabledBrush)
            .then(
                if (!isEffectivelyActive) Modifier.border(1.dp, Color(0xFFCBD5E1), shape) else Modifier
            )
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.5.sp
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    if (iconBadge) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (enabled) 0.22f else 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (enabled) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (enabled) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column(horizontalAlignment = if (subtitle != null) Alignment.Start else Alignment.CenterHorizontally) {
                    Text(
                        text = text,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color.White else Color(0xFF64748B),
                        fontSize = fontSize,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontWeight = FontWeight.Medium,
                            color = if (enabled) Color.White.copy(alpha = 0.85f) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PosGoogleButton(
    text: String = "Continue with Google",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 50.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    testTag: String? = null
) {
    Surface(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = shape,
        color = Color.White,
        shadowElevation = if (enabled && !isLoading) 1.5.dp else 0.dp,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .height(height)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1D4ED8)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Connecting to Google...",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        fontSize = 14.sp
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = text,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) Color(0xFF1E293B) else Color(0xFF94A3B8),
                        fontSize = 14.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PosOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 46.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    testTag: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        ),
        contentPadding = contentPadding,
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .height(height)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun PosDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 46.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    gradient: Brush = PosButtonGradients.Rose,
    testTag: String? = null
) {
    PosGradientButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        gradient = gradient,
        icon = icon,
        iconBadge = true,
        enabled = enabled,
        height = height,
        shape = shape,
        testTag = testTag
    )
}

@Composable
fun PosCompactPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    gradient: Brush = PosButtonGradients.Emerald,
    height: Dp = 34.dp,
    fontSize: TextUnit = 12.sp,
    testTag: String? = null
) {
    PosGradientButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        gradient = gradient,
        icon = icon,
        iconBadge = false,
        height = height,
        fontSize = fontSize,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        testTag = testTag
    )
}
