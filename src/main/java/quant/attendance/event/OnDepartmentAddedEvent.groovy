package quant.attendance.event

/**
 * Created by Administrator on 2017/4/9.
 */
class OnDepartmentAddedEvent {
    final def departmentItem

    OnDepartmentAddedEvent(departmentItem) {
        this.departmentItem = departmentItem
    }
}
