package fyi.appy.permitfairdmvprep.giladkutiel.data.content

import kotlinx.serialization.Serializable

@Serializable
data class ManifestDto(
    val manifestVersion: String,
    val statePacks: List<ManifestStateEntryDto>,
)

@Serializable
data class ManifestStateEntryDto(
    val stateId: String,
    val displayName: String,
    val handbookTitle: String,
    val handbookSourceUrl: String,
    val version: String,
)

@Serializable
data class PackDto(
    val statePack: PackStatePackDto,
    val lessons: List<PackLessonDto>,
    val questions: List<PackQuestionDto>,
    val answerOptions: List<PackAnswerOptionDto>,
)

@Serializable
data class PackStatePackDto(
    val stateId: String,
    val displayName: String,
    val handbookTitle: String,
    val handbookSourceUrl: String,
    val version: String,
)

@Serializable
data class PackLessonDto(
    val id: String,
    val stateId: String,
    val title: String,
    val handbookSection: String,
    val bodyMarkdown: String,
    val sortOrder: Int,
)

@Serializable
data class PackQuestionDto(
    val id: String,
    val stateId: String,
    val lessonId: String,
    val prompt: String,
    val explanation: String,
    val handbookSection: String,
    val sortOrder: Int,
)

@Serializable
data class PackAnswerOptionDto(
    val id: String,
    val questionId: String,
    val text: String,
    val isCorrect: Boolean,
    val sortOrder: Int,
)
