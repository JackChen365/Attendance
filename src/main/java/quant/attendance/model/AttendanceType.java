package quant.attendance.model;

/**
 * Created by cz on 4/23/16.
 * 员工出勤所有状态,
 * 所有状态可自由组合
 * like:
 * type|=TO_WORK
 * type|=OFF_WORK
 * type|=OVER_TIME
 * <p/>
 * 代表该员工,正常上班,且加班
 * type|=LATE
 * type|=UN_CHECK_OUT
 * 代表该员工早上迟到,晚上未打卡.等等.方便后期计算
 */
public interface AttendanceType {

    /**
     * 正常上班
     */
    int TO_WORK = 0x01;
    /**
     * 正常下班
     */
    int OFF_WORK = 0x02;
    /**
     * 迟到
     */
    int LATE = 0x04;
    /**
     * 早退
     */
    int LEVEL_EARLY = 0x08;
    /**
     * 翘班
     */
    int ABSENTEEISM = 0x10;
    /**
     * 早上卡未打
     */
    int UN_CHECK_IN = 0x20;
    /**
     * 晚上卡未打
     */
    int UN_CHECK_OUT = 0x40;
    /**
     * 平时加班
     */
    int OVER_TIME = 0x80;
    /**
     * 周末加班了
     */
    int WEEKEND_OVER_TIME = 0x100;

    /**
     * 节日加班
     */
    int HOLIDAY_OVER_TIME=0x200;
    /**
     * 正常上班
     */
    int NORMA = TO_WORK | OFF_WORK;


}
