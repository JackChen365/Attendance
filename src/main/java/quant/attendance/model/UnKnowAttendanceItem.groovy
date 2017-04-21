package quant.attendance.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

import java.time.LocalDate

/**
 * Created by Administrator on 2017/4/21.
 * 未知待确定的考勤信息,主要包含一些月初,月末,上班不正常的员工.可能存在当月入职,或月末离职员工
 */
class UnKnowAttendanceItem {
    final String name
    final LocalDate entryDate
    final LocalDate departureDate

    UnKnowAttendanceItem(name, LocalDate entryDate, LocalDate departureDate) {
        this.name = name
        this.entryDate = entryDate
        this.departureDate = departureDate
    }

    def toProperty(){
        def property=new UnKnowAttendanceProperty()
        property.name=new SimpleStringProperty(name)
        if(entryDate){
            property.entryYear=new SimpleIntegerProperty(entryDate.year)
            property.entryMonth=new SimpleIntegerProperty(entryDate.monthValue)
            property.entryDay=new SimpleIntegerProperty(entryDate.dayOfMonth)
            property.entryDate=new SimpleStringProperty(entryDate.toString())
        } else {
            property.entryDate=new SimpleStringProperty("####-##-##")
        }
        if(departureDate){
            property.departureYear=new SimpleIntegerProperty(departureDate.year)
            property.departureMonth=new SimpleIntegerProperty(departureDate.monthValue)
            property.departureDay=new SimpleIntegerProperty(departureDate.dayOfMonth)
            property.departureDate=new SimpleStringProperty(departureDate.toString())
        } else {
            property.departureDate=new SimpleStringProperty("####-##-##")
        }
        property.selected=new SimpleBooleanProperty(true)
        property
    }
}
