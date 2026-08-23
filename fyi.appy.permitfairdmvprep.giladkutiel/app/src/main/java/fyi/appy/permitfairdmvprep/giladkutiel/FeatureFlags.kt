package fyi.appy.permitfairdmvprep.giladkutiel

object FeatureFlags {
    /**
     * Launch decision: ship every feature free while the app is new, instead of gating a
     * second practice test / lesson quiz behind the $4.99 lifetime unlock. All the billing
     * code (BillingRepository, EntitlementCache, the Unlock screen) stays in place — flip this
     * back to false and bump versionCode to turn the paywall back on in a later release.
     */
    const val ALL_FEATURES_FREE = true
}
