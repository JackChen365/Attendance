package quant.attendance.exception

import quant.attendance.event.OnExceptionHandleEvent


/**
 * Created by cz on 12/9/16.
 */
class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {
    @Override
    void uncaughtException(Thread t, Throwable e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        new File("${System.currentTimeMillis()}.txt").withWriter {it.write(stackTrace.toString()) }
    }
}
