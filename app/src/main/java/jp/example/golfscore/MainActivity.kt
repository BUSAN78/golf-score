package jp.example.golfscore

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.net.URL
import java.net.URLEncoder
import java.net.HttpURLConnection
import kotlinx.coroutines.delay

data class PlayerScore(
    val name: String = "",
    val ladiesTee: Boolean = false,
    val strokes: List<Int> = List(18) { 0 },
    val putts: List<Int> = List(18) { 0 },
    val obs: List<Int> = List(18) { 0 },
    val bunkers: List<Int> = List(18) { 0 },
    val penalties: List<Int> = List(18) { 0 }
) {
    val totalScore get() = strokes.sum()
    val totalPutts get() = putts.sum()
    val totalOb get() = obs.sum()
    val totalBunkers get() = bunkers.sum()
    val totalPenalties get() = penalties.sum()
}

data class RoundData(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().toString(),
    val course: String = "",
    val region: String = "",
    val weather: String = "晴れ",
    val wind: String = "弱風",
    val frontCourse: String = "",
    val backCourse: String = "",
    val courseOptions: List<String> = emptyList(),
    val golfCourseId: Long = 0,
    val courseDetailJson: String = "",
    val greenName: String = "",
    val courseYardages: List<CourseYardage> = emptyList(),
    val pars: List<Int> = List(18) { 4 },
    val yards: List<Int> = List(18) { 0 },
    val useLongDrive: Boolean = false,
    val useNearPin: Boolean = false,
    val useOlympic: Boolean = false,
    val useTateYoko: Boolean = false,
    val longDriveParticipants: List<Int> = listOf(0, 1, 2, 3),
    val nearPinParticipants: List<Int> = listOf(0, 1, 2, 3),
    val olympicParticipants: List<Int> = listOf(0, 1, 2, 3),
    val tateYokoParticipants: List<Int> = listOf(0, 1, 2, 3),
    val tateYokoPositions: List<Int> = listOf(0, 1, 2, 3),
    val tateYokoScoring: String = "合計スコア",
    val tateYokoWinPoints: Int = 1,
    val tateYokoCarryDraw: Boolean = false,
    val tateYokoRotate: Boolean = false,
    val longDriveHoles: List<Int> = emptyList(),
    val nearPinHoles: List<Int> = emptyList(),
    val longDriveWinners: List<String> = List(18) { "" },
    val nearPinWinners: List<String> = List(18) { "" },
    val olympicGold: List<String> = List(18) { "" },
    val olympicSilver: List<String> = List(18) { "" },
    val olympicBronze: List<String> = List(18) { "" },
    val olympicIron: List<String> = List(18) { "" },
    val teeGround: String = "レギュラー",
    val players: List<PlayerScore> = List(4) { PlayerScore() }
)

data class GolfCourse(
    val id: Long,
    val name: String,
    val address: String,
    val courseNames: List<String> = emptyList(),
    val detailJson: String = "",
    val yardages: List<CourseYardage> = emptyList()
)

data class CourseYardage(
    val course: String,
    val green: String,
    val tee: String,
    val pars: List<Int>,
    val yards: List<Int>
)

class RoundStore(context: Context) {
    private val prefs = context.getSharedPreferences("round_store", Context.MODE_PRIVATE)

    fun load(): List<RoundData> = runCatching {
        val array = JSONArray(prefs.getString("rounds", "[]"))
        List(array.length()) { i -> array.getJSONObject(i).toRound() }
    }.getOrDefault(emptyList())

    fun save(rounds: List<RoundData>) {
        val array = JSONArray()
        rounds.forEach { array.put(it.toJson()) }
        prefs.edit().putString("rounds", array.toString()).apply()
    }

    private fun RoundData.toJson() = JSONObject().apply {
        put("id", id); put("date", date); put("course", course); put("region", region)
        put("weather", weather); put("wind", wind); put("teeGround", teeGround)
        put("frontCourse", frontCourse); put("backCourse", backCourse)
        put("courseOptions", JSONArray(courseOptions))
        put("golfCourseId", golfCourseId); put("courseDetailJson", courseDetailJson); put("greenName", greenName)
        put("courseYardages", JSONArray().apply { courseYardages.forEach { profile -> put(JSONObject().apply {
            put("course", profile.course); put("green", profile.green); put("tee", profile.tee)
            put("pars", JSONArray(profile.pars)); put("yards", JSONArray(profile.yards))
        }) } })
        put("pars", JSONArray(pars)); put("yards", JSONArray(yards))
        put("useLongDrive", useLongDrive); put("useNearPin", useNearPin)
        put("useOlympic", useOlympic); put("useTateYoko", useTateYoko)
        put("longDriveParticipants", JSONArray(longDriveParticipants)); put("nearPinParticipants", JSONArray(nearPinParticipants))
        put("olympicParticipants", JSONArray(olympicParticipants)); put("tateYokoParticipants", JSONArray(tateYokoParticipants))
        put("tateYokoPositions", JSONArray(tateYokoPositions)); put("tateYokoScoring", tateYokoScoring)
        put("tateYokoWinPoints", tateYokoWinPoints); put("tateYokoCarryDraw", tateYokoCarryDraw)
        put("tateYokoRotate", tateYokoRotate)
        put("longDriveHoles", JSONArray(longDriveHoles)); put("nearPinHoles", JSONArray(nearPinHoles))
        put("longDriveWinners", JSONArray(longDriveWinners)); put("nearPinWinners", JSONArray(nearPinWinners))
        put("olympicGold", JSONArray(olympicGold)); put("olympicSilver", JSONArray(olympicSilver))
        put("olympicBronze", JSONArray(olympicBronze)); put("olympicIron", JSONArray(olympicIron))
        put("players", JSONArray().apply { players.forEach { p -> put(JSONObject().apply {
            put("name", p.name); put("ladiesTee", p.ladiesTee)
            put("strokes", JSONArray(p.strokes)); put("putts", JSONArray(p.putts))
            put("obs", JSONArray(p.obs)); put("bunkers", JSONArray(p.bunkers))
            put("penalties", JSONArray(p.penalties))
        }) } })
    }

    private fun JSONObject.toRound() = RoundData(
        id = optString("id", UUID.randomUUID().toString()), date = optString("date", LocalDate.now().toString()),
        course = optString("course"), region = optString("region"), weather = optString("weather", "晴れ"),
        wind = optString("wind", "弱風"),
        frontCourse = optString("frontCourse"), backCourse = optString("backCourse"),
        courseOptions = optJSONArray("courseOptions")?.let { a ->
            List(a.length()) { i -> a.optString(i) }.filter { it.isNotBlank() }
        } ?: emptyList(),
        golfCourseId = optLong("golfCourseId"), courseDetailJson = optString("courseDetailJson"),
        greenName = optString("greenName"),
        courseYardages = optJSONArray("courseYardages")?.let { a -> List(a.length()) { i ->
            val p = a.getJSONObject(i)
            CourseYardage(p.optString("course"), p.optString("green"), p.optString("tee"),
                p.variableIntList("pars"), p.variableIntList("yards"))
        } } ?: emptyList(),
        pars = intList("pars").map { if (it in 3..5) it else 4 },
        yards = intList("yards"),
        useLongDrive = optBoolean("useLongDrive"), useNearPin = optBoolean("useNearPin"),
        useOlympic = optBoolean("useOlympic"), useTateYoko = optBoolean("useTateYoko"),
        longDriveParticipants = participantList("longDriveParticipants"),
        nearPinParticipants = participantList("nearPinParticipants"),
        olympicParticipants = participantList("olympicParticipants"),
        tateYokoParticipants = participantList("tateYokoParticipants"),
        tateYokoPositions = optJSONArray("tateYokoPositions")?.let { a ->
            List(4) { i -> if (i < a.length()) a.optInt(i, i) else i }
        }?.takeIf { it.sorted() == listOf(0, 1, 2, 3) } ?: listOf(0, 1, 2, 3),
        tateYokoScoring = optString("tateYokoScoring", "合計スコア"),
        tateYokoWinPoints = optInt("tateYokoWinPoints", 1).coerceIn(1, 99),
        tateYokoCarryDraw = optBoolean("tateYokoCarryDraw"),
        tateYokoRotate = optBoolean("tateYokoRotate"),
        longDriveHoles = optJSONArray("longDriveHoles")?.let { a -> List(a.length()) { i -> a.optInt(i) }.filter { it in 1..18 } } ?: emptyList(),
        nearPinHoles = optJSONArray("nearPinHoles")?.let { a -> List(a.length()) { i -> a.optInt(i) }.filter { it in 1..18 } } ?: emptyList(),
        longDriveWinners = stringList("longDriveWinners"), nearPinWinners = stringList("nearPinWinners"),
        olympicGold = stringList("olympicGold"), olympicSilver = stringList("olympicSilver"),
        olympicBronze = stringList("olympicBronze"), olympicIron = stringList("olympicIron"),
        teeGround = optString("teeGround", "レギュラー"),
        players = optJSONArray("players")?.let { a -> List(a.length()) { i -> a.getJSONObject(i).toPlayer() } }
            ?.takeIf { it.isNotEmpty() } ?: List(4) { PlayerScore() }
    )

    private fun JSONObject.toPlayer() = PlayerScore(
        name = optString("name"), ladiesTee = optBoolean("ladiesTee"),
        strokes = intList("strokes"), putts = intList("putts"), obs = intList("obs"),
        bunkers = intList("bunkers"), penalties = intList("penalties")
    )

    private fun JSONObject.intList(key: String): List<Int> {
        val a = optJSONArray(key) ?: JSONArray()
        return List(18) { i -> if (i < a.length()) a.optInt(i) else 0 }
    }

    private fun JSONObject.stringList(key: String): List<String> {
        val a = optJSONArray(key) ?: JSONArray()
        return List(18) { i -> if (i < a.length()) a.optString(i) else "" }
    }

    private fun JSONObject.participantList(key: String): List<Int> {
        val a = optJSONArray(key) ?: return listOf(0, 1, 2, 3)
        return List(a.length()) { i -> a.optInt(i, -1) }.filter { it in 0..3 }.distinct()
    }

    private fun JSONObject.variableIntList(key: String): List<Int> {
        val a = optJSONArray(key) ?: return emptyList()
        return List(a.length()) { i -> a.optInt(i) }
    }
}

class CourseProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("course_profiles", Context.MODE_PRIVATE)

    private fun key(round: RoundData): String = listOf(
        round.golfCourseId.takeIf { it > 0 }?.toString() ?: round.course.trim(),
        round.frontCourse.trim(), round.backCourse.trim(), round.greenName.trim(), round.teeGround.trim()
    ).joinToString("|") { it.lowercase() }

    fun save(round: RoundData) {
        if (round.course.isBlank() || round.yards.none { it > 0 }) return
        val value = JSONObject().apply {
            put("pars", JSONArray(round.pars)); put("yards", JSONArray(round.yards))
        }
        prefs.edit().putString(key(round), value.toString()).apply()
    }

    fun load(round: RoundData): Pair<List<Int>, List<Int>>? = runCatching {
        val value = prefs.getString(key(round), null) ?: return null
        val json = JSONObject(value)
        fun ints(name: String): List<Int>? = json.optJSONArray(name)?.let { a -> List(18) { i -> a.optInt(i) } }
        val pars = ints("pars") ?: return null
        val yards = ints("yards") ?: return null
        pars to yards
    }.getOrNull()
}

private fun fetchGoraCourseYardages(golfCourseId: Long): List<CourseYardage> {
    val endpoint = "https://booking.gora.golf.rakuten.co.jp/guide/course_info/layout/disp/c_id/$golfCourseId"
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        connectTimeout = 12_000; readTimeout = 12_000; requestMethod = "GET"
        setRequestProperty("User-Agent", "GOLF-SCORE-Android/1.13.0")
    }
    val status = connection.responseCode
    if (status !in 200..299) error("楽天GORAコース情報: HTTP $status")
    val html = connection.inputStream.bufferedReader().use { it.readText() }
    val payload = Regex("<script[^>]+id=\"__NUXT_DATA__\"[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
        .find(html)?.groupValues?.get(1) ?: error("コース情報の形式を確認できませんでした")
    val values = JSONArray(payload)
    fun scalar(ref: Int): Any? = if (ref in 0 until values.length()) values.opt(ref) else null
    fun objectAt(ref: Int) = scalar(ref) as? JSONObject
    fun arrayAt(ref: Int) = scalar(ref) as? JSONArray
    fun ref(obj: JSONObject, key: String) = obj.optInt(key, -1)
    fun text(ref: Int) = scalar(ref)?.toString().orEmpty()
    fun number(ref: Int) = (scalar(ref) as? Number)?.toInt() ?: scalar(ref)?.toString()?.toIntOrNull() ?: 0
    fun referencedObjects(containerRef: Int): List<JSONObject> {
        val container = objectAt(containerRef) ?: return emptyList()
        val list = arrayAt(ref(container, "list")) ?: return emptyList()
        return List(list.length()) { i -> objectAt(list.optInt(i, -1)) }.filterNotNull()
    }

    val profiles = mutableListOf<CourseYardage>()
    for (index in 0 until values.length()) {
        val course = values.optJSONObject(index) ?: continue
        if (!course.has("courseId") || !course.has("green") || !course.has("tee") || !course.has("layout")) continue
        val courseName = text(ref(course, "name"))
        val greens = referencedObjects(ref(course, "green")).associate { text(ref(it, "greenId")) to text(ref(it, "name")) }
        val tees = referencedObjects(ref(course, "tee")).associate { text(ref(it, "teeId")) to text(ref(it, "name")) }
        val holes = referencedObjects(ref(course, "layout")).sortedBy { number(ref(it, "holeNumber")) }
        greens.forEach { (greenId, greenName) ->
            tees.forEach { (teeId, teeName) ->
                val pars = holes.map { number(ref(it, "par")) }
                val yards = holes.map { hole ->
                    val distances = arrayAt(ref(hole, "distance")) ?: JSONArray()
                    (0 until distances.length()).asSequence().mapNotNull { objectAt(distances.optInt(it, -1)) }
                        .firstOrNull { text(ref(it, "greenId")) == greenId && text(ref(it, "teeId")) == teeId }
                        ?.let { number(ref(it, "value")) } ?: 0
                }
                if (pars.size == 9 && yards.any { it > 0 }) profiles += CourseYardage(courseName, greenName, teeName, pars, yards)
            }
        }
    }
    return profiles.distinctBy { listOf(it.course, it.green, it.tee) }
}

class RakutenSettings(context: Context) {
    private val prefs = context.getSharedPreferences("rakuten_settings", Context.MODE_PRIVATE)
    val applicationId get() = prefs.getString("application_id", "").orEmpty()
    val accessKey get() = prefs.getString("access_key", "").orEmpty()

    fun save(applicationId: String, accessKey: String) {
        prefs.edit()
            .putString("application_id", applicationId.trim())
            .putString("access_key", accessKey.trim())
            .apply()
    }
}

class PlayerDirectory(context: Context) {
    private val prefs = context.getSharedPreferences("player_directory", Context.MODE_PRIVATE)
    val selfName get() = prefs.getString("self_name", "本人").orEmpty().ifBlank { "本人" }
    val friends: List<String> get() = runCatching {
        val array = JSONArray(prefs.getString("friends", "[]"))
        List(array.length()) { array.optString(it) }.filter { it.isNotBlank() }.distinct()
    }.getOrDefault(emptyList())
    val allPlayers get() = (listOf(selfName) + friends).distinct()

    fun save(selfName: String, friends: List<String>) {
        val cleanedSelf = selfName.trim().ifBlank { "本人" }
        val cleanedFriends = friends.map { it.trim() }.filter { it.isNotBlank() && it != cleanedSelf }.distinct()
        prefs.edit()
            .putString("self_name", cleanedSelf)
            .putString("friends", JSONArray(cleanedFriends).toString())
            .apply()
    }

    fun newRoundPlayers() = listOf(PlayerScore(name = selfName)) + List(3) { PlayerScore() }
}

data class OlympicPoints(val gold: Int = 5, val silver: Int = 3, val bronze: Int = 2, val iron: Int = 1)

class GameSettings(context: Context) {
    private val prefs = context.getSharedPreferences("game_settings", Context.MODE_PRIVATE)
    val olympicPoints get() = OlympicPoints(
        gold = prefs.getInt("olympic_gold", 5), silver = prefs.getInt("olympic_silver", 3),
        bronze = prefs.getInt("olympic_bronze", 2), iron = prefs.getInt("olympic_iron", 1)
    )
    fun save(points: OlympicPoints) {
        prefs.edit().putInt("olympic_gold", points.gold).putInt("olympic_silver", points.silver)
            .putInt("olympic_bronze", points.bronze).putInt("olympic_iron", points.iron).apply()
    }
}

private fun createValidationRound(): RoundData {
    val names = listOf("山田 太郎", "佐藤 花子", "鈴木 一郎", "高橋 美咲")
    val pars = listOf(4, 5, 3, 4, 4, 3, 5, 4, 4, 4, 3, 5, 4, 4, 3, 5, 4, 4)
    val yards = pars.map { par -> when (par) { 3 -> 150; 5 -> 490; else -> 360 } }
    val longDriveHoles = listOf(2, 7, 11, 16)
    val nearPinHoles = listOf(3, 6, 12, 15)
    fun assignedWinners(holes: List<Int>) = List(18) { index ->
        if (index + 1 in holes) names[index % names.size] else ""
    }
    val players = names.mapIndexed { playerIndex, name ->
        val strokes = pars.mapIndexed { holeIndex, par ->
            (par + ((holeIndex + playerIndex * 2) % 5) - 1).coerceAtLeast(2)
        }
        val putts = List(18) { holeIndex -> 1 + ((holeIndex + playerIndex) % 3) }
        PlayerScore(
            name = name,
            ladiesTee = playerIndex == 1 || playerIndex == 3,
            strokes = strokes,
            putts = putts,
            obs = List(18) { if ((it + playerIndex) % 7 == 0) 1 else 0 },
            bunkers = List(18) { if ((it + playerIndex * 2) % 6 == 0) 1 else 0 },
            penalties = List(18) { if ((it + playerIndex * 3) % 11 == 0) 1 else 0 }
        )
    }
    return RoundData(
        date = LocalDate.now().toString(),
        course = "検証コース",
        region = "東京都テスト市1-2-3",
        weather = "晴れ",
        wind = "弱風",
        frontCourse = "OUT",
        backCourse = "IN",
        courseOptions = listOf("OUT", "IN"),
        pars = pars,
        yards = yards,
        useLongDrive = true,
        useNearPin = true,
        useOlympic = true,
        useTateYoko = true,
        tateYokoPositions = listOf(2, 0, 3, 1),
        tateYokoScoring = "合計スコア",
        tateYokoWinPoints = 2,
        tateYokoCarryDraw = true,
        tateYokoRotate = true,
        longDriveHoles = longDriveHoles,
        nearPinHoles = nearPinHoles,
        longDriveWinners = assignedWinners(longDriveHoles),
        nearPinWinners = assignedWinners(nearPinHoles),
        olympicGold = List(18) { names[it % 4] },
        olympicSilver = List(18) { names[(it + 1) % 4] },
        olympicBronze = List(18) { names[(it + 2) % 4] },
        olympicIron = List(18) { names[(it + 3) % 4] },
        players = players
    )
}

private val GolfGreen = Color(0xFF176B3A)
private val PaleGreen = Color(0xFFE8F3EB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = GolfGreen, secondary = Color(0xFF4E6E58), surface = Color(0xFFF7FBF6))) {
                GolfScoreApp()
            }
        }
    }
}

@Composable
fun GolfScoreApp() {
    val context = LocalContext.current
    val store = remember { RoundStore(context) }
    val rakutenSettings = remember { RakutenSettings(context) }
    val playerDirectory = remember { PlayerDirectory(context) }
    val gameSettings = remember { GameSettings(context) }
    var rounds by remember { mutableStateOf(store.load().sortedByDescending { it.date }) }
    var editing by remember { mutableStateOf<RoundData?>(null) }
    var viewing by remember { mutableStateOf<RoundData?>(null) }
    var deleteTarget by remember { mutableStateOf<RoundData?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showPlayerSettings by remember { mutableStateOf(false) }
    var showGameSettings by remember { mutableStateOf(false) }
    var gameSettingsRevision by remember { mutableIntStateOf(0) }
    val olympicPoints = remember(gameSettingsRevision) { gameSettings.olympicPoints }
    var page by rememberSaveable { mutableStateOf("home") }

    if (editing != null) {
        EditRoundScreen(
            initial = editing!!,
            olympicPoints = olympicPoints,
            onCancel = { editing = null },
            onAutoSave = { draft ->
                rounds = (rounds.filterNot { it.id == draft.id } + draft).sortedByDescending { it.date }
                store.save(rounds)
            },
            onSave = { saved ->
                rounds = (rounds.filterNot { it.id == saved.id } + saved).sortedByDescending { it.date }
                store.save(rounds)
                editing = null
            }
        )
    } else if (viewing != null) {
        ScorecardScreen(
            round = viewing!!,
            olympicPoints = olympicPoints,
            onBack = { viewing = null; page = "history" },
            onEdit = { editing = viewing; viewing = null }
        )
    } else {
        when (page) {
            "history" -> HistoryScreen(
                rounds = rounds,
                onBack = { page = "home" },
                onNew = { page = "options" },
                onOpen = { viewing = it },
                onDelete = { deleteTarget = it },
                onSettings = { showSettings = true },
                onPlayerSettings = { showPlayerSettings = true },
                onGameSettings = { showGameSettings = true }
            )
            "options" -> RoundOptionsScreen(
                onBack = { page = "home" },
                onStart = { options ->
                    editing = options.copy(players = playerDirectory.newRoundPlayers())
                    page = "history"
                }
            )
            else -> HomeScreen(
                onScoreInput = { page = "options" },
                onHistory = { page = "history" },
                onCreateValidationRound = {
                    val validationRound = createValidationRound()
                    rounds = (rounds + validationRound).sortedByDescending { it.date }
                    store.save(rounds)
                    page = "history"
                },
                onRakutenSettings = { showSettings = true },
                onPlayerSettings = { showPlayerSettings = true },
                onGameSettings = { showGameSettings = true }
            )
        }
    }

    if (showSettings) RakutenSettingsDialog(
        settings = rakutenSettings,
        onDismiss = { showSettings = false },
        onSaved = { showSettings = false }
    )

    if (showPlayerSettings) PlayerSettingsDialog(
        directory = playerDirectory,
        onDismiss = { showPlayerSettings = false },
        onSaved = { showPlayerSettings = false }
    )

    if (showGameSettings) GameSettingsDialog(
        settings = gameSettings,
        onDismiss = { showGameSettings = false },
        onSaved = { gameSettingsRevision++; showGameSettings = false }
    )

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("ラウンドを削除") },
            text = { Text("${target.date} ${target.course.ifBlank { "名称未入力" }} を削除しますか？") },
            confirmButton = { TextButton(onClick = {
                rounds = rounds.filterNot { it.id == target.id }; store.save(rounds); deleteTarget = null
            }) { Text("削除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("キャンセル") } }
        )
    }
}

@Composable
fun HomeScreen(
    onScoreInput: () -> Unit,
    onHistory: () -> Unit,
    onCreateValidationRound: () -> Unit,
    onRakutenSettings: () -> Unit,
    onPlayerSettings: () -> Unit,
    onGameSettings: () -> Unit
) {
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "不明" }
    var menuExpanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.home_golf_course),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f)))
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box {
                    FilledTonalButton(onClick = { menuExpanded = true }) { Text("メニュー") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("プレーヤー設定") }, onClick = { menuExpanded = false; onPlayerSettings() })
                        DropdownMenuItem(text = { Text("ゲーム設定") }, onClick = { menuExpanded = false; onGameSettings() })
                        DropdownMenuItem(text = { Text("楽天GORA設定") }, onClick = { menuExpanded = false; onRakutenSettings() })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("アプリバージョン $version") }, onClick = {}, enabled = false)
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
            Surface(color = Color.Black.copy(alpha = 0.32f), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "GOLF SCORE",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { onCreateValidationRound() })
                        }
                    )
                    Text("ゴルフスコア管理", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("楽しく、スマートにスコア管理", color = Color.White, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onScoreInput, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                Text("スコア入力", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onHistory, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text("履歴", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoundOptionsScreen(onBack: () -> Unit, onStart: (RoundData) -> Unit) {
    var useLongDrive by rememberSaveable { mutableStateOf(false) }
    var useNearPin by rememberSaveable { mutableStateOf(false) }
    var useOlympic by rememberSaveable { mutableStateOf(false) }
    var useTateYoko by rememberSaveable { mutableStateOf(false) }
    var tateYokoPositions by remember { mutableStateOf(listOf(0, 1, 2, 3)) }
    var tateYokoScoring by rememberSaveable { mutableStateOf("合計スコア") }
    var tateYokoWinPoints by rememberSaveable { mutableIntStateOf(1) }
    var tateYokoCarryDraw by rememberSaveable { mutableStateOf(false) }
    var tateYokoRotate by rememberSaveable { mutableStateOf(false) }
    var showTateYokoSettings by remember { mutableStateOf(false) }
    var longDriveHoles by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var nearPinHoles by remember { mutableStateOf<Set<Int>>(emptySet()) }
    BackHandler(onBack = onBack)
    val valid = (!useLongDrive || longDriveHoles.isNotEmpty()) && (!useNearPin || nearPinHoles.isNotEmpty())
    Scaffold(
        topBar = { TopAppBar(title = { Text("ラウンドオプション") }, navigationIcon = { TextButton(onClick = onBack) { Text("戻る") } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("使用するルールを選択してください。", color = Color.Gray)
            OptionCheckbox("ドラコン", useLongDrive) { useLongDrive = it }
            if (useLongDrive) HoleSelection("ドラコンホール", longDriveHoles) { longDriveHoles = it }
            OptionCheckbox("ニアピン", useNearPin) { useNearPin = it }
            if (useNearPin) HoleSelection("ニアピンホール", nearPinHoles) { nearPinHoles = it }
            OptionCheckbox("オリンピック", useOlympic) { useOlympic = it }
            OptionCheckbox("たてよこ", useTateYoko) {
                useTateYoko = it
                if (it) showTateYokoSettings = true
            }
            if (useTateYoko) OutlinedButton(onClick = { showTateYokoSettings = true }, modifier = Modifier.fillMaxWidth()) {
                Text("たてよこの設定を変更")
            }
            if (!valid) Text("ドラコン・ニアピンを使用する場合は、対象ホールを選択してください。", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onStart(RoundData(
                        useLongDrive = useLongDrive, useNearPin = useNearPin,
                        useOlympic = useOlympic, useTateYoko = useTateYoko,
                        tateYokoPositions = tateYokoPositions, tateYokoScoring = tateYokoScoring,
                        tateYokoWinPoints = tateYokoWinPoints, tateYokoCarryDraw = tateYokoCarryDraw,
                        tateYokoRotate = tateYokoRotate,
                        longDriveHoles = longDriveHoles.sorted(), nearPinHoles = nearPinHoles.sorted()
                    ))
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("ラウンド入力を開始") }
        }
    }
    if (showTateYokoSettings) TateYokoSettingsDialog(
        playerLabels = listOf("プレーヤー1", "プレーヤー2", "プレーヤー3", "プレーヤー4"),
        positions = tateYokoPositions, scoring = tateYokoScoring, winPoints = tateYokoWinPoints,
        carryDraw = tateYokoCarryDraw, rotate = tateYokoRotate,
        onDismiss = { showTateYokoSettings = false },
        onSave = { p, s, w, c, r ->
            tateYokoPositions = p; tateYokoScoring = s; tateYokoWinPoints = w
            tateYokoCarryDraw = c; tateYokoRotate = r; showTateYokoSettings = false
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoundOptionsEditor(
    useLongDrive: Boolean, onUseLongDrive: (Boolean) -> Unit,
    longDriveHoles: Set<Int>, onLongDriveHoles: (Set<Int>) -> Unit,
    useNearPin: Boolean, onUseNearPin: (Boolean) -> Unit,
    nearPinHoles: Set<Int>, onNearPinHoles: (Set<Int>) -> Unit,
    useOlympic: Boolean, onUseOlympic: (Boolean) -> Unit,
    useTateYoko: Boolean, onUseTateYoko: (Boolean) -> Unit,
    playerLabels: List<String>,
    longDriveParticipants: Set<Int>, onLongDriveParticipants: (Set<Int>) -> Unit,
    nearPinParticipants: Set<Int>, onNearPinParticipants: (Set<Int>) -> Unit,
    olympicParticipants: Set<Int>, onOlympicParticipants: (Set<Int>) -> Unit,
    tateYokoParticipants: Set<Int>, onTateYokoParticipants: (Set<Int>) -> Unit,
    tateYokoPositions: List<Int>, onTateYokoPositions: (List<Int>) -> Unit,
    tateYokoScoring: String, onTateYokoScoring: (String) -> Unit,
    tateYokoWinPoints: Int, onTateYokoWinPoints: (Int) -> Unit,
    tateYokoCarryDraw: Boolean, onTateYokoCarryDraw: (Boolean) -> Unit,
    tateYokoRotate: Boolean, onTateYokoRotate: (Boolean) -> Unit
) {
    var showTateYokoSettings by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("ラウンド途中でも変更できます。変更後は「保存」を押すか、ホール移動時の自動保存を利用してください。", fontSize = 12.sp, color = Color.Gray)
        OptionCheckbox("ドラコン", useLongDrive, onUseLongDrive)
        if (useLongDrive) {
            ParticipantSelection("ドラコン参加者", playerLabels, longDriveParticipants, 1, onLongDriveParticipants)
            HoleSelection("ドラコンホール", longDriveHoles, onLongDriveHoles)
        }
        OptionCheckbox("ニアピン", useNearPin, onUseNearPin)
        if (useNearPin) {
            ParticipantSelection("ニアピン参加者", playerLabels, nearPinParticipants, 1, onNearPinParticipants)
            HoleSelection("ニアピンホール", nearPinHoles, onNearPinHoles)
        }
        OptionCheckbox("オリンピック", useOlympic, onUseOlympic)
        if (useOlympic) ParticipantSelection("オリンピック参加者", playerLabels, olympicParticipants, 2, onOlympicParticipants)
        OptionCheckbox("たてよこ", useTateYoko) {
            onUseTateYoko(it)
            if (it) showTateYokoSettings = true
        }
        if (useTateYoko) {
            ParticipantSelection("たてよこ参加者", playerLabels, tateYokoParticipants, 4, onTateYokoParticipants)
            Text("たてよこは2対2のため4人参加が必要です。", fontSize = 11.sp, color = Color.Gray)
        }
        if (useTateYoko) OutlinedButton(onClick = { showTateYokoSettings = true }, modifier = Modifier.fillMaxWidth()) {
            Text("たてよこの設定を変更")
        }
        if ((useLongDrive && longDriveHoles.isEmpty()) || (useNearPin && nearPinHoles.isEmpty())) {
            Text("ドラコン・ニアピンを使用する場合は、対象ホールを選択してください。", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
    if (showTateYokoSettings) TateYokoSettingsDialog(
        playerLabels = playerLabels, positions = tateYokoPositions, scoring = tateYokoScoring,
        winPoints = tateYokoWinPoints, carryDraw = tateYokoCarryDraw, rotate = tateYokoRotate,
        onDismiss = { showTateYokoSettings = false },
        onSave = { p, s, w, c, r ->
            onTateYokoPositions(p); onTateYokoScoring(s); onTateYokoWinPoints(w)
            onTateYokoCarryDraw(c); onTateYokoRotate(r); showTateYokoSettings = false
        }
    )
}

@Composable
private fun ParticipantSelection(label: String, players: List<String>, selected: Set<Int>, minimum: Int, onSelected: (Set<Int>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        players.forEachIndexed { index, name ->
            val checked = index in selected
            Row(
                Modifier.fillMaxWidth().clickable {
                    val next = if (checked) selected - index else selected + index
                    if (next.size >= minimum) onSelected(next)
                }.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked, onCheckedChange = {
                    val next = if (checked) selected - index else selected + index
                    if (next.size >= minimum) onSelected(next)
                })
                Text(name.ifBlank { "プレーヤー${index + 1}" })
            }
        }
        if (selected.size < minimum) Text("$minimum 人以上を選択してください。", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TateYokoSettingsDialog(
    playerLabels: List<String>,
    positions: List<Int>,
    scoring: String,
    winPoints: Int,
    carryDraw: Boolean,
    rotate: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<Int>, String, Int, Boolean, Boolean) -> Unit
) {
    val labels = List(4) { index -> playerLabels.getOrNull(index).orEmpty().ifBlank { "プレーヤー${index + 1}" } }
    var workingPositions by remember { mutableStateOf(positions.takeIf { it.sorted() == listOf(0, 1, 2, 3) } ?: listOf(0, 1, 2, 3)) }
    var workingScoring by remember { mutableStateOf(scoring) }
    var workingPoints by remember { mutableIntStateOf(winPoints.coerceIn(1, 99)) }
    var workingCarry by remember { mutableStateOf(carryDraw) }
    var workingRotate by remember { mutableStateOf(rotate) }

    fun movePlayer(positionIndex: Int, playerIndex: Int) {
        val otherPosition = workingPositions.indexOf(playerIndex)
        val updated = workingPositions.toMutableList()
        val oldPlayer = updated[positionIndex]
        updated[positionIndex] = playerIndex
        if (otherPosition >= 0) updated[otherPosition] = oldPlayer
        workingPositions = updated
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("たてよこ設定") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 570.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("4人を四角に配置します。縦は左対右、横は上対下で、各ホールのスコアを比較します。", fontSize = 12.sp, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TateYokoPositionDropdown("左上", workingPositions[0], labels, Modifier.weight(1f)) { movePlayer(0, it) }
                    TateYokoPositionDropdown("右上", workingPositions[1], labels, Modifier.weight(1f)) { movePlayer(1, it) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TateYokoPositionDropdown("左下", workingPositions[2], labels, Modifier.weight(1f)) { movePlayer(2, it) }
                    TateYokoPositionDropdown("右下", workingPositions[3], labels, Modifier.weight(1f)) { movePlayer(3, it) }
                }
                Text("縦：左上＋左下 対 右上＋右下", fontSize = 12.sp)
                Text("横：左上＋右上 対 左下＋右下", fontSize = 12.sp)
                HorizontalDivider()
                ChoiceRow("判定方法", listOf("合計スコア", "ベストスコア"), workingScoring) { workingScoring = it }
                Text(if (workingScoring == "合計スコア") "2人の合計が少ないチームの勝ち" else "チーム内の良い方のスコアが少ないチームの勝ち", fontSize = 11.sp, color = Color.Gray)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("1勝の点数", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    FilledTonalButton(onClick = { workingPoints = (workingPoints - 1).coerceAtLeast(1) }) { Text("−") }
                    Text("${workingPoints}点", Modifier.width(54.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
                    FilledTonalButton(onClick = { workingPoints = (workingPoints + 1).coerceAtMost(99) }) { Text("＋") }
                }
                OptionCheckbox("同点の勝ち点を次ホールへ持ち越す", workingCarry) { workingCarry = it }
                OptionCheckbox("ホールごとに配置を時計回りに交代", workingRotate) { workingRotate = it }
                Text("勝った縦チーム・横チームの各メンバーに設定点を加算します。", fontSize = 11.sp, color = Color.Gray)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(workingPositions, workingScoring, workingPoints, workingCarry, workingRotate) }) { Text("設定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TateYokoPositionDropdown(label: String, selectedIndex: Int, players: List<String>, modifier: Modifier, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = players.getOrElse(selectedIndex) { "プレーヤー${selectedIndex + 1}" }, onValueChange = {}, readOnly = true,
            label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            players.forEachIndexed { index, name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(index); expanded = false })
            }
        }
    }
}

@Composable
private fun OptionCheckbox(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable { onChecked(!checked) }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onChecked)
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HoleSelection(label: String, selected: Set<Int>, onSelected: (Set<Int>) -> Unit) {
    Column(Modifier.padding(start = 8.dp, bottom = 6.dp)) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..18).forEach { hole ->
                FilterChip(
                    selected = hole in selected,
                    onClick = { onSelected(if (hole in selected) selected - hole else selected + hole) },
                    label = { Text(hole.toString()) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(rounds: List<RoundData>, onBack: () -> Unit, onNew: () -> Unit, onOpen: (RoundData) -> Unit, onDelete: (RoundData) -> Unit, onSettings: () -> Unit, onPlayerSettings: () -> Unit, onGameSettings: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appVersion = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "不明"
    }
    Scaffold(
        topBar = { TopAppBar(
            title = { Column { Text("GOLF SCORE", fontWeight = FontWeight.Bold); Text("ラウンド履歴", fontSize = 12.sp) } },
            navigationIcon = { TextButton(onClick = onBack) { Text("戻る") } },
            actions = {
                Box {
                    TextButton(onClick = { menuExpanded = true }) { Text("メニュー") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("楽天GORA設定") },
                            onClick = { menuExpanded = false; onSettings() }
                        )
                        DropdownMenuItem(
                            text = { Text("プレーヤー設定") },
                            onClick = { menuExpanded = false; onPlayerSettings() }
                        )
                        DropdownMenuItem(
                            text = { Text("ゲーム設定") },
                            onClick = { menuExpanded = false; onGameSettings() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("アプリバージョン $appVersion") },
                            onClick = {},
                            enabled = false
                        )
                    }
                }
            }
        ) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onNew, text = { Text("新しいラウンド") }, icon = { Text("＋", fontSize = 22.sp) }) }
    ) { padding ->
        if (rounds.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("⛳", fontSize = 56.sp); Spacer(Modifier.height(16.dp)); Text("ラウンド履歴はまだありません", fontWeight = FontWeight.Bold)
                Text("「新しいラウンド」からスコアを記録しましょう", color = Color.Gray)
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rounds, key = { it.id }) { round ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { onOpen(round) }) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatDate(round.date), fontSize = 13.sp, color = GolfGreen)
                            Text(round.course.ifBlank { "ゴルフ場名なし" }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Text("›", fontSize = 28.sp, color = Color.Gray)
                        TextButton(onClick = { onDelete(round) }) { Text("削除", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(round: RoundData, olympicPoints: OlympicPoints, onBack: () -> Unit, onEdit: () -> Unit) {
    BackHandler(onBack = onBack)
    val activePlayers = round.players.filter { it.name.isNotBlank() }
    val olympicPlayers = round.players.withIndex().filter { it.index in round.olympicParticipants && it.value.name.isNotBlank() }.map { it.value }
    val olympicScorePoints = olympicPlayers.associate { player ->
        player.name to olympicTotal(player.name, olympicPoints, round.olympicGold, round.olympicSilver, round.olympicBronze, round.olympicIron)
    }
    val tateYokoScorePoints = calculateTateYokoPoints(round)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("スコアカード") },
                navigationIcon = { TextButton(onClick = onBack) { Text("戻る") } },
                actions = { TextButton(onClick = onEdit) { Text("編集") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(formatDate(round.date), fontSize = 14.sp)
            Text(round.course.ifBlank { "ゴルフ場名なし" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("前半 ${round.frontCourse.ifBlank { "未設定" }}　後半 ${round.backCourse.ifBlank { "未設定" }}", fontSize = 12.sp, color = Color.Gray)
            Text("ティー：${round.teeGround}", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(6.dp))

            if (activePlayers.isEmpty()) {
                Text("名前が入力されたプレーヤーがいません。", color = Color.Gray)
            } else {
                val horizontalState = rememberScrollState()
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.width(116.dp)) {
                        ScorecardFixedCell("ホール", 36.dp, header = true)
                        ScorecardFixedCell("Par", 34.dp)
                        activePlayers.forEachIndexed { index, player ->
                            val playerColor = playerBackground(index)
                            ScorecardFixedCell(player.name, 40.dp, background = playerColor, bold = true)
                            ScorecardFixedCell("パット", 32.dp, background = playerColor)
                        }
                    }
                    Column(Modifier.weight(1f).horizontalScroll(horizontalState)) {
                        Row {
                            (1..18).forEach { holeNumber ->
                                ScorecardHoleHeaderCell(
                                    holeNumber = holeNumber,
                                    isLongDrive = holeNumber in round.longDriveHoles,
                                    isNearPin = holeNumber in round.nearPinHoles
                                )
                            }
                            ScorecardValueCell("計", 36.dp, header = true, width = 54.dp)
                        }
                        Row {
                            round.pars.forEach { ScorecardValueCell(it.toString(), 34.dp) }
                            ScorecardValueCell(round.pars.sum().toString(), 34.dp, width = 54.dp)
                        }
                        activePlayers.forEachIndexed { playerIndex, player ->
                            val playerColor = playerBackground(playerIndex)
                            Row {
                                player.strokes.forEachIndexed { index, score ->
                                    val isDrWinner = round.longDriveWinners.getOrElse(index) { "" } == player.name
                                    val isNpWinner = round.nearPinWinners.getOrElse(index) { "" } == player.name
                                    val award = when {
                                        isDrWinner && isNpWinner -> "dr/np"
                                        isDrWinner -> "dr"
                                        isNpWinner -> "np"
                                        else -> null
                                    }
                                    ScorecardValueCell(
                                        if (score == 0) "" else score.toString(), 40.dp,
                                        scoreToPar = if (score == 0) null else score - round.pars.getOrElse(index) { 4 },
                                        background = playerColor, award = award
                                    )
                                }
                                ScorecardValueCell(player.totalScore.toString(), 40.dp, width = 54.dp, background = playerColor, bold = true)
                            }
                            Row {
                                player.putts.forEach { putt -> ScorecardValueCell(if (putt == 0) "" else putt.toString(), 32.dp, background = playerColor) }
                                ScorecardValueCell(player.totalPutts.toString(), 32.dp, width = 54.dp, background = playerColor, bold = true)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("スコア枠：○ バーディー以下　□ ボギー　▣ ダブルボギー以上", fontSize = 11.sp, color = Color.Gray)
                if (round.useLongDrive || round.useNearPin) {
                    Spacer(Modifier.height(10.dp))
                    Text("ドラコン・ニアピン結果", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GolfGreen)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("前半" to (1..9), "後半" to (10..18)).forEach { (halfLabel, holes) ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFF4F8F4),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
                            ) {
                                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(halfLabel, fontWeight = FontWeight.Bold, color = GolfGreen)
                                    val targetHoles = holes.filter { it in round.longDriveHoles || it in round.nearPinHoles }
                                    if (targetHoles.isEmpty()) Text("対象なし", fontSize = 11.sp, color = Color.Gray)
                                    targetHoles.forEach { holeNumber ->
                                        if (holeNumber in round.longDriveHoles) {
                                            Text("${holeNumber}H dr　${round.longDriveWinners[holeNumber - 1].ifBlank { "未入力" }}", fontSize = 12.sp)
                                        }
                                        if (holeNumber in round.nearPinHoles) {
                                            Text("${holeNumber}H np　${round.nearPinWinners[holeNumber - 1].ifBlank { "未入力" }}", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("獲得数まとめ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    olympicPlayers.forEachIndexed { index, player ->
                        val drCount = round.longDriveHoles.count { hole -> round.longDriveWinners.getOrElse(hole - 1) { "" } == player.name }
                        val npCount = round.nearPinHoles.count { hole -> round.nearPinWinners.getOrElse(hole - 1) { "" } == player.name }
                        Surface(color = playerBackground(index), shape = RoundedCornerShape(7.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(player.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                Text("ドラコン ${drCount}個　ニアピン ${npCount}個", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (round.useOlympic) {
                    Spacer(Modifier.height(10.dp))
                    Text("オリンピック最終結果", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GolfGreen)
                    activePlayers.forEachIndexed { index, player ->
                        val goldCount = round.olympicGold.count { it == player.name }
                        val silverCount = round.olympicSilver.count { it == player.name }
                        val bronzeCount = round.olympicBronze.count { it == player.name }
                        val ironCount = round.olympicIron.count { it == player.name }
                        Surface(color = playerBackground(index), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(player.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                Text("金$goldCount 銀$silverCount 銅$bronzeCount 鉄$ironCount", fontSize = 12.sp)
                                Spacer(Modifier.width(12.dp))
                                Text("${olympicTotal(player.name, olympicPoints, round.olympicGold, round.olympicSilver, round.olympicBronze, round.olympicIron)}点", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    SettlementSummary("オリンピック清算", olympicScorePoints)
                }
                if (round.useTateYoko) {
                    Spacer(Modifier.height(10.dp))
                    Text("たてよこ最終結果", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GolfGreen)
                    val labels = List(4) { index -> round.players.getOrNull(index)?.name.orEmpty().ifBlank { "プレーヤー${index + 1}" } }
                    val positionNames = round.tateYokoPositions.map { labels.getOrElse(it) { "未設定" } }
                    Text("配置：左上 ${positionNames[0]}／右上 ${positionNames[1]}／左下 ${positionNames[2]}／右下 ${positionNames[3]}", fontSize = 12.sp)
                    Text("${round.tateYokoScoring}・1勝${round.tateYokoWinPoints}点・持越し${if (round.tateYokoCarryDraw) "あり" else "なし"}・交代${if (round.tateYokoRotate) "あり" else "なし"}", fontSize = 12.sp, color = Color.Gray)
                    if (round.players.take(4).count { it.name.isNotBlank() } < 4) {
                        Text("結果計算には名前を登録した4人が必要です。", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    } else {
                        activePlayers.forEachIndexed { index, player ->
                            Surface(color = playerBackground(index), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(player.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text("${tateYokoScorePoints[player.name] ?: 0}点", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        SettlementSummary("たてよこ清算", tateYokoScorePoints)
                    }
                }
                if (round.useOlympic && round.useTateYoko && tateYokoScorePoints.isNotEmpty()) {
                    val combinedPlayers = (olympicScorePoints.keys + tateYokoScorePoints.keys).distinct()
                    val combined = combinedPlayers.associate { name ->
                        name to ((olympicScorePoints[name] ?: 0) + (tateYokoScorePoints[name] ?: 0))
                    }
                    Spacer(Modifier.height(10.dp))
                    SettlementSummary("オリンピック＋たてよこ 合算清算", combined, emphasized = true)
                }
            }
        }
    }
}

private data class SettlementTransfer(val payer: String, val receiver: String, val points: Int)

private fun settlementNet(rawPoints: Map<String, Int>): Map<String, Int> {
    if (rawPoints.isEmpty()) return emptyMap()
    val total = rawPoints.values.sum()
    val playerCount = rawPoints.size
    return rawPoints.mapValues { (_, score) -> score * playerCount - total }
}

private fun settlementTransfers(net: Map<String, Int>): List<SettlementTransfer> {
    val debtors = net.filterValues { it < 0 }.map { it.key to -it.value }.toMutableList()
    val creditors = net.filterValues { it > 0 }.map { it.key to it.value }.toMutableList()
    val result = mutableListOf<SettlementTransfer>()
    var debtorIndex = 0
    var creditorIndex = 0
    while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
        val payment = minOf(debtors[debtorIndex].second, creditors[creditorIndex].second)
        if (payment > 0) result += SettlementTransfer(debtors[debtorIndex].first, creditors[creditorIndex].first, payment)
        debtors[debtorIndex] = debtors[debtorIndex].copy(second = debtors[debtorIndex].second - payment)
        creditors[creditorIndex] = creditors[creditorIndex].copy(second = creditors[creditorIndex].second - payment)
        if (debtors[debtorIndex].second == 0) debtorIndex++
        if (creditors[creditorIndex].second == 0) creditorIndex++
    }
    return result
}

@Composable
private fun SettlementSummary(title: String, rawPoints: Map<String, Int>, emphasized: Boolean = false) {
    val net = settlementNet(rawPoints)
    val transfers = settlementTransfers(net)
    Surface(
        color = if (emphasized) Color(0xFFE3F2FD) else Color(0xFFF4F4F4),
        shape = RoundedCornerShape(8.dp),
        border = if (emphasized) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2)) else null,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (emphasized) Color(0xFF1565C0) else GolfGreen)
            Text("各プレーヤーが全員と点差を清算した結果", fontSize = 11.sp, color = Color.Gray)
            net.forEach { (name, value) ->
                val sign = if (value > 0) "+" else ""
                Text("$name　$sign$value 点", fontWeight = if (value != 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = when { value > 0 -> Color(0xFF1565C0); value < 0 -> Color(0xFFC62828); else -> Color.Gray })
            }
            HorizontalDivider(Modifier.padding(vertical = 3.dp))
            Text("受け渡し", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (transfers.isEmpty()) Text("清算なし", fontSize = 12.sp, color = Color.Gray)
            else transfers.forEach { transfer ->
                Text("${transfer.payer} → ${transfer.receiver}　${transfer.points}点", fontSize = 13.sp)
            }
        }
    }
}

private fun calculateTateYokoPoints(round: RoundData): Map<String, Int> {
    val participants = round.tateYokoParticipants.filter { it in round.players.indices }.distinct()
    if (participants.size != 4 || participants.any { round.players[it].name.isBlank() }) return emptyMap()
    val points = participants.associate { round.players[it].name to 0 }.toMutableMap()
    val basePositions = round.tateYokoPositions.takeIf { it.sorted() == participants.sorted() } ?: participants
    var verticalStake = round.tateYokoWinPoints
    var horizontalStake = round.tateYokoWinPoints

    fun teamValue(indices: List<Int>, hole: Int): Int {
        val scores = indices.map { round.players[it].strokes.getOrElse(hole) { 0 } }
        if (scores.any { it <= 0 }) return Int.MAX_VALUE
        return if (round.tateYokoScoring == "ベストスコア") scores.minOrNull() ?: Int.MAX_VALUE else scores.sum()
    }
    fun award(teamA: List<Int>, teamB: List<Int>, hole: Int, stake: Int): Int {
        val a = teamValue(teamA, hole)
        val b = teamValue(teamB, hole)
        if (a == Int.MAX_VALUE || b == Int.MAX_VALUE) return stake
        if (a == b) return if (round.tateYokoCarryDraw) stake + round.tateYokoWinPoints else round.tateYokoWinPoints
        val winners = if (a < b) teamA else teamB
        winners.forEach { playerIndex ->
            val name = round.players[playerIndex].name
            points[name] = (points[name] ?: 0) + stake
        }
        return round.tateYokoWinPoints
    }

    repeat(18) { hole ->
        val shift = if (round.tateYokoRotate) hole % 4 else 0
        val p = List(4) { index -> basePositions[(index - shift + 4) % 4] }
        verticalStake = award(listOf(p[0], p[2]), listOf(p[1], p[3]), hole, verticalStake)
        horizontalStake = award(listOf(p[0], p[1]), listOf(p[2], p[3]), hole, horizontalStake)
    }
    return points
}

@Composable
private fun ScorecardFixedCell(text: String, height: androidx.compose.ui.unit.Dp, header: Boolean = false, background: Color = Color.White, bold: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(height),
        color = if (header) GolfGreen else background,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = if (header) Color.White else Color.DarkGray, fontSize = 12.sp, fontWeight = if (header || bold) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
        }
    }
}

@Composable
private fun ScorecardHoleHeaderCell(holeNumber: Int, isLongDrive: Boolean, isNearPin: Boolean) {
    Surface(modifier = Modifier.width(46.dp).height(36.dp), color = GolfGreen, border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.5f))) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(holeNumber.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (isLongDrive || isNearPin) {
                Column(Modifier.padding(start = 1.dp), verticalArrangement = Arrangement.Center) {
                    if (isLongDrive) Text("dr", color = Color(0xFFFFB74D), fontSize = 8.sp, fontWeight = FontWeight.Bold, lineHeight = 8.sp)
                    if (isNearPin) Text("np", color = Color(0xFF81D4FA), fontSize = 8.sp, fontWeight = FontWeight.Bold, lineHeight = 8.sp)
                }
            }
        }
    }
}

@Composable
private fun ScorecardValueCell(
    text: String,
    height: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp = 46.dp,
    header: Boolean = false,
    scoreToPar: Int? = null,
    background: Color = Color.White,
    bold: Boolean = false,
    award: String? = null
) {
    val borderWidth = when {
        scoreToPar == null || scoreToPar == 0 -> 0.5.dp
        kotlin.math.abs(scoreToPar) >= 2 -> 2.dp
        else -> 1.5.dp
    }
    val borderColor = when {
        scoreToPar != null && scoreToPar < 0 -> Color(0xFF7B3FA0)
        scoreToPar != null && scoreToPar > 0 -> Color.DarkGray
        else -> Color.LightGray
    }
    val awardBackground = when (award) {
        "dr" -> Color(0xFFE65100)
        "np" -> Color(0xFF1565C0)
        "dr/np" -> Color(0xFF6A1B9A)
        else -> null
    }
    Surface(
        modifier = Modifier.width(width).height(height).padding(if (scoreToPar == null || scoreToPar == 0) 0.dp else 2.dp),
        color = when {
            header -> GolfGreen
            awardBackground != null -> awardBackground
            else -> background
        },
        border = androidx.compose.foundation.BorderStroke(borderWidth, if (header) GolfGreen else borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (award == null) {
                Text(text, color = if (header) Color.White else Color.DarkGray, fontSize = 13.sp, fontWeight = if (header || bold) FontWeight.Bold else FontWeight.Normal)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                    Text(award, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, lineHeight = 7.sp)
                }
            }
        }
    }
}

private fun playerBackground(index: Int): Color = listOf(
    Color(0xFFE3F2FD), Color(0xFFE8F5E9), Color(0xFFFFF3E0), Color(0xFFFCE4EC)
)[index % 4]

@Composable
private fun PlayerSettingsDialog(directory: PlayerDirectory, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var selfName by remember { mutableStateOf(directory.selfName) }
    var friends by remember { mutableStateOf(directory.friends) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("プレーヤー設定") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("本人は新しいラウンドの1人目に自動設定されます。", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(selfName, { selfName = it }, label = { Text("本人の名前") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                HorizontalDivider()
                Text("仲間", fontWeight = FontWeight.Bold)
                friends.forEachIndexed { index, name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(name, { value -> friends = friends.update(index, value) }, label = { Text("仲間${index + 1}") }, singleLine = true, modifier = Modifier.weight(1f))
                        TextButton(onClick = { friends = friends.filterIndexed { i, _ -> i != index } }) { Text("削除") }
                    }
                }
                if (friends.size < 20) OutlinedButton(onClick = { friends = friends + "" }, modifier = Modifier.fillMaxWidth()) { Text("＋ 仲間を追加") }
            }
        },
        confirmButton = {
            Button(onClick = { directory.save(selfName, friends); onSaved() }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}

@Composable
private fun GameSettingsDialog(settings: GameSettings, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val current = remember { settings.olympicPoints }
    var gold by remember { mutableIntStateOf(current.gold) }
    var silver by remember { mutableIntStateOf(current.silver) }
    var bronze by remember { mutableIntStateOf(current.bronze) }
    var iron by remember { mutableIntStateOf(current.iron) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ゲーム設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("オリンピックの配点", fontWeight = FontWeight.Bold)
                Text("初期値は金5点・銀3点・銅2点・鉄1点です。", fontSize = 12.sp, color = Color.Gray)
                OlympicPointField("金", gold) { gold = it }
                OlympicPointField("銀", silver) { silver = it }
                OlympicPointField("銅", bronze) { bronze = it }
                OlympicPointField("鉄", iron) { iron = it }
            }
        },
        confirmButton = {
            Button(onClick = { settings.save(OlympicPoints(gold, silver, bronze, iron)); onSaved() }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}

@Composable
private fun OlympicPointField(label: String, value: Int, onValue: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text -> if (text.length <= 2 && text.all(Char::isDigit)) onValue((text.toIntOrNull() ?: 0).coerceIn(0, 99)) },
        label = { Text("${label}の点数") }, suffix = { Text("点") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RakutenSettingsDialog(settings: RakutenSettings, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val appVersion = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "不明"
    }
    var applicationId by remember { mutableStateOf(settings.applicationId) }
    var accessKey by remember { mutableStateOf(settings.accessKey) }
    var validation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("楽天GORA設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("楽天ウェブサービスで取得した認証情報を入力します。端末内だけに保存されます。", fontSize = 13.sp, color = Color.Gray)
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://webservice.rakuten.co.jp/guide")))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("楽天公式の取得手順を開く") }
                Text("公式ガイドの「registering a new application」または「New App」を押し、楽天にログインしてください。発行されたApplication IDとAccess Keyをこの画面へ入力します。", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(
                    value = applicationId,
                    onValueChange = { applicationId = it; validation = "" },
                    label = { Text("Application ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = accessKey,
                    onValueChange = { accessKey = it; validation = "" },
                    label = { Text("Access Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("アプリバージョン $appVersion", fontSize = 12.sp, color = Color.Gray)
                if (validation.isNotBlank()) Text(validation, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (applicationId.isBlank() || accessKey.isBlank()) validation = "両方の項目を入力してください。"
                else { settings.save(applicationId, accessKey); onSaved() }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoundScreen(initial: RoundData, olympicPoints: OlympicPoints, onCancel: () -> Unit, onAutoSave: (RoundData) -> Unit, onSave: (RoundData) -> Unit) {
    var date by rememberSaveable { mutableStateOf(initial.date) }
    var course by rememberSaveable { mutableStateOf(initial.course) }
    var region by rememberSaveable { mutableStateOf(initial.region) }
    var weather by rememberSaveable { mutableStateOf(initial.weather) }
    var wind by rememberSaveable { mutableStateOf(initial.wind) }
    var frontCourse by rememberSaveable { mutableStateOf(initial.frontCourse) }
    var backCourse by rememberSaveable { mutableStateOf(initial.backCourse) }
    var courseOptions by remember { mutableStateOf(initial.courseOptions) }
    var golfCourseId by rememberSaveable { mutableLongStateOf(initial.golfCourseId) }
    var courseDetailJson by remember { mutableStateOf(initial.courseDetailJson) }
    var courseYardages by remember { mutableStateOf(initial.courseYardages) }
    var greenName by rememberSaveable { mutableStateOf(initial.greenName) }
    var pars by remember { mutableStateOf(initial.pars) }
    var yards by remember { mutableStateOf(initial.yards) }
    var olympicGold by remember { mutableStateOf(initial.olympicGold) }
    var olympicSilver by remember { mutableStateOf(initial.olympicSilver) }
    var olympicBronze by remember { mutableStateOf(initial.olympicBronze) }
    var olympicIron by remember { mutableStateOf(initial.olympicIron) }
    var longDriveWinners by remember { mutableStateOf(initial.longDriveWinners) }
    var nearPinWinners by remember { mutableStateOf(initial.nearPinWinners) }
    var useLongDrive by rememberSaveable { mutableStateOf(initial.useLongDrive) }
    var useNearPin by rememberSaveable { mutableStateOf(initial.useNearPin) }
    var useOlympic by rememberSaveable { mutableStateOf(initial.useOlympic) }
    var useTateYoko by rememberSaveable { mutableStateOf(initial.useTateYoko) }
    var longDriveParticipants by remember { mutableStateOf(initial.longDriveParticipants.toSet()) }
    var nearPinParticipants by remember { mutableStateOf(initial.nearPinParticipants.toSet()) }
    var olympicParticipants by remember { mutableStateOf(initial.olympicParticipants.toSet()) }
    var tateYokoParticipants by remember { mutableStateOf(initial.tateYokoParticipants.toSet()) }
    var tateYokoPositions by remember { mutableStateOf(initial.tateYokoPositions) }
    var tateYokoScoring by rememberSaveable { mutableStateOf(initial.tateYokoScoring) }
    var tateYokoWinPoints by rememberSaveable { mutableIntStateOf(initial.tateYokoWinPoints) }
    var tateYokoCarryDraw by rememberSaveable { mutableStateOf(initial.tateYokoCarryDraw) }
    var tateYokoRotate by rememberSaveable { mutableStateOf(initial.tateYokoRotate) }
    var longDriveHoles by remember { mutableStateOf(initial.longDriveHoles.toSet()) }
    var nearPinHoles by remember { mutableStateOf(initial.nearPinHoles.toSet()) }
    var teeGround by rememberSaveable { mutableStateOf(initial.teeGround) }
    var players by remember { mutableStateOf(initial.players) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var validation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val courseProfileStore = remember { CourseProfileStore(context) }

    fun result() = initial.copy(
        date = date, course = course.trim(), region = region.trim(), weather = weather, wind = wind,
        frontCourse = frontCourse.trim(), backCourse = backCourse.trim(), courseOptions = courseOptions,
        golfCourseId = golfCourseId, courseDetailJson = courseDetailJson, courseYardages = courseYardages,
        greenName = greenName.trim(), pars = pars, yards = yards, teeGround = teeGround, players = players,
        olympicGold = olympicGold, olympicSilver = olympicSilver,
        olympicBronze = olympicBronze, olympicIron = olympicIron,
        longDriveWinners = longDriveWinners, nearPinWinners = nearPinWinners,
        useLongDrive = useLongDrive, useNearPin = useNearPin,
        useOlympic = useOlympic, useTateYoko = useTateYoko,
        longDriveParticipants = longDriveParticipants.sorted(), nearPinParticipants = nearPinParticipants.sorted(),
        olympicParticipants = olympicParticipants.sorted(), tateYokoParticipants = tateYokoParticipants.sorted(),
        tateYokoPositions = tateYokoPositions, tateYokoScoring = tateYokoScoring,
        tateYokoWinPoints = tateYokoWinPoints, tateYokoCarryDraw = tateYokoCarryDraw,
        tateYokoRotate = tateYokoRotate,
        longDriveHoles = longDriveHoles.sorted(), nearPinHoles = nearPinHoles.sorted()
    )
    val latestRound by rememberUpdatedState(result())
    fun applyMatchingYardages() {
        fun normalized(value: String) = value.lowercase().replace(Regex("[\\s　._-]"), "")
        fun matches(profile: CourseYardage, selectedCourse: String) = normalized(profile.course) == normalized(selectedCourse) ||
            normalized(profile.course).contains(normalized(selectedCourse)) || normalized(selectedCourse).contains(normalized(profile.course))
        fun teeMatches(profileTee: String) = normalized(profileTee) == normalized(teeGround) || when (teeGround) {
            "バック" -> normalized(profileTee) in listOf("back", "black", "blue", "バック", "黒", "青")
            "レギュラー" -> normalized(profileTee) in listOf("regular", "white", "レギュラー", "白")
            "フロント" -> normalized(profileTee).contains("front") || normalized(profileTee) in listOf("gold", "フロント", "金")
            else -> false
        }
        fun find(courseName: String) = courseYardages.firstOrNull { profile ->
            matches(profile, courseName) && normalized(profile.green) == normalized(greenName) && teeMatches(profile.tee)
        }
        val front = find(frontCourse)
        val back = find(backCourse)
        if (front != null && back != null) {
            pars = front.pars + back.pars; yards = front.yards + back.yards
        } else courseProfileStore.load(result())?.let { (savedPars, savedYards) -> pars = savedPars; yards = savedYards }
    }
    LaunchedEffect(frontCourse, backCourse, greenName, teeGround, courseYardages) { applyMatchingYardages() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            courseProfileStore.save(latestRound)
            onAutoSave(latestRound)
        }
    }
    LaunchedEffect(useLongDrive, useNearPin, useOlympic, useTateYoko, longDriveHoles, nearPinHoles,
        tateYokoPositions, tateYokoScoring, tateYokoWinPoints, tateYokoCarryDraw, tateYokoRotate,
        longDriveParticipants, nearPinParticipants, olympicParticipants, tateYokoParticipants) {
        delay(300)
        courseProfileStore.save(latestRound)
        onAutoSave(latestRound)
    }
    BackHandler { onCancel() }
    Scaffold(
        topBar = { TopAppBar(title = { Text(if (initial.course.isBlank()) "ラウンド入力" else "ラウンド編集") }, navigationIcon = { TextButton(onClick = onCancel) { Text("戻る") } }, actions = { TextButton(onClick = { if (course.isBlank()) validation = true else onSave(result()) }) { Text("保存", fontWeight = FontWeight.Bold) } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("基本情報") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("スコア") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("オプション") })
            }
            if (tab == 0) BasicInfoTab(
                date, { y, m, d -> date = "%04d-%02d-%02d".format(y, m, d) },
                course, { course = it }, region, { region = it }, weather, { weather = it }, wind, { wind = it },
                frontCourse, { frontCourse = it }, backCourse, { backCourse = it }, courseOptions, { courseOptions = it },
                golfCourseId, { golfCourseId = it }, courseDetailJson, { courseDetailJson = it },
                courseYardages, { courseYardages = it }, greenName, { greenName = it },
                teeGround, { teeGround = it }, players, { players = it }
            )
            else if (tab == 1) ScoreTab(
                players = players, frontCourse = frontCourse, backCourse = backCourse,
                pars = pars, onPars = { pars = it }, yards = yards, onYards = { yards = it },
                useLongDrive = useLongDrive, longDriveHoles = longDriveHoles.sorted(),
                longDriveParticipants = longDriveParticipants,
                useNearPin = useNearPin, nearPinHoles = nearPinHoles.sorted(),
                nearPinParticipants = nearPinParticipants,
                longDriveWinners = longDriveWinners, onLongDriveWinners = { longDriveWinners = it },
                nearPinWinners = nearPinWinners, onNearPinWinners = { nearPinWinners = it },
                useOlympic = useOlympic, olympicParticipants = olympicParticipants, olympicPoints = olympicPoints,
                olympicGold = olympicGold, onOlympicGold = { olympicGold = it },
                olympicSilver = olympicSilver, onOlympicSilver = { olympicSilver = it },
                olympicBronze = olympicBronze, onOlympicBronze = { olympicBronze = it },
                olympicIron = olympicIron, onOlympicIron = { olympicIron = it },
                onHoleAutoSave = { onAutoSave(result()) },
                onPlayers = { players = it }
            )
            else RoundOptionsEditor(
                useLongDrive = useLongDrive, onUseLongDrive = { useLongDrive = it },
                longDriveHoles = longDriveHoles, onLongDriveHoles = { longDriveHoles = it },
                useNearPin = useNearPin, onUseNearPin = { useNearPin = it },
                nearPinHoles = nearPinHoles, onNearPinHoles = { nearPinHoles = it },
                useOlympic = useOlympic, onUseOlympic = { useOlympic = it },
                useTateYoko = useTateYoko, onUseTateYoko = { useTateYoko = it },
                playerLabels = List(4) { players.getOrNull(it)?.name.orEmpty().ifBlank { "プレーヤー${it + 1}" } },
                longDriveParticipants = longDriveParticipants, onLongDriveParticipants = { longDriveParticipants = it },
                nearPinParticipants = nearPinParticipants, onNearPinParticipants = { nearPinParticipants = it },
                olympicParticipants = olympicParticipants, onOlympicParticipants = { olympicParticipants = it },
                tateYokoParticipants = tateYokoParticipants, onTateYokoParticipants = { tateYokoParticipants = it },
                tateYokoPositions = tateYokoPositions, onTateYokoPositions = { tateYokoPositions = it },
                tateYokoScoring = tateYokoScoring, onTateYokoScoring = { tateYokoScoring = it },
                tateYokoWinPoints = tateYokoWinPoints, onTateYokoWinPoints = { tateYokoWinPoints = it },
                tateYokoCarryDraw = tateYokoCarryDraw, onTateYokoCarryDraw = { tateYokoCarryDraw = it },
                tateYokoRotate = tateYokoRotate, onTateYokoRotate = { tateYokoRotate = it }
            )
        }
    }
    if (validation) AlertDialog(onDismissRequest = { validation = false }, title = { Text("入力を確認") }, text = { Text("ゴルフ場名を入力してください。") }, confirmButton = { TextButton(onClick = { validation = false }) { Text("OK") } })
}

@Composable
fun BasicInfoTab(
    date: String, onDate: (Int, Int, Int) -> Unit,
    course: String, onCourse: (String) -> Unit, region: String, onRegion: (String) -> Unit,
    weather: String, onWeather: (String) -> Unit, wind: String, onWind: (String) -> Unit,
    frontCourse: String, onFrontCourse: (String) -> Unit,
    backCourse: String, onBackCourse: (String) -> Unit,
    courseOptions: List<String>, onCourseOptions: (List<String>) -> Unit,
    golfCourseId: Long, onGolfCourseId: (Long) -> Unit,
    courseDetailJson: String, onCourseDetailJson: (String) -> Unit,
    courseYardages: List<CourseYardage>, onCourseYardages: (List<CourseYardage>) -> Unit,
    green: String, onGreen: (String) -> Unit,
    tee: String, onTee: (String) -> Unit, players: List<PlayerScore>, onPlayers: (List<PlayerScore>) -> Unit
) {
    val context = LocalContext.current
    val registeredPlayers = remember { PlayerDirectory(context).allPlayers }
    var showCourseSearch by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("ラウンド情報")
        OutlinedButton(onClick = {
            val d = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
            DatePickerDialog(context, { _, y, m, day -> onDate(y, m + 1, day) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
        }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Text("プレー年月日", fontSize = 11.sp, color = Color.Gray)
                Text(formatDate(date), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        OutlinedTextField(course, onCourse, label = { Text("ゴルフ場名（必須）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { showCourseSearch = true }, modifier = Modifier.fillMaxWidth()) {
            Text("楽天GORAからゴルフ場を検索")
        }
        OutlinedTextField(region, onRegion, label = { Text("地域（例：千葉県）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ChoiceRow("天気", listOf("晴れ", "曇り", "雨"), weather, onWeather)
        ChoiceRow("風", listOf("弱風", "強風"), wind, onWind)
        if (courseOptions.isNotEmpty()) {
            ChoiceRow("前半コース", courseOptions, frontCourse, onFrontCourse)
            ChoiceRow("後半コース", courseOptions, backCourse, onBackCourse)
        } else {
            OutlinedTextField(frontCourse, onFrontCourse, label = { Text("前半コース") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(backCourse, onBackCourse, label = { Text("後半コース") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        val greenOptions = (courseYardages.map { it.green } + listOf("右グリーン", "左グリーン", "Aグリーン", "Bグリーン", "ベント", "コーライ")).filter { it.isNotBlank() }.distinct()
        Text("グリーン", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            greenOptions.forEach { name -> FilterChip(selected = green == name, onClick = { onGreen(name) }, label = { Text(name) }) }
        }
        OutlinedTextField(green, onGreen, label = { Text("グリーン名（自由入力可）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        val teeOptions = courseYardages.filter { green.isBlank() || it.green == green }.map { it.tee }.distinct()
            .ifEmpty { listOf("バック", "レギュラー", "フロント") }
        ChoiceRow("ティーグラウンド", teeOptions, tee, onTee)
        if (courseYardages.isNotEmpty()) Text("楽天GORAから${courseYardages.size}件のコース・グリーン・ティー別距離を保持しています。", fontSize = 11.sp, color = Color.Gray)
        HorizontalDivider(); SectionTitle("プレーヤー（最大4人）")
        players.forEachIndexed { i, p ->
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerDropdown(
                        value = p.name,
                        label = "プレーヤー${i + 1}",
                        options = registeredPlayers,
                        modifier = Modifier.weight(1f),
                        onSelected = { name -> onPlayers(players.update(i, p.copy(name = name))) }
                    )
                    if (players.size > 1) TextButton(onClick = { onPlayers(players.filterIndexed { index, _ -> index != i }) }) { Text("削除") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(p.ladiesTee, { checked -> onPlayers(players.update(i, p.copy(ladiesTee = checked))) }); Text("レディースティーを使用") }
            } }
        }
        if (players.size < 4) OutlinedButton(onClick = { onPlayers(players + PlayerScore()) }, modifier = Modifier.fillMaxWidth()) { Text("＋ プレーヤーを追加") }
        Spacer(Modifier.height(24.dp))
    }
    if (showCourseSearch) CourseSearchDialog(
        initialQuery = course,
        onDismiss = { showCourseSearch = false },
        onSelected = { selected ->
            onCourse(selected.name)
            onRegion(selected.address)
            onCourseOptions(selected.courseNames)
            onGolfCourseId(selected.id)
            onCourseDetailJson(selected.detailJson)
            onCourseYardages(selected.yardages)
            onFrontCourse(selected.courseNames.getOrNull(0).orEmpty())
            onBackCourse(selected.courseNames.getOrNull(1).orEmpty())
            selected.yardages.firstOrNull()?.let { first -> onGreen(first.green); onTee(first.tee) }
            showCourseSearch = false
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDropdown(value: String, label: String, options: List<String>, modifier: Modifier = Modifier, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onSelected(it); expanded = true },
            readOnly = false,
            label = { Text(label) },
            placeholder = { Text("登録済みから選択、または直接入力") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("名前をクリア") }, onClick = { onSelected(""); expanded = false })
            options.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(name); expanded = false })
            }
        }
    }
}

@Composable
private fun CourseSearchDialog(initialQuery: String, onDismiss: () -> Unit, onSelected: (GolfCourse) -> Unit) {
    val context = LocalContext.current
    val savedSettings = remember { RakutenSettings(context) }
    val rakutenApplicationId = remember { savedSettings.applicationId.ifBlank { context.getString(R.string.rakuten_application_id) } }
    val rakutenAccessKey = remember { savedSettings.accessKey.ifBlank { context.getString(R.string.rakuten_access_key) } }
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<GolfCourse>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun search() {
        if (query.isBlank()) { message = "ゴルフ場名の一部を入力してください。"; return }
        if (rakutenApplicationId.isBlank() || rakutenAccessKey.isBlank()) {
            message = "Application IDとAccess Keyが未設定です。履歴画面の「メニュー → 楽天GORA設定」から入力してください。"
            return
        }
        loading = true; message = ""
        Thread {
            val outcome = runCatching {
                val keyword = URLEncoder.encode(query.trim(), "UTF-8")
                val endpoint = "https://openapi.rakuten.co.jp/engine/api/Gora/GoraGolfCourseSearch/20170623" +
                    "?format=json&formatVersion=2&hits=30&applicationId=$rakutenApplicationId" +
                    "&keyword=$keyword"
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    requestMethod = "GET"
                    setRequestProperty("accessKey", rakutenAccessKey)
                    setRequestProperty("Referer", "https://busan78.github.io/golf-score/")
                    setRequestProperty("Origin", "https://busan78.github.io")
                    setRequestProperty("User-Agent", "GOLF-SCORE-Android/1.1.1")
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    val errorJson = runCatching { JSONObject(body) }.getOrNull()
                    val detail = errorJson?.optString("error_description")
                        ?.takeIf { it.isNotBlank() }
                        ?: errorJson?.optString("error")?.takeIf { it.isNotBlank() }
                        ?: body.take(200).ifBlank { "No response body" }
                    error("Rakuten GORA API: HTTP $status / $detail")
                }
                val root = JSONObject(body)
                val items = root.optJSONArray("Items") ?: root.optJSONArray("items") ?: JSONArray()
                List(items.length()) { index ->
                    val raw = items.getJSONObject(index)
                    val item = raw.optJSONObject("Item") ?: raw.optJSONObject("item") ?: raw
                    GolfCourse(
                        id = item.optLong("golfCourseId"),
                        name = item.optString("golfCourseName"),
                        address = item.optString("address")
                    )
                }.filter { it.name.isNotBlank() }
            }
            Handler(Looper.getMainLooper()).post {
                loading = false
                results = outcome.getOrDefault(emptyList())
                message = outcome.exceptionOrNull()?.let { error ->
                    val cause = generateSequence(error) { it.cause }.last()
                    val detail = error.message ?: cause.message ?: cause.javaClass.simpleName
                    "検索エラー：$detail"
                }
                    ?: if (results.isEmpty()) "該当するゴルフ場がありません。" else ""
            }
        }.start()
    }

    fun selectCourse(course: GolfCourse) {
        loading = true
        message = "コース情報を取得中…"
        Thread {
            val outcome = runCatching {
                val endpoint = "https://openapi.rakuten.co.jp/engine/api/Gora/GoraGolfCourseDetail/20170623" +
                    "?format=json&formatVersion=2&applicationId=$rakutenApplicationId&golfCourseId=${course.id}"
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    requestMethod = "GET"
                    setRequestProperty("accessKey", rakutenAccessKey)
                    setRequestProperty("Referer", "https://busan78.github.io/golf-score/")
                    setRequestProperty("Origin", "https://busan78.github.io")
                    setRequestProperty("User-Agent", "GOLF-SCORE-Android/1.2.0")
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) error("楽天GORA詳細API: HTTP $status")
                val root = JSONObject(body)
                val item = root.optJSONObject("Item") ?: root.optJSONObject("item") ?: root
                val names = item.optString("courseName")
                    .split(Regex("[,、，/／・]+"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                val yardages = runCatching { fetchGoraCourseYardages(course.id) }.getOrDefault(emptyList())
                course.copy(courseNames = names, detailJson = body, yardages = yardages)
            }
            Handler(Looper.getMainLooper()).post {
                loading = false
                outcome.onSuccess(onSelected).onFailure { error ->
                    message = "検索エラー：${error.message ?: "コース情報を取得できませんでした。"}"
                }
            }
        }.start()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ゴルフ場を検索") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("ゴルフ場名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(onClick = ::search, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                    Text(if (loading) "検索中…" else "楽天GORAを検索")
                }
                if (message.isNotBlank()) Text(message, Modifier.padding(top = 10.dp), color = if (message.startsWith("検索エラー")) MaterialTheme.colorScheme.error else Color.Gray)
                LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    items(results, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.name, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(item.address) },
                            modifier = Modifier.clickable(enabled = !loading) { selectCourse(item) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("閉じる") } }
    )
}

@Composable
fun ScoreTab(
    players: List<PlayerScore>,
    frontCourse: String,
    backCourse: String,
    pars: List<Int>,
    onPars: (List<Int>) -> Unit,
    yards: List<Int>,
    onYards: (List<Int>) -> Unit,
    useLongDrive: Boolean,
    longDriveHoles: List<Int>,
    longDriveParticipants: Set<Int>,
    useNearPin: Boolean,
    nearPinHoles: List<Int>,
    nearPinParticipants: Set<Int>,
    longDriveWinners: List<String>, onLongDriveWinners: (List<String>) -> Unit,
    nearPinWinners: List<String>, onNearPinWinners: (List<String>) -> Unit,
    useOlympic: Boolean,
    olympicParticipants: Set<Int>,
    olympicPoints: OlympicPoints,
    olympicGold: List<String>, onOlympicGold: (List<String>) -> Unit,
    olympicSilver: List<String>, onOlympicSilver: (List<String>) -> Unit,
    olympicBronze: List<String>, onOlympicBronze: (List<String>) -> Unit,
    olympicIron: List<String>, onOlympicIron: (List<String>) -> Unit,
    onHoleAutoSave: () -> Unit,
    onPlayers: (List<PlayerScore>) -> Unit
) {
    val activeEntries = players.withIndex().filter { it.value.name.isNotBlank() }
    if (activeEntries.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("基本情報でプレーヤー名を入力してください。", color = Color.Gray)
        }
        return
    }
    var hole by rememberSaveable { mutableIntStateOf(0) }
    var activePlayer by rememberSaveable { mutableIntStateOf(0) }
    var activeField by rememberSaveable { mutableIntStateOf(0) } // 0: score, 1: putts
    var pendingScorePlayer by remember { mutableIntStateOf(-1) }
    var pendingScoreHole by remember { mutableIntStateOf(-1) }
    var scoreInputRevision by remember { mutableIntStateOf(0) }
    var firstHoleDisplay by remember { mutableStateOf(true) }
    val scoreAreaScrollState = rememberScrollState()
    val par = pars.getOrElse(hole) { 4 }

    fun assignOlympic(rank: String, name: String) {
        if (name.isNotBlank()) {
            if (olympicGold[hole] == name) onOlympicGold(olympicGold.update(hole, ""))
            if (olympicSilver[hole] == name) onOlympicSilver(olympicSilver.update(hole, ""))
            if (olympicBronze[hole] == name) onOlympicBronze(olympicBronze.update(hole, ""))
            if (olympicIron[hole] == name) onOlympicIron(olympicIron.update(hole, ""))
        }
        when (rank) {
            "gold" -> onOlympicGold(olympicGold.update(hole, name))
            "silver" -> onOlympicSilver(olympicSilver.update(hole, name))
            "bronze" -> onOlympicBronze(olympicBronze.update(hole, name))
            "iron" -> onOlympicIron(olympicIron.update(hole, name))
        }
    }

    LaunchedEffect(hole) {
        pendingScorePlayer = -1
        scoreAreaScrollState.scrollTo(0)
        if (firstHoleDisplay) firstHoleDisplay = false
        else {
            delay(100)
            onHoleAutoSave()
        }
    }

    LaunchedEffect(scoreInputRevision) {
        if (scoreInputRevision > 0) {
            delay(300)
            if (activeField == 0 && activePlayer == pendingScorePlayer && hole == pendingScoreHole) {
                pendingScorePlayer = -1
                activeField = 1
            }
        }
    }

    fun enterNumber(number: Int) {
        val displayIndex = activePlayer.coerceIn(0, activeEntries.lastIndex)
        val playerIndex = activeEntries[displayIndex].index
        val player = players[playerIndex]
        if (activeField == 0) {
            val current = player.strokes[hole]
            if (pendingScorePlayer == displayIndex && pendingScoreHole == hole && current in 0..9) {
                onPlayers(players.update(playerIndex, player.copy(strokes = player.strokes.update(hole, current * 10 + number))))
                pendingScorePlayer = -1
                activeField = 1
            } else {
                onPlayers(players.update(playerIndex, player.copy(strokes = player.strokes.update(hole, number))))
                if (number == 0) {
                    activeField = 1
                } else {
                    pendingScorePlayer = displayIndex
                    pendingScoreHole = hole
                    scoreInputRevision++
                }
            }
        } else {
            onPlayers(players.update(playerIndex, player.copy(putts = player.putts.update(hole, number))))
            if (displayIndex < activeEntries.lastIndex) {
                activePlayer = displayIndex + 1
                activeField = 0
            } else {
                activePlayer = 0
                activeField = 0
            }
        }
    }

    Column(
        Modifier.fillMaxSize().pointerInput(hole) {
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onHorizontalDrag = { change, dragAmount ->
                    totalDrag += dragAmount
                    change.consume()
                },
                onDragEnd = {
                    when {
                        totalDrag < -80f && hole < 17 -> hole++
                        totalDrag > 80f && hole > 0 -> hole--
                    }
                },
                onDragCancel = { totalDrag = 0f }
            )
        }
    ) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF303030)).padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (hole > 0) hole-- }, enabled = hole > 0) { Text("◀", color = Color.White) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (hole < 9) frontCourse.ifBlank { "前半" } else backCourse.ifBlank { "後半" }, color = Color.White, fontSize = 12.sp)
                Text("${hole + 1}H", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                val events = buildList {
                    if (useLongDrive && hole + 1 in longDriveHoles) add("ドラコン")
                    if (useNearPin && hole + 1 in nearPinHoles) add("ニアピン")
                }
                if (events.isNotEmpty()) Text(events.joinToString("・"), color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("左右スワイプでホール移動", color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp)
            }
            TextButton(onClick = { if (hole < 17) hole++ }, enabled = hole < 17) { Text("▶", color = Color.White) }
        }
        Row(Modifier.fillMaxWidth().height(42.dp).background(Color(0xFF404040)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onPars(pars.update(hole, if (par >= 5) 3 else par + 1)) },
                color = Color(0xFF555555), shape = RoundedCornerShape(6.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Text("Par $par", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
            Spacer(Modifier.width(6.dp))
            Surface(modifier = Modifier.width(104.dp).fillMaxHeight(), color = Color.White, shape = RoundedCornerShape(6.dp)) {
                Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = yards.getOrElse(hole) { 0 }.takeIf { it > 0 }?.toString().orEmpty(),
                        onValueChange = { text -> if (text.length <= 3 && text.all(Char::isDigit)) onYards(yards.update(hole, text.toIntOrNull() ?: 0)) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                    )
                    Text("Y", fontSize = 13.sp)
                }
            }
        }

        Row(Modifier.fillMaxWidth().background(Color(0xFF555555)).padding(vertical = 4.dp)) {
            activeEntries.forEachIndexed { index, entry ->
                val player = entry.value
                val playedHoles = (0..hole).filter { player.strokes.getOrElse(it) { 0 } > 0 }
                val total = playedHoles.sumOf { player.strokes[it] }
                val relative = playedHoles.sumOf { player.strokes[it] - pars.getOrElse(it) { 4 } }
                val currentScore = player.strokes.getOrElse(hole) { 0 }
                Column(
                    Modifier.weight(1f).clickable { activePlayer = index; activeField = 0 }.padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(player.name.ifBlank { "P${index + 1}" }, color = Color.White, fontSize = 11.sp, maxLines = 1)
                    Text(if (total == 0) "−" else total.toString(), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text(if (relative > 0) "+$relative" else relative.toString(), color = Color.White, fontSize = 11.sp)
                    Text(scoreLabel(currentScore, par), color = Color.White, fontSize = 10.sp)
                    if (useOlympic) Text("五輪 ${olympicTotal(player.name, olympicPoints, olympicGold, olympicSilver, olympicBronze, olympicIron)}点", color = Color(0xFFFFD54F), fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        ScoreCell("Score", currentScore, activePlayer == index && activeField == 0) { activePlayer = index; activeField = 0; pendingScorePlayer = -1 }
                        ScoreCell("Putts", player.putts.getOrElse(hole) { 0 }, activePlayer == index && activeField == 1, compact = true) { activePlayer = index; activeField = 1; pendingScorePlayer = -1 }
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(scoreAreaScrollState).padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf((0..4).toList(), (5..9).toList()).forEach { numbers ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    numbers.forEach { number ->
                        OutlinedButton(onClick = { enterNumber(number) }, modifier = Modifier.weight(1f).height(43.dp), contentPadding = PaddingValues(0.dp)) {
                            Text(number.toString(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            val selectedEntry = activeEntries[activePlayer.coerceIn(0, activeEntries.lastIndex)]
            val selectedIndex = selectedEntry.index
            val selected = selectedEntry.value
            if ((useLongDrive && hole + 1 in longDriveHoles) || (useNearPin && hole + 1 in nearPinHoles)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (useLongDrive && hole + 1 in longDriveHoles && selectedIndex in longDriveParticipants) {
                        CompetitionCheckbox(
                            label = "ドラコン", winner = longDriveWinners[hole], playerName = selected.name,
                            modifier = Modifier.weight(1f),
                            onChecked = { checked -> onLongDriveWinners(longDriveWinners.update(hole, if (checked) selected.name else "")) }
                        )
                    } else Spacer(Modifier.weight(1f))
                    if (useNearPin && hole + 1 in nearPinHoles && selectedIndex in nearPinParticipants) {
                        CompetitionCheckbox(
                            label = "ニアピン", winner = nearPinWinners[hole], playerName = selected.name,
                            modifier = Modifier.weight(1f),
                            onChecked = { checked -> onNearPinWinners(nearPinWinners.update(hole, if (checked) selected.name else "")) }
                        )
                    } else Spacer(Modifier.weight(1f))
                }
            }
            Text("${selected.name.ifBlank { "プレーヤー${activePlayer + 1}" }} のペナルティ", fontWeight = FontWeight.Bold)
            CounterControl("バンカー", selected.bunkers[hole]) { value ->
                onPlayers(players.update(selectedIndex, selected.copy(bunkers = selected.bunkers.update(hole, value))))
            }
            CounterControl("OB", selected.obs[hole]) { value ->
                onPlayers(players.update(selectedIndex, selected.copy(obs = selected.obs.update(hole, value))))
            }
            CounterControl("1ペナ", selected.penalties[hole]) { value ->
                onPlayers(players.update(selectedIndex, selected.copy(penalties = selected.penalties.update(hole, value))))
            }
            if (useOlympic) {
                HorizontalDivider()
                Text("オリンピック（${hole + 1}H）", fontWeight = FontWeight.Bold)
                val olympicPlayerNames = activeEntries.filter { it.index in olympicParticipants }.map { it.value.name }
                OlympicAwardRow("金 ${olympicPoints.gold}点", olympicGold[hole], olympicPlayerNames) { assignOlympic("gold", it) }
                OlympicAwardRow("銀 ${olympicPoints.silver}点", olympicSilver[hole], olympicPlayerNames) { assignOlympic("silver", it) }
                OlympicAwardRow("銅 ${olympicPoints.bronze}点", olympicBronze[hole], olympicPlayerNames) { assignOlympic("bronze", it) }
                OlympicAwardRow("鉄 ${olympicPoints.iron}点", olympicIron[hole], olympicPlayerNames) { assignOlympic("iron", it) }
            }
        }
    }
}

@Composable
private fun ScoreCell(
    label: String,
    value: Int,
    selected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White, fontSize = 9.sp)
        Surface(
            modifier = Modifier.width(if (compact) 29.dp else 38.dp).height(if (compact) 34.dp else 39.dp).clickable(onClick = onClick),
            color = if (selected) Color(0xFFFFE0B2) else Color.White,
            border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Color(0xFFFF6D00) else Color.Gray)
        ) {
            Box(contentAlignment = Alignment.Center) { Text(if (value == 0) "" else value.toString(), color = Color.Black, fontSize = if (compact) 15.sp else 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun scoreLabel(score: Int, par: Int): String = when {
    score == 0 -> ""
    score - par <= -3 -> "アルバトロス"
    score - par == -2 -> "イーグル"
    score - par == -1 -> "バーディー"
    score == par -> "パー"
    score - par == 1 -> "ボギー"
    score - par == 2 -> "ダブル"
    score - par == 3 -> "トリプル"
    else -> "+${score - par}"
}

@Composable
private fun OlympicAwardRow(label: String, selected: String, players: List<String>, onSelected: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(62.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        PlayerDropdown(value = selected, label = "獲得者", options = players, modifier = Modifier.weight(1f), onSelected = onSelected)
    }
}

private fun olympicTotal(
    playerName: String,
    points: OlympicPoints,
    gold: List<String>, silver: List<String>, bronze: List<String>, iron: List<String>
): Int = gold.count { it == playerName } * points.gold +
    silver.count { it == playerName } * points.silver +
    bronze.count { it == playerName } * points.bronze +
    iron.count { it == playerName } * points.iron

@Composable
private fun CompetitionCheckbox(label: String, winner: String, playerName: String, modifier: Modifier = Modifier, onChecked: (Boolean) -> Unit) {
    val checked = winner == playerName
    Surface(
        modifier = modifier.height(44.dp).clickable { onChecked(!checked) },
        color = if (checked) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
        shape = RoundedCornerShape(7.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (checked) GolfGreen else Color.LightGray)
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.size(36.dp))
            Column {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (winner.isNotBlank() && !checked) Text(winner, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CounterControl(label: String, value: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp)
        FilledTonalButton(onClick = { onValue((value - 1).coerceAtLeast(0)) }, modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 13.dp)) { Text("−") }
        Text(value.toString(), Modifier.width(38.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        FilledTonalButton(onClick = { onValue((value + 1).coerceAtMost(9)) }, modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 13.dp)) { Text("＋") }
    }
}

@Composable private fun TotalItem(label: String, value: Int) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GolfGreen); Text(label, fontSize = 11.sp) } }
@Composable private fun SectionTitle(text: String) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GolfGreen) }

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ChoiceRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column { Text(label, fontSize = 13.sp, color = Color.Gray); FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { options.forEach { option -> FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option) }) } } }
}

private fun <T> List<T>.update(index: Int, value: T) = toMutableList().also { it[index] = value }.toList()
private fun formatDate(date: String): String = runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("yyyy年M月d日")) }.getOrDefault(date)
