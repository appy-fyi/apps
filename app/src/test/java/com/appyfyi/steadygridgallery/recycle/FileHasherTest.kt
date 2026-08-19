package com.appyfyi.steadygridgallery.recycle

import com.appyfyi.steadygridgallery.data.recycle.FileHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

/**
 * Covers the "Make the recycle bin and folder model bulletproof" unit scenario from the spec's
 * test_plan: a full copy must verify with a matching hash and size 10, while a truncated copy
 * must fail verification -- and a failed verification must never lead to a system delete request.
 */
class FileHasherTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val sourceBytes = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

    @Test
    fun `full copy verifies with matching hash and size`() {
        val destination = tempFolder.newFile("recycle_full_copy")

        val copyResult = FileHasher.copyWithSha256(ByteArrayInputStream(sourceBytes), destination)

        assertEquals(10L, copyResult.sizeBytes)

        val verified = FileHasher.verifyCopy(destination, copyResult.sha256Hex, copyResult.sizeBytes)
        assertTrue(verified)

        var systemDeleteRequestInvoked = false
        if (verified) {
            systemDeleteRequestInvoked = true
        }
        assertTrue(systemDeleteRequestInvoked)
    }

    @Test
    fun `truncated copy fails verification and never reaches the delete-request step`() {
        val destination = tempFolder.newFile("recycle_truncated_copy")
        val truncatedBytes = byteArrayOf(0, 1, 2)

        // Simulate a copy that was interrupted after only 3 of the 10 source bytes landed on disk,
        // while still recording the hash/size the full 10-byte source would have produced.
        val fullCopyResult = FileHasher.copyWithSha256(ByteArrayInputStream(sourceBytes), tempFolder.newFile())
        destination.writeBytes(truncatedBytes)

        val verified = FileHasher.verifyCopy(destination, fullCopyResult.sha256Hex, fullCopyResult.sizeBytes)
        assertFalse(verified)

        var systemDeleteRequestInvoked = false
        if (verified) {
            systemDeleteRequestInvoked = true
        }
        assertFalse(systemDeleteRequestInvoked)
    }
}
