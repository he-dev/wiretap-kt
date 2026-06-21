package wiretap.demo

import org.slf4j.LoggerFactory
import wiretap.util.Wiretap
import wiretap.util.Activity
import wiretap.util.ActivityStatus
import wiretap.slf4j.core.beginBuzz
import wiretap.slf4j.core.beginBulk
import wiretap.slf4j.core.logSnap
import wiretap.slf4j.core.useDiagnosticsLogger
import wiretap.util.BulkItem
import wiretap.util.Configuration
import wiretap.util.CountOnlyBulkItem
import wiretap.util.MessagePart
import wiretap.util.MessagePartOptions
import wiretap.util.OmitStatus
import wiretap.util.PropertyName
import wiretap.util.StateItem
import wiretap.util.activity
import wiretap.util.buzz.AddMessagePart
import wiretap.util.buzz.GetLogProperty
import wiretap.util.buzz.MessagePartSource
import wiretap.util.buzz.composeMessageBy
import wiretap.util.buzz.createLogEntryBy

private val logger = LoggerFactory.getLogger("wiretap.demo")

fun main() {
    Configuration.useDiagnosticsLogger()

    createLogEntryBy {
        root = PropertyName("wiretap", "demo")
        registerMessageParts {
            parts.push(root.activity.append("test"), "Demo")
        }
    }

    composeMessageBy {
        messageParts {
            push(root.activity.append("test"), "Demo")
        }
        arrange {
            positional(root.activity.append("test"))
            remaining()
        }
        join {
            joinToString(" | ") { it.value.toString() }
        }
    }

    logger.info("{} demo", Wiretap.name)

    logger.beginBuzz(ImportDocument("customers.csv")) {
        logger.beginBuzz(ParseDocument("csv")) {
            setStatus(ParseDocument.Okay(recordsParsed = 3))
        }

        setStatus(ImportDocument.Okay(recordsSaved = 2))
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

class ImportDocument(private val source: String) : Activity.Buzz(), MessagePartSource {
    override val tags: Set<String> = setOf("import")

    override fun messageParts(root: PropertyName, get: GetLogProperty, add: AddMessagePart) {
        add(
            root.append("source"),
            source,
            MessagePartOptions(label = "Source"),
        )
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
class DeleteFile(private val path: String) : Activity.Buzz() {
    class Okay : ActivityStatus.Okay<DeleteFile>()

    class Fail(exception: Throwable) : ActivityStatus.Fail<DeleteFile>(exception)
}
