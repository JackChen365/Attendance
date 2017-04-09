package quant.attendance.model

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty

/**
 * Created by Administrator on 2017/4/9.
 */
class DepartmentProperty extends RecursiveTreeObject<DepartmentProperty> {
    SimpleIntegerProperty id
    SimpleStringProperty departmentName
    SimpleListProperty<Integer> workDays=[]
    SimpleStringProperty startDate
    SimpleLongProperty startTimeMillis
    SimpleStringProperty endDate
    SimpleLongProperty endTimeMillis

    DepartmentRest toItem() {
        def item=new DepartmentRest()
        item.id=id.get()
        item.departmentName=departmentName.get()
        item.workDays=workDays.get()
        item.startDate=startDate.get()
        item.startTimeMillis=startTimeMillis.get()
        item.endDate=endDate.get()
        item.endTimeMillis=endTimeMillis.get()
        item
    }
}
