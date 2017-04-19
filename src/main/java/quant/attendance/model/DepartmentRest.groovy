package quant.attendance.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Created by Administrator on 2017/4/8.
 */
class DepartmentRest {
    int id
    String departmentName
    List<Integer> workDays
    String startDate
    long startTimeMillis
    String endDate
    long endTimeMillis

    DepartmentRest() {
        workDays =[]
    }

    def toProperty(){
        def property=new DepartmentProperty()
        property.id=new SimpleIntegerProperty(id)
        property.departmentName=new SimpleStringProperty(departmentName)
        property.workDays=new SimpleListProperty<>(FXCollections.observableList(workDays) )
        property.startDate=new SimpleStringProperty(startDate)
        property.startTimeMillis=new SimpleLongProperty(startTimeMillis)
        property.endDate=new SimpleStringProperty(endDate)
        property.endTimeMillis=new SimpleLongProperty(endTimeMillis)
        property
    }

    def startLocalTime(){
        LocalTime.ofSecondOfDay(startTimeMillis/1000 as Long)
    }

    def endLocalTime(){
        LocalTime.ofSecondOfDay(endTimeMillis/1000 as Long)
    }

    def workDayToString(){
        workDays.join(",")
    }

    @Override
    String toString() {
        departmentName
    }
}
