package quant.attendance.widget.datepicker

import javafx.scene.control.DateCell

import java.time.LocalDate

/**
 * Created by cz on 2017/2/28.
 */
class DateCellItem extends DateCell{
    final LocalDate localDate
    LocalDate updateDate

    DateCellItem(LocalDate item) {
        this.localDate = item
    }

    @Override
    void updateItem(LocalDate item, boolean empty) {
        super.updateItem(item, empty)
        this.updateDate=item
        if (item.isBefore(localDate)) {
            setDisable(true)
            setStyle("-fx-background-color: #DDDDDD")
        }
    }
}
