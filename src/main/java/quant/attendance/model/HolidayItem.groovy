package quant.attendance.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

import java.text.DecimalFormat

/**
 * Created by Administrator on 2017/4/11.
 */
class HolidayItem {
    final DecimalFormat formatter = new DecimalFormat("00");
    final String holiday
    final String date
    final String remark
    final boolean isWork
    final int year
    final int month
    final int day
    final String holidayString

    HolidayItem(holiday, date, remark,year,month,day,isWork) {
        this.holiday = holiday
        this.date = date
        this.remark = remark
        this.year=year
        this.month=month
        this.day=day
        this.isWork=isWork
        this.holidayString="${formatter.format(month)}月${formatter.format(day)}日"
    }

    def toProperty(){
        def property=new HolidayProperty()
        property.holiday=new SimpleStringProperty(holiday)
        property.date=new SimpleStringProperty(date)
        property.remark=new SimpleStringProperty(remark)
        property.year=new SimpleIntegerProperty(year)
        property.month=new SimpleIntegerProperty(month)
        property.day=new SimpleIntegerProperty(day)
        property.holidayString=new SimpleStringProperty(holidayString)
        property
    }
}
