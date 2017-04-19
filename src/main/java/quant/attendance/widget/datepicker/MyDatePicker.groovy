package quant.attendance.widget.datepicker

import javafx.scene.control.DatePicker

import java.time.LocalDate

/**
 * Created by cz on 2017/3/2.
 */
class MyDatePicker extends DatePicker{

    MyDatePicker() {
    }

    MyDatePicker(LocalDate var1) {
        super(var1)
    }

    public void setEditError(boolean error){
        String textColor=error?"#ff0000":"#000000";
        String borderColor=error?"#ff0000":"#BCBCBC";
        editor.setStyle("-fx-text-fill: $textColor;")
        setStyle("-fx-border-color: $borderColor;-fx-border-width: 1;");
    }
}
