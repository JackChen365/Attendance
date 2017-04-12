package quant.attendance.ui

import com.jfoenix.controls.JFXColorPicker
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import quant.attendance.prefs.PrefsKey
import quant.attendance.prefs.SharedPrefs

import java.text.DecimalFormat

/**
 * Created by Administrator on 2017/4/12.
 */
class SettingController implements Initializable{
    public static final int DEFAULT_WORD_HOUR=2
    DecimalFormat formatter=new DecimalFormat("00")
    @FXML StackPane root
    @FXML Spinner overWorkSpinner
    @FXML JFXColorPicker lateColorPicker
    @FXML JFXColorPicker levelEarlyColorPicker
    @FXML JFXColorPicker absenteeismColorPicker
    @FXML JFXColorPicker unCheckInColorPicker
    @FXML JFXColorPicker unCheckOutColorPicker
    @FXML JFXColorPicker workColorPicker
    @FXML JFXColorPicker weekWorkColorPicker
    @FXML JFXColorPicker holidayWorkColorPicker

    @Override
    void initialize(URL location, ResourceBundle resources) {
        overWorkSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,6))
        overWorkSpinner.valueProperty().addListener({ observable,oldValue,newValue->
            SharedPrefs.save(PrefsKey.WORK_HOUR,newValue)
        } as ChangeListener<Integer>)
        def workHour=SharedPrefs.get(PrefsKey.WORK_HOUR)
        overWorkSpinner.editor.setText(workHour?:String.valueOf(DEFAULT_WORD_HOUR))
        setColorPickerValue(lateColorPicker,PrefsKey.COLOR_LATE,Color.BROWN)
        setColorPickerValue(levelEarlyColorPicker,PrefsKey.COLOR_LEVEL_EARLY,Color.AZURE)
        setColorPickerValue(absenteeismColorPicker,PrefsKey.COLOR_ABSENTEEISM,Color.RED)
        setColorPickerValue(unCheckInColorPicker,PrefsKey.COLOR_UN_CHECK_IN,Color.BLUE)
        setColorPickerValue(unCheckOutColorPicker,PrefsKey.COLOR_UN_CHECK_OUT,Color.DARKBLUE)
        setColorPickerValue(workColorPicker,PrefsKey.COLOR_OVER_TIME,Color.GREEN)
        setColorPickerValue(weekWorkColorPicker,PrefsKey.COLOR_WEEKEND_OVER_TIME,Color.GREENYELLOW)
        setColorPickerValue(holidayWorkColorPicker,PrefsKey.COLOR_HOLIDAY_OVER_TIME,Color.YELLOW)

    }

    void setColorPickerValue(colorPicker,key,defaultValue){
        def value=SharedPrefs.get(key)
        colorPicker.valueProperty().addListener({ observable,oldValue,newValue->
            def colorValue=getColorPickerValue(newValue)
            SharedPrefs.save(key,colorValue)
        } as ChangeListener<Color>)
        if(value){
            colorPicker.setValue(Color.valueOf(value))
        } else {
            colorPicker.setValue(defaultValue)
        }
    }

    def getColorPickerValue(value){
        int r = (int)Math.round(value.red * 255.0);
        int g = (int)Math.round(value.green * 255.0);
        int b = (int)Math.round(value.blue * 255.0);
        String.format("#%s%s%s",getHexString(r),getHexString(g),getHexString(b))
    }

    def getHexString(value){
        def hexValue
        if(0==value){
            hexValue=formatter.format(value)
        } else {
            hexValue=Integer.toHexString(value)
        }
        hexValue
    }
}
