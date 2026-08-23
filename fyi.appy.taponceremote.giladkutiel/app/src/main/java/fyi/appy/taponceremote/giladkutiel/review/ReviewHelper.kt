package fyi.appy.taponceremote.giladkutiel.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Requests an in-app review right after a natural success moment (a remote
 * command actually reaching a connected device). The Play API is
 * quota-limited server-side and never guarantees the dialog shows — this
 * only ever requests it, it never assumes success.
 */
object ReviewHelper {
    fun maybeRequestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }
}
