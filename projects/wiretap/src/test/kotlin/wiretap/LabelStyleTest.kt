package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.PropertyName
import wiretap.util.buzz.LabelStyle

class LabelStyleTest {
    private val name = PropertyName("wiretap", "activity", "recordID")
    private val label = "recordID value"

    @Test
    fun keepsLabelAsIs() {
        assertEquals("recordID value", LabelStyle.AsIs.applyTo(name, label))
    }

    @Test
    fun capitalizesLabel() {
        assertEquals("RecordID value", LabelStyle.Capital.applyTo(name, label))
    }

    @Test
    fun camelizesLabel() {
        assertEquals("recordIdValue", LabelStyle.Camel.applyTo(name, label))
    }

    @Test
    fun uppercasesLabel() {
        assertEquals("RECORDID VALUE", LabelStyle.Upper.applyTo(name, label))
    }

    @Test
    fun lowercasesLabel() {
        assertEquals("recordid value", LabelStyle.Lower.applyTo(name, label))
    }

    @Test
    fun kebabizesLabel() {
        assertEquals("record-id-value", LabelStyle.Kebab.applyTo(name, label))
    }

    @Test
    fun pascalizesLabel() {
        assertEquals("RecordIdValue", LabelStyle.Pascal.applyTo(name, label))
    }

    @Test
    fun snakeizesLabel() {
        assertEquals("record_id_value", LabelStyle.Snake.applyTo(name, label))
    }

    @Test
    fun titleizesLabel() {
        assertEquals("Record Id Value", LabelStyle.Title.applyTo(name, label))
    }
}
