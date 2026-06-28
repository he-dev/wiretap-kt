package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import wiretap.core.beginBulk
import wiretap.core.beginBuzz
import wiretap.core.logSnap
import wiretap.util.Activity
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.BulkItem
import wiretap.util.Configuration
import wiretap.util.CountOnlyBulkItem
import wiretap.util.Detail
import wiretap.util.DetailBuilder
import wiretap.util.DetailSource
import wiretap.util.OmitStatus
import wiretap.util.QuickBulk
import wiretap.util.QuickItem
import wiretap.util.QuickSnap
import wiretap.util.Remark

class WiretapTest {
    private val logger = CapturingActivityLogger()

    @Test
    fun ambientScopeBuildsNestedPath() {
        logger.beginBuzz(ImportDocument()) { import ->
            logger.beginBuzz(ParseDocument()) { parse ->
                assertEquals(import, parse.parent)
                assertEquals(listOf("ParseDocument", "ImportDocument"), parse.map { it.activity.name })
                parse.setStatus(ParseDocument.Okay())
            }

            import.setStatus(ImportDocument.Okay())
        }

        assertNull(ActivityScope.current())
    }

    @Test
    fun activityLoggerOwnsLifecycleApiAndUsesSink() {
        val entries = mutableListOf<CapturedLog>()
        val logger = CapturingActivityLogger(entries)

        logger.beginBuzz(ImportDocument()) { buzz ->
            buzz.setStatus(ImportDocument.Okay())
        }

        assertEquals(listOf("Ready", "Okay"), entries.map(::statusCode))
        assertEquals(listOf("ImportDocument", "ImportDocument"), entries.map { it["wiretap.activity.name"] })
        assertEquals(listOf("buzz", "buzz"), entries.map { it["wiretap.activity.role"] })
    }

    @Test
    fun firstLastStatusWinsAndLaterStatusEmitsDiagnostic() {
        val entries = mutableListOf<CapturedLog>()
        val diagnostics = mutableListOf<CapturedLog>()
        val previous = Configuration.diagnosticLogger

        try {
            Configuration.logDiagnosticsWith(CapturingActivityLogger(diagnostics))
            CapturingActivityLogger(entries).beginBuzz(ImportDocument()) { buzz ->
                buzz.setStatus(ImportDocument.Okay())
                buzz.setStatus(ImportDocument.Fail())
            }
        } finally {
            Configuration.logDiagnosticsWith(previous)
        }

        assertEquals(listOf("Ready", "Okay"), entries.map(::statusCode))
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
        val entries = mutableListOf<CapturedLog>()
        val logger = CapturingActivityLogger(entries)

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
        assertEquals("DeleteFiles", final["wiretap.activity.name"])
        assertEquals("bulk", final["wiretap.activity.role"])
        assertEquals("external-trace", final["wiretap.trace_id"])
        assertEquals(2, final["wiretap.activity.state.bulk.item_count"])
        assertNotNull(final["wiretap.activity.state.bulk.duration_s"])
        assertNotNull(final["wiretap.activity.state.bulk.throughput_s"])
        assertEquals(true, final.message.contains("Item Count: 2"))
    }

    @Test
    fun quickItemCanSpecifyStatusOmissions() {
        val entries = mutableListOf<CapturedLog>()
        val logger = CapturingActivityLogger(entries)

        logger.beginBulk(QuickBulk("QuickFiles")) { bulk ->
            bulk.beginItem(QuickItem("QuickFile", omitStatuses = setOf(OmitStatus.First))) { item ->
                item.setStatus(QuickItem.Okay())
            }
            bulk.setStatus(QuickBulk.Okay())
        }

        val itemStatuses = entries
            .filter { it["wiretap.activity.name"] == "QuickFile" }
            .map(::statusCode)
        assertEquals(listOf("Okay"), itemStatuses)
    }

    @Test
    fun quickActivitiesCarryRuntimeNamesAndMessages() {
        val entries = mutableListOf<CapturedLog>()
        val logger = CapturingActivityLogger(entries)

        logger.logSnap(
            QuickSnap("InspectCache", "Key: users:active"),
            QuickSnap.Okay("Entries: 42"),
        )

        val entry = entries.single()
        assertEquals("InspectCache", entry["wiretap.activity.name"])
        assertEquals(true, entry.message.contains("Key: users:active"))
    }

    @Test
    fun annotationsFeedDetailsAndRemarks() {
        val entries = mutableListOf<CapturedLog>()
        val logger = CapturingActivityLogger(entries)

        logger.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())

        val entry = entries.single()
        assertEquals(1, entry["wiretap.activity.state.rowIndex"])
        assertEquals("customer-001", entry["wiretap.activity.state.recordId"])
        assertEquals(true, entry.message.contains("Record: customer-001"))
        assertEquals(true, entry.message.contains("Row: 1"))
    }

    @Test
    fun detailsCanCascadeFromParentActivities() {
        val entries = mutableListOf<CapturedLog>()
        val logger = CapturingActivityLogger(entries)

        logger.beginBuzz(ImportDocumentWithState(source = "customers.csv", localOnly = "root-only")) {
            logger.beginBuzz(ParseDocumentWithState(documentType = "csv", localOnly = "parent-only")) {
                logger.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())
            }
        }

        val snap = entries.first { it["wiretap.activity.name"] == "SaveRecord" }
        assertEquals("customers.csv", snap["wiretap.activity.state.source"])
        assertEquals("csv", snap["wiretap.activity.state.documentType"])
        assertEquals("customers.csv", snap["wiretap.activity.state.sourceByInterface"])
        assertNull(snap["wiretap.activity.state.localOnly"])
        assertNull(snap["wiretap.activity.state.localByInterface"])
    }

    private fun statusCode(log: CapturedLog): String =
        (log["wiretap.activity.status.code"] as ActivityStatus<*>).code

    class ImportDocument : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<ImportDocument>()
        class Fail : ActivityStatus.Fail<ImportDocument>()
    }

    class ImportDocumentWithState(
        @Detail(cascade = true)
        val source: String,

        @Detail
        val localOnly: String,
    ) : Activity.Buzz(), DetailSource {
        override fun DetailBuilder.details() {
            add(wiretap.util.DottedName("sourceByInterface"), source) { cascade = true }
            add(wiretap.util.DottedName("localByInterface"), this@ImportDocumentWithState.localOnly)
        }
    }

    class ParseDocument : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<ParseDocument>()
    }

    class ParseDocumentWithState(
        @Detail(cascade = true)
        val documentType: String,

        @Detail
        val localOnly: String,
    ) : Activity.Buzz()

    class DeleteFiles : Activity.Bulk<DeleteFile>() {
        class Okay : ActivityStatus.Okay<DeleteFiles>()
    }

    @CountOnlyBulkItem
    class DeleteFile : Activity.Item() {
        class Okay : ActivityStatus.Okay<DeleteFile>()
        class Fail : ActivityStatus.Fail<DeleteFile>()
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
}
