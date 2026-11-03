package uz.jarvis.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

class JarvisIME : InputMethodService(), KeyboardView.Listener {

    private enum class Page { LETTERS, SYMBOLS, EMOJI, NUMERIC }

    private var kv: KeyboardView? = null
    private lateinit var cm: ClipboardManager
    private var lang = Lang.LOTIN
    private var page = Page.LETTERS
    private var shift = 0
    private var lastShiftTap = 0L
    private var secure = false
    private var clip: String? = null
    private var dismissedClip: String? = null
    private var inputTypeNow = 0

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        readClip()
        dismissedClip = null
        refresh()
    }

    override fun onCreate() {
        super.onCreate()
        cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.addPrimaryClipChangedListener(clipListener)
        val saved = getSharedPreferences("jarvis", Context.MODE_PRIVATE).getInt("lang", 0)
        lang = Lang.values()[saved.coerceIn(0, Lang.values().size - 1)]
    }

    override fun onDestroy() {
        cm.removePrimaryClipChangedListener(clipListener)
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val v = KeyboardView(this)
        v.listener = this
        kv = v
        refresh()
        return v
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        inputTypeNow = info?.inputType ?: 0
        val cls = inputTypeNow and InputType.TYPE_MASK_CLASS
        val variation = inputTypeNow and InputType.TYPE_MASK_VARIATION

        val numeric = cls == InputType.TYPE_CLASS_NUMBER ||
            cls == InputType.TYPE_CLASS_PHONE ||
            cls == InputType.TYPE_CLASS_DATETIME

        secure = (cls == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)

        page = if (numeric) Page.NUMERIC else Page.LETTERS
        shift = 0
        readClip()
        autoCaps()
        refresh()
    }

    private fun readClip() {
        clip = null
        if (secure) return // parol / kod maydonida bufer o'qilmaydi
        try {
            val c = cm.primaryClip
            if (c != null && c.itemCount > 0) {
                val t = c.getItemAt(0).coerceToText(this)?.toString()
                if (!t.isNullOrBlank()) clip = t
            }
        } catch (e: Exception) {
            clip = null
        }
    }

    private fun refresh() {
        val v = kv ?: return
        val shown = if (page == Page.LETTERS && !secure && clip != null && clip != dismissedClip) clip else null
        v.rows = when (page) {
            Page.LETTERS -> Layouts.letters(lang, shown, secure)
            Page.SYMBOLS -> Layouts.symbols()
            Page.EMOJI -> Layouts.emoji()
            Page.NUMERIC -> Layouts.numeric()
        }
        v.shiftState = shift
    }

    private fun autoCaps() {
        if (shift == 2 || page != Page.LETTERS || secure) return
        val ic = currentInputConnection ?: return
        shift = if (ic.getCursorCapsMode(inputTypeNow) != 0) 1 else 0
    }

    override fun onKey(key: Key) {
        val ic = currentInputConnection ?: return
        when (key.type) {
            KType.CHAR, KType.EMOJI_CHAR -> typeText(ic, key.text)
            KType.DIGIT -> ic.commitText(key.text, 1)
            KType.SPACE -> {
                ic.commitText(" ", 1)
                autoCaps()
                refresh()
            }
            KType.DEL -> doDelete(ic)
            KType.ENTER -> doEnter(ic)
            KType.SHIFT -> {
                val now = System.currentTimeMillis()
                shift = when {
                    shift == 0 -> 1
                    shift == 1 && now - lastShiftTap < 400 -> 2
                    else -> 0
                }
                lastShiftTap = now
                refresh()
            }
            KType.LANG -> {
                lang = Lang.values()[(lang.ordinal + 1) % Lang.values().size]
                getSharedPreferences("jarvis", Context.MODE_PRIVATE).edit().putInt("lang", lang.ordinal).apply()
                shift = 0
                autoCaps()
                refresh()
            }
            KType.SYM -> {
                page = Page.SYMBOLS
                refresh()
            }
            KType.ABC -> {
                page = Page.LETTERS
                refresh()
            }
            KType.EMOJI -> {
                page = Page.EMOJI
                refresh()
            }
            KType.SETTINGS -> {
                val i = Intent(this, SettingsActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            }
            KType.CONV_NEW -> convert(ic, Converter.TO_NEW)
            KType.CONV_OLD -> convert(ic, Converter.TO_OLD)
            KType.CONV_CYR -> convert(ic, Converter.TO_CYR)
            KType.PASTE -> {
                ic.commitText(key.text, 1)
                dismissedClip = key.text
                refresh()
            }
        }
    }

    private fun typeText(ic: InputConnection, t: String) {
        val out = when (shift) {
            0 -> t
            1 -> t.replaceFirstChar { it.uppercase() }
            else -> t.uppercase()
        }
        ic.commitText(out, 1)
        if (shift == 1) shift = 0
        autoCaps()
        refresh()
    }

    private fun doDelete(ic: InputConnection) {
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
        val old = shift
        autoCaps()
        if (old != shift) refresh()
    }

    private fun doEnter(ic: InputConnection) {
        val opts = currentInputEditorInfo?.imeOptions ?: 0
        val action = opts and EditorInfo.IME_MASK_ACTION
        val noEnter = (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnter && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
        val old = shift
        autoCaps()
        if (old != shift) refresh()
    }

    /** Belgilangan matnni, bo'lmasa maydondagi hamma matnni o'giradi. */
    private fun convert(ic: InputConnection, mode: Int) {
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) {
            ic.commitText(Converter.apply(mode, sel.toString()), 1)
            return
        }
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        val text: String = extracted?.text?.toString()
            ?: ((ic.getTextBeforeCursor(10000, 0)?.toString() ?: "") +
                (ic.getTextAfterCursor(10000, 0)?.toString() ?: ""))
        if (text.isEmpty()) return
        val converted = Converter.apply(mode, text)
        ic.beginBatchEdit()
        ic.finishComposingText()
        ic.setSelection(0, text.length)
        ic.commitText(converted, 1)
        ic.endBatchEdit()
    }
}
