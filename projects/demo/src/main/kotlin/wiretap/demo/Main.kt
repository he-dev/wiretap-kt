package wiretap.demo

import org.slf4j.LoggerFactory
import wiretap.util.Wiretap
import wiretap.util.Activity
import wiretap.util.ActivityStatus
import wiretap.slf4j.core.beginBuzz
import wiretap.slf4j.core.beginBulk
import wiretap.slf4j.core.logSnap
import wiretap.slf4j.core.useDiagnosticsLogger
import wiretap.util.Configuration
import wiretap.util.MessagePart
import wiretap.util.StateItem

private val logger = LoggerFactory.getLogger("wiretap.demo")

fun main() {
    Configuration.useDiagnosticsLogger()
    logger.info("{} demo", Wiretap.name)

    logger.beginBuzz(ImportDocument("customers.csv")) {
        logger.beginBuzz(ParseDocument("csv")) {
            setStatus(ParseDocument.Okay(recordsParsed = 3))
        }

        setStatus(ImportDocument.Okay(recordsSaved = 2))
        logger.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())
    }

    logger.beginBulk(DeleteFiles()) {
        beginItem(DeleteFile("/tmp/one.csv")) {
            setStatus(DeleteFile.Okay())
        }

        beginItem(DeleteFile("/tmp/two.tmp")) {
            setStatus(DeleteFile.Fail(IllegalStateException("Temporary files are skipped.")))
        }

        setStatus(DeleteFiles.Okay())
    }
}

class ImportDocument(private val source: String) : Activity.Buzz() {
    override val tags: Set<String> = setOf("import")

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

class DeleteFile(private val path: String) : Activity.Buzz() {
    class Okay : ActivityStatus.Okay<DeleteFile>()

    class Fail(exception: Throwable) : ActivityStatus.Fail<DeleteFile>(exception)
}
