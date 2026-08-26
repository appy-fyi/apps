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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import fyi.appy.inksend.giladkutiel.R
import fyi.appy.inksend.giladkutiel.data.model.StyleConfig
import kotlin.math.abs

private const val TAG = "InkSendOverlay"
private const val BUTTON_SIZE_DP = 56
private const val BUTTON_SPACING_DP = 12
private const val DEFAULT_MARGIN_END_PX = 48
private const val DEFAULT_MARGIN_BOTTOM_PX = 220
private const val EMOJI_TEXT_SIZE_SP = 26f
private const val FALLBACK_BUTTON_COLOR = 0xFF5B47E0.toInt()

/**
 * Inflates, positions, and tears down one floating overlay button per saved style via
 * [WindowManager]. All buttons live inside one draggable container, stacked vertically,
 * so a single drag moves the whole set together while each button keeps its own tap target.
 * Each button's icon is that style's configured emoji badge (falling back to a generic
 * icon when the style's badge is set to "None"); its background circle is tinted with that
 * style's own background color so buttons stay visually distinct at any list length. Styles
 * are read fresh from [stylesProvider] at [showOverlay] time so the buttons always reflect
 * the latest saved list; [refreshOverlay] rebuilds an already-visible overlay in place so
 * style edits made mid-display show up immediately too.
 */
class OverlayWindowManager(
    private val context: Context,
    private val stylesProvider: () -> List<StyleConfig>,
    private val onStyleClicked: (StyleConfig) -> Unit,
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
        val styles = stylesProvider()
        if (styles.isEmpty()) return

        val density = context.resources.displayMetrics.density
        val buttonSizePx = (BUTTON_SIZE_DP * density).toInt()
        val spacingPx = (BUTTON_SPACING_DP * density).toInt()
        val containerHeightPx = buttonSizePx * styles.size + spacingPx * (styles.size - 1)

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
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(buttonSizePx, containerHeightPx)
        }
        styles.forEachIndexed { index, style ->
            container.addView(
                createStyleButton(
                    buttonSizePx = buttonSizePx,
                    paddingPx = paddingPx,
                    backgroundColor = colorForStyle(style),
                    emoji = style.emoji,
                    onClick = { onStyleClicked(style) },
                    touchListener = dragListener,
                ),
            )
            if (index != styles.lastIndex) {
                container.addView(
                    View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(buttonSizePx, spacingPx)
                    },
                )
            }
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

    private fun colorForStyle(style: StyleConfig): Int =
        try {
            Color.parseColor(style.backgroundColorHex)
        } catch (_: IllegalArgumentException) {
            FALLBACK_BUTTON_COLOR
        }

    /**
     * A button's icon is its style's emoji when set, otherwise the generic fallback icon —
     * both layered in the same [buttonSizePx] square and centered identically so swapping
     * between them (as the user edits their badge choice) never shifts the tap target.
     */
    private fun createStyleButton(
        buttonSizePx: Int,
        paddingPx: Int,
        backgroundColor: Int,
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
        val buttonBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(backgroundColor)
        }
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(buttonSizePx, buttonSizePx)
            background = buttonBackground
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

    /**
     * Rebuilds the button set in place from the latest [stylesProvider] list — used when
     * styles are added, removed, or edited while the overlay is already visible, so changes
     * in Settings show up immediately instead of only on the next show/hide cycle. A no-op
     * when the overlay isn't currently showing.
     */
    fun refreshOverlay() {
        if (!isShowing) return
        hideOverlay()
        showOverlay()
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
