package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import wiretap.util.ActivityLogger
import wiretap.util.Activity
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.BulkItem
import wiretap.util.BuzzScope
import wiretap.util.CountOnlyBulkItem
import wiretap.util.Configuration
import wiretap.util.OmitStatus
import wiretap.core.beginBuzz
import wiretap.core.beginBulk
import wiretap.util.LogEntryBuilder
import wiretap.util.LogEntryFactory
import wiretap.util.LogEntry
import wiretap.util.Remark
import wiretap.util.DottedName
import wiretap.util.QuickBulk
import wiretap.util.QuickItem
import wiretap.util.QuickSnap
import wiretap.util.Detail
import wiretap.core.logSnap

class WiretapTest {
    private val logger = TestActivityLogger()
    private val messages = java.util.IdentityHashMap<LogEntry, String>()
    private val LogEntry.message: String
        get() = messages[this].orEmpty()

    @Test
    fun ambientScopeBuildsNestedPath() {
        logger.beginBuzz(ImportDocument()) { import ->
            logger.beginBuzz(ParseDocument()) { parse ->
                assertEquals(import, parse.parent)
                assertEquals(
                    listOf("ParseDocument", "ImportDocument"),
                    parse.map { it.activity.name },
                )
                parse.setStatus(ParseDocument.Okay())
            }

            import.setStatus(ImportDocument.Okay())
        }

        assertNull(ActivityScope.current())
    }

    @Test
    fun explicitParentCanCrossAmbientBoundary() {
        val parent = logger.beginBuzz(ImportDocument())

        try {
            parent.close()

            logger.beginBuzz(ParseDocument(), parent = parent) { parse ->
                assertEquals(parent, parse.parent)
                assertEquals(
                    listOf("ParseDocument", "ImportDocument"),
                    parse.map { it.activity.name },
                )
            }
        } finally {
            parent.close()
        }
    }

    @Test
    fun activityLoggerOwnsLifecycleApiAndUsesSink() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.beginBuzz(ImportDocument()) { buzz ->
            buzz.setStatus(ImportDocument.Okay())
        }

        assertEquals(listOf("Ready", "Okay"), entries.map { it["wiretap.activity.status.code"] })
        assertEquals(listOf("ImportDocument", "ImportDocument"), entries.map { it["wiretap.activity.name"] })
        assertEquals(listOf("buzz", "buzz"), entries.map { it["wiretap.activity.role"] })
    }

    @Test
    fun setStatusFreezesDurationForFinalLog() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)
        val activity = ImportDocument()
        val scope = BuzzScope.push(logger, activity)

        scope.setStatus(ImportDocument.Okay())
        val frozenDuration = activity.durationMs
        Thread.sleep(10)
        scope.close()

        assertEquals(frozenDuration, activity.durationMs)
        assertEquals(frozenDuration, entries.last()["wiretap.activity.duration_ms"])
    }

    @Test
    fun firstLastStatusWinsAndLaterStatusEmitsDiagnostic() {
        val entries = mutableListOf<LogEntry>()
        val diagnostics = mutableListOf<LogEntry>()
        val previous = Configuration.diagnosticLogger

        try {
            Configuration.logDiagnosticsWith(TestActivityLogger(diagnostics))
            TestActivityLogger(entries).beginBuzz(ImportDocument()) { buzz ->
                buzz.setStatus(ImportDocument.Okay())
                buzz.setStatus(ImportDocument.Fail())
            }
        } finally {
            Configuration.logDiagnosticsWith(previous)
        }

        assertEquals(listOf("Ready", "Okay"), entries.map { it["wiretap.activity.status.code"] })
        assertEquals(
            "ImportDocument status was already set to [okay]; ignored later status [fail].",
            diagnostics.single().message,
        )
    }

    @Test
    fun beginBuzzBlockClosesScopeAndReturnsValue() {
        val result = logger.beginBuzz(ImportDocument()) { buzz ->
            assertEquals(buzz, ActivityScope.current())
            buzz.setStatus(ImportDocument.Okay())
            "done"
        }

        assertEquals("done", result)
        assertNull(ActivityScope.current())
    }

    @Test
    fun bulkCountsItemStatuses() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.beginBulk(DeleteFiles(), traceId = "external-trace") { bulk ->
            bulk.beginItem(DeleteFile()) { item ->
                item.setStatus(DeleteFile.Okay())
            }

            bulk.beginItem(DeleteFile()) { item ->
                item.setStatus(DeleteFile.Fail())
            }

            bulk.setStatus(DeleteFiles.Okay())
        }

        val final = entries.last()
        val initial = entries.first()
        assertEquals(listOf("DeleteFiles", "DeleteFiles"), entries.map { it["wiretap.activity.name"] })
        assertEquals("DeleteFiles", final["wiretap.activity.name"])
        assertEquals("bulk", final["wiretap.activity.role"])
        assertEquals("external-trace", final["wiretap.trace_id"])
        assertEquals(0, initial["wiretap.activity.state.bulk.item_count"])
        assertEquals(0.0, initial["wiretap.activity.state.bulk.duration_s"])
        assertEquals(0.0, initial["wiretap.activity.state.bulk.throughput_s"])
        assertEquals(2, final["wiretap.activity.state.bulk.item_count"])
        assertNotNull(final["wiretap.activity.state.bulk.duration_s"])
        assertNotNull(final["wiretap.activity.state.bulk.throughput_s"])
        assertEquals(true, final.message.contains("Item Count: 2"))
        assertEquals(true, final.message.contains("Item Duration:"))
        assertEquals(true, final.message.contains("Throughput:"))
    }

    @Test
    fun bulkItemCanOmitOnlyItsFirstStatus() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.beginBulk(IndexReportFiles()) { bulk ->
            bulk.beginItem(IndexReportFile()) { item ->
                item.setStatus(IndexReportFile.Okay())
            }
            bulk.setStatus(IndexReportFiles.Okay())
        }

        val itemStatuses = entries
            .filter { it["wiretap.activity.name"] == "IndexReportFile" }
            .map { it["wiretap.activity.status.code"] }
        assertEquals(listOf("Okay"), itemStatuses)
        assertEquals(
            listOf("item"),
            entries
                .filter { it["wiretap.activity.name"] == "IndexReportFile" }
                .map { it["wiretap.activity.role"] },
        )
    }

    @Test
    fun quickItemCanSpecifyStatusOmissions() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.beginBulk(QuickBulk("QuickFiles")) { bulk ->
            bulk.beginItem(QuickItem("QuickFile", omitStatuses = setOf(OmitStatus.First))) { item ->
                item.setStatus(QuickItem.Okay())
            }
            bulk.setStatus(QuickBulk.Okay())
        }

        val itemStatuses = entries
            .filter { it["wiretap.activity.name"] == "QuickFile" }
            .map { it["wiretap.activity.status.code"] }
        assertEquals(listOf("Okay"), itemStatuses)
    }

    @Test
    fun quickActivitiesCarryRuntimeNamesAndMessages() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.logSnap(
            QuickSnap("InspectCache", "Key: users:active"),
            QuickSnap.Okay("Entries: 42"),
        )

        val entry = entries.single()
        assertEquals("InspectCache", entry["wiretap.activity.name"])
        assertEquals(
            "InspectCache[Okay]; Duration: N/A; Entries: 42",
            entry.message,
        )
    }

    @Test
    fun quickActivityMessageRemainsWhenStatusHasNoMessage() {
        val entries = mutableListOf<LogEntry>()

        TestActivityLogger(entries).logSnap(
            QuickSnap("InspectCache", "Key: users:active"),
            QuickSnap.Okay(),
        )

        assertEquals(
            "InspectCache[Okay]; Duration: N/A; Key: users:active",
            entries.single().message,
        )
    }

    @Test
    fun annotationsFeedStateItemsAndMessageParts() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())

        val entry = entries.single()
        assertEquals(1, entry["wiretap.activity.state.rowIndex"])
        assertEquals("customer-001", entry["wiretap.activity.state.recordId"])
        assertEquals("SaveRecord[Okay]; Duration: N/A; Record: customer-001; Row: 1", entry.message)
    }

    @Test
    fun annotationsOnlyReflectPublicProperties() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.logSnap(PrivateAnnotatedRecord("hidden"), PrivateAnnotatedRecord.Okay())

        val entry = entries.single()
        assertNull(entry["wiretap.activity.state.secret"])
        assertEquals("PrivateAnnotatedRecord[Okay]; Duration: N/A", entry.message)
    }

    @Test
    fun stateItemsCanCascadeFromParentActivities() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.beginBuzz(ImportDocumentWithState(source = "customers.csv", localOnly = "root-only")) { import ->
            logger.beginBuzz(ParseDocumentWithState(documentType = "csv", localOnly = "parent-only")) { parse ->
                logger.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())
                parse.setStatus(ParseDocumentWithState.Okay())
            }

            import.setStatus(ImportDocumentWithState.Okay())
        }

        val snap = entries.first { it["wiretap.activity.name"] == "SaveRecord" }
        assertEquals("customers.csv", snap["wiretap.activity.state.source"])
        assertEquals("csv", snap["wiretap.activity.state.documentType"])
        assertEquals("customers.csv", snap["wiretap.activity.state.sourceByInterface"])
        assertNull(snap["wiretap.activity.state.localOnly"])
        assertNull(snap["wiretap.activity.state.localByInterface"])
    }

    @Test
    fun messagePartLabelsCanBeOmittedDefaultedOrAliased() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.logSnap(MessagePartLabelCase("alpha", "bravo", "charlie"), MessagePartLabelCase.Okay())

        assertEquals(
            "MessagePartLabelCase[Okay]; Duration: N/A; Alias: charlie; defaultLabel: bravo; alpha",
            entries.single().message,
        )
    }

    class ImportDocument : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<ImportDocument>()
        class Fail : ActivityStatus.Fail<ImportDocument>()
    }

    class ImportDocumentWithState(
        @Detail(cascade = true)
        val source: String,

        @Detail
        val localOnly: String,
    ) : Activity.Buzz(), LogEntryFactory {
        override fun LogEntryBuilder.create() {
            features {
                add(DottedName("sourceByInterface"), source) { cascade = true }
                add(DottedName("localByInterface"), this@ImportDocumentWithState.localOnly)
            }
        }

        class Okay : ActivityStatus.Okay<ImportDocumentWithState>()
    }

    class ParseDocument : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<ParseDocument>()
    }

    class ParseDocumentWithState(
        @Detail(cascade = true)
        val documentType: String,

        @Detail
        val localOnly: String,
    ) : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<ParseDocumentWithState>()
    }

    class DeleteFiles : Activity.Bulk<DeleteFile>() {
        class Okay : ActivityStatus.Okay<DeleteFiles>()
    }

    @CountOnlyBulkItem
    class DeleteFile : Activity.Item() {
        class Okay : ActivityStatus.Okay<DeleteFile>()

        class Fail : ActivityStatus.Fail<DeleteFile>()
    }

    class IndexReportFiles : Activity.Bulk<IndexReportFile>() {
        class Okay : ActivityStatus.Okay<IndexReportFiles>()
    }

    @BulkItem(OmitStatus.First)
    class IndexReportFile : Activity.Item() {
        class Okay : ActivityStatus.Okay<IndexReportFile>()
    }

    class SaveRecord(
        @Detail
        @Remark("Row")
        val rowIndex: Int,

        @Detail
        @Remark("Record")
        val recordId: String,
    ) : Activity.Snap() {
        class Okay : ActivityStatus.Okay<SaveRecord>()
    }

    class PrivateAnnotatedRecord(
        @Detail
        @Remark("Secret")
        private val secret: String,
    ) : Activity.Snap() {
        class Okay : ActivityStatus.Okay<PrivateAnnotatedRecord>()
    }

    class MessagePartLabelCase(
        @Remark
        val noLabel: String,

        @Remark("")
        val defaultLabel: String,

        @Remark("Alias")
        val alias: String,
    ) : Activity.Snap() {
        class Okay : ActivityStatus.Okay<MessagePartLabelCase>()
    }

    private inner class TestActivityLogger(
        private val entries: MutableList<LogEntry> = mutableListOf(),
    ) : ActivityLogger {
        override fun log(entry: LogEntry, message: String) {
            entries += entry
            messages[entry] = message
        }
    }
}
