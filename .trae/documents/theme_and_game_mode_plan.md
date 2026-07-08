# Plan: Themes & New Game Mode for 2048 Neon Rush

## Summary
Add theme switching capability (Classic 2048 + Neon Rush themes) and create a new 2048 game variation called "Time Attack 2048" with a countdown timer mechanic.

---

## Part 1: Theme System Implementation

### Current State Analysis
- Single "Neon Rush" theme hardcoded in `ui/theme/Theme.kt`
- Colors defined directly in theme file with no abstraction
- No theme persistence mechanism

### Proposed Changes

#### 1.1 Create Theme Data Models
**File:** `app/src/main/java/com/avfusionapps/game_2048/ui/theme/GameTheme.kt`
```kotlin
sealed class GameTheme(val name: String) {
    abstract val primaryColor: Color
    abstract val secondaryColor: Color
    abstract val backgroundColor: Color
    abstract val surfaceColor: Color
    abstract val tileColors: Map<Int, Color>
    abstract val textColor: Color
    abstract val accentColor: Color

    object NeonRush : GameTheme("Neon Rush") { ... }
    object Classic2048 : GameTheme("Classic 2048") { ... }
}
```

**Rationale:** Abstract theme definition allows easy addition of new themes and centralizes color management.

#### 1.2 Create Theme Repository for Persistence
**File:** `app/src/main/java/com/avfusionapps/game_2048/data/repository/ThemeRepository.kt`
```kotlin
class ThemeRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val currentTheme: Flow<GameTheme>
    
    suspend fun setTheme(theme: GameTheme)
    
    companion object {
        val THEME_KEY = stringPreferencesKey("selected_theme")
    }
}
```

**Rationale:** Persists user theme selection across app restarts using existing DataStore infrastructure.

#### 1.3 Update Theme.kt to Support Dynamic Themes
**File:** `app/src/main/java/com/avfusionapps/game_2048/ui/theme/Theme.kt`
```kotlin
@Composable
fun _2048OriginalTheme(
    theme: GameTheme = GameTheme.NeonRush,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        is GameTheme.NeonRush -> darkColorScheme(...)
        is GameTheme.Classic2048 -> lightColorScheme(...)
    }
    
    CompositionLocalProvider(
        LocalGameTheme provides theme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

val LocalGameTheme = staticCompositionLocalOf<GameTheme> { GameTheme.NeonRush }
```

**Rationale:** Makes theme dynamic and accessible throughout the composition via CompositionLocal.

#### 1.4 Add Theme Settings Screen
**File:** `app/src/main/java/com/avfusionapps/game_2048/ui/screens/ThemeSettingsScreen.kt`
```kotlin
@Composable
fun ThemeSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Choose Theme",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ThemeCard(
            theme = GameTheme.NeonRush,
            isSelected = currentTheme is GameTheme.NeonRush,
            onClick = { viewModel.setTheme(GameTheme.NeonRush) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ThemeCard(
            theme = GameTheme.Classic2048,
            isSelected = currentTheme is GameTheme.Classic2048,
            onClick = { viewModel.setTheme(GameTheme.Classic2048) }
        )
    }
}

@Composable
private fun ThemeCard(
    theme: GameTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) theme.accentColor else theme.surfaceColor
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, theme.accentColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme preview with sample tiles
            ThemePreview(theme)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.textColor
                )
                
                if (isSelected) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.accentColor
                    )
                }
            }
        }
    }
}
```

**Rationale:** Provides intuitive UI for theme selection with live preview.

---

## Part 2: New Game Mode - Time Attack 2048

### Concept
A fast-paced 2048 variant where players race against a countdown timer. Match tiles to add time bonuses. Game ends when timer reaches zero.

### Key Features
- 60-second initial timer
- +5 seconds for each tile merge
- +15 seconds for creating 128+ tile
- Score multiplier based on remaining time
- Separate leaderboard for Time Attack mode

### Implementation

#### 2.1 Create Time Attack Game State
**File:** `app/src/main/java/com/avfusionapps/game_2048/model/TimeAttackState.kt`
```kotlin
data class TimeAttackState(
    val grid: List<List<Tile>>,
    val score: Int,
    val timeRemainingMillis: Long,
    val isGameOver: Boolean,
    val isPaused: Boolean,
    val multiplier: Float = 1.0f,
    val lastBonus: BonusType? = null
)

sealed class BonusType(val timeBonusMillis: Long, val message: String) {
    object MergeBonus : BonusType(5000L, "+5s Merge!")
    object Tile128Bonus : BonusType(15000L, "+15s 128 Tile!")
    object Tile256Bonus : BonusType(15000L, "+15s 256 Tile!")
    object StreakBonus : BonusType(10000L, "+10s Streak!")
}
```

#### 2.2 Create Time Attack ViewModel
**File:** `app/src/main/java/com/avfusionapps/game_2048/viewmodel/TimeAttackViewModel.kt`
```kotlin
@HiltViewModel
class TimeAttackViewModel @Inject constructor(
    private val timeAttackRepository: TimeAttackRepository
) : ViewModel() {

    private val _gameState = MutableStateFlow(TimeAttackState(...))
    val gameState: StateFlow<TimeAttackState> = _gameState.asStateFlow()
    
    private var timerJob: Job? = null
    private val initialTimeMillis = 60_000L // 60 seconds
    
    init {
        startNewGame()
    }
    
    fun startNewGame() {
        timerJob?.cancel()
        _gameState.value = TimeAttackState(
            grid = createEmptyGrid(),
            score = 0,
            timeRemainingMillis = initialTimeMillis,
            isGameOver = false,
            isPaused = false,
            multiplier = 1.0f
        )
        addRandomTile()
        addRandomTile()
        startTimer()
    }
    
    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_gameState.value.timeRemainingMillis > 0 && !_gameState.value.isGameOver) {
                delay(100) // Update every 100ms for smooth countdown
                
                if (!_gameState.value.isPaused) {
                    val newTime = _gameState.value.timeRemainingMillis - 100
                    _gameState.update { it.copy(timeRemainingMillis = maxOf(0, newTime)) }
                    
                    if (newTime <= 0) {
                        endGame()
                    }
                }
            }
        }
    }
    
    fun onSwipe(direction: SwipeDirection) {
        if (_gameState.value.isGameOver || _gameState.value.isPaused) return
        
        val currentGrid = _gameState.value.grid
        val (newGrid, scoreGained, mergedTiles) = moveGrid(currentGrid, direction)
        
        if (newGrid != currentGrid) {
            var timeBonus = 0L
            var multiplier = _gameState.value.multiplier
            var lastBonus: BonusType? = null
            
            // Calculate bonuses
            mergedTiles.forEach { tile ->
                timeBonus += BonusType.MergeBonus.timeBonusMillis
                
                when (tile.value) {
                    128 -> {
                        timeBonus += BonusType.Tile128Bonus.timeBonusMillis
                        lastBonus = BonusType.Tile128Bonus
                    }
                    256 -> {
                        timeBonus += BonusType.Tile128Bonus.timeBonusMillis
                        lastBonus = BonusType.Tile256Bonus
                    }
                    // Higher tiles give even more bonus
                    512 -> timeBonus += 20000L
                    1024 -> timeBonus += 30000L
                    2048 -> timeBonus += 60000L // Full minute bonus for 2048!
                }
                
                // Increase multiplier based on tile value
                multiplier += (tile.value / 1000f) * 0.1f
            }
            
            // Apply time bonus with cap (max 2 minutes)
            val newTime = minOf(120_000L, _gameState.value.timeRemainingMillis + timeBonus)
            
            // Calculate final score with multiplier
            val finalScoreGained = (scoreGained * multiplier).toInt()
            
            _gameState.update {
                it.copy(
                    grid = addRandomTile(newGrid),
                    score = it.score + finalScoreGained,
                    timeRemainingMillis = newTime,
                    multiplier = minOf(5.0f, multiplier), // Cap at 5x
                    lastBonus = lastBonus
                )
            }
            
            // Save high score to repository
            viewModelScope.launch {
                timeAttackRepository.saveTimeAttackHighScore(
                    TimeAttackScore(
                        score = _gameState.value.score,
                        timeSurvived = initialTimeMillis - _gameState.value.timeRemainingMillis,
                        date = Date()
                    )
                )
            }
        }
    }
    
    fun togglePause() {
        _gameState.update { it.copy(isPaused = !it.isPaused) }
    }
    
    private fun endGame() {
        timerJob?.cancel()
        _gameState.update { it.copy(isGameOver = true) }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
```

**Rationale:** Complete Time Attack implementation with countdown timer, time bonuses for merges, score multipliers, and proper state management.

#### 2.3 Create Time Attack UI Screen
**File:** `app/src/main/java/com/avfusionapps/game_2048/ui/screens/TimeAttackScreen.kt`
```kotlin
@Composable
fun TimeAttackScreen(
    navController: NavController,
    viewModel: TimeAttackViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val context = LocalContext.current
    
    // Haptic feedback
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PurpleDarkBackground,
                            PurpleDarkBackground.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with timer and score
            TimeAttackHeader(
                timeRemainingMillis = gameState.timeRemainingMillis,
                score = gameState.score,
                multiplier = gameState.multiplier,
                isPaused = gameState.isPaused,
                onPauseToggle = { viewModel.togglePause() },
                onBack = { navController.popBackStack() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Last bonus notification
            AnimatedVisibility(
                visible = gameState.lastBonus != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                gameState.lastBonus?.let { bonus ->
                    BonusNotification(bonus = bonus)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Game grid with swipe detection
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TimeAttackGameGrid(
                    grid = gameState.grid,
                    onSwipe = { direction ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onSwipe(direction)
                    }
                )
            }
        }
        
        // Pause overlay
        if (gameState.isPaused) {
            PauseOverlay(
                onResume = { viewModel.togglePause() },
                onRestart = { viewModel.startNewGame() },
                onQuit = { navController.popBackStack() }
            )
        }
        
        // Game Over overlay
        if (gameState.isGameOver) {
            TimeAttackGameOverOverlay(
                finalScore = gameState.score,
                timeSurvived = 60_000L - gameState.timeRemainingMillis,
                onPlayAgain = { viewModel.startNewGame() },
                onBackToMenu = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun TimeAttackHeader(
    timeRemainingMillis: Long,
    score: Int,
    multiplier: Float,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onBack: () -> Unit
) {
    val minutes = (timeRemainingMillis / 60000).toInt()
    val seconds = ((timeRemainingMillis % 60000) / 1000).toInt()
    val millis = ((timeRemainingMillis % 1000) / 10).toInt()
    
    // Animate timer color based on urgency
    val timerColor by animateColorAsState(
        targetValue = when {
            timeRemainingMillis < 10_000L -> Color.Red
            timeRemainingMillis < 30_000L -> Color.Yellow
            else -> Color.Green
        },
        label = "timerColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        // Timer display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%02d:%02d.%02d", minutes, seconds, millis),
                style = MaterialTheme.typography.headlineMedium,
                color = timerColor,
                fontWeight = FontWeight.Bold
            )
            
            if (multiplier > 1.0f) {
                Text(
                    text = "${multiplier}x Multiplier!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Yellow
                )
            }
        }
        
        // Pause button
        IconButton(onClick = onPauseToggle) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Resume" else "Pause",
                tint = Color.White
            )
        }
    }
    
    // Score display
    Text(
        text = "Score: ${score.toFormattedString()}",
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun BonusNotification(bonus: BonusType) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = bonus.message,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Additional composables for TimeAttackGameGrid, PauseOverlay, TimeAttackGameOverOverlay...
```

---

## Part 3: Classic 2048 Theme Definition

**File:** Update `GameTheme.kt` with Classic theme

```kotlin
object Classic2048 : GameTheme("Classic 2048") {
    override val primaryColor = Color(0xFF8F7A66)      // Wood tone
    override val secondaryColor = Color(0xFFBBADA0)    // Light wood
    override val backgroundColor = Color(0xFFFAF8EF)   // Cream background
    override val surfaceColor = Color(0xFFBBADA0)      // Game board background
    override val textColor = Color(0xFF776E65)         // Dark gray text
    override val accentColor = Color(0xFF8F7A66)       // Accent matches primary
    
    override val tileColors = mapOf(
        0 to Color(0xFFCDC1B4),      // Empty tile
        2 to Color(0xFFEEE4DA),       // Light cream
        4 to Color(0xFFEDE0C8),       // Cream
        8 to Color(0xFFF2B179),       // Light orange
        16 to Color(0xFFF59563),      // Orange
        32 to Color(0xFFF67C5F),      // Dark orange
        64 to Color(0xFFF65E3B),      // Red-orange
        128 to Color(0xFFEDCF72),     // Yellow
        256 to Color(0xFFEDCC61),     // Dark yellow
        512 to Color(0xFFEDC850),     // Gold
        1024 to Color(0xFFEDC53F),    // Dark gold
        2048 to Color(0xFFEDC22E)     // Bright gold
    )
}
```

---

## Part 4: Navigation & Menu Updates

### Add Time Attack to Main Menu
**File:** Update `MainScreen.kt`

```kotlin
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: GameViewModel
) {
    // ... existing code ...
    
    // Game Mode Selection
    Text(
        text = "Select Game Mode",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Classic Mode Button
    NeonCutCornerButton(
        text = "Classic 2048",
        onClick = { navController.navigate("game?newGame=true") },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(12.dp))
    
    // Time Attack Mode Button
    NeonCutCornerButton(
        text = "Time Attack",
        onClick = { navController.navigate("timeAttack") },
        modifier = Modifier.fillMaxWidth(),
        accentColor = Color(0xFFFF5722) // Orange accent for time attack
    )
    
    // ... rest of existing code ...
}
```

### Update Navigation Graph
**File:** Update `MainActivity.kt` NavHost

```kotlin
composable("timeAttack") {
    val theme by themeViewModel.currentTheme.collectAsState()
    
    _2048OriginalTheme(theme = theme) {
        TimeAttackScreen(
            navController = navController
        )
    }
}

composable("themeSettings") {
    val theme by themeViewModel.currentTheme.collectAsState()
    
    _2048OriginalTheme(theme = theme) {
        ThemeSettingsScreen(
            navController = navController
        )
    }
}
```

---

## Part 5: Settings Integration

### Add Theme Option to Settings
**File:** Update settings or create SettingsScreen

```kotlin
@Composable
fun SettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Theme Selection
        SettingsCard(
            title = "Theme",
            subtitle = "Current: ${currentTheme.name}",
            icon = Icons.Default.Palette,
            onClick = { navController.navigate("themeSettings") }
        )
        
        // ... other settings ...
    }
}
```

---

## Files to Create/Modify

### New Files:
1. `ui/theme/GameTheme.kt` - Theme abstraction with Classic and Neon themes
2. `data/repository/ThemeRepository.kt` - Theme persistence
3. `viewmodel/ThemeViewModel.kt` - Theme state management
4. `ui/screens/ThemeSettingsScreen.kt` - Theme selection UI
5. `model/TimeAttackState.kt` - Time attack game state
6. `viewmodel/TimeAttackViewModel.kt` - Time attack logic
7. `ui/screens/TimeAttackScreen.kt` - Time attack game UI
8. `data/repository/TimeAttackRepository.kt` - Time attack high scores

### Modified Files:
1. `ui/theme/Theme.kt` - Support dynamic theme selection
2. `MainActivity.kt` - Add navigation routes
3. `MainScreen.kt` - Add game mode selection
4. `GameScreen.kt` - May need theme-aware tile colors
5. `build.gradle.kts` - Add Hilt if not present (for ViewModel injection)

---

## Assumptions & Decisions

1. **Theme System:** Using sealed class with CompositionLocal for theme propagation
2. **Time Attack Mechanics:** 60s initial timer with merge bonuses (validated as fun arcade mechanic)
3. **Persistence:** Existing DataStore for theme, Room for Time Attack scores
4. **DI:** Assuming Hilt will be added for proper ViewModel injection
5. **Navigation:** Following existing navigation-compose pattern
6. **Theme Colors:** Classic 2048 uses official color palette from original game

---

## Verification Steps

1. Build project successfully after all changes
2. Navigate to theme settings - verify both themes appear
3. Switch to Classic theme - verify UI updates immediately
4. Kill and restart app - verify theme preference persists
5. Navigate to Time Attack mode from main menu
6. Verify timer starts at 60 seconds and counts down
7. Merge tiles - verify time bonus is added
8. Let timer reach zero - verify game over screen appears
9. Test pause/resume functionality
10. Verify score is saved to local leaderboard

---

## Success Criteria

- [ ] User can switch between Neon Rush and Classic 2048 themes
- [ ] Theme preference persists across app restarts
- [ ] All UI components adapt to selected theme colors
- [ ] Time Attack mode accessible from main menu
- [ ] Timer counts down from 60 seconds with smooth UI updates
- [ ] Time bonuses awarded correctly for tile merges
- [ ] Game over triggers when timer reaches zero
- [ ] Final score is saved and displayed on leaderboard
- [ ] No regression in existing Classic 2048 mode functionality
