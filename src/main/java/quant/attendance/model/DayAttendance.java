package quant.attendance.model;

/**
 * Created by cz on 4/23/16.
 */
public class DayAttendance {
    public Attendance startAttendance;
    public Attendance endAttendance;

    public DayAttendance() {
    }

    @Override
    public String toString() {
        return "start:" + startAttendance + " end:" + endAttendance;
    }
}
