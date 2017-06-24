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
import quant.attendance.model.dynamic.EmployeeAttendance
import quant.attendance.model.dynamic.RestCode
import quant.attendance.model.dynamic.RestCodeItem
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
    final HashMap<String, HashMap<LocalDate, ArrayList<Attendance>>> attendanceItems=[:]
    final List<EmployeeRest> employeeItems=[]
    final List<UnKnowAttendanceItem> unKnowItems=[]
    final long MIN_TIME_MILLIS=1*60*60*1000
    DepartmentRest departmentRest
    LocalDateTime startDateTime,endDateTime
    List<HolidayItem> holidayItems=[]
    final int minWorkHour
    final Map<String,List<EmployeeAttendance>> employeeAttendanceItems
    final Map<String,RestCodeItem> restCodeItems

    public Analyser(HashMap<String, HashMap<LocalDate, ArrayList<Attendance>>> attendanceItems,DepartmentRest departmentRest, List<EmployeeRest> employeeRests,holidayItems,unKnowItems,employeeAttendanceItems,restCodeItems) {
        if (!attendanceItems) {
            InformantRegistry.instance.notifyMessage("员工出勤信息不存在,请文件是否存在,及员工格式!");
        } else {
            this.attendanceItems.putAll(attendanceItems);
        }
        this.departmentRest=departmentRest
        !employeeRests?:this.employeeItems.addAll(employeeRests)
        !holidayItems?:this.holidayItems.addAll(holidayItems)
        !unKnowItems?:this.unKnowItems.addAll(unKnowItems)
        this.employeeAttendanceItems=employeeAttendanceItems
        this.restCodeItems=restCodeItems
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
        HashMap<String, LinkedHashMap<Integer, DayAttendance>> newAttendanceItems = new HashMap<>();
        attendanceItems.each {name, attendanceItem->
            //记录最小开始时间
            LocalDate newDate=startDateTime.toLocalDate()
            LocalDate endDate=endDateTime.toLocalDate()
            RestCodeItem lastRestItem=getRestItemByDate(name,newDate.plusDays(-1))
            DayAttendance lastDayAttendance
            while(0>=newDate.compareTo(endDate)){
                HashMap<Integer, DayAttendance> newAttendanceItem = newAttendanceItems.get(name);
                if (null == newAttendanceItem) {
                    newAttendanceItem = new LinkedHashMap<>();
                    newAttendanceItems.put(name, newAttendanceItem);
                }
                final DayAttendance dayAttendance = new DayAttendance();
                RestCodeItem restCodeItem=getEmployeeWorkTime(name,newDate.year,newDate.monthValue,newDate.dayOfMonth)
                newAttendanceItem.put(newDate, dayAttendance);
                def items = attendanceItem.get(newDate)
                //如果今天没有考勤记录,也不是休息日,记录一个空的考勤信息
                items?.each {item ->
                    long startEmployeeTime=restCodeItem.startTimeMillis
                    long endEmployeeTime=restCodeItem.endTimeMillis
                    //今天休息,从昨天工作到今天.工作天数横跨一天
                    if(RestCode.WEEK==restCodeItem.code){
                        //昨天上班至今天,今天休息,但早上考勤会连续到今天
                        if(lastRestItem.nextDay&&lastDayAttendance){
                            if(!lastDayAttendance.endAttendance||lastDayAttendance.endAttendance.dayTimeMillis<item.dayTimeMillis&&Math.abs(item.dayTimeMillis-lastRestItem.endTimeMillis)<MIN_TIME_MILLIS){
                                //统计最近一次
                                lastDayAttendance.endAttendance=item
                            }
                        } else if(lastDayAttendance){
                            //这里丢弃了考勤起始天,前的一天的连班的考勤信息
                            if(!dayAttendance.startAttendance){
                                dayAttendance.startAttendance=item
                            } else if(!dayAttendance.endAttendance){
                                dayAttendance.endAttendance=item
                            }
                        }
                    } else {
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
                //记录前一天是否工作到今天
                lastRestItem=restCodeItem
                //记录前一天是否工作到今天
                lastDayAttendance=dayAttendance
                newDate=newDate.plusDays(1)
            }
        }
        InformantRegistry.instance.notifyMessage("二次分析出勤信息完成:${newAttendanceItems.size()} 条数据");
        return newAttendanceItems;
    }

    RestCodeItem getRestItemByDate(String name,LocalDate localDate){
        RestCodeItem restCodeItem
        def attendance = employeeAttendanceItems[name]?.find { it.year == localDate.year && it.mouth == localDate.monthValue && it.day == localDate.dayOfMonth }
        if(attendance){restCodeItem=restCodeItems[attendance.code]}
        return restCodeItem
    }

    RestCodeItem getEmployeeWorkTime(String name,int year,int month,int day){
        //定制员工信息
        EmployeeRest employee = employeeItems.find{it.employeeName==name}
        //获取排班信息
        def attendances = employeeAttendanceItems[name]
        def employeeAttendance = attendances.find { it.year == year && it.mouth == month && it.day == day }
        RestCodeItem restCodeItem
        !employeeAttendance?:(restCodeItem = restCodeItems[employeeAttendance.code])
        //部门单独设定员工
        if(!restCodeItem){
            restCodeItem=new RestCodeItem()
            if(employee){
                restCodeItem.startTimeMillis=employee.startTimeMillis
                restCodeItem.startDate=employee.startDate
                restCodeItem.endTimeMillis=employee.endTimeMillis
                restCodeItem.endDate=employee.endDate
            } else {
                restCodeItem.startTimeMillis=departmentRest.startTimeMillis
                restCodeItem.startDate=departmentRest.startDate
                restCodeItem.endTimeMillis=departmentRest.endTimeMillis
                restCodeItem.endDate=departmentRest.endDate
            }
        }
        restCodeItem
    }
    /**
     * 获取分析结果
     */
    public HashMap<String, LinkedHashMap<LocalDate, AttendanceResult>> result() {
        final HashMap<String, LinkedHashMap<Integer, DayAttendance>> newAttendanceItems = analyzerAttendanceItems()
        final HashMap<String, LinkedHashMap<LocalDate, AttendanceResult>> results = new HashMap<>()

        LocalDate newDate=startDateTime.toLocalDate()
        LocalDate endDate=endDateTime.toLocalDate()
        while(0>=newDate.compareTo(endDate)){
            final int month=newDate.monthValue
            final int dayOfMonth = newDate.dayOfMonth;
            final int dayOfWeek = newDate.dayOfWeek.value
            final def findHolidayItem=holidayItems.find {it.month==month&&it.day==dayOfMonth}
            def name="张满库"
            def items=newAttendanceItems[name]
//            newAttendanceItems.each {name, items->
                //获取当天排班,上一天排班
                final RestCodeItem restCodeItem=getRestItemByDate(name,newDate)
                final RestCodeItem lastRestCodeItem=getRestItemByDate(name,newDate.plusDays(-1))
                //自定义设置休息
                final EmployeeRest employee = employeeItems.find{it.employeeName==name}
                //部门单独设定员工,假日条目
                boolean isWeekend
                boolean isHolidayWork=!findHolidayItem?false:!findHolidayItem.isWork
                if(employee){
                    //判断是否为非工作日加班,1:周末2:非假日调休日上班
                    isWeekend=(!employee.workDays.contains(dayOfWeek)&&!findHolidayItem)||isHolidayWork
                } else if(restCodeItem){
                    //排班计划为休息
                    isWeekend=RestCode.WEEK==restCodeItem.code
                } else {
                    //节日休息,或者排班为休息
                    isWeekend=(!departmentRest.workDays.contains(dayOfWeek)&&!findHolidayItem)||isHolidayWork
                }
                //当用户为不确定入职,离职信息员工
                def unKnowItem=unKnowItems.find {it.name==name}
                boolean unCheckUnKnow=!unKnowItem
                if(unKnowItem){
                    //入职/离职时间限定
                    if(unKnowItem.entryDate&&unKnowItem.departureDate){
                        unCheckUnKnow=0>=unKnowItem.entryDate.compareTo(newDate)&&0<=unKnowItem.departureDate.compareTo(newDate)
                    } else if(unKnowItem.entryDate){
                        unCheckUnKnow=0>=unKnowItem.entryDate.compareTo(newDate)
                    } else if(unKnowItem.departureDate){
                        unCheckUnKnow=0<=unKnowItem.departureDate.compareTo(newDate)
                    }
                }
                //分析休息时间,判定当天是否为休息时间,休息则定为加班
                DayAttendance dayAttendance = items.get(newDate);
                dayAttendance=(dayAttendance.startAttendance||dayAttendance.endAttendance)?dayAttendance:null
                //当存在考勤信息或者不为周末节日时,保存信息,或者员工入职时间等于/小于当天
                LocalDateTime entryDateTime=employee?employee.entryDateTime():null
                if((!entryDateTime||entryDateTime&&0<=newDate.compareTo(entryDateTime.toLocalDate()))&& (dayAttendance||!isWeekend)&&unCheckUnKnow){
                    final AttendanceResult dayResult = new AttendanceResult(name, newDate.year,month,dayOfMonth);
                    //分析结果集
                    HashMap<LocalDate, AttendanceResult> resultItems = results.get(name);
                    if (null == resultItems) {
                        results.put(name, resultItems = new LinkedHashMap<>());
                    }
                    resultItems.put(newDate, dayResult);
                    //标准上班下班时间值(分)
                    if (isWeekend) {
                        weekendAttendance(isHolidayWork,dayAttendance,dayResult,restCodeItem)
                    } else if (dayAttendance) {
                        RestCodeItem restItem=getEmployeeWorkTime(name,newDate.year,newDate.monthValue,newDate.dayOfMonth)
                        workAttendance(restItem,dayAttendance,dayResult)
                    } else {
                        //TODO 增加异常员工信息过滤
                        dayResult.type |= AttendanceType.ABSENTEEISM;//缺勤
                    }
                }
//            }
            newDate=newDate.plusDays(1)
        }
        InformantRegistry.instance.notifyMessage("分析出勤信息完成!");
        return results;
    }

    void workAttendance(RestCodeItem restItem,dayAttendance,AttendanceResult dayResult){
        //上班卡
        long startEmployeeTime=restItem.startTimeMillis
        long endEmployeeTime=restItem.endTimeMillis
        Attendance startAttendance = dayAttendance.startAttendance;
        Attendance endAttendance = dayAttendance.endAttendance;
        Attendance overAttendance=dayAttendance.overAttendance
        if (null != startAttendance) {
            dayResult.startTime = startAttendance.toString()
            //检测上班卡是否迟到
            if (startAttendance.dayTimeMillis > startEmployeeTime) {
                dayResult.type |= AttendanceType.LATE
            } else {
                dayResult.type |= AttendanceType.TO_WORK
            }
        } else {
            dayResult.type |= AttendanceType.UN_CHECK_IN//上班卡未打
        }
        //下班卡
        if (null != endAttendance) {
            dayResult.endTime = endAttendance.toString()
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
        if (endAttendance) {
            dayResult.overMinute =(endEmployeeTime-endAttendance.dayTimeMillis)/1000/60;//加班时长(分)
            if (0<dayResult.overMinute&&dayResult.overMinute >= minWorkHour * 60) {
                dayResult.type |= AttendanceType.OVER_TIME;
            } else if(startAttendance){
                def workTimeMillis = LocalDateTime.of(endAttendance.year, endAttendance.month, endAttendance.day, endAttendance.hour, endAttendance.minute, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                        LocalDateTime.of(startAttendance.year, startAttendance.month, startAttendance.day, startAttendance.hour, startAttendance.minute, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                dayResult.workMinute=workTimeMillis/60/1000
                //工作时间小于正常工作时间1小时,记录未知状态,此时可能请假,出差
                if((restItem.endTimeMillis-restItem.startTimeMillis)-workTimeMillis>1*60*60*1000){
                    dayResult.type |= AttendanceType.UN_KNOW_WORK_TIME;
                }
            }
        }
    }

    void weekendAttendance(isHolidayWork,dayAttendance,AttendanceResult dayResult,RestCodeItem restCodeItem){
        dayResult.isWeekend = true
        if (restCodeItem&&restCodeItem.code==RestCode.WEEK||null==dayAttendance) {
            //休息
            dayResult.type|=AttendanceType.WEEK;
        } else {
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
