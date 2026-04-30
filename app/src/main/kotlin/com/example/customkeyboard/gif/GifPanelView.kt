package com.example.customkeyboard.gif

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.util.concurrent.Executors

/**
 * GIF picker panel with a built-in mini keyboard for search.
 *
 * Tapping the search bar reveals a compact QWERTY strip at the bottom.
 * No system EditText focus is used — avoids spawning a second keyboard.
 */
class GifPanelView(context: Context) : LinearLayout(context) {

    var onGifSelected: ((url: String) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor    = Executors.newSingleThreadExecutor()

    private lateinit var searchDisplay:  TextView
    private lateinit var scrollView:     ScrollView
    private lateinit var gridLayout:     GridLayout
    private lateinit var loadMoreBtn:    Button
    private lateinit var statusLabel:    TextView
    private lateinit var retryBtn:       Button
    private lateinit var miniKeyboard:   LinearLayout

    private var currentQuery: String  = ""
    private var currentPage:  Int     = 1
    private var hasNext:      Boolean = false
    private var isLoading:    Boolean = false
    private var keyboardOpen: Boolean = false

    private val COLS = 3

    // Mini QWERTY rows
    private val ROW1 = "QWERTYUIOP"
    private val ROW2 = "ASDFGHJKL"
    private val ROW3 = "ZXCVBNM"

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        buildUI()
        loadTrending(reset = true)
    }

    // ── UI construction ───────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private fun buildUI() {
        // ── Search bar ──
        val searchRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(Color.parseColor("#2C2C2E"))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        searchDisplay = TextView(context).apply {
            text = "Search GIFs…"
            setTextColor(Color.parseColor("#8E8E93"))
            textSize = 14f
            isFocusable = false
            isFocusableInTouchMode = false
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val clearBtn = TextView(context).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(dp(8), 0, 0, 0)
            setOnClickListener { clearSearch() }
        }

        searchRow.addView(TextView(context).apply {
            text = "⌕"
            textSize = 18f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(0, 0, dp(8), 0)
        })
        searchRow.addView(searchDisplay)
        searchRow.addView(clearBtn)

        // Tap anywhere on the search row to open/close the mini keyboard
        searchRow.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) toggleMiniKeyboard()
            true
        }
        addView(searchRow)

        // ── Status label ──
        statusLabel = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.topMargin = dp(8)
            }
        }
        addView(statusLabel)

        // ── Retry button ──
        retryBtn = Button(context).apply {
            text = "Retry"
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#3A3A3C"))
            visibility = View.GONE
            layoutParams = LayoutParams(dp(100), dp(36)).also {
                it.gravity = Gravity.CENTER_HORIZONTAL
                it.topMargin = dp(8)
                it.bottomMargin = dp(8)
            }
            setOnClickListener {
                retryBtn.visibility = View.GONE
                if (currentQuery.isEmpty()) loadTrending(reset = true)
                else loadSearch(currentQuery, reset = true)
            }
        }
        addView(retryBtn)

        // ── Scrollable GIF grid ──
        scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val gridWrapper = LinearLayout(context).apply { orientation = VERTICAL }
        gridLayout = GridLayout(context).apply {
            columnCount = COLS
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        gridWrapper.addView(gridLayout)
        loadMoreBtn = Button(context).apply {
            text = "Load more"
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#3A3A3C"))
            visibility = View.GONE
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40)).also {
                it.topMargin = dp(4); it.bottomMargin = dp(4)
                it.leftMargin = dp(8); it.rightMargin = dp(8)
            }
            setOnClickListener { loadNextPage() }
        }
        gridWrapper.addView(loadMoreBtn)
        scrollView.addView(gridWrapper)
        addView(scrollView)

        // ── Mini keyboard (hidden by default) ──
        miniKeyboard = buildMiniKeyboard()
        miniKeyboard.visibility = View.GONE
        addView(miniKeyboard)
    }

    // ── Mini keyboard ─────────────────────────────────────────────────────────

    private fun buildMiniKeyboard(): LinearLayout {
        val kb = LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundColor(Color.parseColor("#1C1C1E"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(0, dp(4), 0, dp(4))
        }

        fun makeRow(chars: String, extraPadDp: Int = 0): LinearLayout {
            return LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40)).also {
                    it.bottomMargin = dp(3)
                }
                setPadding(dp(extraPadDp), 0, dp(extraPadDp), 0)
                chars.forEach { ch ->
                    addView(makeMiniKey(ch.toString()) { appendSearchChar(ch.lowercaseChar()) })
                }
            }
        }

        kb.addView(makeRow(ROW1))
        kb.addView(makeRow(ROW2, 8))

        // Row 3: letters + backspace
        val row3 = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40)).also {
                it.bottomMargin = dp(3)
            }
            setPadding(dp(4), 0, dp(4), 0)
        }
        ROW3.forEach { ch ->
            row3.addView(makeMiniKey(ch.toString()) { appendSearchChar(ch.lowercaseChar()) })
        }
        row3.addView(makeMiniKey("⌫", widthDp = 44, color = Color.parseColor("#3A3A3C")) {
            deleteSearchChar()
        })
        kb.addView(row3)

        // Bottom row: space + done
        val bottomRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40))
            setPadding(dp(4), 0, dp(4), 0)
        }
        bottomRow.addView(makeMiniKey("space", widthWeight = 3f, color = Color.parseColor("#3A3A3C")) {
            appendSearchChar(' ')
        })
        bottomRow.addView(makeMiniKey("✕ Done", widthWeight = 1.5f, color = Color.parseColor("#FF6B8A")) {
            toggleMiniKeyboard()
        })
        kb.addView(bottomRow)

        return kb
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeMiniKey(
        label: String,
        widthDp: Int = 0,
        widthWeight: Float = 1f,
        color: Int = Color.parseColor("#2C2C2E"),
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            text = label
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(color)
            isFocusable = false
            isFocusableInTouchMode = false

            val margin = dp(2)
            layoutParams = if (widthDp > 0) {
                LinearLayout.LayoutParams(dp(widthDp), LinearLayout.LayoutParams.MATCH_PARENT).also {
                    it.setMargins(margin, 0, margin, 0)
                }
            } else {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, widthWeight).also {
                    it.setMargins(margin, 0, margin, 0)
                }
            }

            // Use touch instead of click to avoid focus issues in IME
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) onClick()
                true
            }
        }
    }

    private fun toggleMiniKeyboard() {
        keyboardOpen = !keyboardOpen
        miniKeyboard.visibility = if (keyboardOpen) View.VISIBLE else View.GONE
        // Tint the search bar to indicate active state
        searchDisplay.setTextColor(
            if (keyboardOpen) Color.WHITE else
                if (currentQuery.isEmpty()) Color.parseColor("#8E8E93") else Color.WHITE
        )
    }

    // ── Search input ──────────────────────────────────────────────────────────

    fun appendSearchChar(ch: Char) {
        currentQuery += ch
        updateSearchDisplay()
        scheduleSearch()
    }

    fun deleteSearchChar() {
        if (currentQuery.isNotEmpty()) {
            currentQuery = currentQuery.dropLast(1)
            updateSearchDisplay()
            scheduleSearch()
        }
    }

    fun clearSearch() {
        currentQuery = ""
        updateSearchDisplay()
        if (keyboardOpen) toggleMiniKeyboard()
        loadTrending(reset = true)
    }

    private var searchDebounce: Runnable? = null
    private fun scheduleSearch() {
        searchDebounce?.let { mainHandler.removeCallbacks(it) }
        searchDebounce = Runnable {
            if (currentQuery.isEmpty()) loadTrending(reset = true)
            else loadSearch(currentQuery, reset = true)
        }.also { mainHandler.postDelayed(it, 400) }
    }

    private fun updateSearchDisplay() {
        searchDisplay.text = if (currentQuery.isEmpty()) "Search GIFs…" else currentQuery
        searchDisplay.setTextColor(
            if (currentQuery.isEmpty()) Color.parseColor("#8E8E93") else Color.WHITE
        )
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private fun loadTrending(reset: Boolean) {
        if (reset) { currentPage = 1; clearGrid() }
        setLoading(true)
        executor.execute {
            val page = KlipyRepository.trending(currentPage)
            mainHandler.post { onPageLoaded(page, reset) }
        }
    }

    private fun loadSearch(query: String, reset: Boolean) {
        if (reset) { currentPage = 1; clearGrid() }
        setLoading(true)
        executor.execute {
            val page = KlipyRepository.search(query, currentPage)
            mainHandler.post { onPageLoaded(page, reset) }
        }
    }

    private fun loadNextPage() {
        currentPage++
        if (currentQuery.isEmpty()) loadTrending(reset = false)
        else loadSearch(currentQuery, reset = false)
    }

    private fun onPageLoaded(page: KlipyRepository.GifPage, reset: Boolean) {
        setLoading(false)
        if (page.error != null) {
            showStatus("Failed to load GIFs\n${page.error}", showRetry = true)
            return
        }
        if (page.items.isEmpty() && reset) {
            showStatus(if (currentQuery.isEmpty()) "No trending GIFs" else "No results for \"$currentQuery\"")
            return
        }
        hideStatus()
        hasNext = page.hasNext
        loadMoreBtn.visibility = if (hasNext) View.VISIBLE else View.GONE
        page.items.forEach { addGifCell(it) }
    }

    // ── Grid cell ─────────────────────────────────────────────────────────────

    private fun addGifCell(item: KlipyRepository.GifItem) {
        val cellSize = (resources.displayMetrics.widthPixels - dp(4) * (COLS + 1)) / COLS

        val cell = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#2C2C2E"))
            val spec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            layoutParams = GridLayout.LayoutParams(spec, spec).also {
                it.width  = cellSize
                it.height = cellSize
                it.setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            setOnClickListener { onGifSelected?.invoke(item.fullUrl) }
        }

        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        cell.addView(imageView)

        val placeholder = View(context).apply {
            setBackgroundColor(Color.parseColor("#3A3A3C"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        cell.addView(placeholder)

        Glide.with(context)
            .asGif()
            .load(item.previewUrl)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .placeholder(ColorDrawable(Color.parseColor("#3A3A3C")))
            .override(cellSize, cellSize)
            .centerCrop()
            .into(object : CustomTarget<GifDrawable>(cellSize, cellSize) {
                override fun onResourceReady(resource: GifDrawable, t: Transition<in GifDrawable>?) {
                    placeholder.visibility = View.GONE
                    imageView.setImageDrawable(resource)
                    resource.start()
                }
                override fun onLoadCleared(p: android.graphics.drawable.Drawable?) {
                    imageView.setImageDrawable(null)
                }
            })

        gridLayout.addView(cell)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun clearGrid() {
        gridLayout.removeAllViews()
        loadMoreBtn.visibility = View.GONE
        scrollView.scrollTo(0, 0)
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        if (loading) showStatus("Loading…") else hideStatus()
    }

    private fun showStatus(msg: String, showRetry: Boolean = false) {
        statusLabel.text = msg
        statusLabel.visibility = View.VISIBLE
        retryBtn.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    private fun hideStatus() {
        statusLabel.visibility = View.GONE
        retryBtn.visibility = View.GONE
    }

    private fun dp(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            resources.displayMetrics).toInt()
}
