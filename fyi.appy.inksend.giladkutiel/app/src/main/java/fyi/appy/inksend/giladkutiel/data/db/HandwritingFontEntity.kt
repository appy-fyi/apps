package fyi.appy.inksend.giladkutiel.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "handwriting_fonts")
data class HandwritingFontEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Internal-storage path to the compiled .ttf. */
    val filePath: String,
    /** 0-62, drives the resume/progress state on HandwritingFontCreator. */
    val glyphsCompleted: Int,
    val createdAt: Instant,
)
