package quant.attendance.model;

import java.time.LocalTime;

/**
 * Created by cz on 4/23/16.
 */
public class Attendance implements Cloneable{
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

    @Override
    public Attendance clone() {
        Attendance newItem=new Attendance();
        newItem.department=department;
        newItem.name=name;
        newItem.year=year;
        newItem.month=month;
        newItem.day=day;
        newItem.hour=hour;
        newItem.minute=minute;
        newItem.second=second;
        newItem.timeMillis=timeMillis;
        return newItem;
    }
}
