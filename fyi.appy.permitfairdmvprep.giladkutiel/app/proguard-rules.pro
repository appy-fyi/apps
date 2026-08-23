# Add project specific ProGuard rules here.

# review-ktx's SAM-converted OnSuccessListener references an annotation class that's
# compile-time-only in play-services-basement — safe to suppress the R8 warning.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
