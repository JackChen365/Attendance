package quant.attendance.widget.datepicker

import com.sun.javafx.scene.control.skin.DatePickerSkin
import javafx.event.EventHandler
import javafx.scene.control.PopupControl
import javafx.scene.input.MouseEvent

/**
 * Created by Administrator on 2017/2/27.
 */
class MyDatePickerSkin extends DatePickerSkin{

    MyDatePickerSkin(MyDatePicker datePicker) {
        super(datePicker)
        arrowButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            void handle(MouseEvent event) {
                if(datePicker.isShowing()){
                    datePicker.superHide()
                } else {
                    datePicker.show()
                }
            }
        })
    }

    PopupControl getPopupWindow() {
        return super.getPopup()
    }
}
