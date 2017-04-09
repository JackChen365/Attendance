package quant.attendance.model;

import java.time.LocalTime;

/**
 * Created by cz on 4/23/16.
 */
public class Attendance {
    public String department;
    public String name;
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;
    public int second;
    public long timeMillis;

    @Override
    public String toString() {
        return year + "/" + month + "/" + day + " " + hour + ":" + minute + ":" + second;
    }

    public long getDayTimeMillis(){
        return LocalTime.of(hour,minute,second).toSecondOfDay()*1000;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Attendance)) {
            return false;
        }
        Attendance item = (Attendance) obj;
        return timeMillis > item.timeMillis;
    }
}
