package quant.attendance.model

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

import java.time.LocalDate

/**
 * Created by Administrator on 2017/4/21.
 */
class UnKnowAttendanceProperty  extends RecursiveTreeObject<UnKnowAttendanceProperty> {
    SimpleStringProperty name
    SimpleIntegerProperty entryYear
    SimpleIntegerProperty entryMonth
    SimpleIntegerProperty entryDay
    SimpleIntegerProperty departureYear
    SimpleIntegerProperty departureMonth
    SimpleIntegerProperty departureDay
    SimpleStringProperty entryDate
    SimpleStringProperty departureDate
    SimpleBooleanProperty selected

    UnKnowAttendanceProperty() {
    }
}
