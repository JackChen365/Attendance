package quant.attendance.ui

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXSnackbar
import com.jfoenix.controls.JFXTextField
import com.jfoenix.controls.JFXToggleNode
import com.jfoenix.controls.JFXTreeTableColumn
import com.jfoenix.controls.JFXTreeTableView
import com.jfoenix.controls.RecursiveTreeItem
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.SingleSelectionModel
import javafx.scene.control.TreeItem
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import quant.attendance.StageManager
import quant.attendance.bus.RxBus
import quant.attendance.callback.InitializableArgs
import quant.attendance.database.DbHelper
import quant.attendance.event.OnDepartmentAddedEvent
import quant.attendance.event.OnEmployeeAddedEvent
import quant.attendance.model.DepartmentProperty
import quant.attendance.model.DepartmentRest
import quant.attendance.model.EmployeeProperty
import quant.attendance.model.EmployeeRest
import quant.attendance.scheduler.MainThreadSchedulers
import quant.attendance.widget.TimeSpinner
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers

import java.time.LocalTime

/**
 * Created by Administrator on 2017/4/8.
 */
class AddEmployeeController implements Initializable {
    final int DEFAULT_WORK_DAY=5;
    final int MIN_WORK_TIME_MILLIS=4*60*60*1000
    @FXML StackPane root
    @FXML Label titleLabel
    @FXML JFXSnackbar snackBar
    @FXML ComboBox<DepartmentRest> comboBox
    @FXML JFXTextField employeeField
    @FXML TimeSpinner startTimeSpinner
    @FXML TimeSpinner endTimeSpinner
    @FXML JFXTreeTableView employeeTable
    @FXML JFXTreeTableColumn departmentName
    @FXML JFXTreeTableColumn employeeName
    @FXML JFXTreeTableColumn startTime
    @FXML JFXTreeTableColumn endTime

    @FXML HBox weekContainer
    @FXML JFXButton addButton
    @FXML JFXButton exitButton
    //dialog
    @FXML JFXDialog dialog
    @FXML Label dialogTitle
    @FXML Label dialogContent
    @FXML JFXButton dialogAcceptButton
    @FXML JFXButton dialogCancelButton
    final def employeeProperties = FXCollections.observableArrayList()
    final List<EmployeeRest> employeeItems=[]
    boolean ensureWorkTime, ensureWorkDay



    @Override
    void initialize(URL location, ResourceBundle resources) {
        initEmployeeData()
        snackBar.registerSnackbarContainer(root)
        startTimeSpinner.valueFactory.setValue(LocalTime.of(9, 0))
        endTimeSpinner.valueFactory.setValue(LocalTime.of(18, 0))

        dialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
        dialogCancelButton.setOnMouseClicked({dialog.close()})
        initEvent()
    }

    private initEvent() {
        comboBox.valueProperty().addListener({ observable, oldValue, newValue ->
            employeeField.setText(null)
            employeeTable.setPredicate({ property -> property.value.departmentId.get()==newValue.id})
        } as ChangeListener<DepartmentRest>)
        addButton.setOnMouseClicked({
            if(validatorDepartmentInfo()){
                //添加信息
                def item=new EmployeeRest()
                def selectItem=comboBox.selectionModel.selectedItem
                item.employeeName=employeeField.text
                item.departmentId=selectItem.id
                item.departmentName=selectItem.departmentName
                item.workDays=workDayItems
                item.startDate=startTimeSpinner.text
                item.startTimeMillis=startTimeSpinner.timeMillis
                item.endDate=endTimeSpinner.text
                item.endTimeMillis=endTimeSpinner.timeMillis
                DbHelper.helper.insertEmployeeRest(item)
                employeeItems<<item

                ensureWorkTime=false
                ensureWorkDay=false
                employeeField.setText(null)
                snackBar.fireEvent(new JFXSnackbar.SnackbarEvent("添加员工信息成功!",null,2000, null))
                //更新表格数据
                def employeeProperty=item.toProperty()
                employeeProperties.add(employeeProperty)
                employeeTable.refresh()
                RxBus.post(new OnEmployeeAddedEvent(item))
            }
        })
        exitButton.setOnMouseClicked({ StageManager.instance.getStage(this)?.close() })
    }

    private Subscription initEmployeeData() {
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
                    !departmentItems ?: comboBox.getItems().addAll(departmentItems)
                    comboBox.getSelectionModel().selectFirst()

                    initPropertyTable()
                    initTableItems(employeeItems)
                }, { e -> e.printStackTrace() })
    }

    def validatorDepartmentInfo(){
        def result=false
        def startTimeMillis=startTimeSpinner.timeMillis
        def endTimeMillis=endTimeSpinner.timeMillis
        def selectItem=comboBox.selectionModel.selectedItem
        if(!employeeField.text){
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent("请输入员工姓名!",null,2000, null))
        } else if(startTimeMillis>endTimeMillis){
            startTimeSpinner.setEditError(true)
            endTimeSpinner.setEditError(true)
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent("工作起始时间不能小于结束时间!",null,2000, null))
        } else if(employeeItems.find {it.departmentId==selectItem.id&&it.employeeName==employeeField.text}){
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent("该部门己存在员工,如重名,请区分填写!",null,2000, null))
        } else {
            if(MIN_WORK_TIME_MILLIS<endTimeMillis-startTimeMillis){
                ensureWorkTime=true
            } else if(!ensureWorkTime){
                dialogTitle.setText("温馨提示")
                dialogContent.setText("工作时数太短,请确认,若必须添加,请重新添加即可!")
                dialogAcceptButton.setOnMouseClicked({
                    ensureWorkTime=true
                    dialog.close()
                })
                dialog.show(root)
            } else if(DEFAULT_WORK_DAY<=workDayItems.size()){
                ensureWorkDay=true
            } else if(!ensureWorkDay){
                dialogTitle.setText("温馨提示")
                dialogContent.setText("工作天数小于法定天数,请确认,若必须添加,请重新添加即可!")
                dialogAcceptButton.setOnMouseClicked({
                    ensureWorkDay=true
                    dialog.close()
                })
                dialog.show(root)
            }
            result=ensureWorkTime&&ensureWorkDay
        }
        result
    }

    private void initPropertyTable() {
        departmentName.setCellValueFactory({ departmentName.validateValue(it) ? it.value.value.departmentName : departmentName.getComputedValue(it) })
        employeeName.setCellValueFactory({ employeeName.validateValue(it) ? it.value.value.employeeName : employeeName.getComputedValue(it) })
        startTime.setCellValueFactory({ startTime.validateValue(it) ? it.value.value.startDate : startTime.getComputedValue(it) })
        endTime.setCellValueFactory({ endTime.validateValue(it) ? it.value.value.endDate : endTime.getComputedValue(it) })
    }

    def initTableItems(def items) {
        if(items){
            employeeItems.clear()
            items.each{employeeItems<<it}
            items.each{ employeeProperties.add(it.toProperty()) }
        }
        employeeTable.setRoot(new RecursiveTreeItem<EmployeeProperty>(employeeProperties, { it.getChildren() }))
        employeeTable.setPredicate({ property -> property.value.departmentId.get()==comboBox.selectionModel.selectedItem.id})
        employeeTable.setShowRoot(false)
    }
    /**
     * 获得工作天数条目
     * @return
     */
    def getWorkDayItems() {
        int index = 0
        def selectItems = []
        weekContainer.children.each { JFXToggleNode node ->
            index++
            if (node.selectedProperty().value) {
                selectItems << index
            }
        }
        selectItems
    }

    public void handleDepartmentClick(Event event) {

    }
}
