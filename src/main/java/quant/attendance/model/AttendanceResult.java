package quant.attendance.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cz on 4/23/16.
 * employee result
 */
public class AttendanceResult {
    public final String name;//姓名
    public final int year;
    public final int month;
    public final int day;//天
    public String department;//部门
    public String startTime;//开始时间
    public String endTime;//结束时间
    public LocalDateTime incomeDate;//入职日期
    public String departureDate;//离职日期
    public boolean isWeekend;//周末
    public int overMinute;//加班时间(分)
    public int workMinute;
    public int type;

    public AttendanceResult(String name,String department, int year, int month, int day) {
        this.name = name;
        this.department=department;
        this.year = year;
        this.month = month;
        this.day = day;
    }

    @Override
    public String toString() {
        List<String> typeValues=new ArrayList<>();
        if(0!=(AttendanceType.LATE&type)){typeValues.add("迟到");}
        if(0!=(AttendanceType.ABSENTEEISM&type)){typeValues.add("旷工");}
        if(0!=(AttendanceType.HOLIDAY_OVER_TIME&type)){typeValues.add("节日加班");}
        if(0!=(AttendanceType.LEVEL_EARLY&type)){typeValues.add("早退");}
        if(0!=(AttendanceType.OFF_WORK&type)){typeValues.add("正常下班");}
        if(0!=(AttendanceType.OVER_TIME&type)){typeValues.add("平时加班");}
        if(0!=(AttendanceType.TO_WORK&type)){typeValues.add("正常上班");}
        if(0!=(AttendanceType.UN_CHECK_IN&type)){typeValues.add("上班未打卡");}
        if(0!=(AttendanceType.UN_CHECK_OUT&type)){typeValues.add("下班未打卡");}
        if(0!=(AttendanceType.UN_KNOW_WORK_TIME&type)){typeValues.add("未知情况");}
        if(0!=(AttendanceType.WEEK&type)){typeValues.add("休息");}
        return name+" "+year+"/"+month+"/"+day+" "+typeValues;
    }
}
