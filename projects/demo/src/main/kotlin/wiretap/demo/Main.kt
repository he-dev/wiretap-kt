package wiretap.demo

import org.slf4j.LoggerFactory
import wiretap.util.Wiretap
import wiretap.util.Activity
import wiretap.util.ActivityStatus
import wiretap.core.beginBuzz
import wiretap.core.beginBulk
import wiretap.core.logSnap
import wiretap.slf4j.core.ActivityLogger
import wiretap.util.MessagePart
import wiretap.util.StateItem

private val logger = LoggerFactory.getLogger("wiretap.demo")
private val wiretap = ActivityLogger.getLogger("wiretap.demo")

fun main() {
    logger.info("{} demo", Wiretap.name)

    wiretap.beginBuzz(ImportDocument("customers.csv")).use { import ->
        wiretap.beginBuzz(ParseDocument("csv")).use { parse ->
            parse.setStatus(ParseDocument.Okay(recordsParsed = 3))
        }

        import.setStatus(ImportDocument.Okay(recordsSaved = 2))
        wiretap.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())
    }

    wiretap.beginBulk(DeleteFiles()).use { bulk ->
        bulk.beginItem(DeleteFile("/tmp/one.csv")).use { item ->
            item.setStatus(DeleteFile.Okay())
        }

        bulk.beginItem(DeleteFile("/tmp/two.tmp")).use { item ->
            item.setStatus(DeleteFile.Fail(IllegalStateException("Temporary files are skipped.")))
        }

        bulk.setStatus(DeleteFiles.Okay())
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
