package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import wiretap.util.ActivityLogger
import wiretap.util.Activity
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.core.beginBuzz
import wiretap.core.beginBulk
import wiretap.util.ActivityLogRecord
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
        logger.beginBuzz(ImportDocument()).use { import ->
            logger.beginBuzz(ParseDocument()).use { parse ->
                assertEquals(import, parse.parent)
                assertEquals(1, parse.depth)
                assertEquals("ImportDocument/ParseDocument", parse.path)
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

            logger.beginBuzz(ParseDocument(), parent = parent).use { child ->
                assertEquals(parent, child.parent)
                assertEquals("ImportDocument/ParseDocument", child.path)
            }
        } finally {
            parent.close()
        }
    }

    @Test
    fun activityLoggerOwnsLifecycleApiAndUsesSink() {
        val records = mutableListOf<ActivityLogRecord>()
        val logger = TestActivityLogger(records)

        logger.beginBuzz(ImportDocument()).use { scope ->
            scope.setStatus(ImportDocument.Okay())
        }

        assertEquals(listOf("Ready", "Okay"), records.map { it.status.code })
        assertEquals(listOf("ImportDocument", "ImportDocument"), records.map { it.scope.activity.name })
    }

    @Test
    fun bulkCountsItemStatuses() {
        val records = mutableListOf<ActivityLogRecord>()
        val logger = TestActivityLogger(records)

        logger.beginBulk(DeleteFiles()).use { bulk ->
            bulk.beginItem(DeleteFile()).use { item ->
                item.setStatus(DeleteFile.Okay())
            }

            bulk.beginItem(DeleteFile()).use { item ->
                item.setStatus(DeleteFile.Fail())
            }

            bulk.setStatus(DeleteFiles.Okay())
        }

        val final = records.last()
        assertEquals("DeleteFiles", final.scope.activity.name)
        assertEquals(2, final.stateItems["wiretap.activity.state.item_count"])
        assertEquals(1, final.stateItems["wiretap.activity.state.okay_count"])
        assertEquals(1, final.stateItems["wiretap.activity.state.fail_count"])
    }

    @Test
    fun annotationsFeedStateItemsAndMessageParts() {
        val records = mutableListOf<ActivityLogRecord>()
        val logger = TestActivityLogger(records)

        logger.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())

        val record = records.single()
        assertEquals(1, record.stateItems["wiretap.activity.state.rowIndex"])
        assertEquals("customer-001", record.stateItems["wiretap.activity.state.recordId"])
        assertEquals("SaveRecord[Okay]; Duration: N/A; Row: 1; Record: customer-001", record.message)
    }

    @Test
    fun annotationsOnlyReflectPublicProperties() {
        val records = mutableListOf<ActivityLogRecord>()
        val logger = TestActivityLogger(records)

        logger.logSnap(PrivateAnnotatedRecord("hidden"), PrivateAnnotatedRecord.Okay())

        val record = records.single()
        assertNull(record.stateItems["wiretap.activity.state.secret"])
        assertEquals("PrivateAnnotatedRecord[Okay]; Duration: N/A", record.message)
    }

    @Test
    fun stateItemsCanCascadeFromParentActivities() {
        val records = mutableListOf<ActivityLogRecord>()
        val logger = TestActivityLogger(records)

        logger.beginBuzz(ImportDocumentWithState(source = "customers.csv", localOnly = "root-only")).use { import ->
            logger.beginBuzz(ParseDocumentWithState(documentType = "csv", localOnly = "parent-only")).use { parse ->
                logger.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())
                parse.setStatus(ParseDocumentWithState.Okay())
            }

            import.setStatus(ImportDocumentWithState.Okay())
        }

        val snap = records.first { it.scope.activity is SaveRecord }
        assertEquals("customers.csv", snap.stateItems["wiretap.activity.state.source"])
        assertEquals("csv", snap.stateItems["wiretap.activity.state.documentType"])
        assertNull(snap.stateItems["wiretap.activity.state.localOnly"])
    }

    @Test
    fun messagePartLabelsCanBeOmittedDefaultedOrAliased() {
        val records = mutableListOf<ActivityLogRecord>()
        val logger = TestActivityLogger(records)

        logger.logSnap(MessagePartLabelCase("alpha", "bravo", "charlie"), MessagePartLabelCase.Okay())

        assertEquals(
            "MessagePartLabelCase[Okay]; Duration: N/A; alpha; defaultLabel: bravo; Alias: charlie",
            records.single().message,
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

    class DeleteFiles : Activity.Bulk<DeleteFile>() {
        class Okay : ActivityStatus.Okay<DeleteFiles>()
    }

    class DeleteFile : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<DeleteFile>()

        class Fail : ActivityStatus.Fail<DeleteFile>()
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
        private val records: MutableList<ActivityLogRecord> = mutableListOf(),
    ) : ActivityLogger {
        override fun log(record: ActivityLogRecord) {
            records += record
        }
    }
}
