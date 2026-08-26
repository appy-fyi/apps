package fyi.appy.inksend.giladkutiel.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Requests Play's in-app review flow at most once per process lifetime, from
 * a genuine positive pause point (right after both required permissions —
 * overlay and accessibility — become granted, see `MainActivity`), never on
 * first launch or after an error. The real API is quota-limited server-side
 * and may silently no-op; nothing here assumes the dialog actually appeared.
 */
object InAppReviewHelper {
    private val requestedThisSession = AtomicBoolean(false)

    fun maybeRequestReview(activity: Activity) {
        if (!requestedThisSession.compareAndSet(false, true)) return
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }
}
