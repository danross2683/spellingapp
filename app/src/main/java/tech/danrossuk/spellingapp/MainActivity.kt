package tech.danrossuk.spellingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.danrossuk.spellingapp.ui.theme.SpellingAppTheme
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpellingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpellingGameFragment(gameViewModel)
                }
            }
        }
    }
}

// Data class for high scores
data class HighScore(val name: String, val score: Int)

// Composable function for the spelling game
@Composable
fun SpellingGameFragment(gameViewModel: GameViewModel) {
    val playerName by remember { gameViewModel::playerName }
    val highScores by remember { gameViewModel::highScores }
    val startTime by remember { gameViewModel::startTime }
    val duration by remember { gameViewModel::duration }
    val gameWords by remember { gameViewModel::gameWords }
    val wordIndex by remember { gameViewModel::wordIndex }
    val currentWord by remember { gameViewModel::currentWord }
    val options by remember { gameViewModel::options }
    val gameStatus by remember { gameViewModel::gameStatus }
    val score by remember { gameViewModel::score }
    val showHighScoreTable by remember { gameViewModel::showHighScoreTable }

    LaunchedEffect(gameStatus) {
        if (gameStatus == GameStatus.Playing || gameStatus == GameStatus.Restart) {
            gameViewModel.startGame()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Spell the word:", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(currentWord, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))

        Spacer(modifier = Modifier.height(16.dp))

        options.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                pair.forEach { option ->
                    Button(onClick = { gameViewModel.checkSelection(option) }, modifier = Modifier.padding(4.dp)) {
                        Text(option)
                    }
                }
            }
        }

        // Display the score
        Text("Score: $score", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))

        // Display high score table if applicable
        if (showHighScoreTable) {
            ShowHighScores(
                highScores = highScores,
                playerName = playerName,
                onNameChange = { gameViewModel.playerName = it },
                onSubmit = {
                    gameViewModel.highScores = generateRandomHighScores(score).sortedByDescending { it.score }
                }
            )
        }
    }
}


@Composable
fun ShowHighScores(
    highScores: List<HighScore>,
    playerName: String,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column {
        Text("High Scores", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        LazyColumn {
            items(highScores) { highScore ->
                Text("${highScore.name}: ${highScore.score}", fontSize = 16.sp)
            }
        }
        if (playerName.isBlank()) {
            OutlinedTextField(
                value = playerName,
                onValueChange = onNameChange,
                label = { Text("Your Initials") },
                singleLine = true,
                maxLines = 1, // Ensure only one letter is accepted before auto-submitting
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onSubmit, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Submit")
            }
        }
    }
}

// Generate random high scores for demonstration
fun generateRandomHighScores(playerScore: Int): List<HighScore> {
    val maxPossibleScore = 40
    val minHighScore = (maxPossibleScore * 0.15).toInt() + 10 // Minimum high score set to 10
    val maxHighScore = (maxPossibleScore * 0.85).toInt() // Maximum high score set to 85% of max possible score
    val randomScores = List(5) { HighScore(generateRandomName(), (minHighScore..maxHighScore).random()) }
    return (randomScores + HighScore("You", playerScore)).sortedByDescending { it.score }
}

fun generateRandomName(): String = List(3) { ('A'..'Z').random() }.joinToString("")

private fun generateGameWords(): List<String> {
    // Replace these example words with your actual game words
    return listOf(
        "accommodate", "rhythm", "conscious", "embarrass", "parallel",
        "liaison", "occurrence", "recommend", "supersede", "noticeable"
    )
}
private fun generateOptions(correctWord: String): List<String> {
    val options = mutableSetOf(correctWord)
    while (options.size < 4) {
        val wordVariant = correctWord.toMutableList().apply {
            val charIndex = indices.random()
            this[charIndex] = ('a'..'z').filterNot { it == this[charIndex] }.random()
        }.joinToString("")
        options.add(wordVariant)
    }
    return options.toList().shuffled()
}

// Enums for game status
enum class GameStatus {
    Playing, Correct, Wrong, Invalid, Finished, Restart
}

// ViewModel class for the game logic
class GameViewModel : ViewModel() {
    var playerName by mutableStateOf("")
    var highScores by mutableStateOf(emptyList<HighScore>())
    var startTime by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var gameWords by mutableStateOf(emptyList<String>())
    var wordIndex by mutableStateOf(0)
    var currentWord by mutableStateOf("")
    var options by mutableStateOf(emptyList<String>())
    var gameStatus by mutableStateOf(GameStatus.Playing)
    var score by mutableStateOf(0)
    var showHighScoreTable by mutableStateOf(false) // Control the visibility of the high score table

    fun startGame() {
        gameWords = generateGameWords().shuffled().take(10)
        wordIndex = 0
        gameStatus = GameStatus.Playing
        nextWord()
        startTime = System.currentTimeMillis()
    }

    fun nextWord() {
        if (wordIndex < gameWords.size) {
            currentWord = gameWords[wordIndex]
            options = generateOptions(currentWord)
        } else {
            gameStatus = GameStatus.Finished
            duration = System.currentTimeMillis() - startTime
            submitHighScore() // Call submitHighScore() when the game is finished
        }
    }

    fun checkSelection(option: String) {
        if (option.equals(currentWord, ignoreCase = true)) {
            score += 4 // Award 4 points for a correct answer
        } else {
            score-- // Deduct 1 point for an incorrect answer
            options = options.minus(option) // Remove the incorrect option
        }
        wordIndex++
        nextWord()
    }

    fun submitHighScore() {
        showHighScoreTable = true
        highScores = generateRandomHighScores(score).sortedByDescending { it.score }
    }
}

@Composable
fun ShowHighScoresDialog(
    highScores: List<HighScore>,
    playerName: String,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column {
        Text("High Scores", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        LazyColumn {
            items(highScores) { highScore ->
                Text("${highScore.name}: ${highScore.score}", fontSize = 16.sp)
            }
        }
        if (playerName.isBlank()) {
            OutlinedTextField(
                value = playerName,
                onValueChange = onNameChange,
                label = { Text("Your Initials") },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onSubmit, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Submit")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SpellingAppTheme {
        SpellingGameFragment(gameViewModel = GameViewModel())
    }
}
