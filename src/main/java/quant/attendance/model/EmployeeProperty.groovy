package quant.attendance.model

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty

/**
 * Created by Administrator on 2017/4/9.
 */
class EmployeeProperty  extends RecursiveTreeObject<EmployeeProperty> {
    SimpleIntegerProperty id
    SimpleIntegerProperty departmentId
    SimpleStringProperty departmentName
    SimpleListProperty<Integer> workDays=[]
    SimpleStringProperty employeeName
    SimpleStringProperty startDate
    SimpleLongProperty startTimeMillis
    SimpleStringProperty endDate
    SimpleLongProperty endTimeMillis
    SimpleLongProperty entryTimeMillis
    SimpleStringProperty entryTime
}
