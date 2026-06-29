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
import wiretap.util.Configuration
import wiretap.util.data.Remark
import wiretap.util.OmitStatus
import wiretap.util.DottedName
import wiretap.util.data.Detail
import wiretap.util.data.DetailBuilder
import wiretap.util.data.DetailSource
import wiretap.util.activity
import wiretap.util.state
import wiretap.util.data.RemarkBuilder
import wiretap.util.data.RemarkSource

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

class ImportDocument(private val source: String) : Activity.Buzz(), DetailSource, RemarkSource {
    override val tags: Set<String> = setOf("import")

    override fun DetailBuilder.details() {
        add(DottedName("source"), source) { cascade = true }
        add(DottedName("mode"), "document")
    }

    override fun RemarkBuilder.remarks() {
        add(root.activity.state.append("source")) { label = "Source" }
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
    @Detail
    @Remark("Row")
    val rowIndex: Int,

    @Detail
    @Remark("Record")
    val recordId: String,
) : Activity.Snap() {
    override val tags: Set<String> = setOf("storage")

    class Okay : ActivityStatus.Okay<SaveRecord>()
}

class DeleteFiles : Activity.Bulk<DeleteFile>() {
    class Okay : ActivityStatus.Okay<DeleteFiles>()
}

class DeleteFile(private val path: String) : Activity.BulkItem(OmitStatus.First) {
    class Okay : ActivityStatus.Okay<DeleteFile>()

    class Fail(exception: Throwable) : ActivityStatus.Fail<DeleteFile>(exception)
}
