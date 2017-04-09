package quant.attendance.event

/**
 * Created by Administrator on 2017/4/9.
 */
class OnEmployeeAddedEvent {
    final def employeeItem

    OnEmployeeAddedEvent(employeeItem) {
        this.employeeItem = employeeItem
    }
}
