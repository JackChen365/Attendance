package quant.attendance.util

import quant.attendance.excel.InformantRegistry
import quant.attendance.model.Attendance
import quant.attendance.model.UnKnowAttendanceItem
import quant.attendance.model.dynamic.EmployeeAttendance
import quant.attendance.model.dynamic.RestCode
import quant.attendance.model.dynamic.RestCodeItem

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Created by Administrator on 2017/4/21.
 */
class AnalyserUnKnow {
    final HashMap<String, HashMap<LocalDate, ArrayList<Attendance>>> attendanceItems
    final Map<String,List<EmployeeAttendance>> employeeAttendanceItems
    final Map<String,RestCodeItem> restCodeItems
    final def holidays=[]
    AnalyserUnKnow(HashMap<String, HashMap<LocalDate, ArrayList<Attendance>>> attendanceItems,employeeAttendanceItems,restCodeItems,holidays) {
        this.attendanceItems = [:]
        this.employeeAttendanceItems=employeeAttendanceItems
        this.restCodeItems=restCodeItems
        !holidays?:this.holidays.addAll(holidays)
        if(attendanceItems){
            this.attendanceItems.putAll(attendanceItems)
        }
    }

    /**
     * 分析出勤信息
     */
    def analyzerDate() {
        def startSecondMillis=0,endSecondMillis=0
        attendanceItems.each { name, attendanceItem->
            attendanceItem.each { localDate, items->
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
        def startDateTime=getNormalDate(LocalDateTime.ofInstant(new Date(startSecondMillis).toInstant(),ZoneId.systemDefault()).toLocalDate(),true)
        def endDateTime=getNormalDate(LocalDateTime.ofInstant(new Date(endSecondMillis).toInstant(),ZoneId.systemDefault()).toLocalDate(),false)
        [startDateTime,endDateTime]
    }

    /**
     * 获得正常的月考勤日期,因最大结束日期可能存在节假日信息
     */
    def getNormalDate(LocalDate localDate,boolean previous){
        def findClosure={ date-> holidays.find {it.year==date.year&&it.month==date.monthValue&&it.day==date.dayOfMonth} }
        def holiday=findClosure.call(localDate)
        while(holiday&&!holiday.isWork){
            localDate=localDate.plusDays(previous?1:-1)
            holiday=findClosure.call(localDate)
        }
        localDate
    }



    /**
     * 分析月初未上班人群,以及月末未上班人群,可能存在,新入职人员,以及离职人员,供用户查看
     * @return
     */
    public def analyzerUnKnowItems(){
        LocalDate startDate,endDate
        (startDate,endDate)=analyzerDate()
        final def unKnowItems=[]
        attendanceItems.each { name,value->
            //查找月初
            def dayItems=value.keySet()
            LocalDate entryDate, departureDate
            //找月初小于
            if(0!=startDate.compareTo(dayItems[0])){
                entryDate=findInterceptDate(name,startDate,dayItems[0],)
            }
            if(0!=endDate.compareTo(dayItems[-1])){
                departureDate=findInterceptDate(name,dayItems[-1],endDate,false)
            }
            //查找月末
            if(entryDate||departureDate){
                unKnowItems<<new UnKnowAttendanceItem(name,entryDate,departureDate)
            }
        }
        unKnowItems
    }

    def findInterceptDate(String name,LocalDate startDate,LocalDate endDate,boolean next=true){
        LocalDate localDate
        LocalDate newDate=startDate
        while(0!=newDate.compareTo(endDate)){
            def restCodeItem = getRestItemByDate(name, newDate)
            if(!restCodeItem){
                localDate=next?endDate:startDate
                break;
            } else if(restCodeItem.code!=RestCode.WEEK){
                localDate=newDate
            }
            newDate=newDate.plusDays(1)
        }
        return localDate
    }

    RestCodeItem getRestItemByDate(String name,LocalDate localDate){
        RestCodeItem restCodeItem
        def attendance = employeeAttendanceItems[name]?.find { it.year == localDate.year && it.mouth == localDate.monthValue && it.day == localDate.dayOfMonth }
        if(attendance){restCodeItem=restCodeItems[attendance.code]}
        return restCodeItem
    }
}
