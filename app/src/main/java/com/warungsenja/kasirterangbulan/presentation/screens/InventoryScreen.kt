package com.warungsenja.kasirterangbulan.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.warungsenja.kasirterangbulan.domain.model.ProductItem
import com.warungsenja.kasirterangbulan.domain.model.ProductVariant
import com.warungsenja.kasirterangbulan.presentation.theme.Emerald300
import com.warungsenja.kasirterangbulan.presentation.theme.Emerald400
import com.warungsenja.kasirterangbulan.presentation.theme.Emerald500
import com.warungsenja.kasirterangbulan.presentation.theme.Red400
import com.warungsenja.kasirterangbulan.presentation.theme.Red500
import com.warungsenja.kasirterangbulan.presentation.theme.Yellow300
import com.warungsenja.kasirterangbulan.presentation.theme.Yellow400

import com.warungsenja.kasirterangbulan.presentation.theme.Zinc100
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc300
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc400
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc500
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc700
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc800
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc900
import com.warungsenja.kasirterangbulan.presentation.theme.Zinc950
import com.warungsenja.kasirterangbulan.presentation.util.Formatter

private data class VariantFormState(
    val id: String,
    val name: String,
    val costPrice: String,
    val unitPrice: String
)

@Composable
fun InventoryScreen(
    products: List<ProductItem>,
    onUpdateProducts: (List<ProductItem>) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("ALL") }
    var editingProduct by remember { mutableStateOf<ProductItem?>(null) }
    var showFormModal by remember { mutableStateOf(false) }
    var deleteConfirmProduct by remember { mutableStateOf<ProductItem?>(null) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    val categories = remember(products) {
        listOf("ALL") + products.map { it.category }.distinct()
    }

    val filtered = remember(products, search, categoryFilter) {
        val q = search.trim().lowercase()
        products
            .filter { item ->
                val matchQuery = q.isEmpty() ||
                    item.name.lowercase().contains(q) ||
                    item.barcode.lowercase().contains(q) ||
                    item.category.lowercase().contains(q)
                val matchCat = categoryFilter == "ALL" || item.category == categoryFilter
                matchQuery && matchCat
            }
            .sortedWith { a, b ->
                val numA = Regex("^(\\d+)").find(a.name)?.groupValues?.get(1)?.toIntOrNull()
                val numB = Regex("^(\\d+)").find(b.name)?.groupValues?.get(1)?.toIntOrNull()
                if (numA != null && numB != null) {
                    numA.compareTo(numB)
                } else {
                    a.name.compareTo(b.name)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daftar Menu & Varian Harga",
                    color = Zinc100,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kelola nomor urut menu, varian rasa, harga modal (HPP), dan harga jual.",
                    color = Zinc400,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    editingProduct = null
                    showFormModal = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Yellow400,
                    contentColor = Zinc950
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Menu Baru", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }

        if (bannerMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Emerald500.copy(alpha = 0.12f))
                    .border(1.dp, Emerald500.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(text = bannerMessage!!, color = Emerald300, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Search & Filter
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Cari nama menu, nomor, kode...", color = Zinc500, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Zinc500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Yellow400,
                unfocusedBorderColor = Zinc800,
                focusedContainerColor = Zinc900,
                unfocusedContainerColor = Zinc900,
                focusedTextColor = Zinc100,
                unfocusedTextColor = Zinc100
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val selected = categoryFilter == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Yellow400 else Zinc900)
                        .border(1.dp, if (selected) Yellow300 else Zinc800, RoundedCornerShape(10.dp))
                        .clickable { categoryFilter = cat }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (cat == "ALL") "Semua (${products.size})" else cat,
                        color = if (selected) Zinc950 else Zinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Product Cards List
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Zinc900)
                    .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada menu yang sesuai filter pencarian.", color = Zinc500, fontSize = 13.sp)
            }
        } else {
            filtered.forEach { item ->
                val variants = if (item.variants.isNotEmpty()) {
                    item.variants
                } else {
                    listOf(ProductVariant("default", "Original", item.purchasePrice, item.sellingPrice))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Zinc900.copy(alpha = 0.85f))
                        .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.barcode,
                                    color = Yellow400,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Yellow400.copy(alpha = 0.12f))
                                        .border(1.dp, Yellow400.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = item.category,
                                    color = Zinc400,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Zinc800)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = "${variants.size} Varian",
                                    color = Emerald400,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.name,
                                color = Zinc100,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    editingProduct = item
                                    showFormModal = true
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Zinc800)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Menu",
                                    tint = Yellow400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { deleteConfirmProduct = item },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Red500.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Menu",
                                    tint = Red400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Variants Breakdown Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Zinc950.copy(alpha = 0.7f))
                            .border(1.dp, Zinc800.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("VARIAN RASA", color = Zinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                            Text("MODAL", color = Zinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("HARGA JUAL", color = Zinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("LABA", color = Zinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                        }

                        variants.forEach { v ->
                            val profit = v.sellingPrice - v.purchasePrice
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = v.name,
                                    color = Zinc100,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = Formatter.formatRupiah(v.purchasePrice),
                                    color = Zinc400,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = Formatter.formatRupiah(v.sellingPrice),
                                    color = Yellow400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "+${Formatter.formatRupiah(profit)}",
                                    color = Emerald400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (deleteConfirmProduct != null) {
        val target = deleteConfirmProduct!!
        Dialog(onDismissRequest = { deleteConfirmProduct = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Zinc900)
                    .border(1.dp, Zinc800, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Hapus Menu?", color = Zinc100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Apakah Anda yakin ingin menghapus menu \"${target.name}\" (${target.barcode})?",
                    color = Zinc400,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { deleteConfirmProduct = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Zinc300),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onUpdateProducts(products.filterNot { it.id == target.id })
                            bannerMessage = "Menu \"${target.name}\" berhasil dihapus."
                            deleteConfirmProduct = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Red500, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ya, Hapus", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showFormModal) {
        ProductFormDialog(
            existing = editingProduct,
            allProducts = products,
            onDismiss = { showFormModal = false },
            onSave = { savedProduct ->
                val nextList = if (editingProduct != null) {
                    products.map { if (it.id == savedProduct.id) savedProduct else it }
                } else {
                    products + savedProduct
                }
                onUpdateProducts(nextList)
                bannerMessage = "Menu \"${savedProduct.name}\" berhasil disimpan!"
                showFormModal = false
            }
        )
    }
}

@Composable
private fun ProductFormDialog(
    existing: ProductItem?,
    allProducts: List<ProductItem>,
    onDismiss: () -> Unit,
    onSave: (ProductItem) -> Unit
) {
    val nextNumber = remember(allProducts) {
        val maxNum = allProducts.mapNotNull {
            Regex("^(\\d+)").find(it.name)?.groupValues?.get(1)?.toIntOrNull()
        }.maxOrNull() ?: allProducts.size
        maxNum + 1
    }

    val initialMenuNumber = remember(existing) {
        if (existing != null) {
            Regex("^(\\d+)\\.\\s*").find(existing.name)?.groupValues?.get(1) ?: nextNumber.toString()
        } else {
            nextNumber.toString()
        }
    }

    val initialCleanName = remember(existing) {
        if (existing != null) {
            existing.name.replace(Regex("^\\d+\\.\\s*"), "")
        } else {
            ""
        }
    }

    var menuNumber by remember { mutableStateOf(initialMenuNumber) }
    var name by remember { mutableStateOf(initialCleanName) }
    var sku by remember {
        mutableStateOf(
            existing?.barcode ?: "TB-${(allProducts.size + 1).toString().padStart(3, '0')}"
        )
    }
    var category by remember { mutableStateOf(existing?.category ?: "Terang Bulan") }
    var unit by remember { mutableStateOf(existing?.unit ?: "Box") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val variantsState = remember {
        mutableStateListOf<VariantFormState>().apply {
            if (existing != null && existing.variants.isNotEmpty()) {
                existing.variants.forEach { v ->
                    add(VariantFormState(v.id, v.name, v.purchasePrice.toString(), v.sellingPrice.toString()))
                }
            } else if (existing != null) {
                add(VariantFormState("var-1", "Original", existing.purchasePrice.toString(), existing.sellingPrice.toString()))
            } else {
                add(VariantFormState("var-1", "Coklat", "7000", "13000"))
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp))
                .background(Zinc900)
                .border(1.dp, Zinc800, RoundedCornerShape(20.dp))
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing != null) "Edit Menu & Varian" else "Tambah Menu Baru",
                    color = Zinc100,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Zinc400)
                }
            }

            if (errorMsg != null) {
                Text(text = errorMsg!!, color = Red400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = menuNumber,
                    onValueChange = { menuNumber = it.filter { ch -> ch.isDigit() } },
                    label = { Text("No. Urut", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.35f)
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it.uppercase() },
                    label = { Text("Kode SKU", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.65f)
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Menu (contoh: Terang Bulan Premium)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Satuan (Box/Porsi)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Multi-Variant Editor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Yellow400, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Daftar Varian & Harga (${variantsState.size})",
                        color = Yellow400,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        variantsState.add(
                            VariantFormState(
                                id = "var-${System.currentTimeMillis()}-${variantsState.size + 1}",
                                name = "",
                                costPrice = "8000",
                                unitPrice = "15000"
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Yellow400.copy(alpha = 0.15f),
                        contentColor = Yellow300
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Varian", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            variantsState.forEachIndexed { idx, v ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Zinc950)
                        .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Varian #${idx + 1}",
                            color = Zinc400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (variantsState.size > 1) {
                            IconButton(
                                onClick = { variantsState.removeAt(idx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Varian",
                                    tint = Red400,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = v.name,
                        onValueChange = { variantsState[idx] = v.copy(name = it) },
                        label = { Text("Nama Rasa / Varian", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = v.costPrice,
                            onValueChange = { variantsState[idx] = v.copy(costPrice = it.filter { ch -> ch.isDigit() }) },
                            label = { Text("Harga Modal (Rp)", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = v.unitPrice,
                            onValueChange = { variantsState[idx] = v.copy(unitPrice = it.filter { ch -> ch.isDigit() }) },
                            label = { Text("Harga Jual (Rp)", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Zinc800, contentColor = Zinc300),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batal", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val cleanName = name.trim()
                        if (cleanName.isBlank()) {
                            errorMsg = "Nama menu wajib diisi."
                            return@Button
                        }
                        val validVariants = variantsState.mapNotNull { vf ->
                            val vName = vf.name.trim()
                            if (vName.isEmpty()) null
                            else ProductVariant(
                                id = vf.id,
                                name = vName,
                                purchasePrice = vf.costPrice.toLongOrNull() ?: 0L,
                                sellingPrice = vf.unitPrice.toLongOrNull() ?: 0L
                            )
                        }
                        if (validVariants.isEmpty()) {
                            errorMsg = "Minimal harus ada 1 varian rasa yang memiliki nama."
                            return@Button
                        }
                        val numPrefix = menuNumber.trim().ifBlank { "1" }
                        val finalName = "$numPrefix. $cleanName"
                        val firstVar = validVariants.first()
                        val product = ProductItem(
                            id = existing?.id ?: "prod-${System.currentTimeMillis()}",
                            barcode = sku.trim().ifBlank { existing?.barcode ?: "899700100${System.currentTimeMillis() % 1000}" },
                            name = finalName,
                            category = category.trim().ifBlank { "Terang Bulan" },
                            unit = unit.trim().ifBlank { "Box" },
                            variants = validVariants,
                            purchasePrice = firstVar.purchasePrice,
                            sellingPrice = firstVar.sellingPrice,
                            stock = existing?.stock ?: 100,
                            minStock = existing?.minStock ?: 10,
                            location = existing?.location ?: "Etalase Utama",
                            lastUpdated = Formatter.nowIsoString()
                        )
                        onSave(product)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Yellow400, contentColor = Zinc950),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simpan Menu", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
