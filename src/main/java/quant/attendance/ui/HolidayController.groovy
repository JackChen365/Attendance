package quant.attendance.ui
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXTreeTableColumn
import com.jfoenix.controls.JFXTreeTableView
import com.jfoenix.controls.RecursiveTreeItem
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.stage.FileChooser
import quant.attendance.StageManager
import quant.attendance.model.EmployeeProperty
import quant.attendance.model.HolidayItem
import quant.attendance.prefs.FilePrefs
import quant.attendance.util.FileUtils
import quant.attendance.widget.drag.DragTextField
/**
 * Created by cz on 2017/4/11.
 */
    class HolidayController implements Initializable {
        @FXML DragTextField propertyField
        @FXML JFXButton fileChoose

        @FXML JFXTreeTableView holidayTable
        @FXML JFXTreeTableColumn yearColumn
        @FXML JFXTreeTableColumn holidayColumn
        @FXML JFXTreeTableColumn dateColumn
        @FXML JFXTreeTableColumn noteColumn
        @FXML JFXButton exitButton

        final def holidayProperties = FXCollections.observableArrayList()
        @Override
        void initialize(URL location, ResourceBundle resources) {
            initHolidayPropertyTable(holidayItems)
            setButtonMouseClicked(fileChoose,StageManager.instance.getStage(this)){processProperty(it)}
            propertyField.setDragListener{ processProperty(it)}
            exitButton.setOnMouseClicked({StageManager.instance.getStage(this)?.close()})
        }

        /**
         * 设置文件选择器的点击
         * @param button
         * @param stage
         * @param closure
         * @return
         */
        def setButtonMouseClicked(button,stage,closure){
            button.setOnMouseClicked({
                final FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("请选择一个年假日配置文件");
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("properties", "*.properties"));
                def file=fileChooser.showOpenDialog(stage)
                if(file&&file.exists()){closure(file)}
            })
        }

        void processProperty(File file){
            if(file.name.endsWith("properties")){
                def year,items
                (year,items)=getPropertiesFileItems(file)
                holidayItems<<[(year):items]
                !items?:items.each{ holidayProperties.add(it.toProperty()) }
                holidayTable.refresh()
                //拷贝文件
                File targetFile=new File(FilePrefs.HOLIDAY_FOLDER,file.name)
                targetFile<<file.text
            }
        }

        /**
         * 初始化所有的假日条目
         * @return
         */
    def getHolidayItems() {
        final def holidayItems = [:]
        def folder = FilePrefs.HOLIDAY_FOLDER
        folder.listFiles().each {
            def year,items
            (year,items)=getPropertiesFileItems(it)
            holidayItems<<[(year):items]
        }
        holidayItems
    }
    def getPropertiesFileItems(File file){
        def matcher = file.name =~ /(\d{4}).+/
        def year
        final def holidays = []
        !matcher ?: (year = Integer.valueOf(matcher[0][1]))
        def properties = FileUtils.loadProperty(file)
        properties.each {
//                1-22(W),1-27,1-28,1-29,1-30,1-31,2-1,2-2,2-4(W)
            def holiday = it.key
            matcher = it.value =~ /((\d{1,2})\-(\d{1,2})(\((W)\))?)\s*,?\s*/
            while (matcher.find()) {
                def date = matcher.group(1)
                def month = Integer.valueOf(matcher.group(2))
                def day = Integer.valueOf(matcher.group(3))
                def isWord = matcher.group(5)
                holidays << new HolidayItem(holiday, date, isWord ? "节日工作" : "节日休息",year, month, day, isWord ? true : false)
            }
        }
        holidays.sort {o1,o2-> (o1.month*31+o1.day)-(o2.month*31+o2.day) }
        [year,holidays]
    }



    private void initHolidayPropertyTable(items) {
        yearColumn.setCellValueFactory({ yearColumn.validateValue(it) ? it.value.value.year : yearColumn.getComputedValue(it) })
        holidayColumn.setCellValueFactory({ holidayColumn.validateValue(it) ? it.value.value.holiday : holidayColumn.getComputedValue(it) })
        dateColumn.setCellValueFactory({ dateColumn.validateValue(it) ? it.value.value.holidayString: dateColumn.getComputedValue(it) })
        noteColumn.setCellValueFactory({ noteColumn.validateValue(it) ? it.value.value.remark : noteColumn.getComputedValue(it) })

        !items?:items.each{ item-> item.value.each{holidayProperties.add(it.toProperty()) }}
        holidayTable.setRoot(new RecursiveTreeItem<EmployeeProperty>(holidayProperties, { it.getChildren() }))
        holidayTable.setShowRoot(false)
    }
}
