package quant.attendance.util

import quant.attendance.excel.InformantRegistry
import quant.attendance.model.Attendance
import quant.attendance.model.AttendanceResult
import quant.attendance.model.AttendanceType
import quant.attendance.model.DayAttendance
import quant.attendance.model.DepartmentRest
import quant.attendance.model.Employee
import quant.attendance.model.EmployeeRest

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Created by Administrator on 2017/4/9.
 */
class Analyser {
    final HashMap<String, HashMap<Integer, ArrayList<Attendance>>> attendanceItems=[:]
    final List<EmployeeRest> employeeItems=[]
    final long MIN_TIME_MILLIS=2*60*60*1000
    DepartmentRest departmentRest
    LocalDateTime startDateTime,endDateTime

    public Analyser(HashMap<String, HashMap<Integer, ArrayList<Attendance>>> attendanceItems,DepartmentRest departmentRest, List<EmployeeRest> employeeRests) {
        if (!attendanceItems) {
            InformantRegistry.instance.notifyMessage("员工出勤信息不存在,请文件是否存在,及员工格式!");
        } else {
            this.attendanceItems.putAll(attendanceItems);
        }
        this.departmentRest=departmentRest
        this.employeeItems.addAll(employeeRests)
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
                    Attendance startAttendance = dayAttendance.startAttendance;
                    Attendance endAttendance = dayAttendance.endAttendance;
                    long startEmployeeTime,endEmployeeTime
                    (startEmployeeTime,endEmployeeTime)=getEmployeeWorkTime(name)
                    if (null == startAttendance) {
                        //默认时间为上午,或者在员工上下班时间内1个小时,且处是为1为常规考虑,2为非常规,比如16点上班员工
                        if (Math.abs(item.dayTimeMillis-startEmployeeTime) < MIN_TIME_MILLIS) {
                            dayAttendance.startAttendance = item;//先记录早的
                        }
                    } else if (dayAttendance.startAttendance.timeMillis > item.timeMillis) {
                        dayAttendance.startAttendance = item;
                    }
                    if (null == endAttendance) {
                        //默认为时间在下午,员工下班时间2个小时以内的,算下班打算
                        if (Math.abs(item.dayTimeMillis-endEmployeeTime) < MIN_TIME_MILLIS) {
                            dayAttendance.endAttendance = item;//记录晚
                        }
                    } else if (dayAttendance.endAttendance.timeMillis < item.timeMillis) {
                        dayAttendance.endAttendance = item;
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
        while(!newDateTime.toLocalDate().equals(endDateTime.toLocalDate())){
            final int today = newDateTime.dayOfMonth;
            final int dayOfWeek = newDateTime.dayOfWeek.value
            newAttendanceItems.each {name, items->
                EmployeeRest employee = employeeItems.find{it.employeeName==name}
                //自定义设置休息
                boolean isWeekend
                //部门单独设定员工
                if(employee){
                    isWeekend=employee.workDays.contains(dayOfWeek)
                } else {
                    isWeekend=departmentRest.workDays.contains(dayOfWeek)
                }
                long startEmployeeTime,endEmployeeTime
                (startEmployeeTime,endEmployeeTime)=getEmployeeWorkTime(name)
                //分析休息时间,判定当天是否为休息时间,休息则定为加班
                DayAttendance dayAttendance = items.get(today);
                final AttendanceResult dayResult = new AttendanceResult(name, today);
                //分析结果集
                if (null != dayAttendance) {
                    HashMap<Integer, AttendanceResult> resultItems = results.get(name);
                    if (null == resultItems) {
                        resultItems = new HashMap<>();
                        results.put(name, resultItems);
                    }
                    resultItems.put(today, dayResult);
                }
                //标准上班下班时间值(分)
                if (isWeekend) {
                    dayResult.isWeekend = isWeekend;
                    if (null != dayAttendance) {
                        Attendance startAttendance = dayAttendance.startAttendance;
                        Attendance endAttendance = dayAttendance.endAttendance;
                        //休息日加班,不按正常出勤日期算,只按此人正常上班时间
                        dayResult.type |= AttendanceType.WEEKEND_OVER_TIME;
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
                } else if (null != dayAttendance) {
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
                            //根据上班时间计算下班时间是否超时
                            //工作时长,正常上班总时间为9个小时,加上午休1小时,而三班倒人员,只上8小时,所以这里动态化
                            if (endAttendance.dayTimeMillis >= endEmployeeTime) {
                                dayResult.type |= AttendanceType.OFF_WORK;//正常下班
                            } else {
                                dayResult.type |= AttendanceType.LEVEL_EARLY;//早退了
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
                        int type = dayResult.type;
                        if (type == (type | AttendanceType.TO_WORK) && type == (type | AttendanceType.OFF_WORK)) {
                            int workMinute = endAttendance.hour * 60 + endAttendance.minute - startAttendance.hour * 60 + startAttendance.minute;
                            //超出俩小时,算加班
                            dayResult.overMinute = workMinute - endEmployeeTime;//加班时长(分)
                            if (dayResult.overMinute >= 2 * 60) {
                                dayResult.type |= AttendanceType.OVER_TIME;
                            }
                        }
                    }
                } else {
                    dayResult.type |= AttendanceType.ABSENTEEISM;//缺勤
                }
            }
            newDateTime=newDateTime.plusDays(1)
        }
        InformantRegistry.instance.notifyMessage("分析出勤信息完成!");
        return results;
    }

}
