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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.warungsenja.kasirterangbulan.domain.model.CartItem
import com.warungsenja.kasirterangbulan.domain.model.ProductItem
import com.warungsenja.kasirterangbulan.domain.model.ProductVariant
import com.warungsenja.kasirterangbulan.domain.model.StoreSettings
import com.warungsenja.kasirterangbulan.domain.model.TransactionRecord
import com.warungsenja.kasirterangbulan.presentation.theme.Red400
import com.warungsenja.kasirterangbulan.presentation.theme.Yellow400
import com.warungsenja.kasirterangbulan.presentation.theme.YellowAmberGradient
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc100
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc200
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc300
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc400
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc800
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc900
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc950
import com.warungsenja.kasirterangbulan.presentation.util.Formatter

@Composable
fun KasirScreen(
    products: List<ProductItem>,
    settings: StoreSettings,
    onCheckout: (List<CartItem>, String, String, (TransactionRecord, Boolean, String) -> Unit) -> Unit,
    onPrintReceipt: (TransactionRecord) -> Pair<Boolean, String>
) {
    var cart by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var customerName by remember { mutableStateOf("Pelanggan Umum") }
    var note by remember { mutableStateOf("") }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    // Variant Picker Dialog state
    var pickerProduct by remember { mutableStateOf<ProductItem?>(null) }
    var completedTrx by remember { mutableStateOf<TransactionRecord?>(null) }

    val totalQty = cart.sumOf { it.qty }
    val totalAmount = cart.sumOf { it.subtotal }

    fun addOrMergeCartItem(
        product: ProductItem,
        variantName: String,
        unitPrice: Long,
        unitCost: Long,
        qtyToAdd: Int
    ) {
        val cleanVariant = variantName.trim().ifBlank { product.category.ifBlank { "Original" } }
        val cleanPrice = unitPrice.coerceAtLeast(0L)
        val cleanCost = unitCost.coerceAtLeast(0L)
        val cleanQty = qtyToAdd.coerceAtLeast(1)
        val itemKey = "${product.id}::${cleanVariant.lowercase()}::$cleanPrice"

        val existing = cart.firstOrNull { it.cartItemKey == itemKey }
        cart = if (existing != null) {
            cart.map {
                if (it.cartItemKey == itemKey) {
                    val nextQty = it.qty + cleanQty
                    it.copy(qty = nextQty, subtotal = nextQty * it.unitPrice)
                } else {
                    it
                }
            }
        } else {
            cart + CartItem(
                cartItemKey = itemKey,
                productId = product.id,
                barcode = product.barcode,
                name = product.name,
                category = cleanVariant,
                qty = cleanQty,
                unit = product.unit,
                unitCost = cleanCost,
                unitPrice = cleanPrice,
                subtotal = cleanQty * cleanPrice,
                availableStock = product.stock
            )
        }
        feedbackBanner = "+$cleanQty ${product.name} ($cleanVariant) ditambahkan."
    }

    fun updateQty(itemKey: String, nextQty: Int) {
        cart = if (nextQty <= 0) {
            cart.filterNot { it.cartItemKey == itemKey }
        } else {
            cart.map {
                if (it.cartItemKey == itemKey) {
                    it.copy(qty = nextQty, subtotal = nextQty * it.unitPrice)
                } else {
                    it
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Feedback Banner
        if (feedbackBanner != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Yellow400.copy(alpha = 0.15f))
                    .border(1.dp, Yellow400.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feedbackBanner!!,
                    color = Yellow400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { feedbackBanner = null },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Yellow400)
                }
            }
        }

        // Menu Selection Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Zinc900)
                .border(1.dp, Zinc800, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YellowAmberGradient)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Pilih Menu Terang Bulan",
                    color = Zinc950,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Klik menu untuk memilih varian rasa dan harga.",
                    color = Zinc950.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (products.isEmpty()) {
                    Text(
                        text = "Belum ada daftar menu. Tambahkan menu di tab Menu (Owner).",
                        color = Zinc400,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    products.forEach { product ->
                        val variantsList = product.effectiveVariants()
                        val minPrice = variantsList.minOf { it.sellingPrice }
                        val maxPrice = variantsList.maxOf { it.sellingPrice }
                        val selectedCount = cart.filter { it.productId == product.id }.sumOf { it.qty }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selectedCount > 0) Yellow400.copy(alpha = 0.1f) else Zinc950
                                )
                                .border(
                                    1.dp,
                                    if (selectedCount > 0) Yellow400 else Zinc800,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { pickerProduct = product }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "No. ${product.barcode}",
                                    color = Yellow400,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${variantsList.size} Varian",
                                    color = Zinc400,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = product.name,
                                color = Zinc100,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = variantsList.joinToString(", ") { it.name },
                                color = Zinc400,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (minPrice != maxPrice) {
                                        "${Formatter.formatRupiah(minPrice)} - ${Formatter.formatRupiah(maxPrice)}"
                                    } else {
                                        Formatter.formatRupiah(minPrice)
                                    },
                                    color = Yellow400,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedCount > 0) Yellow400 else Zinc800)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (selectedCount > 0) "$selectedCount Dipilih" else "+ Pilih",
                                        color = if (selectedCount > 0) Zinc950 else Zinc300,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cart & Checkout Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Zinc900)
                .border(1.dp, Zinc800, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Yellow400,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Keranjang Pesanan (${cart.size})",
                        color = Zinc100,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (cart.isNotEmpty()) {
                    Text(
                        text = "Kosongkan",
                        color = Red400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { cart = emptyList() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Zinc950)
                        .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada menu dipilih.",
                        color = Zinc400,
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cart.forEach { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Zinc950)
                                .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        color = Zinc100,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Varian: ${item.category} · ${Formatter.formatRupiah(item.unitPrice)}",
                                        color = Yellow400,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                IconButton(
                                    onClick = { updateQty(item.cartItemKey, 0) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = Red400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Zinc900)
                                            .border(1.dp, Zinc800, RoundedCornerShape(8.dp))
                                            .clickable { updateQty(item.cartItemKey, item.qty - 1) }
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = null,
                                            tint = Zinc200,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "${item.qty}",
                                        color = Zinc100,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Zinc900)
                                            .border(1.dp, Zinc800, RoundedCornerShape(8.dp))
                                            .clickable { updateQty(item.cartItemKey, item.qty + 1) }
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Zinc200,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = Formatter.formatRupiah(item.subtotal),
                                    color = Yellow400,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Nama Pelanggan", fontSize = 11.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Yellow400,
                    unfocusedBorderColor = Zinc800,
                    focusedContainerColor = Zinc950,
                    unfocusedContainerColor = Zinc950,
                    focusedTextColor = Zinc100,
                    unfocusedTextColor = Zinc100
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan Pesanan (Contoh: Manis sedang)", fontSize = 11.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Yellow400,
                    unfocusedBorderColor = Zinc800,
                    focusedContainerColor = Zinc950,
                    unfocusedContainerColor = Zinc950,
                    focusedTextColor = Zinc100,
                    unfocusedTextColor = Zinc100
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Zinc950)
                    .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Porsi", color = Zinc400, fontSize = 12.sp)
                    Text(
                        "$totalQty pcs",
                        color = Zinc100,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total Bayar",
                        color = Zinc200,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        Formatter.formatRupiah(totalAmount),
                        color = Yellow400,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (cart.isNotEmpty()) {
                        onCheckout(cart, customerName, note) { trx, _, msg ->
                            completedTrx = trx
                            cart = emptyList()
                            note = ""
                            feedbackBanner = msg
                        }
                    }
                },
                enabled = cart.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Yellow400,
                    contentColor = Zinc950,
                    disabledContainerColor = Zinc800,
                    disabledContentColor = Zinc400
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Simpan & Cetak Struk",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Variant & Price Picker Dialog
    if (pickerProduct != null) {
        VariantPickerDialog(
            product = pickerProduct!!,
            onDismiss = { pickerProduct = null },
            onConfirm = { variantName, price, cost, qty ->
                addOrMergeCartItem(pickerProduct!!, variantName, price, cost, qty)
                pickerProduct = null
            }
        )
    }

    // Completed Transaction Receipt Dialog
    if (completedTrx != null) {
        CompletedReceiptDialog(
            trx = completedTrx!!,
            settings = settings,
            onDismiss = { completedTrx = null },
            onPrint = {
                val (_, msg) = onPrintReceipt(completedTrx!!)
                feedbackBanner = msg
            }
        )
    }
}

@Composable
private fun VariantPickerDialog(
    product: ProductItem,
    onDismiss: () -> Unit,
    onConfirm: (variantName: String, sellingPrice: Long, purchasePrice: Long, qty: Int) -> Unit
) {
    val variants = product.effectiveVariants()
    var selectedVariant by remember { mutableStateOf<ProductVariant>(variants.first()) }
    var customVariantName by remember { mutableStateOf(variants.first().name) }
    var customSellingPrice by remember { mutableStateOf(variants.first().sellingPrice.toString()) }
    var qty by remember { mutableIntStateOf(1) }

    val parsedPrice = customSellingPrice.filter { it.isDigit() }.toLongOrNull() ?: 0L

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Zinc900)
                .border(1.dp, Yellow400.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YellowAmberGradient)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No. ${product.barcode}",
                        color = Zinc950.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.name,
                        color = Zinc950,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Zinc950)
                }
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pilih Varian Menu",
                    color = Zinc300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                variants.forEach { v ->
                    val isSelected = selectedVariant.id == v.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Yellow400 else Zinc950)
                            .border(
                                1.dp,
                                if (isSelected) Yellow400 else Zinc800,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedVariant = v
                                customVariantName = v.name
                                customSellingPrice = v.sellingPrice.toString()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = v.name,
                            color = if (isSelected) Zinc950 else Zinc100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Formatter.formatRupiah(v.sellingPrice),
                            color = if (isSelected) Zinc950 else Yellow400,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = customVariantName,
                    onValueChange = { customVariantName = it },
                    label = { Text("Nama Varian (Bisa Disesuaikan)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Yellow400,
                        unfocusedBorderColor = Zinc800,
                        focusedContainerColor = Zinc950,
                        unfocusedContainerColor = Zinc950,
                        focusedTextColor = Zinc100,
                        unfocusedTextColor = Zinc100
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customSellingPrice,
                        onValueChange = { customSellingPrice = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Jual (Rp)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Yellow400,
                            unfocusedBorderColor = Zinc800,
                            focusedContainerColor = Zinc950,
                            unfocusedContainerColor = Zinc950,
                            focusedTextColor = Yellow400,
                            unfocusedTextColor = Yellow400
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Zinc950)
                                .border(1.dp, Zinc800, RoundedCornerShape(10.dp))
                                .clickable { qty = (qty - 1).coerceAtLeast(1) }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Zinc200)
                        }
                        Text(
                            text = "$qty",
                            color = Zinc100,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Zinc950)
                                .border(1.dp, Zinc800, RoundedCornerShape(10.dp))
                                .clickable { qty += 1 }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Zinc200)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Zinc950)
                        .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subtotal Item", color = Zinc400, fontSize = 12.sp)
                    Text(
                        text = Formatter.formatRupiah(parsedPrice * qty),
                        color = Yellow400,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Zinc950,
                            contentColor = Zinc300
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            onConfirm(
                                customVariantName,
                                parsedPrice,
                                selectedVariant.purchasePrice,
                                qty
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Yellow400,
                            contentColor = Zinc950
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Masukkan Keranjang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedReceiptDialog(
    trx: TransactionRecord,
    settings: StoreSettings,
    onDismiss: () -> Unit,
    onPrint: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Zinc900)
                .border(1.dp, Yellow400.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YellowAmberGradient)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Zinc950)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transaksi Selesai",
                        color = Zinc950,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Zinc950)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Zinc950)
                        .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = settings.storeName,
                        color = Yellow400,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = settings.storeAddress,
                        color = Zinc400,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("No. Struk", color = Zinc400, fontSize = 11.sp)
                        Text(
                            trx.code,
                            color = Yellow400,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Waktu", color = Zinc400, fontSize = 11.sp)
                        Text(Formatter.formatDateTime(trx.timestamp), color = Zinc200, fontSize = 11.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pelanggan", color = Zinc400, fontSize = 11.sp)
                        Text(trx.recipientOrSupplier, color = Zinc200, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    trx.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.name} (${item.category}) x${item.qty}",
                                color = Zinc200,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Formatter.formatRupiah(item.subtotal),
                                color = Zinc100,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL", color = Yellow400, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            Formatter.formatRupiah(trx.totalAmount),
                            color = Yellow400,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPrint,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Yellow400,
                            contentColor = Zinc950
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Struk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Zinc950,
                            contentColor = Zinc200
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
