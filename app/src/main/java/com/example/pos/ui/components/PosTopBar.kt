package com.example.pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.auth.CurrentStaffSession
import com.example.pos.data.local.OrganizationEntity
import com.example.pos.data.local.ShiftEntity
import com.example.pos.data.model.StaffRole
import com.example.pos.data.repository.SyncConnectionStatus
import com.example.pos.data.repository.SyncUiState
import com.example.pos.ui.i18n.AppLanguage
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSky

@Composable
fun PosTopBar(
    org: OrganizationEntity?,
    shift: ShiftEntity?,
    staffSession: CurrentStaffSession,
    syncState: SyncUiState,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onLanguageToggle: () -> Unit = {},
    onShiftClick: () -> Unit,
    onSyncClick: () -> Unit,
    onLockClick: () -> Unit,
    onSwitchRoleClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val strings = LocalPosStrings.current
    val roleColor = when (staffSession.role) {
        StaffRole.OWNER -> PosEmerald
        StaffRole.ADMIN -> PosPrimary
        StaffRole.MANAGER -> PosSky
        StaffRole.STOREKEEPER -> PosAmber
        StaffRole.CASHIER -> Color(0xFF8B5CF6)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .testTag("pos_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App & Store Brand (Shortened to Business Name, Branch and Till)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = "POS",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = org?.name ?: "OmniPOS Store",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(roleColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = staffSession.role.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = roleColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text(
                        text = "${org?.name ?: "Store"} • ${strings.tillInfo("1", staffSession.staffName.ifBlank { "Staff" })}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Icons: Language Pill + Shift Indicator + Single Sync Icon + Lock Terminal + Switch Operator
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language Switch Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable(onClick = onLanguageToggle)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("language_switch_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.NEPALI) "🇳🇵 ने" else "🇬🇧 EN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Shift Status Pill
                val isShiftOpen = shift != null && shift.status == "OPEN"
                val isShiftDraft = shift != null && shift.status == "CLOSING"
                val badgeBg = when {
                    isShiftOpen -> Color(0xFFD1FAE5)
                    isShiftDraft -> PosAmber.copy(alpha = 0.2f)
                    else -> Color(0xFFFEE2E2)
                }
                val dotColor = when {
                    isShiftOpen -> PosEmerald
                    isShiftDraft -> PosAmber
                    else -> PosRose
                }
                val textColor = when {
                    isShiftOpen -> Color(0xFF065F46)
                    isShiftDraft -> Color(0xFF92400E)
                    else -> Color(0xFF991B1B)
                }
                val statusText = when {
                    isShiftOpen -> strings.shiftOpen
                    isShiftDraft -> strings.shiftClosingDraft
                    else -> strings.shiftClosed
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeBg)
                        .clickable(onClick = onShiftClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("shift_status_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Single Clean Sync Icon with Badged Count
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier.size(34.dp).testTag("sync_status_badge")
                ) {
                    if (syncState.pendingCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = PosAmber) {
                                    Text("${syncState.pendingCount}", fontSize = 9.sp)
                                }
                            }
                        ) {
                            SyncIcon(syncState.status)
                        }
                    } else {
                        SyncIcon(syncState.status)
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Cashier Lock Button
                IconButton(
                    onClick = onLockClick,
                    modifier = Modifier.size(34.dp).testTag("lock_register_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = strings.lockRegister,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Switch Staff / Operator Button
                IconButton(
                    onClick = onSwitchRoleClick,
                    modifier = Modifier.size(34.dp).testTag("switch_staff_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = strings.switchOperator,
                        tint = roleColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncIcon(status: SyncConnectionStatus) {
    when (status) {
        SyncConnectionStatus.SYNCING -> {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Syncing",
                tint = PosPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        SyncConnectionStatus.ONLINE -> {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = "Online",
                tint = PosEmerald,
                modifier = Modifier.size(18.dp)
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Offline",
                tint = PosAmber,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
