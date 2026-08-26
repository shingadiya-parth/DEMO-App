package com.example.core.config

enum class PuzzleDifficulty(val displayName: String, val baseReward: Long) {
    EASY("Easy", 15L),
    MEDIUM("Medium", 25L),
    HARD("Hard", 50L)
}

enum class PuzzleCategory(val displayName: String, val iconEmoji: String) {
    LOGIC("Logic", "🧩"),
    MATH("Math", "🔢"),
    GENERAL_KNOWLEDGE("General Knowledge", "🌍"),
    PATTERN("Pattern", "🔍"),
    WORD("Word", "📝"),
    VISUAL("Visual", "🎨")
}

/**
 * Server-authoritative definition of a puzzle question.
 * Contains the authoritative answer and explanation.
 */
data class PuzzleDefinition(
    val puzzleId: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val difficulty: PuzzleDifficulty,
    val category: PuzzleCategory,
    val rewardAmount: Long = difficulty.baseReward,
    val timeLimitSeconds: Int = 30,
    val enabled: Boolean = true
)

/**
 * Client-safe representation of the puzzle question delivered to the UI.
 * Does NOT contain the correctAnswerIndex or answer key.
 */
data class ClientPuzzleQuestion(
    val puzzleId: String,
    val question: String,
    val options: List<String>,
    val difficulty: PuzzleDifficulty,
    val category: PuzzleCategory,
    val rewardAmount: Long,
    val timeLimitSeconds: Int
)

/**
 * Centralized configuration for the Puzzle Game Engine.
 */
object PuzzleConfig {
    const val GAME_ID = "puzzle"
    const val GAME_TITLE = "Brain Puzzle"

    var puzzleGameEnabled: Boolean = true
    var dailyPuzzleLimit: Int = 5
    var defaultTimeLimitSeconds: Int = 30
    var timerToleranceBufferSeconds: Int = 5 // Network latency tolerance for timer verification
    var adExtraPuzzleEnabled: Boolean = false
    var maximumAdExtraPuzzlesPerDay: Int = 3

    /**
     * Authoritative built-in puzzle catalog covering diverse categories and difficulties.
     */
    val puzzleCatalog: List<PuzzleDefinition> = listOf(
        // Logic
        PuzzleDefinition(
            puzzleId = "puz_log_01",
            question = "If all Bloops are Razzies and all Razzies are Lizzies, are all Bloops definitely Lizzies?",
            options = listOf("Yes, always", "No, never", "Only sometimes", "Cannot be determined"),
            correctAnswerIndex = 0,
            explanation = "By deductive transitive logic: Bloops ⊆ Razzies ⊆ Lizzies, so all Bloops are Lizzies.",
            difficulty = PuzzleDifficulty.EASY,
            category = PuzzleCategory.LOGIC,
            rewardAmount = 15L
        ),
        PuzzleDefinition(
            puzzleId = "puz_log_02",
            question = "A father is 4 times as old as his son. In 20 years, he will be twice as old as his son. How old is the son now?",
            options = listOf("5 years", "10 years", "15 years", "20 years"),
            correctAnswerIndex = 1,
            explanation = "Let son's age = S. Father = 4S. In 20 years: 4S + 20 = 2(S + 20) => 4S + 20 = 2S + 40 => 2S = 20 => S = 10.",
            difficulty = PuzzleDifficulty.MEDIUM,
            category = PuzzleCategory.LOGIC,
            rewardAmount = 25L
        ),

        // Math
        PuzzleDefinition(
            puzzleId = "puz_math_01",
            question = "Which number correctly completes the series: 3, 9, 27, 81, ?",
            options = listOf("162", "243", "324", "729"),
            correctAnswerIndex = 1,
            explanation = "Each term is multiplied by 3 (powers of 3): 81 * 3 = 243.",
            difficulty = PuzzleDifficulty.EASY,
            category = PuzzleCategory.MATH,
            rewardAmount = 15L
        ),
        PuzzleDefinition(
            puzzleId = "puz_math_02",
            question = "What is the value of: 15 + (6 × 4) ÷ 3 - 7?",
            options = listOf("12", "16", "21", "24"),
            correctAnswerIndex = 1,
            explanation = "Using PEMDAS: (6 × 4) = 24. 24 ÷ 3 = 8. 15 + 8 - 7 = 16.",
            difficulty = PuzzleDifficulty.MEDIUM,
            category = PuzzleCategory.MATH,
            rewardAmount = 25L
        ),
        PuzzleDefinition(
            puzzleId = "puz_math_03",
            question = "A shirt costs ₹800 after a 20% discount. What was its original price before the discount?",
            options = listOf("₹960", "₹1,000", "₹1,050", "₹1,200"),
            correctAnswerIndex = 1,
            explanation = "Original price = 800 / (1 - 0.20) = 800 / 0.80 = ₹1,000.",
            difficulty = PuzzleDifficulty.HARD,
            category = PuzzleCategory.MATH,
            rewardAmount = 50L
        ),

        // Pattern
        PuzzleDefinition(
            puzzleId = "puz_pat_01",
            question = "Look at the letter sequence: B, D, G, K, P, ? What comes next?",
            options = listOf("S", "U", "V", "W"),
            correctAnswerIndex = 2,
            explanation = "Differences between alphabet positions increase by 1: +2(D), +3(G), +4(K), +5(P), +6(V).",
            difficulty = PuzzleDifficulty.MEDIUM,
            category = PuzzleCategory.PATTERN,
            rewardAmount = 25L
        ),
        PuzzleDefinition(
            puzzleId = "puz_pat_02",
            question = "Find the missing number: 2, 6, 12, 20, 30, 42, ?",
            options = listOf("52", "54", "56", "64"),
            correctAnswerIndex = 2,
            explanation = "Differences: +4, +6, +8, +10, +12. Next diff is +14, so 42 + 14 = 56 (also n*(n+1) for n=7).",
            difficulty = PuzzleDifficulty.MEDIUM,
            category = PuzzleCategory.PATTERN,
            rewardAmount = 25L
        ),

        // General Knowledge
        PuzzleDefinition(
            puzzleId = "puz_gk_01",
            question = "Which celestial body is known as the 'Red Planet' in our solar system?",
            options = listOf("Venus", "Mars", "Jupiter", "Saturn"),
            correctAnswerIndex = 1,
            explanation = "Mars appears reddish due to the pervasive iron oxide (rust) on its surface.",
            difficulty = PuzzleDifficulty.EASY,
            category = PuzzleCategory.GENERAL_KNOWLEDGE,
            rewardAmount = 15L
        ),
        PuzzleDefinition(
            puzzleId = "puz_gk_02",
            question = "What is the primary currency unit of India?",
            options = listOf("Rupee", "Dinar", "Riyal", "Dollar"),
            correctAnswerIndex = 0,
            explanation = "The Indian Rupee (INR / ₹) is the official currency of India.",
            difficulty = PuzzleDifficulty.EASY,
            category = PuzzleCategory.GENERAL_KNOWLEDGE,
            rewardAmount = 15L
        ),

        // Word & Riddle
        PuzzleDefinition(
            puzzleId = "puz_word_01",
            question = "I speak without a mouth and hear without ears. I have no body, but I come alive with wind. What am I?",
            options = listOf("A shadow", "An echo", "A cloud", "A flame"),
            correctAnswerIndex = 1,
            explanation = "An echo has no physical body or mouth but repeats sound through air waves.",
            difficulty = PuzzleDifficulty.HARD,
            category = PuzzleCategory.WORD,
            rewardAmount = 50L
        ),
        PuzzleDefinition(
            puzzleId = "puz_word_02",
            question = "Which word is an anagram of 'LISTEN'?",
            options = listOf("SILENT", "INSECT", "NESTLE", "LISTED"),
            correctAnswerIndex = 0,
            explanation = "'SILENT' contains the exact same letters (L-I-S-T-E-N) rearranged.",
            difficulty = PuzzleDifficulty.EASY,
            category = PuzzleCategory.WORD,
            rewardAmount = 15L
        )
    )

    fun getActivePuzzles(): List<PuzzleDefinition> = puzzleCatalog.filter { it.enabled }
}
