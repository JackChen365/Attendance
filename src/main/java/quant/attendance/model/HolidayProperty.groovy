package quant.attendance.model

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

/**
 * Created by Administrator on 2017/4/11.
 */
class HolidayProperty  extends RecursiveTreeObject<HolidayProperty> {
    SimpleStringProperty holiday
    SimpleStringProperty date
    SimpleStringProperty remark
    SimpleIntegerProperty year
    SimpleIntegerProperty month
    SimpleIntegerProperty day
    SimpleStringProperty holidayString

    @Override
    String toString() {
        return "$holiday(${month}月${day}日)"
    }
}
