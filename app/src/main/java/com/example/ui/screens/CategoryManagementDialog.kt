package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Category
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val appLang by viewModel.appLanguage.collectAsState()
    val isId = appLang == "id"
    
    // Tabs: 0 for EXPENSE, 1 for INCOME
    var selectedTabState by remember { mutableStateOf(0) }
    
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editCategoryName by remember { mutableStateOf("") }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

    val currentType = if (selectedTabState == 0) "EXPENSE" else "INCOME"
    val filteredCategories = categories.filter { it.type == currentType }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val dialogWidth = if (screenWidth < 600) (screenWidth * 0.94).dp else 520.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .width(dialogWidth)
                .heightIn(max = (screenHeight * 0.85).dp)
                .fillMaxHeight(0.85f)
                .padding(12.dp)
                .testTag("category_management_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "Kelola Kategori" else "Manage Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs (Modern Segmented Control)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val segments = listOf(
                        0 to (if (isId) "Pengeluaran" else "Expense"),
                        1 to (if (isId) "Pemasukan" else "Income")
                    )
                    segments.forEach { (index, label) ->
                        val isSelected = selectedTabState == index
                        val tabItemShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = tabItemShape
                                )
                                .clip(tabItemShape)
                                .clickable { selectedTabState = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add Category Input Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text(if (isId) "Kategori baru..." else "New category...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("new_category_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.addCategory(newCategoryName.trim(), currentType)
                                newCategoryName = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .width(56.dp)
                            .testTag("add_category_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of Categories
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredCategories.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isId) "Kategori tidak ditemukan." else "No categories found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredCategories, key = { it.id }) { category ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("category_list_item_${category.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    editingCategory = category
                                                    editCategoryName = category.name
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Category",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    deletingCategory = category
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Category",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Name Dialog
    if (editingCategory != null) {
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = {
                Text(
                    if (isId) "Ubah Nama Kategori" else "Edit Category Name",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editCategoryName,
                        onValueChange = { editCategoryName = it },
                        singleLine = true,
                        placeholder = { Text(if (isId) "Nama kategori" else "Category name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_category_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val catToEdit = editingCategory
                        if (catToEdit != null && editCategoryName.isNotBlank()) {
                            viewModel.updateCategory(catToEdit.copy(name = editCategoryName.trim()))
                            editingCategory = null
                        }
                    },
                    modifier = Modifier.testTag("edit_category_save"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isId) "Simpan" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text(if (isId) "Batal" else "Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingCategory != null) {
        val catToDelete = deletingCategory
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = {
                Text(
                    if (isId) "Hapus Kategori?" else "Delete Category?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    if (isId) "Apakah Anda yakin ingin menghapus kategori \"${catToDelete?.name}\"? Transaksi di kategori ini tidak akan dihapus, tetapi tidak lagi dikategorikan." 
                    else "Are you sure you want to delete the category \"${catToDelete?.name}\"? Transactions in this category will not be deleted, but they will no longer be categorized."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catToDelete != null) {
                            viewModel.deleteCategory(catToDelete)
                            deletingCategory = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("delete_category_confirm"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isId) "Hapus" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text(if (isId) "Batal" else "Cancel")
                }
            }
        )
    }
}
