package com.chethan616.clearpdf.ui

import com.chethan616.clearpdf.ui.viewer.QuickTool
import com.chethan616.clearpdf.ui.viewer.QuickToolbarPrefs
import com.chethan616.clearpdf.ui.viewer.ToolDocKind
import com.chethan616.clearpdf.ui.viewer.ToolGroup
import com.chethan616.clearpdf.ui.viewer.appliesTo
import com.chethan616.clearpdf.ui.viewer.toolLayout
import com.chethan616.clearpdf.ui.viewer.toolbarSlots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickToolbarTest {
    @Test
    fun slotsGrowWithTheScreen() {
        // as many as the bar on the page shows: five on a phone (a sixth came out too small)
        assertEquals(5, toolbarSlots(411))
        assertEquals(6, toolbarSlots(460))
        assertEquals(8, toolbarSlots(800))
        // the default bar fits a phone held upright
        assertTrue(QuickTool.DEFAULT_BAR.size <= toolbarSlots(360))
    }

    @Test
    fun theDockDropsOfficeToolsAndRepeats() {
        // an old layout may name a tool a PDF does not have, or name one twice
        assertEquals(
            listOf(QuickTool.Scan),
            QuickToolbarPrefs.validBar(listOf(QuickTool.Scan, QuickTool.Present, QuickTool.Scan)),
        )
        // nothing left: the dock's default tools
        assertEquals(QuickTool.DEFAULT_BAR, QuickToolbarPrefs.validBar(emptyList()))
        assertEquals(QuickTool.DEFAULT_BAR, QuickToolbarPrefs.validBar(listOf(QuickTool.Present)))
    }

    @Test
    fun idsAreStable() {
        assertEquals(QuickTool.entries.size, QuickTool.entries.map { it.id }.toSet().size)
        for (t in QuickTool.entries) assertEquals(t, QuickTool.of(t.id))
    }

    @Test
    fun everyToolHasOnePlaceAndTheGroupsKeepTheirOrder() {
        // the group order is fixed: edit, pages, convert, security
        assertEquals(
            listOf(ToolGroup.EditAndMarkUp, ToolGroup.Pages, ToolGroup.Convert, ToolGroup.Security),
            ToolGroup.entries.toList(),
        )
        // a grouped tool belongs to that one group; only the header's and the office bars' tools have none
        assertEquals(
            setOf(QuickTool.ReadAloud, QuickTool.Print, QuickTool.Present, QuickTool.ExportPdf, QuickTool.SaveAs),
            QuickTool.entries.filter { it.group == null }.toSet(),
        )
        for (t in QuickTool.entries.filter { it.group != null }) {
            assertEquals(t.id, 1, ToolGroup.entries.count { group -> QuickTool.entries.filter { it.group == group }.contains(t) })
        }
    }

    @Test
    fun everyToolAppearsOnceAcrossTheDockTheHeaderAndMore() {
        for (kind in ToolDocKind.entries) {
            for (slots in listOf(2, 5, 8)) {
                val layout = toolLayout(kind, QuickTool.DEFAULT_BAR, slots)
                val all = layout.dock + layout.header + layout.more
                assertEquals("$kind at $slots", all.size, all.toSet().size)
                for (t in layout.dock + layout.more) assertTrue("$kind: $t", t.appliesTo(kind))
                // the header never repeats the dock
                assertTrue(layout.header.none { it in layout.dock })
            }
        }
        // for a PDF nothing is left out: dock, header and More cover every tool a PDF has
        val pdf = toolLayout(ToolDocKind.Pdf, QuickTool.DEFAULT_BAR, 5)
        assertEquals(QuickTool.entries.filter { it.appliesTo(ToolDocKind.Pdf) }.toSet(), (pdf.dock + pdf.header + pdf.more).toSet())
    }

    @Test
    fun aPdfsMoreListHoldsNoOfficeToolAndNoHeaderTool() {
        for (arranged in listOf(QuickTool.DEFAULT_BAR, listOf(QuickTool.Scan, QuickTool.Protect, QuickTool.Comment))) {
            val more = toolLayout(ToolDocKind.Pdf, arranged, 5).more
            assertTrue(more.none { it == QuickTool.Present || it == QuickTool.ExportPdf || it == QuickTool.SaveAs })
            assertTrue(more.none { it == QuickTool.ReadAloud || it == QuickTool.Print || it == QuickTool.Edit })
            assertTrue(more.none { it in arranged.take(5) })
        }
    }

    @Test
    fun theMoreListIsInGroupOrderThenDeclarationOrder() {
        val more = toolLayout(ToolDocKind.Pdf, QuickTool.DEFAULT_BAR, 5).more
        // the group order never changes, whatever the dock holds
        assertEquals(
            more.sortedWith(compareBy<QuickTool>({ it.group!!.ordinal }, { it.ordinal })),
            more,
        )
        for (dock in listOf(QuickTool.DEFAULT_BAR, QuickTool.DEFAULT_BAR.reversed(), listOf(QuickTool.Scan, QuickTool.Edit))) {
            val list = toolLayout(ToolDocKind.Pdf, dock, 5).more
            assertEquals(list.sortedWith(compareBy<QuickTool>({ it.group!!.ordinal }, { it.ordinal })), list)
        }
    }

    @Test
    fun takingAToolOffTheDockPutsItBackInItsOwnGroup() {
        val withComment = toolLayout(ToolDocKind.Pdf, QuickTool.DEFAULT_BAR, 5)
        val withoutComment = toolLayout(ToolDocKind.Pdf, QuickTool.DEFAULT_BAR - QuickTool.Comment, 5)
        // on the dock: not in the list
        assertTrue(QuickTool.Comment in withComment.dock)
        assertTrue(QuickTool.Comment !in withComment.more)
        // off the dock: back in its group, at its own place, and the rest of the list keeps its order
        assertTrue(QuickTool.Comment in withoutComment.more)
        assertEquals(ToolGroup.EditAndMarkUp, withoutComment.more.first { it == QuickTool.Comment }.group)
        assertEquals(withComment.more, withoutComment.more.filter { it != QuickTool.Comment })
        // a tool the dock has no room for belongs in its group too
        val squeezed = toolLayout(ToolDocKind.Pdf, QuickTool.DEFAULT_BAR, 2)
        assertTrue(squeezed.more.containsAll(QuickTool.DEFAULT_BAR.drop(2)))
        assertTrue(squeezed.more.all { it.group != null })
    }

    @Test
    fun anOfficeDocumentKeepsItsOwnBarAndItsOwnList() {
        for (kind in listOf(ToolDocKind.Word, ToolDocKind.Slides)) {
            val fixed = QuickTool.barFor(kind, QuickTool.DEFAULT_BAR)
            val layout = toolLayout(kind, QuickTool.DEFAULT_BAR, fixed.size)
            // the arranged PDF bar never reaches an office document
            assertEquals(fixed, layout.dock)
            assertTrue(layout.more.isEmpty())
            assertTrue(layout.header.isEmpty())
        }
    }
}
