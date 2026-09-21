package ru.severmax.control

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(this)
        setContent { AppTheme { App() } }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFFFF8A4C))
    } else {
        lightColorScheme(primary = Color(0xFFE8590C))
    }
    MaterialTheme(colorScheme = colors, content = content)
}

fun toast(ctx: Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

fun fmt(t: Long): String =
    SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date(t))

fun sendCommand(ctx: Context, text: String) {
    if (Repo.phone.isBlank()) {
        toast(ctx, "Сначала укажите номер SIM подогревателя во вкладке «Настройки»")
        return
    }
    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        toast(ctx, "Нет разрешения на отправку SMS")
        return
    }
    Sms.send(ctx, Repo.phone, text)
        .onSuccess {
            Repo.addLog(false, text)
            toast(ctx, "Отправлено: $text")
        }
        .onFailure { toast(ctx, "Ошибка отправки: ${it.message}") }
}

@Composable
fun App() {
    var tab by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS))
    }
    Scaffold(
        topBar = { Header() },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Text("🔥") }, label = { Text("Управление") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Text("📋") }, label = { Text("Журнал") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Text("⚙️") }, label = { Text("Настройки") }
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> ControlScreen()
                1 -> LogScreen()
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Севермакс",
            modifier = Modifier.height(40.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Text("$label: ${value.roundToInt()}")
    Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
}

@Composable
fun StatusCard() {
    val st by Repo.status.collectAsState()
    val ctx = LocalContext.current
    Section("Состояние (по последнему SMS)") {
        val label = when (st.on) {
            true -> "🔥 Работает"
            false -> "⏹ Выключен"
            null -> "Нет данных"
        }
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (st.up != null) {
            Text("Верхний порог: ${st.up} °C, нижний: ${st.down ?: "—"} °C")
        }
        if (st.csq != null) {
            Text("Сигнал GSM (CSQ): ${st.csq} из 31")
        }
        Text(
            if (st.updated > 0) "Последний ответ: ${fmt(st.updated)}" else "Ответов пока нет",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = { sendCommand(ctx, Cmd.status()) }) {
            Text("Запросить статус (C)")
        }
    }
}

@Composable
fun ControlScreen() {
    val ctx = LocalContext.current
    var minutes by remember { mutableFloatStateOf(30f) }
    var temp by remember { mutableFloatStateOf(65f) }
    var up by remember { mutableFloatStateOf(90f) }
    var down by remember { mutableFloatStateOf(30f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusCard()

        Section("Запуск и остановка") {
            LabeledSlider(
                "Время работы, мин", minutes, { minutes = (it / 5f).roundToInt() * 5f },
                5f..180f, 34
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { sendCommand(ctx, Cmd.start(minutes.roundToInt())) },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text("▶ Запуск") }
                Button(
                    onClick = { sendCommand(ctx, Cmd.stop()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text("⏹ Стоп") }
            }
            Text(
                "Запуск: K*время. После остановки повторный запуск возможен не ранее чем через 3 минуты.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Section("Температура нагрева") {
            LabeledSlider("Температура, °C", temp, { temp = it }, 30f..90f, 59)
            Button(
                onClick = { sendCommand(ctx, Cmd.setTemp(Repo.fw, temp.roundToInt())) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Установить температуру") }
            Text(
                "Только записывает значение, подогреватель не запускает. Следующий запуск (K) будет с этой температурой.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Section("Режим догрева") {
            LabeledSlider("Верхний порог, °C", up, { up = it }, 40f..90f, 49)
            LabeledSlider("Нижний порог, °C", down, { down = it }, 30f..80f, 49)
            Button(
                onClick = {
                    val u = up.roundToInt()
                    val d = down.roundToInt()
                    if (u - d < 10) {
                        toast(ctx, "Разница между порогами должна быть не менее 10 °C")
                    } else {
                        sendCommand(ctx, Cmd.boost(u, d))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Запустить в режиме догрева") }
            Text(
                "Поддерживает температуру жидкости между порогами. Следующий запуск командой K снова пойдёт в обычном режиме.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun LogScreen() {
    val entries by Repo.log.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Журнал SMS", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { Repo.clearLog() }) { Text("Очистить") }
        }
        if (entries.isEmpty()) Text("Пока пусто")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries.asReversed()) { e -> LogRow(e) }
        }
    }
}

@Composable
fun LogRow(e: LogEntry) {
    val err = if (e.incoming) ErrorCodes.find(e.text) else null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (e.incoming) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                (if (e.incoming) "← Получено  " else "→ Отправлено  ") + fmt(e.time),
                style = MaterialTheme.typography.labelSmall
            )
            Text(e.text, fontWeight = FontWeight.Medium)
            if (err != null) {
                Text("${err.first}: ${err.second}", color = MaterialTheme.colorScheme.error)
                Text(err.third, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var phone by remember { mutableStateOf(Repo.phone) }
    var code by remember { mutableStateOf(Repo.code) }
    var fw by remember { mutableStateOf(Repo.fw) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Section("Подогреватель") {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; Repo.phone = it.trim() },
                label = { Text("Номер SIM подогревателя") },
                placeholder = { Text("+79001234567") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Команда температуры зависит от версии прошивки:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = fw == "NFPZ",
                    onClick = { fw = "NFPZ"; Repo.fw = "NFPZ" },
                    label = { Text("NFPZ") }
                )
                FilterChip(
                    selected = fw == "CGPZ",
                    onClick = { fw = "CGPZ"; Repo.fw = "CGPZ" },
                    label = { Text("CGPZ") }
                )
            }
        }

        Section("Привязка этого телефона") {
            Text(
                "Отправьте команду привязки с этого телефона в свободный слот (1–4). " +
                    "Подтверждение: SMS «ADDAUTH OK!». На SIM подогревателя должен быть положительный баланс."
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it; Repo.code = it.trim() },
                label = { Text("Код в команде привязки (по инструкции 123456)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf('A', 'B', 'C', 'D').forEachIndexed { i, c ->
                    OutlinedButton(
                        onClick = { sendCommand(ctx, Cmd.bind(Repo.code, c)) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Слот ${i + 1}") }
                }
            }
        }

        Section("Коды неисправностей") {
            ErrorCodes.all.forEach { (c, title, advice) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$c — $title", fontWeight = FontWeight.SemiBold)
                    Text(advice, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
