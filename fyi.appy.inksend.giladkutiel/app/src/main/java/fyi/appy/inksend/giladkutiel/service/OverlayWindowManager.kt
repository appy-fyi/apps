package fyi.appy.inksend.giladkutiel.service

import android.annotation.SuppressLint
import android.content.Context
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
private const val BUTTON_COLOR = 0xFF5B47E0.toInt()
private const val BUTTON_EMOJI = "✨"

/**
 * Inflates, positions, and tears down the single floating overlay button via [WindowManager].
 * There is now exactly one button: tapping it renders the typed text with a style chosen
 * automatically from the text's content (see
 * [fyi.appy.inksend.giladkutiel.data.model.AutoStyle]), so the overlay no longer needs to
 * reflect any user-configured style list.
 */
class OverlayWindowManager(
    private val context: Context,
    private val onButtonClicked: () -> Unit,
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
            text = BUTTON_EMOJI
            textSize = EMOJI_TEXT_SIZE_SP
            gravity = Gravity.CENTER
        }
        val buttonBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(BUTTON_COLOR)
        }
        return FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(buttonSizePx, buttonSizePx)
            background = buttonBackground
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            addView(emojiLabel)
            setOnClickListener { onButtonClicked() }
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
