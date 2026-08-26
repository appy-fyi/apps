package fyi.appy.inksend.giladkutiel.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs

private const val TAG = "InkSendOverlay"
private const val BUTTON_SIZE_DP = 56
private const val DEFAULT_MARGIN_END_PX = 48
private const val DEFAULT_MARGIN_BOTTOM_PX = 220
private const val EMOJI_TEXT_SIZE_SP = 26f

// A light, near-white lavender fill (a pale tint of the app's #5B47E0 accent) with a faint
// accent-tinted hairline so the circle stays visible on both light and dark chat backdrops.
private const val BUTTON_BG_COLOR = 0xFFF4F1FE.toInt()
private const val BUTTON_STROKE_COLOR = 0x265B47E0
private const val BUTTON_STROKE_WIDTH_DP = 1

/** Unicode 16 "splatter" (ink drop) — the app's namesake glyph. */
private const val BUTTON_EMOJI = "🫟"

/**
 * Inflates, positions, and tears down the single floating overlay button via [WindowManager].
 * There is now exactly one button: tapping it renders the typed text with a style chosen
 * automatically from the text's content (see
 * [fyi.appy.inksend.giladkutiel.data.model.AutoStyle]). While the user types, [setAppearance]
 * / [resetAppearance] recolour it and swap its glyph so it previews the look a tap produces.
 */
class OverlayWindowManager(
    private val context: Context,
    private val onButtonClicked: () -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var overlayView: View? = null
    private var isShowing = false

    // Live handles to the button's parts, non-null only while the overlay is shown.
    private var emojiView: TextView? = null
    private var backgroundDrawable: GradientDrawable? = null

    // Current previewed look, remembered so a rebuilt button keeps the last appearance.
    private var currentBgColor = BUTTON_BG_COLOR
    private var currentEmoji = BUTTON_EMOJI

    // Remembered across hide/show cycles so a drag "sticks" while typing continues.
    private var marginEndPx = DEFAULT_MARGIN_END_PX
    private var marginBottomPx = DEFAULT_MARGIN_BOTTOM_PX

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    fun showOverlay() {
        if (isShowing) return

        val density = context.resources.displayMetrics.density
        val buttonSizePx = (BUTTON_SIZE_DP * density).toInt()

        val params = WindowManager.LayoutParams(
            buttonSizePx,
            buttonSizePx,
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
        val button = createButton(buttonSizePx, paddingPx, createDragListener(params))

        overlayView = button
        try {
            windowManager.addView(overlayView, params)
            isShowing = true
        } catch (e: Exception) {
            Log.e(TAG, "showOverlay: addView failed", e)
            overlayView = null
        }
    }

    private fun createButton(
        buttonSizePx: Int,
        paddingPx: Int,
        touchListener: View.OnTouchListener,
    ): FrameLayout {
        val emojiLabel = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            text = currentEmoji
            textSize = EMOJI_TEXT_SIZE_SP
            gravity = Gravity.CENTER
        }
        val strokePx = (BUTTON_STROKE_WIDTH_DP * context.resources.displayMetrics.density).toInt()
        val buttonBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(currentBgColor)
            setStroke(strokePx, BUTTON_STROKE_COLOR)
        }
        emojiView = emojiLabel
        backgroundDrawable = buttonBackground
        return FrameLayout(context).apply {
            // Square bounds + an OVAL background == a perfect circle.
            layoutParams = ViewGroup.LayoutParams(buttonSizePx, buttonSizePx)
            background = buttonBackground
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            addView(emojiLabel)
            setOnClickListener { onButtonClicked() }
            setOnTouchListener(touchListener)
        }
    }

    /**
     * Previews the look the typed text will render with: recolours the button's fill and
     * swaps its glyph. [backgroundColorHex] is a "#RRGGBB" string straight from the chosen
     * style; an unparseable value falls back to the neutral fill. The hairline border is
     * left untouched so the circle stays visible on any fill. Remembered across hide/show
     * cycles and applied live when the button is on screen.
     */
    fun setAppearance(backgroundColorHex: String, emoji: String) {
        val color = runCatching { Color.parseColor(backgroundColorHex) }.getOrDefault(BUTTON_BG_COLOR)
        currentBgColor = color
        currentEmoji = emoji
        backgroundDrawable?.setColor(color)
        emojiView?.text = emoji
    }

    /** Restores the neutral default look — light lavender fill, ink-drop glyph. */
    fun resetAppearance() {
        currentBgColor = BUTTON_BG_COLOR
        currentEmoji = BUTTON_EMOJI
        backgroundDrawable?.setColor(BUTTON_BG_COLOR)
        emojiView?.text = BUTTON_EMOJI
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
        emojiView = null
        backgroundDrawable = null
        isShowing = false
    }

    private fun screenWidthPx(): Int = context.resources.displayMetrics.widthPixels
    private fun screenHeightPx(): Int = context.resources.displayMetrics.heightPixels

    private fun clamp(value: Int, min: Int, max: Int): Int = value.coerceIn(min, max.coerceAtLeast(min))
}
