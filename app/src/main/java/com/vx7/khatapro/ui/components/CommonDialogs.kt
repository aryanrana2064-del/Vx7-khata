package com.vx7.khatapro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vx7.khatapro.core.utils.CurrencyFormatter
import com.vx7.khatapro.core.utils.DateUtils
import com.vx7.khatapro.data.database.entities.CustomerEntity
import com.vx7.khatapro.data.database.models.OrderWithDetails
import java.util.Calendar

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (isDestructive) Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun AddCustomerDialog(
    initialCustomer: CustomerEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }
    var notes by remember { mutableStateOf(initialCustomer?.notes ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialCustomer == null) "Add New Customer" else "Edit Customer",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Customer Name *") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        if (it.isNotBlank()) phoneError = null
                    },
                    label = { Text("Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    supportingText = phoneError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / City (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (name.trim().isBlank()) {
                        nameError = "Customer name is required"
                        hasError = true
                    }
                    if (phone.trim().isBlank()) {
                        phoneError = "Mobile number is required"
                        hasError = true
                    }
                    if (!hasError) {
                        onSave(name.trim(), phone.trim(), address.trim(), notes.trim())
                    }
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text(if (initialCustomer == null) "Add Customer" else "Update")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrderDialog(
    customers: List<CustomerEntity>,
    selectedCustomerPreselect: CustomerEntity? = null,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (
        customerId: Long,
        item: String,
        quantity: Double,
        rate: Double,
        orderDate: Long,
        initialDeposit: Double,
        depositDate: Long,
        notes: String
    ) -> Unit
) {
    var selectedCustomer by remember {
        mutableStateOf(selectedCustomerPreselect ?: customers.firstOrNull())
    }
    var expandedDropdown by remember { mutableStateOf(false) }

    var item by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var rateStr by remember { mutableStateOf("") }
    var initialDepositStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    var itemError by remember { mutableStateOf<String?>(null) }
    var qtyError by remember { mutableStateOf<String?>(null) }
    var rateError by remember { mutableStateOf<String?>(null) }
    var depositError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }

    val qty = quantityStr.toDoubleOrNull() ?: 0.0
    val rate = rateStr.toDoubleOrNull() ?: 0.0
    val calculatedTotal = (qty * rate).coerceAtLeast(0.0)

    val deposit = initialDepositStr.toDoubleOrNull() ?: 0.0
    val calculatedBalance = (calculatedTotal - deposit).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create New Order", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Customer Selector
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "Select Customer",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Customer *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        isError = customerError != null,
                        supportingText = customerError?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text("${cust.name} (${cust.phone})") },
                                onClick = {
                                    selectedCustomer = cust
                                    customerError = null
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Item Name
                OutlinedTextField(
                    value = item,
                    onValueChange = {
                        item = it
                        if (it.isNotBlank()) itemError = null
                    },
                    label = { Text("Item / Description *") },
                    isError = itemError != null,
                    supportingText = itemError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quantity & Rate in Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = {
                            quantityStr = it
                            if ((it.toDoubleOrNull() ?: 0.0) > 0) qtyError = null
                        },
                        label = { Text("Quantity *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = qtyError != null,
                        supportingText = qtyError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = {
                            rateStr = it
                            if ((it.toDoubleOrNull() ?: 0.0) >= 0) rateError = null
                        },
                        label = { Text("Rate ($currencySymbol) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = rateError != null,
                        supportingText = rateError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                // Live Calculation Card
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calculated Total:", fontWeight = FontWeight.Medium)
                        Text(
                            CurrencyFormatter.format(calculatedTotal, currencySymbol),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Initial Deposit
                OutlinedTextField(
                    value = initialDepositStr,
                    onValueChange = {
                        initialDepositStr = it
                        val depVal = it.toDoubleOrNull() ?: 0.0
                        if (depVal <= calculatedTotal) depositError = null
                    },
                    label = { Text("Initial Deposit ($currencySymbol) (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = depositError != null,
                    supportingText = depositError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Balance Card
                Surface(
                    color = if (calculatedBalance > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Remaining Balance:", fontWeight = FontWeight.Medium)
                        Text(
                            CurrencyFormatter.format(calculatedBalance, currencySymbol),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (calculatedBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Order Notes (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (selectedCustomer == null) {
                        customerError = "Please select a customer"
                        hasError = true
                    }
                    if (item.trim().isBlank()) {
                        itemError = "Item name is required"
                        hasError = true
                    }
                    if (qty <= 0) {
                        qtyError = "Qty must be > 0"
                        hasError = true
                    }
                    if (rate < 0 || rateStr.isBlank()) {
                        rateError = "Valid rate required"
                        hasError = true
                    }
                    if (deposit < 0) {
                        depositError = "Deposit cannot be negative"
                        hasError = true
                    }
                    if (deposit > calculatedTotal) {
                        depositError = "Deposit cannot exceed total"
                        hasError = true
                    }

                    if (!hasError && selectedCustomer != null) {
                        val now = System.currentTimeMillis()
                        onSave(
                            selectedCustomer!!.id,
                            item.trim(),
                            qty,
                            rate,
                            now,
                            deposit,
                            now,
                            notes.trim()
                        )
                    }
                },
                modifier = Modifier.testTag("save_order_button")
            ) {
                Text("Create Order")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    orders: List<OrderWithDetails>,
    preselectedOrder: OrderWithDetails? = null,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (
        orderId: Long,
        customerId: Long,
        amount: Double,
        paymentDate: Long,
        paymentMethod: String,
        notes: String
    ) -> Unit
) {
    // Only orders with positive balance unless preselected
    val eligibleOrders = remember(orders) {
        val list = orders.filter { it.balance > 0.001 }
        if (preselectedOrder != null && !list.contains(preselectedOrder)) {
            listOf(preselectedOrder) + list
        } else {
            list
        }
    }

    var selectedOrder by remember {
        mutableStateOf(preselectedOrder ?: eligibleOrders.firstOrNull())
    }
    var expandedDropdown by remember { mutableStateOf(false) }

    var amountStr by remember {
        mutableStateOf(selectedOrder?.balance?.let { "%.2f".format(it) } ?: "")
    }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var expandedMethodDropdown by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    var orderError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val methods = listOf("Cash", "UPI", "Bank Transfer", "Cheque", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Record Payment", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (eligibleOrders.isEmpty()) {
                    Text(
                        "No orders with outstanding balance found! Create an order first.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    // Order selector
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedOrder?.let { "Order #${it.order.id} • ${it.customerName} (Due: ${CurrencyFormatter.format(it.balance, currencySymbol)})" } ?: "Select Order",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Order *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            isError = orderError != null,
                            supportingText = orderError?.let { { Text(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            eligibleOrders.forEach { ord ->
                                DropdownMenuItem(
                                    text = {
                                        Text("#${ord.order.id} - ${ord.customerName} - ${ord.order.item} (Due: ${CurrencyFormatter.format(ord.balance, currencySymbol)})")
                                    },
                                    onClick = {
                                        selectedOrder = ord
                                        amountStr = "%.2f".format(ord.balance)
                                        orderError = null
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Order Balance Indicator
                    selectedOrder?.let { ord ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Remaining Due:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    CurrencyFormatter.format(ord.balance, currencySymbol),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Amount
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            val amt = it.toDoubleOrNull() ?: 0.0
                            val maxBalance = selectedOrder?.balance ?: 0.0
                            if (amt > 0 && amt <= maxBalance + 0.01) amountError = null
                        },
                        label = { Text("Payment Amount ($currencySymbol) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = amountError != null,
                        supportingText = amountError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_amount_input")
                    )

                    // Payment Method Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedMethodDropdown,
                        onExpandedChange = { expandedMethodDropdown = !expandedMethodDropdown }
                    ) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMethodDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMethodDropdown,
                            onDismissRequest = { expandedMethodDropdown = false }
                        ) {
                            methods.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        paymentMethod = m
                                        expandedMethodDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Reference / Notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = eligibleOrders.isNotEmpty(),
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val maxBalance = selectedOrder?.balance ?: 0.0
                    var hasError = false

                    if (selectedOrder == null) {
                        orderError = "Please select an order"
                        hasError = true
                    }
                    if (amt <= 0) {
                        amountError = "Amount must be greater than 0"
                        hasError = true
                    } else if (amt > maxBalance + 0.01) {
                        amountError = "Amount exceeds order balance ($currencySymbol$maxBalance)"
                        hasError = true
                    }

                    if (!hasError && selectedOrder != null) {
                        onSave(
                            selectedOrder!!.order.id,
                            selectedOrder!!.order.customerId,
                            amt,
                            System.currentTimeMillis(),
                            paymentMethod,
                            notes.trim()
                        )
                    }
                },
                modifier = Modifier.testTag("save_payment_button")
            ) {
                Text("Save Payment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
