package wiretap.demo

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import wiretap.coroutines.core.beginBulk
import wiretap.coroutines.core.beginBuzz
import wiretap.util.Activity
import wiretap.util.ActivityStatus
import wiretap.slf4j.core.logSnap
import wiretap.slf4j.core.useDiagnosticsLogger
import wiretap.util.BulkItem
import wiretap.util.Configuration
import wiretap.util.MessagePart
import wiretap.util.OmitStatus
import wiretap.util.PropertyName
import wiretap.util.StateItem
import wiretap.util.activity
import wiretap.util.state
import wiretap.util.LogPropertyRegistry
import wiretap.util.MessagePartRegistry
import wiretap.util.LogPropertySource
import wiretap.util.MessagePartSource

private val logger = LoggerFactory.getLogger("wiretap.demo")

fun main() = runBlocking {
    Configuration.useDiagnosticsLogger()

    logger.beginBuzz(ImportDocument("customers.csv")) { import ->
        launch {
            parseDocument()
        }

        import.setStatus(ImportDocument.Okay(recordsSaved = 2))
        logger.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())
    }

    logger.beginBulk(DeleteFiles()) { bulk ->
        bulk.beginItem(DeleteFile("/tmp/one.csv")) { item ->
            item.setStatus(DeleteFile.Okay())
        }

        bulk.beginItem(DeleteFile("/tmp/two.tmp")) { item ->
            item.setStatus(DeleteFile.Fail(IllegalStateException("Temporary files are skipped.")))
        }

        bulk.setStatus(DeleteFiles.Okay())
    }
}

suspend fun parseDocument() {
    parseCsv()
}

suspend fun parseCsv() {
    logger.beginBuzz(ParseDocument("csv")) { parse ->
        parse.setStatus(ParseDocument.Okay(recordsParsed = 3))
    }
}

class ImportDocument(private val source: String) : Activity.Buzz(), LogPropertySource, MessagePartSource {
    override val tags: Set<String> = setOf("import")

    override fun LogPropertyRegistry.logProperties(root: PropertyName) {
        register(root.activity.state.append("source"), source) { cascade = true }
        register(root.activity.state.append("mode"), "document")
    }

    override fun MessagePartRegistry.messageParts(root: PropertyName) {
        property(root.state.append("source")) { label = "Source" }
    }

    class Okay(val recordsSaved: Int) : ActivityStatus.Okay<ImportDocument>()
}

class ParseDocument(private val documentType: String) : Activity.Buzz() {
    override val tags: Set<String> = setOf("parse")

    class Okay(val recordsParsed: Int) : ActivityStatus.Okay<ParseDocument>()
}

// note: The missing variant demonstrates diagnostics and the default-configuration fallback.
@Configuration.Use("missing")
class SaveRecord(
    @StateItem
    @MessagePart("Row")
    val rowIndex: Int,

    @StateItem
    @MessagePart("Record")
    val recordId: String,
) : Activity.Snap() {
    override val tags: Set<String> = setOf("storage")

    class Okay : ActivityStatus.Okay<SaveRecord>()
}

class DeleteFiles : Activity.Bulk<DeleteFile>() {
    class Okay : ActivityStatus.Okay<DeleteFiles>()
}

@BulkItem(OmitStatus.First)
class DeleteFile(private val path: String) : Activity.Item() {
    class Okay : ActivityStatus.Okay<DeleteFile>()

    class Fail(exception: Throwable) : ActivityStatus.Fail<DeleteFile>(exception)
}
