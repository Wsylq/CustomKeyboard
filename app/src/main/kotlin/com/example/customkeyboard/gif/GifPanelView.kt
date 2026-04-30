package com.example.customkeyboard.gif

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.util.concurrent.Executors

/**
 * Full GIF picker panel — replaces the emoji grid when the GIF tab is selected.
 *
 * The search bar is a non-focusable display-only TextView. Typing is driven
 * externally via [appendSearchChar] / [deleteSearchChar] so the host keyboard
 * view can feed key presses into it without spawning a second keyboard.
 */
class GifPanelView(context: Context) : LinearLayout(context) {

    var onGifSelected: ((url: String) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor    = Executors.newSingleThreadExecutor()

    private lateinit var searchDisplay: TextView   // shows current query (not focusable)
    private lateinit var scrollView:    ScrollView
    private lateinit var gridLayout:    GridLayout
    private lateinit var loadMoreBtn:   Button
    private lateinit var statusLabel:   TextView
    private lateinit var retryBtn:      Button

    private var currentQuery: String  = ""
    private var currentPage:  Int     = 1
    private var hasNext:      Boolean = false
    private var isLoading:    Boolean = false

    private val COLS = 3

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        buildUI()
        loadTrending(reset = true)
    }

    // ── External input API (called by KeyboardView) ───────────────────────────

    /** Append a character to the search query (called when user taps a letter key). */
    fun appendSearchChar(ch: Char) {
        currentQuery += ch
        updateSearchDisplay()
        scheduleSearch()
    }

    /** Delete the last character from the search query (called on backspace). */
    fun deleteSearchChar() {
        if (currentQuery.isNotEmpty()) {
            currentQuery = currentQuery.dropLast(1)
            updateSearchDisplay()
            scheduleSearch()
        }
    }

    /** Clear the search query entirely. */
    fun clearSearch() {
        currentQuery = ""
        updateSearchDisplay()
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

    // ── UI construction ───────────────────────────────────────────────────────

    private fun buildUI() {
        // Search bar (display only — not focusable)
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
        addView(searchRow)

        // Status label (loading / error)
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

        // Retry button (shown on error)
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

        // Scrollable grid
        scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val gridWrapper = LinearLayout(context).apply {
            orientation = VERTICAL
        }

        gridLayout = GridLayout(context).apply {
            columnCount = COLS
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        gridWrapper.addView(gridLayout)

        // Load more button
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
            showStatus("No GIFs found")
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
