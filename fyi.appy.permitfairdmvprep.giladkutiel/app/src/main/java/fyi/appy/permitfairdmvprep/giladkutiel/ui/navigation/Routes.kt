package fyi.appy.permitfairdmvprep.giladkutiel.ui.navigation

object Routes {
    const val NO_LESSON = "none"

    const val STATE_SELECT_PATTERN = "state_select?allowAutoSkip={allowAutoSkip}"
    fun stateSelect(allowAutoSkip: Boolean = true) = "state_select?allowAutoSkip=$allowAutoSkip"

    const val HOME = "home"

    const val LESSON_PATTERN = "lesson/{lessonId}"
    fun lesson(lessonId: String) = "lesson/$lessonId"

    const val QUIZ_PATTERN = "quiz/{quizMode}/{lessonId}"
    fun quiz(quizMode: String, lessonId: String = NO_LESSON) = "quiz/$quizMode/$lessonId"

    const val RESULTS_PATTERN = "results/{attemptId}"
    fun results(attemptId: String) = "results/$attemptId"

    const val UNLOCK = "unlock"
    const val SETTINGS = "settings"
}
