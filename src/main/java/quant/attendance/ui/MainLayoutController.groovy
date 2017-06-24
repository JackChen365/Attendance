package quant.attendance.ui

import com.google.common.io.Files
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXSnackbar
import com.jfoenix.controls.JFXTreeTableColumn
import com.jfoenix.controls.JFXTreeTableView
import com.jfoenix.controls.RecursiveTreeItem
import com.sun.javafx.PlatformUtil
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.cell.CheckBoxTreeTableCell
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import org.fxmisc.richtext.StyleClassedTextArea
import quant.attendance.StageManager
import quant.attendance.bus.RxBus
import quant.attendance.database.DbHelper
import quant.attendance.event.OnDepartmentAddedEvent
import quant.attendance.event.OnEmployeeAddedEvent
import quant.attendance.excel.reader.ExcelEmployeeAttendanceReader
import quant.attendance.excel.reader.ExcelReaderA
import quant.attendance.excel.reader.ExcelReaderC
import quant.attendance.excel.reader.ExcelRestCodeReader
import quant.attendance.excel.writer.ExcelWriter
import quant.attendance.excel.InformantRegistry
import quant.attendance.model.DepartmentProperty
import quant.attendance.model.EmployeeProperty
import quant.attendance.model.HolidayItem
import quant.attendance.model.UnKnowAttendanceProperty
import quant.attendance.prefs.FilePrefs
import quant.attendance.scheduler.MainThreadSchedulers
import quant.attendance.util.Analyser
import quant.attendance.util.AnalyserUnKnow
import quant.attendance.util.FileUtils
import quant.attendance.widget.drag.DragTextField
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Created by Administrator on 2017/4/8.
 */
class MainLayoutController implements Initializable{
    @FXML StackPane root
    @FXML JFXTreeTableView<DepartmentProperty> departmentTable
    @FXML JFXTreeTableColumn departmentName
    @FXML JFXTreeTableColumn startTime
    @FXML JFXTreeTableColumn endTime

    @FXML DragTextField attendanceFile

    @FXML JFXTreeTableView<EmployeeProperty> employeeTable
    @FXML JFXTreeTableColumn departmentEmployeeName
    @FXML JFXTreeTableColumn employeeName
    @FXML JFXTreeTableColumn employeeEntryName
    @FXML JFXTreeTableColumn employeeStartTime
    @FXML JFXTreeTableColumn employeeEndTime
    @FXML JFXCheckBox checkBox

    @FXML JFXDialog unKnowDialog
    @FXML JFXButton acceptButton
    @FXML JFXButton cancelButton
    @FXML JFXTreeTableView<UnKnowAttendanceProperty> unKnowTable
    @FXML JFXTreeTableColumn unKnowEmployeeCell
    @FXML JFXTreeTableColumn entryDateCell
    @FXML JFXTreeTableColumn departureDateCell
    @FXML JFXTreeTableColumn acceptCell

    @FXML JFXDialog dialog
    @FXML JFXButton dialogCancelButton
    @FXML JFXButton dialogAcceptButton

    @FXML JFXButton openButton
    @FXML JFXButton saveButton
    @FXML JFXButton exitButton
    @FXML JFXSnackbar snackBar
    @FXML JFXButton fileChoose1
    @FXML StyleClassedTextArea messageArea
    final def departmentItems=[], employeeItems=[]
    final def departmentProperties = FXCollections.observableArrayList()
    final def employeeProperties = FXCollections.observableArrayList()
    final def unKnowProperties = FXCollections.observableArrayList()

    @Override
    void initialize(URL location, ResourceBundle resources) {
        final def stage=StageManager.instance.getStage(this)
        initTableItems()
        initEvent()
        attendanceFile.setDragListener{ processAttendanceFile(it)}
        exitButton.setOnMouseClicked({Platform.exit()})
        setButtonMouseClicked(fileChoose1,stage,{processAttendanceFile(it)})

        snackBar.registerSnackbarContainer(root)
        dialog.setTransitionType(JFXDialog.DialogTransition.CENTER)
        dialogCancelButton.setOnMouseClicked({dialog.close()})

        //待确认员工信息
        checkBox.selectedProperty().addListener({observable,oldValue,newValue->
            unKnowProperties.each { it.selected.setValue(newValue) }
        } as ChangeListener<Boolean>)
        unKnowDialog.setTransitionType(JFXDialog.DialogTransition.CENTER)
        unKnowDialog.setOverlayClose(false)//点击外围不消失
        cancelButton.setOnMouseClicked({
            messageArea.clear()
            unKnowDialog.close()
        })

        dialogAcceptButton.setOnMouseClicked({
            handleNewDepartmentAction()
            dialog.close()
        })
    }


    void initTableItems() {
        Observable.create({ sub ->
            def departmentItems = DbHelper.helper.queryDepartment()
            def employeeItems = DbHelper.helper.queryEmployeeRest()
            sub.onNext([departmentItems, employeeItems])
            sub.onCompleted()
        }).subscribeOn(Schedulers.io()).
                observeOn(MainThreadSchedulers.mainThread()).
                subscribe({
                    def departmentItems, employeeItems
                    (departmentItems, employeeItems) = it
                    this.departmentItems.addAll(departmentItems)
                    this.employeeItems.addAll(employeeItems)
                    initDepartmentPropertyTable(departmentItems)
                    initEmployeePropertyTable(employeeItems)
                    initUnKnowPropertyTable([])
                }, { e -> e.printStackTrace() })
    }

    private void initDepartmentPropertyTable(items) {
        departmentName.setCellValueFactory({ departmentName.validateValue(it) ? it.value.value.departmentName : departmentName.getComputedValue(it) })
        startTime.setCellValueFactory({ startTime.validateValue(it) ? it.value.value.startDate : startTime.getComputedValue(it) })
        endTime.setCellValueFactory({ endTime.validateValue(it) ? it.value.value.endDate : endTime.getComputedValue(it) })
        !items?:items.each{ departmentProperties.add(it.toProperty()) }
        departmentTable.setRoot(new RecursiveTreeItem<DepartmentProperty>(departmentProperties, { it.getChildren() }))
        departmentTable.setShowRoot(false)
        departmentTable.selectionModel.select(0)
        departmentTable.selectionModel.selectedIndex=0
        final ContextMenu menu = new ContextMenu();
        final MenuItem newDepartmentItem= new MenuItem("新的部门");
        final MenuItem deleteAllSelectedItem= new MenuItem("删除选中部门");
        newDepartmentItem.setOnAction({handleNewDepartmentAction()})
        deleteAllSelectedItem.setOnAction({ departmentProperties.isEmpty()?: deleteSelectDepartment(departmentTable.selectionModel.selectedItem.value) })
        menu.getItems().addAll(newDepartmentItem,deleteAllSelectedItem);
        departmentTable.setContextMenu(menu);
    }

    /**
     * 删除选中部门
     * @param departmentProperty
     */
    def deleteSelectDepartment(DepartmentProperty item) {
        if(item){
            //删除部门
            DbHelper.helper.deleteDepartment(item.id.intValue(),item.departmentName.value)
            //删除所在部门所有员工
            DbHelper.helper.deleteEmployeeByDepartmentId(item.id.intValue())
            departmentProperties.remove(item)
            departmentTable.refresh()
            //删除集合内所有员工
            employeeProperties.removeAll {item.id.intValue()==it.departmentId.get()}
            employeeTable.refresh()
        }
    }

    private void initEmployeePropertyTable(items) {
        departmentEmployeeName.setCellValueFactory({ departmentEmployeeName.validateValue(it) ? it.value.value.departmentName : departmentEmployeeName.getComputedValue(it) })
        employeeName.setCellValueFactory({ employeeName.validateValue(it) ? it.value.value.employeeName : employeeName.getComputedValue(it) })
        employeeEntryName.setCellValueFactory({ employeeEntryName.validateValue(it) ? it.value.value.entryTime : employeeEntryName.getComputedValue(it) })
        employeeStartTime.setCellValueFactory({ employeeStartTime.validateValue(it) ? it.value.value.startDate : employeeStartTime.getComputedValue(it) })
        employeeEndTime.setCellValueFactory({ employeeEndTime.validateValue(it) ? it.value.value.endDate : employeeEndTime.getComputedValue(it) })
        !items?:items.each{ employeeProperties.add(it.toProperty()) }
        employeeTable.setRoot(new RecursiveTreeItem<EmployeeProperty>(employeeProperties, { it.getChildren() }))
        !items?:employeeTable.setPredicate({ it.value.departmentId.get()==departmentTable.selectionModel.selectedItem.value.id.get() })
        employeeTable.setShowRoot(false)

        final ContextMenu menu = new ContextMenu();
        final MenuItem newEmployeeItem= new MenuItem("新的员工");
        final MenuItem deleteAllSelectedItem= new MenuItem("删除选中员工");
        final MenuItem importEmployee= new MenuItem("批量导入员工信息");
        newEmployeeItem.setOnAction({handleNewEmployeeAction()})
        deleteAllSelectedItem.setOnAction({ employeeProperties.isEmpty()?: deleteSelectEmployee(employeeTable.selectionModel.selectedItem.value) })
        importEmployee.setOnAction{StageManager.instance.newStage(getClass().getClassLoader().getResource("fxml/import_employee.fxml"),true, 640, 720)?.show()}
        menu.getItems().addAll(newEmployeeItem,deleteAllSelectedItem,importEmployee);
        employeeTable.setContextMenu(menu);
    }

    def deleteSelectEmployee(EmployeeProperty item) {
        if(item){
            DbHelper.helper.deleteEmployeeRest(item.id.intValue(),item.employeeName.value)
            employeeProperties.remove(item)
            employeeTable.refresh()
        }
    }

    /**
     * 初始化未知待确认条目
     * @param items
     */
    private void initUnKnowPropertyTable(items) {
        unKnowEmployeeCell.setCellValueFactory({ unKnowEmployeeCell.validateValue(it) ? it.value.value.name : unKnowEmployeeCell.getComputedValue(it) })
        entryDateCell.setCellValueFactory({ entryDateCell.validateValue(it) ? it.value.value.entryDate : entryDateCell.getComputedValue(it) })
        departureDateCell.setCellValueFactory({ departureDateCell.validateValue(it) ? it.value.value.departureDate : departureDateCell.getComputedValue(it) })

        acceptCell.setCellValueFactory({ acceptCell.validateValue(it) ? it.value.value.selected : acceptCell.getComputedValue(it) })
        acceptCell.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(acceptCell));
        !items?:items.each{ unKnowProperties.add(it.toProperty()) }
        unKnowTable.setRoot(new RecursiveTreeItem<UnKnowAttendanceProperty>(unKnowProperties, { it.getChildren() }))
        unKnowTable.setShowRoot(false)
    }

    void initEvent() {
        RxBus.subscribe(OnDepartmentAddedEvent.class){
            departmentProperties.add(it.departmentItem.toProperty())
            departmentTable.refresh()
            if(1==departmentProperties.size()){
                departmentTable.selectionModel.select(0)
            }
        }
        RxBus.subscribe(OnEmployeeAddedEvent.class){
            employeeItems<<it.employeeItem
            employeeProperties.add(it.employeeItem.toProperty())
            employeeTable.refresh()
        }
        InformantRegistry.instance.addListener({messageArea.appendText("$it\n")})
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
            fileChooser.setTitle("请选择一个excel考勤文件");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Excel", "*.xlsx","*.xls"));
            def file=fileChooser.showOpenDialog(stage)
            if(file&&file.exists()){closure(file)}
        })
    }

    /**
     * 处理考勤文件
     * @param file
     */
    def processAttendanceFile(File file) {
        if(!file.name.endsWith(".xlsx")&&!file.name.endsWith(".xls")){
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent("请选择一个excel文件!",null,2000, null))
        } else {
            Observable.create({ sub ->
                def attendanceItems = new ExcelReaderC().attendanceRead(file)
                def employeeAttendanceItems = new ExcelEmployeeAttendanceReader().attendanceRead(file)
                def restCodeItems = new ExcelRestCodeReader().attendanceRead(file)
                def selectDepartment = departmentTable.selectionModel.selectedItem.value.toItem()
                def selectEmployeeItems = employeeItems.findAll { it.departmentId == selectDepartment.id }
                //获得日期模板
                def holidayItems = getHolidayItems()
                def holidays = holidayItems.get(LocalDate.now().year)
                if (!holidays) {
                    //TODO 配置模板提示
                } else {
                    def writeExcelClosure = { unKnowItems ->
                        def analyser = new Analyser(attendanceItems, selectDepartment, selectEmployeeItems, holidays, unKnowItems,employeeAttendanceItems,restCodeItems)
                        def result = analyser.result()
                        def excelWriter = new ExcelWriter(analyser.startDateTime, analyser.endDateTime, result, selectDepartment, selectEmployeeItems, holidays, unKnowItems,employeeAttendanceItems,restCodeItems)
                        sub.onNext(excelWriter.writeExcel())
                        sub.onCompleted()
                    }
                    def analyserUnKnow = new AnalyserUnKnow(attendanceItems,employeeAttendanceItems,restCodeItems,holidays)
                    def unKnowItems = analyserUnKnow.analyzerUnKnowItems()
                    if (unKnowItems) {
                        //存在未知条目,通知用户确认
                        Platform.runLater{
                            unKnowProperties.clear()
                            unKnowItems.each { unKnowProperties.add(it.toProperty()) }
                            unKnowTable.refresh()
                            acceptButton.setOnMouseClicked{
                                //过滤未选择员工
                                unKnowItems.removeAll { item ->
                                    !unKnowProperties.find {
                                        item.name == it.name.value
                                    }.selected.value
                                }
                                writeExcelClosure.call(unKnowItems)
                                unKnowDialog.close()
                            }
                            checkBox.setSelected(true)
                            unKnowDialog.show(root)
                        }
                    } else {
                        writeExcelClosure.call(null)
                    }
                }
            }).subscribeOn(Schedulers.io()).
                    observeOn(MainThreadSchedulers.mainThread()).
                    subscribe({ target ->
                        saveButton.setDisable(false)
                        openButton.setDisable(false)
                        final def stage = StageManager.instance.getStage(this)
                        saveButton.setOnMouseClicked({ saveExcelFile(stage, target) })
                        //打开文件
                        openButton.setOnMouseClicked({
                            if (PlatformUtil.mac) {
                                "open $target.absolutePath".execute()
                            } else if (PlatformUtil.windows) {
                                "cmd  /c  start  $target.absolutePath".execute()
                            }
                        })
                    }, { e -> e.printStackTrace() })
        }
    }


    /**
     * 文件另类为
     * @param stage
     * @return
     */
    def saveExcelFile(stage,File target) {
        FileChooser fileChooser = new FileChooser()
        fileChooser.setTitle("保存考勤文件")
        File file = fileChooser.showSaveDialog(stage)
        if (file.absolutePath!=target.absolutePath) {
            if(!file.name.endsWith(".xls")&&!file.name.endsWith(".xlsx")){
                file=new File(file.parentFile,"${file.name}.xls")
            }
            Files.copy(target,file)
            !target.exists()?:target.delete()
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent("保存文件${file.name}成功!",null,2000, null))
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

    public void handleDepartmentClick(Event event) {
        if(!employeeProperties.isEmpty()){
            employeeTable.setPredicate({ it.value.departmentId.get()==departmentTable.selectionModel.selectedItem.value.id.get() })
        }
    }

    public void handleAboutAction(ActionEvent actionEvent) {
        //TODO 关于
    }

    public void handleNewDepartmentAction(ActionEvent actionEvent=null) {
        StageManager.instance.newStage(getClass().getClassLoader().getResource("fxml/add_department.fxml"),true, 640, 720)?.show()
    }

    public void handleNewEmployeeAction(ActionEvent actionEvent=null) {
        println departmentTable.currentItemsCount
        if(departmentProperties.isEmpty()){
            dialog.show(root)
        } else {
            StageManager.instance.newStage(getClass().getClassLoader().getResource("fxml/add_employee.fxml"), 640, 720)?.show()
        }
    }

    public void handleHolidayAction(ActionEvent actionEvent) {
        StageManager.instance.newStage(getClass().getClassLoader().getResource("fxml/holiday_layout.fxml"), 640, 720)?.show()
    }

    public void handleSettingAction(ActionEvent actionEvent) {
        StageManager.instance.newStage(getClass().getClassLoader().getResource("fxml/setting_layout.fxml"), 640, 720)?.show()
    }
}
