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

private class ImportDocument(private val source: String) : Activity.Buzz() {
    override val name: String = "ImportDocument"

    override val tags: Set<String> = setOf("import")

    class Okay(val recordsSaved: Int) : ActivityStatus.Okay<ImportDocument>()
}

private class ParseDocument(private val documentType: String) : Activity.Buzz() {
    override val name: String = "ParseDocument"

    override val tags: Set<String> = setOf("parse")

    class Okay(val recordsParsed: Int) : ActivityStatus.Okay<ParseDocument>()
}

private class SaveRecord(
    @StateItem
    @MessagePart("Row")
    private val rowIndex: Int,

    @StateItem
    @MessagePart("Record")
    private val recordId: String,
) : Activity.Snap() {
    override val name: String = "SaveRecord"

    override val tags: Set<String> = setOf("storage")

    class Okay : ActivityStatus.Okay<SaveRecord>()
}

private class DeleteFiles : Activity.Bulk<DeleteFile>() {
    override val name: String = "DeleteFiles"

    class Okay : ActivityStatus.Okay<DeleteFiles>()
}

private class DeleteFile(private val path: String) : Activity.Buzz() {
    override val name: String = "DeleteFile"

    class Okay : ActivityStatus.Okay<DeleteFile>()

    class Fail(exception: Throwable) : ActivityStatus.Fail<DeleteFile>(exception)
}
