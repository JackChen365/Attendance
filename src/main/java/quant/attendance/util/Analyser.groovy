package quant.attendance.util

import quant.attendance.excel.InformantRegistry
import quant.attendance.model.Attendance
import quant.attendance.model.AttendanceResult
import quant.attendance.model.AttendanceType
import quant.attendance.model.DayAttendance
import quant.attendance.model.DepartmentRest
import quant.attendance.model.EmployeeRest
import quant.attendance.model.HolidayItem
import quant.attendance.model.UnKnowAttendanceItem
import quant.attendance.prefs.PrefsKey
import quant.attendance.prefs.SharedPrefs
import quant.attendance.ui.SettingController

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Created by Administrator on 2017/4/9.
 */
class Analyser {
    final HashMap<String, HashMap<Integer, ArrayList<Attendance>>> attendanceItems=[:]
    final List<EmployeeRest> employeeItems=[]
    final List<UnKnowAttendanceItem> unKnowItems=[]
    final long MIN_TIME_MILLIS=2*60*60*1000
    DepartmentRest departmentRest
    LocalDateTime startDateTime,endDateTime
    List<HolidayItem> holidayItems=[]
    final int minWorkHour

    public Analyser(HashMap<String, HashMap<Integer, ArrayList<Attendance>>> attendanceItems,DepartmentRest departmentRest, List<EmployeeRest> employeeRests,holidayItems,unKnowItems) {
        if (!attendanceItems) {
            InformantRegistry.instance.notifyMessage("员工出勤信息不存在,请文件是否存在,及员工格式!");
        } else {
            this.attendanceItems.putAll(attendanceItems);
        }
        this.departmentRest=departmentRest
        !employeeRests?:this.employeeItems.addAll(employeeRests)
        !holidayItems?:this.holidayItems.addAll(holidayItems)
        !unKnowItems?:this.unKnowItems.addAll(unKnowItems)
        //最小加班时长
        def workHourValue=SharedPrefs.get(PrefsKey.WORK_HOUR)
        minWorkHour=workHourValue?Integer.valueOf(workHourValue):SettingController.DEFAULT_WORD_HOUR
        //分析出勤日期
        (startDateTime,endDateTime)=analyzerDate()
    }

    /**
     * 分析出勤信息
     */
    def analyzerDate() {
        def startSecondMillis=0,endSecondMillis=0
        attendanceItems.each { name, attendanceItem->
            attendanceItem.each { day, items->
                items.each {
                    def attendanceTimeMillis=LocalDateTime.of(it.year,it.month,it.day,it.hour,it.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    if (0==startSecondMillis||startSecondMillis> attendanceTimeMillis) {
                        startSecondMillis=attendanceTimeMillis
                    } else if(0==endSecondMillis||endSecondMillis<attendanceTimeMillis){
                        endSecondMillis=attendanceTimeMillis
                    }
                }
            }
        }
        //分析出勤信息
        def startDateTime=LocalDateTime.ofInstant(new Date(startSecondMillis).toInstant(),ZoneId.systemDefault())
        def endDateTime=LocalDateTime.ofInstant(new Date(endSecondMillis).toInstant(),ZoneId.systemDefault())
        InformantRegistry.instance.notifyMessage("计算出勤信息为:$startDateTime.year/$startDateTime.monthValue 起始天:$startDateTime.dayOfMonth  结束日期:$endDateTime.monthValue 月$endDateTime.dayOfMonth")
        [startDateTime,endDateTime]
    }

    /**
     * 二次分析出勤记录,将多条记录的信息,分解为,最早上班日期-最晚上班日期集
     *
     * @return
     */
    private HashMap<String, HashMap<Integer, DayAttendance>> analyzerAttendanceItems() {
        //2:简化出勤信息,提取出当前最早打卡时间,最晚打卡时间
        HashMap<String, HashMap<Integer, DayAttendance>> newAttendanceItems = new HashMap<>();
        attendanceItems.each {name, attendanceItem->
            //记录最小开始时间
            attendanceItem.each {day, items->
                HashMap<Integer, DayAttendance> newAttendanceItem = newAttendanceItems.get(name);
                if (null == newAttendanceItem) {
                    newAttendanceItem = new HashMap<>();
                    newAttendanceItems.put(name, newAttendanceItem);
                }
                final DayAttendance dayAttendance = new DayAttendance();
                newAttendanceItem.put(day, dayAttendance);
                items.each {item ->
                    long startEmployeeTime,endEmployeeTime
                    (startEmployeeTime,endEmployeeTime)=getEmployeeWorkTime(name)
                    if (!dayAttendance.startAttendance&&(item.dayTimeMillis<startEmployeeTime||item.dayTimeMillis-startEmployeeTime<MIN_TIME_MILLIS)) {
                        //默认时间为上午,员工上班时间小于正常考勤时间即记录,大过的话,显示为迟到.预定在2小时以内
                        dayAttendance.startAttendance = item;//先记录早的
                    } else if (dayAttendance.startAttendance&&dayAttendance.startAttendance.timeMillis > item.timeMillis) {
                        dayAttendance.startAttendance = item;
                    } else if (!dayAttendance.endAttendance&&(item.dayTimeMillis>endEmployeeTime||endEmployeeTime-item.dayTimeMillis<MIN_TIME_MILLIS)) {
                        //默认为时间在下午,员工大过考勤时间,或在考勤时间范围内2小时,可能存在早退
                        dayAttendance.endAttendance = item;//记录晚
                    } else if (dayAttendance.endAttendance&&dayAttendance.endAttendance.timeMillis < item.timeMillis) {
                        dayAttendance.endAttendance = item;
                    } else if(!dayAttendance.startAttendance&&!dayAttendance.endAttendance){
                        //如果两个都为空,则判断条目靠近上午,还是下午
                        if((item.dayTimeMillis-startEmployeeTime)/2<(endEmployeeTime-startEmployeeTime)){
                            dayAttendance.startAttendance = item;
                        } else {
                            dayAttendance.endAttendance = item;
                        }
                    } else if(!dayAttendance.startAttendance){
                        dayAttendance.startAttendance=item
                    } else if(!dayAttendance.endAttendance){
                        dayAttendance.endAttendance=item
                    }
                }
            }
        }
        InformantRegistry.instance.notifyMessage("二次分析出勤信息完成:${newAttendanceItems.size()} 条数据");
        return newAttendanceItems;
    }

    def getEmployeeWorkTime(name){
        EmployeeRest employee = employeeItems.find{it.employeeName==name}
        //自定义设置休息
        int startEmployeeTime,endEmployeeTime
        //部门单独设定员工
        if(employee){
            startEmployeeTime=employee.startTimeMillis
            endEmployeeTime=employee.endTimeMillis
        } else {
            startEmployeeTime=departmentRest.startTimeMillis
            endEmployeeTime=departmentRest.endTimeMillis
        }
        [startEmployeeTime,endEmployeeTime]
    }
    /**
     * 获取分析结果
     */
    public HashMap<String, HashMap<Integer, AttendanceResult>> result() {
        HashMap<String, HashMap<Integer, DayAttendance>> newAttendanceItems = analyzerAttendanceItems()
        final HashMap<String, HashMap<Integer, AttendanceResult>> results = new HashMap<>()

        LocalDateTime newDateTime=startDateTime
        while(0>=newDateTime.toLocalDate().compareTo(endDateTime.toLocalDate())){
            final int month=newDateTime.monthValue
            final int dayOfMonth = newDateTime.dayOfMonth;
            final int dayOfWeek = newDateTime.dayOfWeek.value
            final def findHolidayItem=holidayItems.find {it.month==month&&it.day==dayOfMonth}
            newAttendanceItems.each {name, items->
                //自定义设置休息
                final EmployeeRest employee = employeeItems.find{it.employeeName==name}
                //部门单独设定员工,假日条目
                boolean isWeekend
                boolean isHolidayWork=!findHolidayItem?false:!findHolidayItem.isWork
                if(employee){
                    //判断是否为非工作日加班,1:周末2:非假日调休日上班
                    isWeekend=!employee.workDays.contains(dayOfWeek)||isHolidayWork
                } else {
                    isWeekend=!departmentRest.workDays.contains(dayOfWeek)||isHolidayWork
                }
                //当用户为不确定入职,离职信息员工
                def unKnowItem=unKnowItems.find {it.name==name}
                boolean unCheckUnKnow=!unKnowItem
                if(unKnowItem){
                    //入职/离职时间限定
                    if(unKnowItem.entryDate&&unKnowItem.departureDate){
                        unCheckUnKnow=unKnowItem.entryDate&&unKnowItem.entryDate.dayOfMonth<=dayOfMonth&&unKnowItem.departureDate&&unKnowItem.departureDate.dayOfMonth>=dayOfMonth
                    } else if(unKnowItem.entryDate){
                        unCheckUnKnow=unKnowItem.entryDate&&unKnowItem.entryDate.dayOfMonth<=dayOfMonth
                    } else if(unKnowItem.departureDate){
                        unCheckUnKnow=unKnowItem.departureDate&&unKnowItem.departureDate.dayOfMonth>=dayOfMonth
                    }
                }
                //分析休息时间,判定当天是否为休息时间,休息则定为加班
                DayAttendance dayAttendance = items.get(dayOfMonth);
                //当存在考勤信息或者不为周末节日时,保存信息,或者员工入职时间等于/小于当天
                LocalDateTime entryDateTime=employee?employee.entryDateTime():null
                if((!entryDateTime||entryDateTime&&0<=newDateTime.compareTo(entryDateTime))&& (dayAttendance||!isWeekend)&&unCheckUnKnow){
                    final AttendanceResult dayResult = new AttendanceResult(name, dayOfMonth);
                    //分析结果集
                    HashMap<Integer, AttendanceResult> resultItems = results.get(name);
                    if (null == resultItems) {
                        results.put(name, resultItems = new HashMap<>());
                    }
                    resultItems.put(dayOfMonth, dayResult);
                    //标准上班下班时间值(分)
                    if (isWeekend) {
                        weekendAttendance(isHolidayWork,dayAttendance,dayResult)
                    } else if (dayAttendance) {
                        long startEmployeeTime,endEmployeeTime
                        (startEmployeeTime,endEmployeeTime)=getEmployeeWorkTime(name)
                        workAttendance(startEmployeeTime,endEmployeeTime,dayAttendance,dayResult)
                    } else {
                        //TODO 增加异常员工信息过滤
                        dayResult.type |= AttendanceType.ABSENTEEISM;//缺勤
                    }
                }
            }
            newDateTime=newDateTime.plusDays(1)
        }
        InformantRegistry.instance.notifyMessage("分析出勤信息完成!");
        return results;
    }

    void workAttendance(long startEmployeeTime,long endEmployeeTime,dayAttendance,AttendanceResult dayResult){
        //上班卡
        Attendance startAttendance = dayAttendance.startAttendance;
        Attendance endAttendance = dayAttendance.endAttendance;
        if (null != startAttendance) {
            dayResult.startTime = startAttendance.toString();
            //检测上班卡是否迟到
            if (startAttendance.dayTimeMillis > startEmployeeTime) {
                dayResult.type |= AttendanceType.LATE;
            } else {
                dayResult.type |= AttendanceType.TO_WORK;
            }
        } else {
            dayResult.type |= AttendanceType.UN_CHECK_IN;//上班卡未打
        }
        //下班卡
        if (null != endAttendance) {
            dayResult.endTime = endAttendance.toString();
            if (null != startAttendance) {
                //工作时长,正常上班总时间减去实际时间,如果早上没迟到,则按工作时长计算,否则按下班时间算
                if(0!=(dayResult.type&AttendanceType.LATE)){
                    //早上已经迟到
                    if (endAttendance.dayTimeMillis>=endEmployeeTime){
                        dayResult.type |= AttendanceType.OFF_WORK;//正常下班
                    } else {
                        dayResult.type |= AttendanceType.LEVEL_EARLY;//早退了
                    }
                } else {
                    //早上未迟到
                    if (endAttendance.dayTimeMillis-startAttendance.dayTimeMillis >= endEmployeeTime-startEmployeeTime) {
                        dayResult.type |= AttendanceType.OFF_WORK;//正常下班
                    } else {
                        dayResult.type |= AttendanceType.LEVEL_EARLY;//早退了
                    }
                }
            } else {
                //上班未打卡,根据标准时间计算
                if (endAttendance.timeMillis>endEmployeeTime) {
                    dayResult.type |= AttendanceType.OFF_WORK;//正常下班
                } else {
                    //早上未打卡,早退
                    dayResult.type |= AttendanceType.LEVEL_EARLY;
                }
            }
        } else {
            //下班卡未打
            dayResult.type |= AttendanceType.UN_CHECK_OUT;//早退了
        }
        //计算加班
        if (null != startAttendance && null != endAttendance) {
            int workMinute = (endAttendance.hour * 60 + endAttendance.minute) - (startAttendance.hour * 60 + startAttendance.minute);
            //超出几小时,算加班
            dayResult.overMinute = workMinute - (endEmployeeTime-startEmployeeTime)/60/1000;//加班时长(分)
            if (0<dayResult.overMinute&&dayResult.overMinute >= minWorkHour * 60) {
                dayResult.type |= AttendanceType.OVER_TIME;
            }
        }
    }

    void weekendAttendance(isHolidayWork,dayAttendance,AttendanceResult dayResult){
        dayResult.isWeekend = true
        if (null != dayAttendance) {
            Attendance startAttendance = dayAttendance.startAttendance;
            Attendance endAttendance = dayAttendance.endAttendance;
            //休息日加班,不按正常出勤日期算,只按此人正常上班时间
            if(isHolidayWork){
                dayResult.type |= AttendanceType.HOLIDAY_OVER_TIME;
            } else {
                dayResult.type |= AttendanceType.WEEKEND_OVER_TIME;
            }
            if (null != startAttendance) {
                dayResult.type |= AttendanceType.TO_WORK;
                dayResult.startTime = startAttendance.toString();
            } else {
                dayResult.type |= AttendanceType.UN_CHECK_IN;//上班卡未打
            }
            if (null != endAttendance) {
                dayResult.type |= AttendanceType.OFF_WORK;
                dayResult.endTime = endAttendance.toString();
            } else {
                dayResult.type |= AttendanceType.UN_CHECK_OUT;//下班卡未打
            }
            //加班时间
            if (null != startAttendance && null != endAttendance) {
                dayResult.overMinute = (endAttendance.hour * 60 + endAttendance.minute) - (startAttendance.hour * 60 + startAttendance.minute);
            }
        }
    }

}
