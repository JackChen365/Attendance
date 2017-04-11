package quant.attendance.ui

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTreeTableColumn
import com.jfoenix.controls.JFXTreeTableView
import javafx.fxml.FXML
import javafx.fxml.Initializable
import quant.attendance.prefs.FilePrefs
import quant.attendance.widget.drag.DragTextField

/**
 * Created by cz on 2017/4/11.
 */
class HolidayController implements Initializable {
    @FXML DragTextField propertyField
    @FXML JFXButton fileChoose

    @FXML JFXTreeTableView holidayTable
    @FXML JFXTreeTableColumn holidayColumn
    @FXML JFXTreeTableColumn dateColumn
    @FXML JFXTreeTableColumn noteColumn
    @Override
    void initialize(URL location, ResourceBundle resources) {
        def folder=FilePrefs.HOLIDAY_FOLDER
        folder.listFiles().each {

        }
    }
}
