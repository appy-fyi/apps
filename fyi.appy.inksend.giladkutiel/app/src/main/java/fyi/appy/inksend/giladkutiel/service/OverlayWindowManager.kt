package fyi.appy.inksend.giladkutiel.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import fyi.appy.inksend.giladkutiel.R
import kotlin.math.abs

private const val TAG = "InkSendOverlay"
private const val BUTTON_SIZE_DP = 56
private const val BUTTON_SPACING_DP = 12
private const val DEFAULT_MARGIN_END_PX = 48
private const val DEFAULT_MARGIN_BOTTOM_PX = 220
private const val EMOJI_TEXT_SIZE_SP = 26f

/**
 * Inflates, positions, and tears down the pair of floating overlay style buttons via
 * [WindowManager]. Both buttons live inside one draggable container, stacked vertically,
 * so a single drag moves the pair together while each button keeps its own tap target.
 * Each button's icon is that style's configured emoji badge (falling back to a generic
 * icon when the style's badge is set to "None"), read fresh at [showOverlay] time so the
 * buttons always reflect the latest saved styles.
 */
class OverlayWindowManager(
    private val context: Context,
    private val primaryEmoji: () -> String,
    private val secondaryEmoji: () -> String,
    private val onPrimaryClicked: () -> Unit,
    private val onSecondaryClicked: () -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var overlayView: View? = null
    private var isShowing = false

    // Remembered across hide/show cycles so a drag "sticks" while typing continues.
    private var marginEndPx = DEFAULT_MARGIN_END_PX
    private var marginBottomPx = DEFAULT_MARGIN_BOTTOM_PX

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    fun showOverlay() {
        if (isShowing) return

        val density = context.resources.displayMetrics.density
        val buttonSizePx = (BUTTON_SIZE_DP * density).toInt()
        val spacingPx = (BUTTON_SPACING_DP * density).toInt()
        val containerHeightPx = buttonSizePx * 2 + spacingPx

        val params = WindowManager.LayoutParams(
            buttonSizePx,
            containerHeightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = marginEndPx
            y = marginBottomPx
        }

        val paddingPx = (12 * density).toInt()
        val dragListener = createDragListener(params)
        val primaryButton = createStyleButton(
            buttonSizePx = buttonSizePx,
            paddingPx = paddingPx,
            backgroundRes = R.drawable.bg_overlay_button,
            emoji = primaryEmoji(),
            onClick = onPrimaryClicked,
            touchListener = dragListener,
        )
        val secondaryButton = createStyleButton(
            buttonSizePx = buttonSizePx,
            paddingPx = paddingPx,
            backgroundRes = R.drawable.bg_overlay_button_secondary,
            emoji = secondaryEmoji(),
            onClick = onSecondaryClicked,
            touchListener = dragListener,
        )
        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(buttonSizePx, spacingPx)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(buttonSizePx, containerHeightPx)
            addView(primaryButton)
            addView(spacer)
            addView(secondaryButton)
        }

        overlayView = container
        try {
            windowManager.addView(overlayView, params)
            isShowing = true
        } catch (e: Exception) {
            Log.e(TAG, "showOverlay: addView failed", e)
            overlayView = null
        }
    }

    /**
     * A button's icon is its style's emoji when set, otherwise the generic fallback icon —
     * both layered in the same [buttonSizePx] square and centered identically so swapping
     * between them (as the user edits their badge choice) never shifts the tap target.
     */
    private fun createStyleButton(
        buttonSizePx: Int,
        paddingPx: Int,
        backgroundRes: Int,
        emoji: String,
        onClick: () -> Unit,
        touchListener: View.OnTouchListener,
    ): FrameLayout {
        val fallbackIcon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            setImageResource(R.drawable.ic_style_convert)
            visibility = if (emoji.isBlank()) View.VISIBLE else View.GONE
        }
        val emojiLabel = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            text = emoji
            textSize = EMOJI_TEXT_SIZE_SP
            gravity = Gravity.CENTER
            visibility = if (emoji.isBlank()) View.GONE else View.VISIBLE
        }
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(buttonSizePx, buttonSizePx)
            setBackgroundResource(backgroundRes)
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            addView(fallbackIcon)
            addView(emojiLabel)
            setOnClickListener { onClick() }
            setOnTouchListener(touchListener)
        }
    }

    /**
     * Drags reposition the button (updating the margins gravity BOTTOM|END is anchored to);
     * a touch that never moves past [touchSlop] is treated as a click instead.
     */
    private fun createDragListener(params: WindowManager.LayoutParams): View.OnTouchListener {
        var downRawX = 0f
        var downRawY = 0f
        var downMarginEndPx = 0
        var downMarginBottomPx = 0
        var moved = false

        return View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    downMarginEndPx = marginEndPx
                    downMarginBottomPx = marginBottomPx
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - downRawX
                    val deltaY = event.rawY - downRawY
                    if (!moved && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                        moved = true
                    }
                    if (moved) {
                        // END/BOTTOM margins shrink as the button moves right/down.
                        marginEndPx = clamp(
                            downMarginEndPx - deltaX.toInt(),
                            0,
                            screenWidthPx() - params.width,
                        )
                        marginBottomPx = clamp(
                            downMarginBottomPx - deltaY.toInt(),
                            0,
                            screenHeightPx() - params.height,
                        )
                        params.x = marginEndPx
                        params.y = marginBottomPx
                        try {
                            windowManager.updateViewLayout(overlayView, params)
                        } catch (e: Exception) {
                            Log.e(TAG, "drag: updateViewLayout failed", e)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    fun hideOverlay() {
        if (!isShowing || overlayView == null) return
        try {
            windowManager.removeView(overlayView)
        } catch (e: Exception) {
            Log.e(TAG, "hideOverlay: removeView failed", e)
        }
        overlayView = null
        isShowing = false
    }

    private fun screenWidthPx(): Int = context.resources.displayMetrics.widthPixels
    private fun screenHeightPx(): Int = context.resources.displayMetrics.heightPixels

    private fun clamp(value: Int, min: Int, max: Int): Int = value.coerceIn(min, max.coerceAtLeast(min))
}
