package quant.attendance.model;

import java.time.LocalDateTime;

/**
 * Created by cz on 4/23/16.
 * employee result
 */
public class AttendanceResult {
    public final String name;//姓名
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

    public AttendanceResult(String name, int day) {
        this.name = name;
        this.day = day;
    }
}
