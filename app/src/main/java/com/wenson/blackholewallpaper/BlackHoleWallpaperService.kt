package com.wenson.blackholewallpaper

import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.MotionEvent
import kotlin.math.*

// Personal Black Hole — live wallpaper Android
// Mesma ideia do protótipo web: o buraco cresce enquanto você "trabalha"
// (fica sem tocar na tela / sem eventos de interação) e encolhe quando
// você toca a tela (equivalente ao botão "pausei").
//
// Ajuste esses dois valores pra mudar o ritmo do ciclo:
private const val WORK_PERIOD_SEC = 55 * 60f   // tempo pra crescer até o máximo
private const val BREAK_SEC = 5 * 60f          // tempo de encolhimento no fim do ciclo
private const val CYCLE_SEC = WORK_PERIOD_SEC + BREAK_SEC

class BlackHoleWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = BlackHoleEngine()

    inner class BlackHoleEngine : Engine() {
        private val handler = android.os.Handler(android.os.Looper.getMainLooper())
        private var visible = false
        private var width = 0
        private var height = 0

        private val startTime = System.currentTimeMillis()
        private var sessionStart = startTime
        private var lastTouch = startTime

        private val drawRunnable = Runnable { drawFrame() }

        private val bgPaint = Paint().apply { color = Color.parseColor("#05060A") }
        private val textPaint = Paint().apply {
            color = Color.parseColor("#7FD0FF")
            textSize = 30f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        private val codeLines = listOf(
            "const growth = clamp(phase / workSec, 0, 1);",
            "function trackSession(startedAt) {",
            "  return Date.now() - startedAt;",
            "}",
            "// TODO: lembrar de levantar da cadeira",
            "class Session extends Component {",
            "  render() { return this.state.hole; }",
            "}",
            "// horizonte de eventos: r = 2GM/c²",
        )

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            width = w
            height = h
            super.onSurfaceChanged(holder, format, w, h)
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (v) handler.post(drawRunnable) else handler.removeCallbacks(drawRunnable)
        }

        override fun onTouchEvent(event: MotionEvent) {
            lastTouch = System.currentTimeMillis()
            if (event.action == MotionEvent.ACTION_UP) {
                // toque = "pausei": reinicia o ciclo de crescimento
                sessionStart = System.currentTimeMillis()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun growthFactor(now: Long): Float {
            val elapsedSec = (now - sessionStart) / 1000f
            val phase = elapsedSec % CYCLE_SEC
            var g = if (phase < WORK_PERIOD_SEC) {
                phase / WORK_PERIOD_SEC
            } else {
                1f - (phase - WORK_PERIOD_SEC) / BREAK_SEC
            }
            val idleSec = (now - lastTouch) / 1000f
            val idleFade = max(0f, 1f - idleSec / 8f)
            g *= 0.15f + 0.85f * idleFade
            return g.coerceIn(0f, 1f)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null && width > 0 && height > 0) {
                    render(canvas)
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
            handler.removeCallbacks(drawRunnable)
            if (visible) handler.postDelayed(drawRunnable, 33) // ~30fps
        }

        private fun render(canvas: Canvas) {
            val now = System.currentTimeMillis()
            val g = growthFactor(now)

            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val cx = width / 2f
            val cy = height / 2f
            val baseRadius = 10f + g * min(width, height) * 0.30f

            // fundo de "código" distorcido perto da borda (lensing simplificado)
            var y = 60f
            for (line in codeLines) {
                drawWarpedLine(canvas, line, 40f, y, cx, cy, baseRadius)
                y += 46f
            }

            drawAccretionArc(canvas, cx, cy, baseRadius * 3.1f, baseRadius * 1.35f, 0.34f, true)

            // halo
            val glow = RadialGradient(
                cx, cy, baseRadius * 2.1f,
                intArrayOf(
                    Color.argb(255, 0, 0, 0),
                    Color.argb(46, 255, 190, 120),
                    Color.argb(26, 140, 180, 255),
                    Color.argb(0, 0, 0, 0)
                ),
                floatArrayOf(0f, 0.5f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
            val glowPaint = Paint().apply { shader = glow; isAntiAlias = true }
            canvas.drawCircle(cx, cy, baseRadius * 2.1f, glowPaint)

            // sombra do horizonte de eventos
            val shadowPaint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
            canvas.drawCircle(cx, cy, baseRadius, shadowPaint)

            // anel de fóton
            val ringPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = Color.argb((190 + g * 50).toInt(), 255, 235, 210)
                isAntiAlias = true
                maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(cx, cy, baseRadius * 1.04f, ringPaint)

            drawAccretionArc(canvas, cx, cy, baseRadius * 3.1f, baseRadius * 1.35f, 0.34f, false)
        }

        // desenha metade do disco (atrás/cima ou frente/baixo), com
        // Doppler beaming: lado esquerdo mais brilhante (se aproximando)
        private fun drawAccretionArc(
            canvas: Canvas, cx: Float, cy: Float,
            outer: Float, inner: Float, tilt: Float, back: Boolean
        ) {
            canvas.save()
            if (back) {
                canvas.clipRect(cx - outer, cy - outer, cx + outer, cy)
            } else {
                canvas.clipRect(cx - outer, cy, cx + outer, cy + outer)
            }

            val steps = 90
            val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
            val path = Path()
            for (i in 0 until steps) {
                val t0 = (i / steps.toFloat()) * 2 * PI.toFloat()
                val t1 = ((i + 1) / steps.toFloat()) * 2 * PI.toFloat()
                val mid = (t0 + t1) / 2
                val side = cos(mid)
                val beaming = 1 - side * 0.55f
                val alpha = (0.35f * beaming * 255).toInt().coerceIn(0, 255)
                val color = if (side > 0) Color.argb(alpha, 255, 150, 90)
                else Color.argb(alpha, 255, 225, 190)

                path.reset()
                val outerRect = RectF(cx - outer, cy - outer * tilt, cx + outer, cy + outer * tilt)
                val innerRect = RectF(cx - inner, cy - inner * tilt, cx + inner, cy + inner * tilt)
                path.arcTo(outerRect, Math.toDegrees(t0.toDouble()).toFloat(), Math.toDegrees((t1 - t0).toDouble()).toFloat(), false)
                path.arcTo(innerRect, Math.toDegrees(t1.toDouble()).toFloat(), Math.toDegrees((t0 - t1).toDouble()).toFloat(), false)
                path.close()
                paint.color = color
                canvas.drawPath(path, paint)
            }

            val edgePaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.argb(100, 255, 240, 220)
                isAntiAlias = true
            }
            canvas.drawOval(
                RectF(cx - inner * 1.05f, cy - inner * tilt * 1.05f, cx + inner * 1.05f, cy + inner * tilt * 1.05f),
                edgePaint
            )
            canvas.restore()
        }

        private fun drawWarpedLine(canvas: Canvas, text: String, startX: Float, y: Float, cx: Float, cy: Float, radius: Float) {
            var x = startX
            val charW = 17f
            val lensRange = radius * 4.2f
            for (ch in text) {
                if (x > width - 40) break
                val px = x + charW / 2
                val py = y
                val dx = px - cx
                val dy = py - cy
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

                var offset = 0f
                var alpha = 255
                var scale = 1f
                if (dist < lensRange) {
                    val t = 1f - dist / lensRange
                    offset = t * t * radius * 1.4f
                    scale = 1f + t * 1.2f
                    alpha = (255 * (1f - t * 1.15f).coerceIn(0f, 1f)).toInt()
                }
                val dirX = dx / dist
                val dirY = dy / dist
                val drawX = px + dirX * offset
                val drawY = py + dirY * offset

                textPaint.alpha = alpha
                textPaint.textSize = 30f * scale
                canvas.drawText(ch.toString(), drawX, drawY, textPaint)
                x += charW
            }
        }
    }
}
