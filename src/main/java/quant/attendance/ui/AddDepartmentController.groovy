package quant.attendance.ui

import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXSnackbar
import com.jfoenix.controls.JFXTextField
import com.jfoenix.controls.JFXToggleNode
import com.jfoenix.controls.JFXTreeTableColumn
import com.jfoenix.controls.JFXTreeTableView
import com.jfoenix.controls.RecursiveTreeItem
import javafx.collections.FXCollections
import javafx.event.Event
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TreeItem
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import quant.attendance.StageManager
import quant.attendance.bus.RxBus
import quant.attendance.callback.InitializableArgs
import quant.attendance.database.DbHelper
import quant.attendance.event.OnDepartmentAddedEvent
import quant.attendance.model.DepartmentProperty
import quant.attendance.model.DepartmentRest
import quant.attendance.prefs.PrefsKey
import quant.attendance.prefs.SharedPrefs
import quant.attendance.scheduler.MainThreadSchedulers
import quant.attendance.widget.TimeSpinner
import rx.Observable
import rx.schedulers.Schedulers

import java.time.LocalTime

/**
 * Created by Administrator on 2017/4/8.
 */
class AddDepartmentController implements InitializableArgs<Boolean>{
    final int DEFAULT_WORK_DAY=5;
    final int MIN_WORK_TIME_MILLIS=4*60*60*1000
    @FXML StackPane root
    @FXML Label titleLabel
    @FXML JFXSnackbar snackBar
    @FXML JFXTextField departmentField
    @FXML TimeSpinner startTimeSpinner
    @FXML TimeSpinner endTimeSpinner
    @FXML JFXTreeTableView departmentTable
    @FXML JFXTreeTableColumn departmentName
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

    final def departmentProperties = FXCollections.observableArrayList()
    final List<DepartmentRest> departmentItems =[]
    boolean ensureWorkTime, ensureWorkDay

    @Override
    void initializeWithArgs(Boolean init) {
        snackBar.registerSnackbarContainer(root)
        initPropertyTable()
        if(!init){
            titleLabel.setText("你好 欢迎使用!")
            exitButton.setText("开始使用")
            exitButton.setOnMouseClicked({
                if(!departmentItems){
                    dialogTitle.setText("温馨提示")
                    dialogContent.setText("请必须添加一个部门出勤信息,否则将无部门参照信息!")
                    dialogAcceptButton.setOnMouseClicked({dialog.close()})
                    dialog.show(root)
                } else {
                    SharedPrefs.save(PrefsKey.INIT,true)
                    StageManager.instance.getStage(this)?.close()
                    StageManager.instance.newStage(getClass().getClassLoader().getResource("fxml/main_layout.fxml"), 960, 720)?.show()
                }
            })
        } else {
            titleLabel.setText("添加新的部门考勤信息")
            exitButton.setText("退出")
            exitButton.setOnMouseClicked({ StageManager.instance.getStage(this)?.close()})
        }
        startTimeSpinner.valueFactory.setValue(LocalTime.of(9, 0))
        endTimeSpinner.valueFactory.setValue(LocalTime.of(18, 0))

        dialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
        dialogCancelButton.setOnMouseClicked({dialog.close()})

        addButton.setOnMouseClicked({
            if(validatorDepartmentInfo()){
                //添加信息
                def item=new DepartmentRest()
                item.departmentName=departmentField.text
                item.workDays=workDayItems
                item.startDate=startTimeSpinner.text
                item.startTimeMillis=startTimeSpinner.timeMillis
                item.endDate=endTimeSpinner.text
                item.endTimeMillis=endTimeSpinner.timeMillis
                DbHelper.helper.insertDepartment(item)
                departmentItems<<item
                departmentField.setText(null)
                snackBar.fireEvent(new JFXSnackbar.SnackbarEvent("添加部门成功!",null,2000, null))
                //更新表格数据
                departmentProperties.add(item.toProperty())
                departmentTable.refresh()
                RxBus.post(new OnDepartmentAddedEvent(item))
            }
        })
    }

    def validatorDepartmentInfo(){
        def result=false
        def startTimeMillis=startTimeSpinner.timeMillis
        def endTimeMillis=endTimeSpinner.timeMillis
        if(!departmentField.text){
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent("请输入部门名称!",null,2000, null))
        } else if(departmentItems.find {it.departmentName==departmentField.text}){
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent("存在相同部门,请检测,并区分开来!",null,2000, null))
        } else if(startTimeMillis>endTimeMillis){
            startTimeSpinner.setEditError(true)
            endTimeSpinner.setEditError(true)
            snackBar.fireEvent(new JFXSnackbar.SnackbarEvent("工作起始时间不能小于结束时间!",null,2000, null))
        } else {
            if(MIN_WORK_TIME_MILLIS<endTimeMillis-startTimeMillis){
                ensureWorkTime=true
            } else {
                dialogTitle.setText("温馨提示")
                dialogContent.setText("工作时数太短,请确认!")
                dialogAcceptButton.setOnMouseClicked({ensureWorkTime=true})
                dialog.show(root)
            }
            if(DEFAULT_WORK_DAY<=workDayItems.size()){
                ensureWorkDay=true
            } else {
                dialogTitle.setText("温馨提示")
                dialogContent.setText("工作天数小于法定天数,请确认!")
                dialogAcceptButton.setOnMouseClicked({ensureWorkDay=true})
                dialog.show(root)
            }
            result=ensureWorkTime&&ensureWorkDay
        }
        result
    }

    private void initPropertyTable() {
        departmentName.setCellValueFactory({
            departmentName.validateValue(it) ? it.value.value.departmentName : departmentName.getComputedValue(it)
        })
        startTime.setCellValueFactory({
            startTime.validateValue(it) ? it.value.value.startDate : startTime.getComputedValue(it)
        })
        endTime.setCellValueFactory({
            endTime.validateValue(it) ? it.value.value.endDate : endTime.getComputedValue(it)
        })

        Observable.create({ sub->
            def items=DbHelper.helper.queryDepartment()
            sub.onNext(items)
            sub.onCompleted()
        }).subscribeOn(Schedulers.io()).
                observeOn(MainThreadSchedulers.mainThread()).
                subscribe({ initTableItems(it)},{e->e.printStackTrace()})
    }

    def initTableItems(def items) {
        if(items){
            departmentItems.clear()
            items.each{departmentItems<<it}
            items.each{ departmentProperties.add(it.toProperty()) }
        }
        departmentTable.setRoot(new RecursiveTreeItem<DepartmentProperty>(departmentProperties, { it.getChildren() }))
        departmentTable.setShowRoot(false)
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
