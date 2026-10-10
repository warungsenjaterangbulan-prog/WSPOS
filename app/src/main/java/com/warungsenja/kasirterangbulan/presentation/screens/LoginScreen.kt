package com.warungsenja.kasirterangbulan.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warungsenja.kasirterangbulan.domain.model.StoreSettings
import com.warungsenja.kasirterangbulan.domain.model.UserRole
import com.warungsenja.kasirterangbulan.presentation.theme.Red400
import com.warungsenja.kasirterangbulan.presentation.theme.Yellow400
import com.warungsenja.kasirterangbulan.presentation.theme.YellowAmberGradient
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc100
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc300
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc400
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc800
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc900
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc950

@Composable
fun LoginScreen(
    settings: StoreSettings,
    onLogin: (UserRole, String) -> Boolean
) {
    var selectedRole by remember { mutableStateOf(UserRole.KASIR) }
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Zinc950)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Zinc900)
                .border(1.dp, Zinc800, RoundedCornerShape(24.dp))
        ) {
            // Top / Left Store Identity Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YellowAmberGradient)
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Zinc950),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Store",
                        tint = Yellow400,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = settings.storeName,
                    color = Zinc950,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aplikasi Kasir Penjualan & Manajemen Menu Terang Bulan.",
                    color = Zinc950.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Zinc950.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = settings.storeAddress,
                        color = Zinc950,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Telp/WA: ${settings.storePhone} · ${settings.operationalHours}",
                        color = Zinc950.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }

            // Bottom / Right Login Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Zinc950)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Masuk Operator",
                    color = Zinc100,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pilih akses peran dan masukkan PIN keamanan.",
                    color = Zinc400,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Role Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoleOptionButton(
                        modifier = Modifier.weight(1f),
                        title = "Kasir Penjualan",
                        isSelected = selectedRole == UserRole.KASIR,
                        icon = Icons.Default.Person,
                        onClick = {
                            selectedRole = UserRole.KASIR
                            pin = ""
                            errorMsg = null
                        }
                    )
                    RoleOptionButton(
                        modifier = Modifier.weight(1f),
                        title = "Owner / Admin",
                        isSelected = selectedRole == UserRole.OWNER,
                        icon = Icons.Default.Shield,
                        onClick = {
                            selectedRole = UserRole.OWNER
                            pin = ""
                            errorMsg = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "PIN Akses (${selectedRole.displayName})",
                    color = Zinc300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 8) {
                            pin = it
                            errorMsg = null
                        }
                    },
                    placeholder = { Text("Masukkan PIN", color = Zinc400, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Yellow400)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Yellow400,
                        unfocusedBorderColor = Zinc800,
                        focusedContainerColor = Zinc900,
                        unfocusedContainerColor = Zinc900,
                        focusedTextColor = Yellow400,
                        unfocusedTextColor = Yellow400
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 0-9 Numpad Grid
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { rowDigits ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowDigits.forEach { d ->
                            NumpadKey(
                                label = d,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (pin.length < 8) {
                                        pin += d
                                        errorMsg = null
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumpadKey(
                        label = "Reset",
                        modifier = Modifier.weight(1f),
                        isAction = true,
                        onClick = {
                            pin = ""
                            errorMsg = null
                        }
                    )
                    NumpadKey(
                        label = "0",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (pin.length < 8) {
                                pin += "0"
                                errorMsg = null
                            }
                        }
                    )
                    NumpadKey(
                        label = "Hapus",
                        modifier = Modifier.weight(1f),
                        isAction = true,
                        onClick = {
                            if (pin.isNotEmpty()) {
                                pin = pin.dropLast(1)
                                errorMsg = null
                            }
                        }
                    )
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Red400.copy(alpha = 0.15f))
                            .border(1.dp, Red400.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = errorMsg!!,
                            color = Red400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (!onLogin(selectedRole, pin)) {
                            errorMsg = "PIN tidak sesuai untuk peran yang dipilih."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Yellow400,
                        contentColor = Zinc950
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Buka Kasir Terang Bulan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleOptionButton(
    modifier: Modifier = Modifier,
    title: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Yellow400 else Zinc900)
            .border(
                1.dp,
                if (isSelected) Yellow400 else Zinc800,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) Zinc950 else Zinc300,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = if (isSelected) Zinc950 else Zinc300,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NumpadKey(
    label: String,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Zinc900)
            .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isAction) Zinc400 else Yellow400,
            fontSize = if (isAction) 12.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (isAction) FontFamily.SansSerif else FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}
