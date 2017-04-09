package quant.attendance.event

/**
 * Created by cz on 2017/3/9.
 */
class OnExceptionHandleEvent {
    final String stackTrace

    OnExceptionHandleEvent(String stackTrace) {
        this.stackTrace = stackTrace
    }
}
