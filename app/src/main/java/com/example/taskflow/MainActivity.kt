package com.example.taskflow

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val priority: String,
    val category: String,
    val dueDate: String,
    val isImportant: Boolean,
    val isCompleted: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TaskFlowApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFlowApp(context: Context) {

    val prefs = remember {
        context.getSharedPreferences("taskflow_data", Context.MODE_PRIVATE)
    }

    var tasks by remember { mutableStateOf(loadTasks(prefs)) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Newest") }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark", false)) }

    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    var editingTask by remember { mutableStateOf<Task?>(null) }
    var deletedTask by remember { mutableStateOf<Task?>(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var category by remember { mutableStateOf("Study") }
    var dueDate by remember { mutableStateOf("") }
    var important by remember { mutableStateOf(false) }

    val today = SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(Date())

    fun save(updated: List<Task>) {
        tasks = updated
        saveTasks(prefs, updated)
    }

    fun resetForm() {
        editingTask = null
        title = ""
        description = ""
        priority = "Medium"
        category = "Study"
        dueDate = ""
        important = false
    }

    fun openAdd() {
        resetForm()
        showDialog = true
    }

    fun openEdit(task: Task) {
        editingTask = task
        title = task.title
        description = task.description
        priority = task.priority
        category = task.category
        dueDate = task.dueDate
        important = task.isImportant
        showDialog = true
    }

    fun saveTask() {
        if (title.isBlank()) return

        val old = editingTask

        if (old == null) {
            val newTask = Task(
                id = (tasks.maxOfOrNull { it.id } ?: 0) + 1,
                title = title.trim(),
                description = description.trim(),
                priority = priority,
                category = category,
                dueDate = dueDate,
                isImportant = important,
                isCompleted = false
            )
            save(tasks + newTask)
        } else {
            save(tasks.map {
                if (it.id == old.id) {
                    it.copy(
                        title = title.trim(),
                        description = description.trim(),
                        priority = priority,
                        category = category,
                        dueDate = dueDate,
                        isImportant = important
                    )
                } else it
            })
        }

        showDialog = false
        resetForm()
    }

    val filtered = tasks
        .filter {
            it.title.contains(search, true) ||
                    it.description.contains(search, true) ||
                    it.category.contains(search, true)
        }
        .filter {
            when (filter) {
                "Active" -> !it.isCompleted
                "Completed" -> it.isCompleted
                "Today" -> it.dueDate == today
                "Important" -> it.isImportant && !it.isCompleted
                else -> true
            }
        }
        .let { list ->
            when (sortBy) {
                "Priority" -> list.sortedByDescending {
                    when (it.priority) {
                        "High" -> 3
                        "Medium" -> 2
                        else -> 1
                    }
                }
                "Due Date" -> list.sortedBy { it.dueDate }
                "A-Z" -> list.sortedBy { it.title.lowercase() }
                else -> list.sortedByDescending { it.id }
            }
        }

    val completed = tasks.count { it.isCompleted }
    val active = tasks.count { !it.isCompleted }
    val importantCount = tasks.count { it.isImportant && !it.isCompleted }
    val todayCount = tasks.count { it.dueDate == today && !it.isCompleted }

    val progress =
        if (tasks.isEmpty()) 0f
        else completed.toFloat() / tasks.size

    val streak =
        tasks.count { it.isCompleted }.coerceAtMost(7)

    val background =
        if (darkMode) Color(0xFF121212)
        else Color(0xFFF7F5FC)

    Scaffold(
        containerColor = background,

        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6750A4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column {
                            Text(
                                "TaskFlow",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Plan • Focus • Achieve",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },

                actions = {
                    Text("🌙")
                    Switch(
                        checked = darkMode,
                        onCheckedChange = {
                            darkMode = it
                            prefs.edit()
                                .putBoolean("dark", it)
                                .apply()
                        }
                    )
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { openAdd() },
                containerColor = Color(0xFF6750A4)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = Color.White
                )
            }
        }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF6750A4)
                    )
                ) {
                    Column(
                        Modifier.padding(22.dp)
                    ) {
                        Text(
                            "✨ Turn your plans into progress.",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Organize your day, complete your goals and build your productivity streak.",
                            color = Color.White.copy(alpha = .85f)
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardCard(
                        "Total",
                        tasks.size.toString(),
                        "📋",
                        Modifier.weight(1f)
                    )
                    DashboardCard(
                        "Active",
                        active.toString(),
                        "🔥",
                        Modifier.weight(1f)
                    )
                    DashboardCard(
                        "Done",
                        completed.toString(),
                        "✅",
                        Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "📈 Productivity Progress",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${(progress * 100).toInt()}%",
                                color = Color(0xFF6750A4),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            when {
                                progress == 1f && tasks.isNotEmpty() ->
                                    "🏆 Perfect day! Everything is completed."
                                progress >= .7f ->
                                    "🔥 Excellent progress! Keep going."
                                progress > 0f ->
                                    "🎯 Good start. You can do more!"
                                else ->
                                    "🚀 Add your first task and start your journey."
                            }
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InsightCard(
                        "Today",
                        todayCount.toString(),
                        "📅",
                        Modifier.weight(1f)
                    )
                    InsightCard(
                        "Important",
                        importantCount.toString(),
                        "⭐",
                        Modifier.weight(1f)
                    )
                    InsightCard(
                        "Streak",
                        "$streak 🔥",
                        "🏆",
                        Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search tasks") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    maxLines = 1
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf(
                        "All",
                        "Active",
                        "Completed",
                        "Today",
                        "Important"
                    ).forEach {
                        FilterChip(
                            selected = filter == it,
                            onClick = { filter = it },
                            label = { Text(it) }
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📝 Your Tasks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = { showSortDialog = true }
                    ) {
                        Text("Sort ↕")
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "📝",
                                style = MaterialTheme.typography.displaySmall
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "No tasks found",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Tap + to create your first task."
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { task ->
                    TaskCard(
                        task = task,

                        onComplete = {
                            save(
                                tasks.map {
                                    if (it.id == task.id) {
                                        it.copy(
                                            isCompleted =
                                                !it.isCompleted
                                        )
                                    } else it
                                }
                            )
                        },

                        onImportant = {
                            save(
                                tasks.map {
                                    if (it.id == task.id) {
                                        it.copy(
                                            isImportant =
                                                !it.isImportant
                                        )
                                    } else it
                                }
                            )
                        },

                        onEdit = {
                            openEdit(task)
                        },

                        onDelete = {
                            deletedTask = task
                            save(
                                tasks.filter {
                                    it.id != task.id
                                }
                            )
                        }
                    )
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "🏆 Achievements",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            when {
                                completed >= 10 ->
                                    "🌟 Productivity Legend — 10+ tasks completed!"
                                completed >= 5 ->
                                    "🔥 Productivity Master — 5+ tasks completed!"
                                completed >= 1 ->
                                    "🎯 First Step — You completed your first task!"
                                else ->
                                    "🚀 Complete tasks to unlock achievements!"
                            }
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "📊 Weekly summary: $completed tasks completed"
                        )
                    }
                }
            }

            if (deletedTask != null) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF6750A4)
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                "Task deleted",
                                color = Color.White
                            )

                            TextButton(
                                onClick = {
                                    deletedTask?.let {
                                        save(tasks + it)
                                    }
                                    deletedTask = null
                                }
                            ) {
                                Text(
                                    "UNDO",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        TaskDialog(
            editing = editingTask != null,
            title = title,
            description = description,
            priority = priority,
            category = category,
            dueDate = dueDate,
            important = important,

            onTitle = { title = it },
            onDescription = { description = it },
            onPriority = { priority = it },
            onCategory = { category = it },
            onDueDate = { dueDate = it },
            onImportant = { important = it },

            onSave = { saveTask() },
            onDismiss = {
                showDialog = false
                resetForm()
            }
        )
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = {
                showSortDialog = false
            },

            title = {
                Text("Sort Tasks")
            },

            text = {
                Column {
                    listOf(
                        "Newest",
                        "Priority",
                        "Due Date",
                        "A-Z"
                    ).forEach {
                        TextButton(
                            onClick = {
                                sortBy = it
                                showSortDialog = false
                            }
                        ) {
                            Text(
                                if (sortBy == it)
                                    "✓ $it"
                                else it
                            )
                        }
                    }
                }
            },

            confirmButton = {}
        )
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    emoji: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.padding(13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(title)
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    value: String,
    emoji: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji)
            Text(
                value,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onComplete: () -> Unit,
    onImportant: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor =
        when (task.priority) {
            "High" -> Color(0xFFE53935)
            "Medium" -> Color(0xFFFB8C00)
            else -> Color(0xFF43A047)
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (task.isCompleted)
                    Color(0xFFE8F5E9)
                else
                    Color.White
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = {
                    onComplete()
                }
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        task.title,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.width(6.dp))

                    if (task.isImportant) {
                        Text("⭐")
                    }
                }

                if (task.description.isNotBlank()) {
                    Text(task.description)
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "● ${task.priority}",
                        color = priorityColor,
                        fontWeight = FontWeight.Bold
                    )

                    Text("📁 ${task.category}")

                    if (task.dueDate.isNotBlank()) {
                        Text("📅 ${task.dueDate}")
                    }
                }

                Text(
                    if (task.isCompleted)
                        "✅ Completed"
                    else
                        "⏳ In Progress",
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                IconButton(
                    onClick = onImportant
                ) {
                    Icon(
                        if (task.isImportant)
                            Icons.Default.Star
                        else
                            Icons.Default.StarBorder,
                        contentDescription = "Important"
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        }
    }
}

@Composable
fun TaskDialog(
    editing: Boolean,
    title: String,
    description: String,
    priority: String,
    category: String,
    dueDate: String,
    important: Boolean,
    onTitle: (String) -> Unit,
    onDescription: (String) -> Unit,
    onPriority: (String) -> Unit,
    onCategory: (String) -> Unit,
    onDueDate: (String) -> Unit,
    onImportant: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                if (editing)
                    "✏️ Edit Task"
                else
                    "✨ Add New Task"
            )
        },

        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitle,
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescription,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "Priority",
                    fontWeight = FontWeight.Bold
                )

                Row {
                    listOf(
                        "Low",
                        "Medium",
                        "High"
                    ).forEach {
                        TextButton(
                            onClick = {
                                onPriority(it)
                            }
                        ) {
                            Text(
                                if (priority == it)
                                    "✓ $it"
                                else it
                            )
                        }
                    }
                }

                Text(
                    "Category",
                    fontWeight = FontWeight.Bold
                )

                Row {
                    listOf(
                        "Study",
                        "Personal",
                        "Work"
                    ).forEach {
                        TextButton(
                            onClick = {
                                onCategory(it)
                            }
                        ) {
                            Text(
                                if (category == it)
                                    "✓ $it"
                                else it
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = onDueDate,
                    label = {
                        Text("Due Date (e.g. 20 Jul 2026)")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = important,
                        onCheckedChange = onImportant
                    )

                    Text("⭐ Mark as Important")
                }
            }
        },

        confirmButton = {
            Button(
                onClick = onSave
            ) {
                Text(
                    if (editing)
                        "Save Changes"
                    else
                        "Add Task"
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

fun saveTasks(
    prefs: android.content.SharedPreferences,
    tasks: List<Task>
) {
    val array = JSONArray()

    tasks.forEach {
        val obj = JSONObject()

        obj.put("id", it.id)
        obj.put("title", it.title)
        obj.put("description", it.description)
        obj.put("priority", it.priority)
        obj.put("category", it.category)
        obj.put("dueDate", it.dueDate)
        obj.put("isImportant", it.isImportant)
        obj.put("isCompleted", it.isCompleted)

        array.put(obj)
    }

    prefs.edit()
        .putString("tasks", array.toString())
        .apply()
}

fun loadTasks(
    prefs: android.content.SharedPreferences
): List<Task> {
    val saved =
        prefs.getString("tasks", null)
            ?: return emptyList()

    val result = mutableListOf<Task>()

    try {
        val array = JSONArray(saved)

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            result.add(
                Task(
                    id = obj.getInt("id"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    priority = obj.getString("priority"),
                    category = obj.optString(
                        "category",
                        "Study"
                    ),
                    dueDate = obj.getString("dueDate"),
                    isImportant = obj.optBoolean(
                        "isImportant",
                        false
                    ),
                    isCompleted = obj.getBoolean(
                        "isCompleted"
                    )
                )
            )
        }
    } catch (_: Exception) {
        return emptyList()
    }

    return result
}