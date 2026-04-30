package com.example.customkeyboard.gif

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
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
 * Layout:
 *   ┌─────────────────────────────────┐
 *   │  🔍 Search GIFs...              │  ← search bar
 *   ├─────────────────────────────────┤
 *   │  [GIF] [GIF] [GIF]              │  ← 3-column scrollable grid
 *   │  [GIF] [GIF] [GIF]              │
 *   │  ...                            │
 *   │  [Load more]                    │
 *   └─────────────────────────────────┘
 *
 * Tapping a GIF calls [onGifSelected] with the full GIF URL.
 */
class GifPanelView(context: Context) : LinearLayout(context) {

    var onGifSelected: ((url: String) -> Unit)? = null

    private val mainHandler  = Handler(Looper.getMainLooper())
    private val executor     = Executors.newSingleThreadExecutor()

    private lateinit var searchBox:    EditText
    private lateinit var scrollView:   ScrollView
    private lateinit var gridLayout:   GridLayout
    private lateinit var loadMoreBtn:  Button
    private lateinit var statusLabel:  TextView

    private var currentQuery: String = ""
    private var currentPage:  Int    = 1
    private var hasNext:      Boolean = false
    private var isLoading:    Boolean = false

    private val COLS = 3

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1C1C1E"))
        buildUI()
        loadTrending(reset = true)
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private fun buildUI() {
        // Search bar
        val searchRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(Color.parseColor("#2C2C2E"))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        searchBox = EditText(context).apply {
            hint = "Search GIFs..."
            setHintTextColor(Color.parseColor("#8E8E93"))
            setTextColor(Color.WHITE)
            textSize = 14f
            background = null
            setSingleLine(true)
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            addTextChangedListener(object : TextWatcher {
                private var debounce: Runnable? = null
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    debounce?.let { mainHandler.removeCallbacks(it) }
                    val q = s?.toString()?.trim() ?: ""
                    debounce = Runnable {
                        currentQuery = q
                        if (q.isEmpty()) loadTrending(reset = true)
                        else loadSearch(q, reset = true)
                    }.also { mainHandler.postDelayed(it, 400) }
                }
            })
        }

        val clearBtn = TextView(context).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(dp(8), 0, 0, 0)
            setOnClickListener {
                searchBox.text.clear()
                currentQuery = ""
                loadTrending(reset = true)
            }
        }

        searchRow.addView(TextView(context).apply {
            text = "⌕"
            textSize = 18f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(0, 0, dp(8), 0)
        })
        searchRow.addView(searchBox)
        searchRow.addView(clearBtn)
        addView(searchRow)

        // Status label (loading / error)
        statusLabel = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(40))
        }
        addView(statusLabel)

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

        // Loading placeholder
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

    private fun showStatus(msg: String) {
        statusLabel.text = msg
        statusLabel.visibility = View.VISIBLE
    }

    private fun hideStatus() {
        statusLabel.visibility = View.GONE
    }

    private fun dp(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            resources.displayMetrics).toInt()
}
