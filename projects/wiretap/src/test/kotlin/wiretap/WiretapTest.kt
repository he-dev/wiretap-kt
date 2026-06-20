package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import wiretap.util.ActivityLogger
import wiretap.util.Activity
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.BulkStat
import wiretap.util.BulkStatsOptIn
import wiretap.util.BulkDurationUnit
import wiretap.util.BulkItem
import wiretap.util.BulkScopeProfile
import wiretap.util.BulkThroughputUnit
import wiretap.util.BulkUnit
import wiretap.util.CountOnlyBulkItem
import wiretap.util.OmitStatus
import wiretap.core.beginBuzz
import wiretap.core.beginBulk
import wiretap.util.LogEntry
import wiretap.util.MessagePart
import wiretap.util.StateItem
import wiretap.core.logSnap
import wiretap.util.Wiretap

class WiretapTest {
    private val logger = TestActivityLogger()

    @Test
    fun exposesName() {
        assertEquals("wiretap", Wiretap.name)
    }

    @Test
    fun ambientScopeBuildsNestedPath() {
        logger.beginBuzz(ImportDocument()) {
            val import = this

            logger.beginBuzz(ParseDocument()) {
                assertEquals(import, parent)
                assertEquals(1, depth)
                assertEquals("ImportDocument/ParseDocument", path)
                setStatus(ParseDocument.Okay())
            }

            setStatus(ImportDocument.Okay())
        }

        assertNull(ActivityScope.current())
    }

    @Test
    fun explicitParentCanCrossAmbientBoundary() {
        val parent = logger.beginBuzz(ImportDocument())

        try {
            parent.close()

            logger.beginBuzz(ParseDocument(), parent = parent) {
                assertEquals(parent, this.parent)
                assertEquals("ImportDocument/ParseDocument", path)
            }
        } finally {
            parent.close()
        }
    }

    @Test
    fun activityLoggerOwnsLifecycleApiAndUsesSink() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.beginBuzz(ImportDocument()) {
            setStatus(ImportDocument.Okay())
        }

        assertEquals(listOf("Ready", "Okay"), entries.map { it["wiretap.activity.status.code"] })
        assertEquals(listOf("ImportDocument", "ImportDocument"), entries.map { it["wiretap.activity.name"] })
    }

    @Test
    fun beginBuzzBlockClosesScopeAndReturnsValue() {
        val result = logger.beginBuzz(ImportDocument()) {
            assertEquals(this, ActivityScope.current())
            setStatus(ImportDocument.Okay())
            "done"
        }

        assertEquals("done", result)
        assertNull(ActivityScope.current())
    }

    @Test
    fun bulkCountsItemStatuses() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.beginBulk(DeleteFiles(), traceId = "external-trace") {
            beginItem(DeleteFile()) {
                setStatus(DeleteFile.Okay())
            }

            beginItem(DeleteFile()) {
                setStatus(DeleteFile.Fail())
            }

            setStatus(DeleteFiles.Okay())
        }

        val final = entries.last()
        assertEquals(listOf("DeleteFiles", "DeleteFiles"), entries.map { it["wiretap.activity.name"] })
        assertEquals("DeleteFiles", final["wiretap.activity.name"])
        assertEquals("external-trace", final["wiretap.trace_id"])
        assertEquals(2, final["wiretap.activity.state.bulk.item_count"])
        assertEquals(1, final["wiretap.activity.state.bulk.okay_count"])
        assertEquals(1, final["wiretap.activity.state.bulk.fail_count"])
        assertNotNull(final["wiretap.activity.state.bulk.duration_s"])
        assertNotNull(final["wiretap.activity.state.bulk.throughput_min"])
    }

    @Test
    fun bulkItemCanOmitOnlyItsFirstStatus() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.beginBulk(IndexReportFiles()) {
            beginItem(IndexReportFile()) {
                setStatus(IndexReportFile.Okay())
            }
            setStatus(IndexReportFiles.Okay())
        }

        val itemStatuses = entries
            .filter { it["wiretap.activity.name"] == "IndexReportFile" }
            .map { it["wiretap.activity.status.code"] }
        assertEquals(listOf("Okay"), itemStatuses)
    }

    @Test
    fun explicitItemOmissionsOverrideItsAnnotations() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)

        logger.beginBulk(DeleteFiles()) {
            beginItem(DeleteFile(), omitStatuses = emptySet()) {
                setStatus(DeleteFile.Okay())
            }
            setStatus(DeleteFiles.Okay())
        }

        val itemStatuses = entries
            .filter { it["wiretap.activity.name"] == "DeleteFile" }
            .map { it["wiretap.activity.status.code"] }
        assertEquals(listOf("Ready", "Okay"), itemStatuses)
    }

    @Test
    fun explicitBulkProfileOverridesAnnotations() {
        val entries = mutableListOf<LogEntry>()
        val logger = TestActivityLogger(entries)
        val profile = BulkScopeProfile(
            stats = setOf(BulkStat.RateByStatus),
            durationUnit = BulkUnit.Milliseconds,
            throughputUnit = BulkUnit.Seconds,
        )

        logger.beginBulk(DeleteFiles(), profile) {
            beginItem(DeleteFile()) {
                setStatus(DeleteFile.Okay())
            }
            setStatus(DeleteFiles.Okay())
        }

        val final = entries.last()
        assertNotNull(final["wiretap.activity.state.bulk.duration_ms"])
        assertNotNull(final["wiretap.activity.state.bulk.throughput_s"])
        assertEquals(1.0, final["wiretap.activity.state.bulk.okay_rate"])
        assertFalse(final.containsKey("wiretap.activity.state.bulk.okay_count"))
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

        logger.beginBuzz(ImportDocumentWithState(source = "customers.csv", localOnly = "root-only")) {
            logger.beginBuzz(ParseDocumentWithState(documentType = "csv", localOnly = "parent-only")) {
                logger.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())
                setStatus(ParseDocumentWithState.Okay())
            }

            setStatus(ImportDocumentWithState.Okay())
        }

        val snap = entries.first { it["wiretap.activity.name"] == "SaveRecord" }
        assertEquals("customers.csv", snap["wiretap.activity.state.source"])
        assertEquals("csv", snap["wiretap.activity.state.documentType"])
        assertNull(snap["wiretap.activity.state.localOnly"])
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
    }

    class ImportDocumentWithState(
        @StateItem(cascade = true)
        val source: String,

        @StateItem
        val localOnly: String,
    ) : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<ImportDocumentWithState>()
    }

    class ParseDocument : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<ParseDocument>()
    }

    class ParseDocumentWithState(
        @StateItem(cascade = true)
        val documentType: String,

        @StateItem
        val localOnly: String,
    ) : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<ParseDocumentWithState>()
    }

    @BulkStatsOptIn(BulkStat.CountByStatus)
    @BulkDurationUnit(BulkUnit.Seconds)
    @BulkThroughputUnit(BulkUnit.Minutes)
    class DeleteFiles : Activity.Bulk<DeleteFile>() {
        class Okay : ActivityStatus.Okay<DeleteFiles>()
    }

    @CountOnlyBulkItem
    class DeleteFile : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<DeleteFile>()

        class Fail : ActivityStatus.Fail<DeleteFile>()
    }

    class IndexReportFiles : Activity.Bulk<IndexReportFile>() {
        class Okay : ActivityStatus.Okay<IndexReportFiles>()
    }

    @BulkItem(OmitStatus.First)
    class IndexReportFile : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<IndexReportFile>()
    }

    class SaveRecord(
        @StateItem
        @MessagePart("Row")
        val rowIndex: Int,

        @StateItem
        @MessagePart("Record")
        val recordId: String,
    ) : Activity.Snap() {
        class Okay : ActivityStatus.Okay<SaveRecord>()
    }

    class PrivateAnnotatedRecord(
        @StateItem
        @MessagePart("Secret")
        private val secret: String,
    ) : Activity.Snap() {
        class Okay : ActivityStatus.Okay<PrivateAnnotatedRecord>()
    }

    class MessagePartLabelCase(
        @MessagePart
        val noLabel: String,

        @MessagePart("")
        val defaultLabel: String,

        @MessagePart("Alias")
        val alias: String,
    ) : Activity.Snap() {
        class Okay : ActivityStatus.Okay<MessagePartLabelCase>()
    }

    private class TestActivityLogger(
        private val entries: MutableList<LogEntry> = mutableListOf(),
    ) : ActivityLogger {
        override fun log(entry: LogEntry) {
            entries += entry
        }
    }
}
