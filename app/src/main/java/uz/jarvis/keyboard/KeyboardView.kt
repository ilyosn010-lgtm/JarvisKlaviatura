package uz.jarvis.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class KeyboardView(ctx: Context) : View(ctx) {

    interface Listener {
        fun onKey(key: Key)
    }

    var listener: Listener? = null

    var rows: List<Row> = emptyList()
        set(value) {
            field = value
            rects = null
            requestLayout()
            invalidate()
        }

    var shiftState: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    private class Pal(val top: Int, val bot: Int, val text: Int, val stroke: Int)

    private val d = resources.displayMetrics.density
    private val sp = resources.displayMetrics.scaledDensity
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var rects: List<List<RectF>>? = null
    private var pressedR = -1
    private var pressedC = -1
    private val handler = Handler(Looper.getMainLooper())
    private val gap = 5f * d
    private val pad = 6f * d
    private val rad = 9f * d

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val k = keyAt(pressedR, pressedC)
            if (k != null && k.type == KType.DEL) {
                listener?.onKey(k)
                handler.postDelayed(this, 55)
            }
        }
    }

    private fun keyAt(r: Int, c: Int): Key? = rows.getOrNull(r)?.keys?.getOrNull(c)

    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w)
        var total = pad * 2
        for (r in rows) total += r.heightDp * d + gap
        setMeasuredDimension(width, total.toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        rects = null
    }

    private fun computeRects(): List<List<RectF>> {
        val res = ArrayList<List<RectF>>()
        var y = pad
        for (r in rows) {
            val rh = r.heightDp * d
            var totalW = 0f
            for (k in r.keys) totalW += k.weight
            val avail = width - 2 * pad - gap * (r.keys.size - 1)
            val unit = avail / totalW
            var x = pad
            val list = ArrayList<RectF>()
            for (k in r.keys) {
                val kw = unit * k.weight
                list.add(RectF(x, y, x + kw, y + rh))
                x += kw + gap
            }
            res.add(list)
            y += rh + gap
        }
        return res
    }

    private fun getRects(): List<List<RectF>> {
        val cur = rects
        if (cur != null) return cur
        val n = computeRects()
        rects = n
        return n
    }

    private fun lighten(c: Int): Int {
        val r = Color.red(c) + (255 - Color.red(c)) / 5
        val g = Color.green(c) + (255 - Color.green(c)) / 5
        val b = Color.blue(c) + (255 - Color.blue(c)) / 5
        return Color.rgb(r, g, b)
    }

    private fun palette(k: Key): Pal = when (k.style) {
        KStyle.NORMAL -> Pal(Color.rgb(54, 60, 72), Color.rgb(36, 41, 50), Color.WHITE, Color.rgb(80, 86, 100))
        KStyle.SPECIAL -> Pal(Color.rgb(66, 72, 84), Color.rgb(46, 51, 62), Color.WHITE, Color.rgb(90, 96, 110))
        KStyle.DIGIT -> Pal(Color.rgb(14, 70, 96), Color.rgb(8, 38, 60), Color.rgb(0, 229, 255), Color.rgb(0, 229, 255))
        KStyle.TEAL -> Pal(Color.rgb(26, 196, 175), Color.rgb(14, 150, 136), Color.rgb(10, 40, 36), Color.rgb(120, 240, 225))
        KStyle.NEWLETTER -> Pal(Color.rgb(150, 60, 200), Color.rgb(100, 30, 150), Color.WHITE, Color.rgb(190, 120, 255))
        KStyle.YELLOW -> Pal(Color.rgb(255, 222, 60), Color.rgb(255, 186, 0), Color.BLACK, Color.rgb(255, 240, 150))
        KStyle.CHIP -> Pal(Color.rgb(12, 90, 120), Color.rgb(8, 60, 90), Color.WHITE, Color.rgb(0, 229, 255))
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(Color.rgb(10, 14, 28))
        val rs = getRects()
        for (ri in rows.indices) {
            for (ci in rows[ri].keys.indices) {
                drawKey(c, rows[ri].keys[ci], rs[ri][ci], ri == pressedR && ci == pressedC)
            }
        }
    }

    private fun drawKey(c: Canvas, k: Key, r: RectF, pressed: Boolean) {
        var p = palette(k)
        if (k.type == KType.SHIFT && shiftState > 0) {
            p = Pal(Color.rgb(90, 245, 225), Color.rgb(30, 200, 180), Color.rgb(10, 40, 36), Color.WHITE)
        }
        val top = if (pressed) lighten(p.top) else p.top
        val bot = if (pressed) lighten(p.bot) else p.bot

        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(r.left, r.top, r.left, r.bottom, top, bot, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, rad, rad, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f * d
        paint.color = p.stroke
        c.drawRoundRect(r, rad, rad, paint)

        when (k.type) {
            KType.EMOJI -> drawSmiley(c, r.centerX(), r.centerY(), min(r.height(), r.width()) * 0.34f)
            KType.SHIFT, KType.DEL, KType.ENTER, KType.LANG, KType.SETTINGS -> drawIcon(c, k, r, p.text)
            else -> drawLabel(c, k, r, p.text)
        }
    }

    private fun drawLabel(c: Canvas, k: Key, r: RectF, color: Int) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        val isEmoji = k.type == KType.EMOJI_CHAR
        paint.typeface = if (isEmoji || k.type == KType.SPACE) Typeface.DEFAULT else Typeface.DEFAULT_BOLD
        paint.color = if (k.type == KType.SPACE) Color.rgb(170, 176, 190) else color
        val base = when {
            isEmoji -> 24f
            k.label.length <= 2 -> 22f
            else -> 14f
        }
        paint.textSize = base * sp
        val maxW = r.width() - 8 * d
        val w = paint.measureText(k.label)
        if (w > maxW && w > 0f) paint.textSize = paint.textSize * maxW / w
        val y = r.centerY() - (paint.descent() + paint.ascent()) / 2
        c.drawText(k.label, r.centerX(), y, paint)
    }

    private fun drawIcon(c: Canvas, k: Key, r: RectF, color: Int) {
        val cx = r.centerX()
        val cy = r.centerY()
        val s = min(r.height(), r.width()) * 0.26f
        paint.reset()
        paint.isAntiAlias = true
        paint.color = color
        paint.strokeWidth = 2.2f * d
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.style = Paint.Style.STROKE
        when (k.type) {
            KType.SHIFT -> {
                val path = Path()
                path.moveTo(cx, cy - s)
                path.lineTo(cx - s, cy)
                path.lineTo(cx - s * 0.45f, cy)
                path.lineTo(cx - s * 0.45f, cy + s * 0.85f)
                path.lineTo(cx + s * 0.45f, cy + s * 0.85f)
                path.lineTo(cx + s * 0.45f, cy)
                path.lineTo(cx + s, cy)
                path.close()
                paint.style = if (shiftState > 0) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
                c.drawPath(path, paint)
                if (shiftState == 2) {
                    paint.style = Paint.Style.STROKE
                    c.drawLine(cx - s * 0.45f, cy + s * 1.25f, cx + s * 0.45f, cy + s * 1.25f, paint)
                }
            }
            KType.DEL -> {
                val path = Path()
                path.moveTo(cx - s * 1.2f, cy)
                path.lineTo(cx - s * 0.4f, cy - s * 0.75f)
                path.lineTo(cx + s * 1.1f, cy - s * 0.75f)
                path.lineTo(cx + s * 1.1f, cy + s * 0.75f)
                path.lineTo(cx - s * 0.4f, cy + s * 0.75f)
                path.close()
                c.drawPath(path, paint)
                val a = s * 0.3f
                val mx = cx + s * 0.25f
                c.drawLine(mx - a, cy - a, mx + a, cy + a, paint)
                c.drawLine(mx - a, cy + a, mx + a, cy - a, paint)
            }
            KType.ENTER -> {
                val path = Path()
                path.moveTo(cx + s, cy - s * 0.7f)
                path.lineTo(cx + s, cy + s * 0.3f)
                path.lineTo(cx - s, cy + s * 0.3f)
                c.drawPath(path, paint)
                c.drawLine(cx - s, cy + s * 0.3f, cx - s * 0.35f, cy - s * 0.25f, paint)
                c.drawLine(cx - s, cy + s * 0.3f, cx - s * 0.35f, cy + s * 0.85f, paint)
            }
            KType.LANG -> {
                c.drawCircle(cx, cy, s, paint)
                c.drawOval(RectF(cx - s * 0.45f, cy - s, cx + s * 0.45f, cy + s), paint)
                c.drawLine(cx - s, cy, cx + s, cy, paint)
            }
            KType.SETTINGS -> {
                c.drawCircle(cx, cy, s * 0.5f, paint)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val x1 = cx + (s * 0.8f * cos(a)).toFloat()
                    val y1 = cy + (s * 0.8f * sin(a)).toFloat()
                    val x2 = cx + (s * 1.15f * cos(a)).toFloat()
                    val y2 = cy + (s * 1.15f * sin(a)).toFloat()
                    c.drawLine(x1, y1, x2, y2, paint)
                }
            }
            else -> {}
        }
    }

    private fun drawSmiley(c: Canvas, cx: Float, cy: Float, rr: Float) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 205, 0)
        c.drawCircle(cx, cy, rr, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * d
        paint.color = Color.rgb(214, 140, 0)
        c.drawCircle(cx, cy, rr, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(200, 255, 120, 110)
        c.drawCircle(cx - rr * 0.55f, cy + rr * 0.25f, rr * 0.2f, paint)
        c.drawCircle(cx + rr * 0.55f, cy + rr * 0.25f, rr * 0.2f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f * d
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(90, 45, 0)
        val ey = cy - rr * 0.25f
        c.drawArc(RectF(cx - rr * 0.62f, ey - rr * 0.2f, cx - rr * 0.2f, ey + rr * 0.2f), 200f, 140f, false, paint)
        c.drawArc(RectF(cx + rr * 0.2f, ey - rr * 0.2f, cx + rr * 0.62f, ey + rr * 0.2f), 200f, 140f, false, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(110, 40, 10)
        c.drawArc(RectF(cx - rr * 0.5f, cy - rr * 0.05f, cx + rr * 0.5f, cy + rr * 0.6f), 0f, 180f, true, paint)
        paint.color = Color.rgb(255, 110, 120)
        c.drawArc(RectF(cx - rr * 0.3f, cy + rr * 0.2f, cx + rr * 0.3f, cy + rr * 0.62f), 0f, 180f, true, paint)
    }

    private fun hit(x: Float, y: Float): IntArray? {
        val rs = getRects()
        val g = gap / 2
        for (ri in rs.indices) {
            for (ci in rs[ri].indices) {
                val r = rs[ri][ci]
                if (x >= r.left - g && x <= r.right + g && y >= r.top - g && y <= r.bottom + g) {
                    return intArrayOf(ri, ci)
                }
            }
        }
        return null
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val h = hit(e.x, e.y)
                val nr = h?.get(0) ?: -1
                val nc = h?.get(1) ?: -1
                if (nr != pressedR || nc != pressedC) {
                    handler.removeCallbacks(repeatRunnable)
                    pressedR = nr
                    pressedC = nc
                    val k = keyAt(nr, nc)
                    if (k != null) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (k.type == KType.DEL) {
                            listener?.onKey(k)
                            handler.postDelayed(repeatRunnable, 380)
                        }
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(repeatRunnable)
                val k = keyAt(pressedR, pressedC)
                pressedR = -1
                pressedC = -1
                invalidate()
                if (k != null && k.type != KType.DEL) listener?.onKey(k)
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(repeatRunnable)
                pressedR = -1
                pressedC = -1
                invalidate()
            }
        }
        return true
    }
}
