package quant.attendance.util

import quant.attendance.excel.InformantRegistry
import quant.attendance.model.Attendance
import quant.attendance.model.UnKnowAttendanceItem

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Created by Administrator on 2017/4/21.
 */
class AnalyserUnKnow {
    final HashMap<String, HashMap<Integer, ArrayList<Attendance>>> attendanceItems

    AnalyserUnKnow(HashMap<String, HashMap<Integer, ArrayList<Attendance>>> attendanceItems) {
        this.attendanceItems = [:]
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
        [startDateTime,endDateTime]
    }

    /**
     * 分析月初未上班人群,以及月末未上班人群,可能存在,新入职人员,以及离职人员,供用户查看
     * @return
     */
    public def analyzerUnKnowItems(){
        def startDateTime,endDateTime
        (startDateTime,endDateTime)=analyzerDate()
        final int year=startDateTime.year
        final int month=startDateTime.monthValue
        final int startDay=startDateTime.dayOfMonth
        final int endDay=endDateTime.dayOfMonth
        final def unKnowItems=[]
        attendanceItems.each {
            if(it.value){
                //查找月初
                def dayItems=it.value.keySet()
                LocalDate entryDate, departureDate
                if(startDay<dayItems[0]){
                    entryDate=LocalDate.of(year,month,dayItems[0])
                }
                //查找月末
                if(endDay>dayItems[-1]){
                    departureDate=LocalDate.of(year,month,dayItems[-1])
                }
                if(entryDate||departureDate){
                    unKnowItems<<new UnKnowAttendanceItem(it.key,entryDate,departureDate)
                }
            }
        }
        unKnowItems
    }
}
