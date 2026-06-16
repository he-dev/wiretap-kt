package wiretap.demo

import org.slf4j.LoggerFactory
import wiretap.Wiretap
import wiretap.util.Activity
import wiretap.util.ActivityStatus
import wiretap.util.beginBuzz
import wiretap.util.logSnap
import wiretap.slf4j.WiretapSlf4j

private val logger = LoggerFactory.getLogger("wiretap.demo")
private val wiretap = WiretapSlf4j.getLogger("wiretap.demo")

fun main() {
    logger.info("{} demo", Wiretap.name)

    wiretap.beginBuzz(ImportDocument("customers.csv")).use { import ->
        wiretap.beginBuzz(ParseDocument("csv")).use { parse ->
            parse.setStatus(ParseDocument.Okay(recordsParsed = 3))
        }

        import.setStatus(ImportDocument.Okay(recordsSaved = 2))
        wiretap.logSnap(SaveRecord(rowIndex = 1, recordId = "customer-001"), SaveRecord.Okay())
    }
}

private class ImportDocument(private val source: String) : Activity.Buzz() {
    override val name: String = "ImportDocument"

    override val tags: List<String> = listOf("import")

    class Okay(val recordsSaved: Int) : ActivityStatus.Okay<ImportDocument>()
}

private class ParseDocument(private val documentType: String) : Activity.Buzz() {
    override val name: String = "ParseDocument"

    override val tags: List<String> = listOf("parse")

    class Okay(val recordsParsed: Int) : ActivityStatus.Okay<ParseDocument>()
}

private class SaveRecord(
    private val rowIndex: Int,
    private val recordId: String,
) : Activity.Snap() {
    override val name: String = "SaveRecord"

    override val tags: List<String> = listOf("storage")

    class Okay : ActivityStatus.Okay<SaveRecord>()
}
