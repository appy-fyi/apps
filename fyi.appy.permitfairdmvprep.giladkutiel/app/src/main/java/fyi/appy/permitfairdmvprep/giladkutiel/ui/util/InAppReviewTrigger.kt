package fyi.appy.permitfairdmvprep.giladkutiel.ui.util

import android.app.Activity
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Play In-App Review (appy build-spec §7) — called once, right after a learner finishes
 * scoring a quiz, a natural positive pause point. The real API is quota-limited server-side
 * and never guarantees the dialog actually shows, so nothing here depends on it having shown.
 */
suspend fun requestInAppReviewIfPossible(activity: Activity) {
    runCatching {
        val manager = ReviewManagerFactory.create(activity)
        val reviewInfo = manager.requestReview()
        manager.launchReview(activity, reviewInfo)
    }
}
