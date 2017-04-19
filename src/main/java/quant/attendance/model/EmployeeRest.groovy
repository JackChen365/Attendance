package quant.attendance.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Created by Administrator on 2017/4/8.
 */
class EmployeeRest {
    int id
    int departmentId
    String departmentName
    List<Integer> workDays
    String employeeName
    String startDate
    long startTimeMillis
    String endDate
    long endTimeMillis
    long entryTimeMillis

    EmployeeRest() {
        workDays=[]
    }

    def toProperty(){
        def property=new EmployeeProperty()
        property.id=new SimpleIntegerProperty(id)
        property.departmentId=new SimpleIntegerProperty(departmentId)
        property.departmentName=new SimpleStringProperty(departmentName)
        property.workDays=new SimpleListProperty<>(FXCollections.observableList(workDays) )
        property.employeeName=new SimpleStringProperty(employeeName)
        property.startDate=new SimpleStringProperty(startDate)
        property.startTimeMillis=new SimpleLongProperty(startTimeMillis)
        property.endDate=new SimpleStringProperty(endDate)
        property.endTimeMillis=new SimpleLongProperty(endTimeMillis)
        property.entryTimeMillis=new SimpleLongProperty(entryTimeMillis)
        def entryDate=entryDateTime().toLocalDate()
        property.entryTime=new SimpleStringProperty(entryDate.toString())
        property
    }

    def entryDateTime(){
        LocalDateTime.ofInstant(new Date(entryTimeMillis).toInstant(),ZoneId.systemDefault())
    }

    def workDayToString(){
        workDays.join(",")
    }

    @Override
    String toString() {
        return employeeName
    }
}
