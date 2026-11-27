package com.chethan616.clearpdf.ui.viewer

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.chethan616.clearpdf.R
import com.chethan616.clearpdf.ui.AppViewModel
import com.chethan616.clearpdf.ui.BarIcon
import com.chethan616.clearpdf.ui.LucideIcons
import com.chethan616.clearpdf.ui.OpenDocument
import com.chethan616.clearpdf.ui.pdfColors
import kotlin.math.roundToInt

/** The fixed groups of a PDF's More list, in the order they are always shown. */
enum class ToolGroup(@StringRes val label: Int) {
    EditAndMarkUp(R.string.tool_group_edit),
    Pages(R.string.tool_group_pages),
    Convert(R.string.tool_group_convert),
    Security(R.string.tool_group_security),
}

/**
 * A tool that can sit on the viewer's bottom toolbar or in More: [label] on the bar, [title] in
 * More, [subTools] when it opens a row of sub-tools (the bar's corner mark), and [group], the
 * More list's section (null for the tools the header menu owns and for an office document's own).
 */
enum class QuickTool(
    val id: String,
    @StringRes val label: Int,
    @StringRes val title: Int,
    val icon: ImageVector,
    val hue: Color,
    val subTools: Boolean = false,
    val group: ToolGroup? = null,
) {
    Edit("edit", R.string.common_tool_edit_pdf, R.string.common_tool_edit_pdf, LucideIcons.FilePenLine, ToolHues.edit, group = ToolGroup.EditAndMarkUp),
    Comment("comment", R.string.common_tool_comment, R.string.common_tool_comment, LucideIcons.MessageSquareText, ToolHues.comment, subTools = true, group = ToolGroup.EditAndMarkUp),
    Highlight("highlight", R.string.common_tool_highlight, R.string.common_tool_highlight, LucideIcons.Highlighter, ToolHues.text, subTools = true, group = ToolGroup.EditAndMarkUp),
    Draw("draw", R.string.common_tool_draw, R.string.common_tool_draw, LucideIcons.PenLine, ToolHues.sign, group = ToolGroup.EditAndMarkUp),
    FillSign("fillsign", R.string.common_tool_fill_sign, R.string.common_tool_fill_sign, LucideIcons.Signature, ToolHues.sign, subTools = true, group = ToolGroup.EditAndMarkUp),
    Export("export", R.string.common_tool_export, R.string.common_tool_export, LucideIcons.FileOutput, ToolHues.export, group = ToolGroup.Convert),
    Combine("combine", R.string.toolbar_label_combine, R.string.common_tool_combine, LucideIcons.Combine, ToolHues.combine, group = ToolGroup.Pages),
    Compress("compress", R.string.toolbar_label_compress, R.string.common_tool_compress, LucideIcons.FileArchive, ToolHues.compress, group = ToolGroup.Convert),
    Protect("protect", R.string.tool_encryption, R.string.tool_encryption, LucideIcons.FileLock2, ToolHues.protect, group = ToolGroup.Security),
    Organize("organize", R.string.toolbar_label_organize, R.string.common_tool_organize, LucideIcons.LayoutGrid, ToolHues.organize, group = ToolGroup.Pages),
    Crop("crop", R.string.toolbar_label_crop, R.string.common_tool_crop, LucideIcons.Crop, ToolHues.crop, group = ToolGroup.Pages),
    Ocr("ocr", R.string.toolbar_label_recognize, R.string.common_tool_recognize_text, LucideIcons.ScanText, ToolHues.ocr, group = ToolGroup.Convert),
    Create("create", R.string.common_tool_create_pdf, R.string.common_tool_create_pdf, LucideIcons.FilePlus2, ToolHues.create, group = ToolGroup.Pages),
    Text("text", R.string.toolbar_label_add_text, R.string.toolbar_tool_text, LucideIcons.Type, ToolHues.text, group = ToolGroup.EditAndMarkUp),
    Scan("scan", R.string.common_tool_scan, R.string.common_tool_scan, LucideIcons.Camera, ToolHues.scan, group = ToolGroup.Pages),
    ReadAloud("readaloud", R.string.common_tool_read_aloud, R.string.common_tool_read_aloud, LucideIcons.Volume2, ToolHues.read),

    // office documents' own tools (their engines: WordCraft, DeckCraft)
    Present("present", R.string.office_present, R.string.office_present, LucideIcons.Presentation, ToolHues.read),
    ExportPdf("exportpdf", R.string.office_export_pdf, R.string.office_export_pdf, LucideIcons.FilePdf, ToolHues.export),
    SaveAs("saveas", R.string.office_save_as, R.string.office_save_as, LucideIcons.SaveAs, ToolHues.export),
    Print("print", R.string.sheet_print, R.string.sheet_print, LucideIcons.Printer, ToolHues.export);

    companion object {
        /** The bar as it comes: the reference's default tools and order, the five a phone's bar holds (Export is in More tools). */
        val DEFAULT_BAR = listOf(Edit, Comment, Highlight, Draw, FillSign)

        /** A Word document's bar: its engine reads it aloud, exports and converts it, and prints it. */
        val WORD = listOf(Edit, ReadAloud, ExportPdf, SaveAs, Print)

        /** A presentation's bar: a full-screen show first. */
        val SLIDES = listOf(Present, ReadAloud, ExportPdf, SaveAs, Print)

        /** The bar for [kind]: an office document's own tools, else the PDF bar as arranged. */
        fun barFor(kind: ToolDocKind, arranged: List<QuickTool>): List<QuickTool> = when (kind) {
            ToolDocKind.Slides -> SLIDES
            ToolDocKind.Word -> WORD
            ToolDocKind.Pdf -> arranged
        }

        /** The bar for [doc]: an office document's own tools, else the PDF bar as arranged. */
        fun barFor(doc: com.chethan616.clearpdf.ui.OpenDocument, arranged: List<QuickTool>): List<QuickTool> =
            barFor(docKind(doc), arranged)

        fun of(id: String): QuickTool? = entries.firstOrNull { it.id == id }
    }
}

/** What kind of document a tool list is being built for. */
enum class ToolDocKind { Pdf, Word, Slides }

fun docKind(doc: com.chethan616.clearpdf.ui.OpenDocument): ToolDocKind = when {
    doc.isSlides -> ToolDocKind.Slides
    doc.isOffice -> ToolDocKind.Word
    else -> ToolDocKind.Pdf
}

/** Tools only an office document has: never in a PDF's dock or lists. */
private val OfficeOnlyTools = setOf(QuickTool.Present, QuickTool.ExportPdf, QuickTool.SaveAs)

/**
 * The tools the header (More) menu owns. The dock may still hold them; a PDF's More list never
 * does, so no action has two homes.
 */
private val HeaderOwnedTools = setOf(QuickTool.Edit, QuickTool.ReadAloud, QuickTool.Print)

fun QuickTool.appliesTo(kind: ToolDocKind): Boolean = kind != ToolDocKind.Pdf || this !in OfficeOnlyTools

/** The dock's tools: an office document's fixed bar, else the arranged one, as many as [slots] hold. */
fun dockTools(kind: ToolDocKind, arranged: List<QuickTool>, slots: Int): List<QuickTool> =
    QuickTool.barFor(kind, arranged).filter { it.appliesTo(kind) }.distinct().take(slots)

/** The header menu's own tool rows, in a fixed order, without the tools the dock already shows. */
fun headerTools(kind: ToolDocKind, dock: List<QuickTool>): List<QuickTool> = when (kind) {
    ToolDocKind.Pdf -> listOf(QuickTool.Edit, QuickTool.ReadAloud, QuickTool.Print).filter { it !in dock }
    else -> QuickTool.barFor(kind, emptyList()).filter { it !in dock }
}

/**
 * A PDF's More list: every tool that applies, not on the dock and not in the header menu, in the
 * fixed group order and, inside a group, in declaration order. Never the user's arrangement.
 */
fun moreTools(kind: ToolDocKind, arranged: List<QuickTool>, slots: Int): List<QuickTool> {
    val dock = dockTools(kind, arranged, slots)
    return when (kind) {
        ToolDocKind.Pdf -> ToolGroup.entries.flatMap { group ->
            QuickTool.entries.filter { it.group == group && it !in dock && it !in HeaderOwnedTools }
        }
        // an office bar does not fit a narrow dock: the header keeps what is left, so the two
        // lists never show the same tool
        else -> QuickTool.barFor(kind, emptyList()).filter { it !in dock && it !in headerTools(kind, dock) }
    }
}

/** Where every tool of [kind] appears: the dock, the header menu's rows, and the More list. */
class ToolLayout(val dock: List<QuickTool>, val header: List<QuickTool>, val more: List<QuickTool>)

fun toolLayout(kind: ToolDocKind, arranged: List<QuickTool>, slots: Int): ToolLayout {
    val dock = dockTools(kind, arranged, slots)
    return ToolLayout(dock, headerTools(kind, dock), moreTools(kind, arranged, slots))
}

/** How many tools fit on the bar beside More at a screen [widthDp] wide. */
fun toolbarSlots(widthDp: Int): Int = dockSlots(widthDp)

/** The dock's room when its tools carry their names (Clean appearance needs a little more). */
fun toolbarSlots(widthDp: Int, clean: Boolean): Int =
    if (clean) ((widthDp - 24) / 76 - 1).coerceIn(1, 8) else dockSlots(widthDp)

/** The dock's tools, in the order the user arranged them (kept in the app's preferences). */
@Stable
class QuickToolbarPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("quick_toolbar", Context.MODE_PRIVATE)

    var bar by mutableStateOf(validBar(read()))
        private set

    private fun read(): List<QuickTool> =
        prefs.getString("bar", null)?.let { csv ->
            csv.split(',').filter { it.isNotBlank() }.mapNotNull(QuickTool::of)
        }.orEmpty()

    fun save(newBar: List<QuickTool>) {
        bar = validBar(newBar)
        // a layout saved before the More list was derived kept a "grid" order: it is never read
        // again, so drop it once rather than leaving it to confuse a later version
        prefs.edit().putString("bar", bar.joinToString(",") { it.id }).remove("grid").apply()
    }

    companion object {
        /** [order] without office-only tools or repeats; empty, the dock's default tools. */
        fun validBar(order: List<QuickTool>): List<QuickTool> {
            val tools = order.filter { it.appliesTo(ToolDocKind.Pdf) }.distinct()
            return tools.ifEmpty { QuickTool.DEFAULT_BAR }
        }
    }
}

/** Do what [tool] does on the bar or in More tools. */
fun AppViewModel.runQuickTool(doc: OpenDocument, tool: QuickTool) {
    if (doc.isOffice && tool !in QuickTool.barFor(doc, emptyList())) {
        tell(com.chethan616.clearpdf.text(com.chethan616.clearpdf.R.string.msg_word_reading_only))
        return
    }
    when (tool) {
        // a Word document is edited by its own engine: typing and formatting, not PDF editing
        QuickTool.Edit -> if (doc.isOffice) startWordEdit(doc) else enterTool(doc, ViewerTool.Edit)
        QuickTool.Comment -> enterTool(doc, ViewerTool.Comment)
        QuickTool.Highlight -> enterTool(doc, ViewerTool.Markup)
        QuickTool.Draw -> enterTool(doc, ViewerTool.Draw)
        QuickTool.FillSign -> enterTool(doc, ViewerTool.FillSign)
        QuickTool.Text -> enterTool(doc, ViewerTool.Text)
        QuickTool.Export -> openExport(doc)
        QuickTool.Combine -> openCombine(doc)
        QuickTool.Compress -> openCompress(doc)
        QuickTool.Protect -> openProtect(doc)
        QuickTool.Organize -> {
            doc.controller.pagesOpen = true
            organizing = true
        }
        QuickTool.Crop -> openCrop(doc)
        QuickTool.Ocr -> recognizeText(doc)
        QuickTool.Create -> openCreate()
        QuickTool.Scan -> startScan()
        QuickTool.ReadAloud -> startReadAloud(doc)
        QuickTool.Present -> presenting = doc
        QuickTool.ExportPdf -> exportOffice(doc, "pdf")
        QuickTool.SaveAs -> doc.controller.sheet = ViewerSheet.SaveAs
        QuickTool.Print -> print(doc)
    }
}

/**
 * Customize dock: the tools the bottom bar holds, in the order it holds them. Drag one along the
 * capsule to move it, tap one to take it off; tap a tool below to put it on. A full dock makes
 * room by dropping its last tool. The tick keeps the arrangement, the cross leaves it.
 *
 * Only the dock is arranged. The More list is derived from it in a fixed order, so nothing here
 * can reorder it.
 */
@Composable
fun CustomizeDock(vm: AppViewModel, onDone: () -> Unit) {
    val c = pdfColors
    val prefs = vm.quickToolbar
    val slots = toolbarSlots(LocalConfiguration.current.screenWidthDp, com.chethan616.clearpdf.ui.theme.clear.clean)
    val bar = remember { prefs.bar.take(slots).toMutableStateList() }
    // every tool that could sit on the dock, in the app's own order: never arranged here
    val rest = QuickTool.entries.filter { it.appliesTo(ToolDocKind.Pdf) && it !in bar }
    val rects = remember { mutableStateMapOf<QuickTool, Rect>() }
    var barRect by remember { mutableStateOf(Rect.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf<QuickTool?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }

    fun put(t: QuickTool) {
        if (t in bar) return
        if (bar.size >= slots) bar.removeAt(bar.lastIndex)
        bar.add(t)
    }

    fun move(t: QuickTool, at: Offset) {
        if (!barRect.contains(at)) return
        bar.remove(t)
        bar.add(bar.count { (rects[it]?.center?.x ?: 0f) < at.x }.coerceIn(0, bar.size), t)
    }

    val wobble by rememberInfiniteTransition(label = "jiggle").animateFloat(-1.5f, 1.5f, infiniteRepeatable(tween(140), RepeatMode.Reverse), label = "angle")

    @Composable
    fun DockTool(t: QuickTool) {
        val description = stringResource(R.string.toolbar_tool_on_bar, stringResource(t.title))
        Box(
            Modifier
                .onGloballyPositioned { rects[t] = it.boundsInRoot() }
                .alpha(if (dragging == t) 0.3f else 1f)
                .rotate(if (dragging == null) wobble else 0f)
                .pointerInput(t) {
                    detectDragGestures(
                        onDragStart = { p ->
                            dragging = t
                            dragPos = (rects[t]?.topLeft ?: Offset.Zero) + p
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragPos += amount
                        },
                        onDragEnd = {
                            move(t, dragPos)
                            dragging = null
                        },
                        onDragCancel = { dragging = null },
                    )
                }
                // a tap takes it off the dock: the More list takes it back in its own group
                .clickable(role = Role.Button) { bar.remove(t) }
                .semantics { contentDescription = description },
        ) { BarGlyph(t.icon, menuInk()) }
    }

    Box(Modifier.fillMaxWidth().onGloballyPositioned { origin = it.boundsInRoot().topLeft }) {
        Column {
            // as the menus' headers: the title, a glass close, and the blue tick that keeps the dock
            val ink = menuInk()
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_customize_dock), style = com.chethan616.clearpdf.ui.theme.ClearType.Title.copy(fontSize = 18.sp), color = ink, modifier = Modifier.weight(1f))
                com.chethan616.clearpdf.ui.glass.GlassCircle(onDone, size = 36.dp, label = stringResource(R.string.common_cancel)) {
                    com.chethan616.clearpdf.ui.glass.GlassIcon(LucideIcons.X, null, Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                com.chethan616.clearpdf.ui.glass.GlassCircle({
                    prefs.save(bar.toList())
                    onDone()
                }, size = 36.dp, tint = com.chethan616.clearpdf.ui.theme.Candy.Sky, label = stringResource(R.string.toolbar_save)) {
                    com.chethan616.clearpdf.ui.glass.GlassIcon(LucideIcons.Check, null, Modifier.size(18.dp))
                }
            }
            Text(
                pluralStringResource(R.plurals.toolbar_customize_hint, slots, slots),
                style = com.chethan616.clearpdf.ui.theme.ClearType.Label, color = ink.copy(alpha = 0.62f),
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp),
            )
            Text(
                stringResource(R.string.toolbar_section_toolbar).uppercase(), style = com.chethan616.clearpdf.ui.theme.ClearType.Eyebrow, color = ink.copy(alpha = 0.55f),
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp),
            )
            // the dock as it looks on the page: a capsule of its glyphs, the blue More circle at its end
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f).height(56.dp).clip(com.chethan616.clearpdf.ui.glass.Capsule)
                        .background(if (c.dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = if (c.dark) 0.18f else 0.8f), com.chethan616.clearpdf.ui.glass.Capsule)
                        .onGloballyPositioned { barRect = it.boundsInRoot() }
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (t in bar) DockTool(t)
                    // room left on the dock: faint places to drop a tool
                    repeat(slots - bar.size) { Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(ink.copy(alpha = 0.18f))) } }
                }
                Spacer(Modifier.width(10.dp))
                // More always ends the dock
                Box(Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape).background(com.chethan616.clearpdf.ui.theme.Candy.Sky), contentAlignment = Alignment.Center) {
                    Icon(LucideIcons.LayoutGrid, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Text(
                stringResource(R.string.settings_off_the_dock).uppercase(), style = com.chethan616.clearpdf.ui.theme.ClearType.Eyebrow, color = ink.copy(alpha = 0.55f),
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp),
            )
            // the tools that are not on the dock, four to a row, in the app's own order
            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                rest.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        row.forEach { t ->
                            val title = stringResource(t.title)
                            val description = stringResource(R.string.toolbar_tool_off_dock, title)
                            Box(
                                Modifier.weight(1f), contentAlignment = Alignment.TopCenter,
                            ) {
                                Box(
                                    Modifier.clickable(role = Role.Button) { put(t) }.semantics { contentDescription = description },
                                ) { ToolTileFace(t.icon, title, t.hue) }
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        // the tool under the finger
        val t = dragging
        if (t != null) {
            val half = with(LocalDensity.current) { 36.dp.toPx() }
            Box(
                Modifier.offset { IntOffset((dragPos.x - origin.x - half).roundToInt(), (dragPos.y - origin.y - half).roundToInt()) }
                    .graphicsLayer { scaleX = 1.08f; scaleY = 1.08f },
            ) { ToolTileFace(t.icon, stringResource(t.title), t.hue, Modifier.width(72.dp)) }
        }
    }
}

/** A tool on the toolbar's capsule: its glyph alone, as the bar on the page shows it. */
@Composable
private fun BarGlyph(icon: ImageVector, ink: Color) {
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = ink, modifier = Modifier.size(24.dp)) }
}
