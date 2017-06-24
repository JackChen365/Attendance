package quant.attendance.excel.writer

import jxl.CellView
import jxl.format.Alignment
import jxl.write.*
import quant.attendance.excel.InformantRegistry
import quant.attendance.model.AttendanceResult
import quant.attendance.model.AttendanceType
import quant.attendance.model.DepartmentRest
import quant.attendance.model.EmployeeRest
import quant.attendance.model.dynamic.EmployeeAttendance
import quant.attendance.model.dynamic.RestCode
import quant.attendance.model.dynamic.RestCodeItem

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime


//----------------------------------------------------------
// thank you groovy static import!
//----------------------------------------------------------
import static jxl.format.Colour.VIOLET2 as COLOR_ABSENTEEISM
import static jxl.format.Colour.BLUE2 as COLOR_LATE
import static jxl.format.Colour.DARK_BLUE2 as COLOR_UN_CHECK_IN
import static jxl.format.Colour.DARK_RED2 as COLOR_UN_CHECK_OUT
import static jxl.format.Colour.LIGHT_TURQUOISE2 as COLOR_LEVEL_EARLY
import static jxl.format.Colour.PINK2 as COLOR_OVER_TIME
import static jxl.format.Colour.PLUM2 as COLOR_WEEKEND_OVER_TIME
import static jxl.format.Colour.TEAL2 as COLOR_HOLIDAY_OVER_TIME
import static jxl.format.Colour.YELLOW2 as COLOR_UN_KNOW_WORK
/**
 * Created by Administrator on 2017/4/9.
 */
class ExcelWriter extends AbsExcelWriter{
    private final int INFO_LENGTH = 4;

    ExcelWriter(LocalDateTime startDateTime, LocalDateTime endDateTime, HashMap<String, HashMap<LocalDate, AttendanceResult>> results, DepartmentRest departmentRest, Object employeeItems,def holidayItems,unKnowItems,employeeAttendanceItems,restCodeItems) {
        super(startDateTime, endDateTime, results, departmentRest, employeeItems,holidayItems,unKnowItems,employeeAttendanceItems,restCodeItems)
    }

    @Override
    void write(WritableWorkbook wwb, LocalDateTime startDateTime, LocalDateTime endDateTime, HashMap<String, HashMap<LocalDate, AttendanceResult>> results, List<EmployeeRest> employeeItems,holidayItems) throws WriteException {
        writeSheet1(wwb,startDateTime,endDateTime,results,employeeItems)
        writeSheet2(wwb,startDateTime,endDateTime,results,employeeItems)
        writeSheet3(wwb,startDateTime,endDateTime,results,employeeItems,holidayItems)
    }
/**
     * 写入汇总数据
     * 将汇总所有员工 本月所有天数的出勤信息,与异常信息
     *
     * @param startDateTime          计算起始日期
     * @param endDateTime         计算结束日期
     * @param wwb           excel对象
     * @param results       操作数据集
     * @param employeeItems 操作员工信息集
     * @param unEmployee    未录入员工集
     * @throws WriteException
     */
    private void writeSheet1(WritableWorkbook wwb,LocalDateTime startDateTime,LocalDateTime endDateTime,
                        HashMap<String, HashMap<LocalDate, AttendanceResult>> results,List<EmployeeRest> employeeItems) throws WriteException {
        // 添加第一个工作表并设置第一个Sheet的名字
        WritableSheet[] sheets = wwb.getSheets();
        WritableSheet sheet = wwb.createSheet("${startDateTime.year}_${startDateTime.monthValue}考勤", !sheets ? 0 : sheets.length)
        //结构==
        //              3/1(3格)
        //姓名(上下两格)   上班  下班  加班
        //添加第一列,姓名
        int length = endDateTime.dayOfYear - startDateTime.dayOfYear + INFO_LENGTH + 1;
        String[] titles = new String[length];
        titles[0] = "姓名";
        titles[1] = "上班时间";
        titles[2] = "下班时间";
        titles[3] = "休息时间";
        int titleIndex=INFO_LENGTH
        def newDate = startDateTime.toLocalDate()
        def endDate = endDateTime.toLocalDate()
        while(0>=newDate.compareTo(endDate)){
            titles[titleIndex++] = newDate.monthValue + "/" + newDate.dayOfMonth;
            newDate=newDate.plusDays(1)
        }
        String[] attendanceTitles = ["出勤编号","上班时间","下班时间","实际上班", "实际下班", "加班"]
        String[] weekDays = ["一", "二", "三", "四", "五", "六", "日"]
        //插入标题
        for (int i = 0; i < length; i++) {
            if (INFO_LENGTH > i) {
                sheet.addCell(new Label(i, 1, titles[i]));
            } else {
                //0 1 2 3 4 7 10 13
                //0 1 2 3 4 5 6  7
                WritableCellFormat wc = getCellFormat();
                int dayOfWeek =LocalDate.of(startDateTime.year, startDateTime.monthValue, i - INFO_LENGTH + 1).getDayOfWeek().getValue();
                if (6 <= dayOfWeek) {
                    wc.setBorder(Border.ALL, BorderLineStyle.THIN);
                    wc.setBackground(jxl.format.Colour.YELLOW);
                }
                int start = getDayIndex(i,attendanceTitles.length);
                sheet.mergeCells(start, 0,start+ attendanceTitles.length-1, 0);
                sheet.addCell(new Label(start, 0, titles[i] + "(星期" + weekDays[dayOfWeek - 1] + ")", wc));
                //插入副title
                for (int k = 0; k < attendanceTitles.length; k++) {
                    sheet.addCell(new Label(start + k, 1, attendanceTitles[k], wc));
                }
            }
        }
        //插入数据
        int index = 2;
        WritableCellFormat wc = getCellFormat();
        for (Map.Entry<String, HashMap<LocalDate, AttendanceResult>> entry : results.entrySet()) {
            String name = entry.getKey();
            HashMap<LocalDate, AttendanceResult> resultItems = entry.getValue();
            sheet.addCell(new Label(0, index, name, wc));//插入名字
            EmployeeRest employee = employeeItems.find({it.employeeName==name});
            CellView cellView = new CellView();
            cellView.setAutosize(true); //设置自动大小
            sheet.setColumnView(1, cellView);//根据内容自动设置列宽
            //插入员工配置信息
            def attendances = employeeAttendanceItems[name]
            if(attendances&&attendances.any {it.year==newDate.year&&it.mouth==newDate.monthValue&&it.day==newDate.dayOfMonth}){
                sheet.addCell(new Label(1, index, "###", wc));
                sheet.addCell(new Label(2, index, "###", wc));
                sheet.addCell(new Label(3, index, "###", wc));
            } else {
                sheet.addCell(new Label(1, index, employee?employee.startDate:departmentRest.startDate, getCellFormat(Alignment.LEFT)));
                sheet.addCell(new Label(2, index, employee?employee.endDate:departmentRest.endDate, wc));
                sheet.addCell(new Label(3, index, employee?employee.workDays.join(","):departmentRest.workDays.join(","), wc));
            }
            titleIndex=INFO_LENGTH
            final int finalIndex = index;
            newDate = startDateTime.toLocalDate()
            endDate = endDateTime.toLocalDate()
            while(0>=newDate.compareTo(endDate)){
                try {
                    titles[titleIndex++] = newDate.monthValue + "/" + newDate.dayOfMonth;
                    int dayIndex = getDayIndex(newDate.dayOfMonth + INFO_LENGTH - 1,attendanceTitles.length);
                    //出勤信息编号
                    if(attendances&&attendances.any {it.year==newDate.year&&it.mouth==newDate.monthValue&&it.day==newDate.dayOfMonth}){
                        def attendance = attendances.find { it.year == newDate.year && it.mouth == newDate.monthValue && it.day == newDate.dayOfMonth }
                        def restCodeItem = restCodeItems[attendance.code]
                        sheet.addCell(new Label(dayIndex, finalIndex, RestCode.WEEK==restCodeItem.code?"休":restCodeItem.code, getCellFormat(RestCode.WEEK==restCodeItem.code?COLOR_HOLIDAY_OVER_TIME:null,Alignment.CENTRE)));//出勤编号
                        sheet.addCell(new Label(dayIndex + 1, finalIndex, restCodeItem.startDate, getCellFormat(RestCode.WEEK==restCodeItem.code?COLOR_HOLIDAY_OVER_TIME:null,Alignment.CENTRE)));//上班时间
                        sheet.addCell(new Label(dayIndex + 2, finalIndex, restCodeItem.endDate, getCellFormat(RestCode.WEEK==restCodeItem.code?COLOR_HOLIDAY_OVER_TIME:null,Alignment.CENTRE)));//下班时间
                    } else {
                        sheet.addCell(new Label(dayIndex, finalIndex, "###", getCellFormat(COLOR_HOLIDAY_OVER_TIME,Alignment.CENTRE)));//出勤编号
                        sheet.addCell(new Label(dayIndex + 1, finalIndex, "###", getCellFormat(COLOR_HOLIDAY_OVER_TIME,Alignment.CENTRE)));//上班时间
                        sheet.addCell(new Label(dayIndex + 2, finalIndex,"###", getCellFormat(COLOR_HOLIDAY_OVER_TIME,Alignment.CENTRE)));//下班时间
                    }
                    def result = resultItems.get(newDate)
                    if(result){
                        int type = result.type;
                        if (type == (type | AttendanceType.TO_WORK)) {
                            sheet.addCell(new Label(dayIndex + 3, finalIndex, result.startTime, getCellFormat(Alignment.LEFT)));//正常上班
                        }
                        if (type == (type | AttendanceType.OFF_WORK)) {
                            sheet.addCell(new Label(dayIndex + 4, finalIndex, result.endTime, getCellFormat(Alignment.LEFT)));//正常下班
                        }
                        if (type == (type | AttendanceType.LATE)) {
                            sheet.addCell(new Label(dayIndex + 3, finalIndex, result.startTime, getCellFormat(COLOR_LATE, Alignment.LEFT)));//迟到
                        }
                        if (type == (type | AttendanceType.LEVEL_EARLY)) {
                            sheet.addCell(new Label(dayIndex + 4, finalIndex, result.endTime, getCellFormat(COLOR_LEVEL_EARLY, Alignment.LEFT)));//早退
                        }
                        if (type == (type | AttendanceType.ABSENTEEISM)) {
                            sheet.addCell(new Label(dayIndex + 5, finalIndex, "未上班", getCellFormat(COLOR_ABSENTEEISM, Alignment.LEFT)));//翘班
                        }
                        if (type == (type | AttendanceType.UN_CHECK_IN)) {
                            sheet.addCell(new Label(dayIndex+3, finalIndex, "早上未打卡", getCellFormat(COLOR_UN_CHECK_IN, Alignment.LEFT)));//早上未打卡
                        }
                        if (type == (type | AttendanceType.UN_CHECK_OUT)) {
                            sheet.addCell(new Label(dayIndex + 4, finalIndex, "晚上未打卡", getCellFormat(COLOR_UN_CHECK_OUT, Alignment.LEFT)));//晚上未打卡
                        }
                        int overHour = result.overMinute / 60;
                        if (type == (type | AttendanceType.OVER_TIME)) {
                            sheet.addCell(new Label(dayIndex + 5, finalIndex, overHour + "H", getCellFormat(COLOR_OVER_TIME)));//平时加班
                        }
                        if (type == (type | AttendanceType.WEEKEND_OVER_TIME)) {
                            sheet.addCell(new Label(dayIndex + 5, finalIndex, overHour + "H", getCellFormat(COLOR_WEEKEND_OVER_TIME)));//周末加班
                        }
                        if (0!= (type & AttendanceType.HOLIDAY_OVER_TIME)) {
                            sheet.addCell(new Label(dayIndex + 5, finalIndex, overHour + "H", getCellFormat(COLOR_HOLIDAY_OVER_TIME)));//周末加班
                        }
                        sheet.setColumnView(dayIndex + 3, getBestNum(result.startTime));
                        sheet.setColumnView(dayIndex + 4, getBestNum(result.endTime));
                    }
                } catch (WriteException e) {
                    e.printStackTrace();
                }
                newDate=newDate.plusDays(1)
            }
            index++;
            InformantRegistry.getInstance().notifyMessage("员工:" + name + " 考勤信息汇总完毕!");
        }
        InformantRegistry.getInstance().notifyMessage("初始化员工考勤完成!");
    }

    /**
     * 以条汇总员工 所有异常信息(迟到早退,翘班,加班)等信息
     *
     * @param wwb           excel操作对象
     * @param year          计算年份
     * @param month         计算月份
     * @param results       计算数据集
     * @param employeeItems 计算员工信息集
     * @throws WriteException
     */
    private void writeSheet2(WritableWorkbook wwb,LocalDateTime startDateTime,LocalDateTime endDateTime, HashMap<String, HashMap<LocalDate, AttendanceResult>> results,List<EmployeeRest> employeeItems) throws WriteException {
        //二次分析,将分析出的异常信息,再次生成一个sheet
        int year=startDateTime.year
        int month=startDateTime.monthValue
        WritableSheet[] sheets = wwb.getSheets();
        WritableSheet sheet = wwb.createSheet(year + "_" + month + "考勤列表", null == sheets ? 1 : sheets.length + 1);
        def maxValueItems=[:]//单列最长字符
        String[] titles = ["姓名", "日期", "上班时间", "下班时间", "出勤时间编号", "上班日期", "早上记录", "晚上记录", "备注"]
        for (int i = 0; i < titles.length; i++) {
            WritableCellFormat wc = getCellFormat();
            sheet.addCell(new Label(i, 0, titles[i], wc));
            maxValueItems<<[(i):titles[i]]
        }
        int index = 0;
        for (Map.Entry<String, HashMap<Integer, AttendanceResult>> entry : results.entrySet()) {
            String name = entry.getKey();
            HashMap<LocalDate, AttendanceResult> resultItems = entry.getValue();
            for (Map.Entry<LocalDate, AttendanceResult> resultEntry : resultItems.entrySet()) {
                LocalDate localDate = resultEntry.getKey();
                AttendanceResult result = resultEntry.getValue();
                int type = result.type;
                int overHour = result.overMinute / 60;
                for(int attendanceType=AttendanceType.LATE;attendanceType<=AttendanceType.UN_KNOW_WORK_TIME;attendanceType*=2){
                    if(0!=(type&attendanceType)){
                        //上下班不正常,平时加班,周末加班.都记录下名字,待下方添加异常数据
                        //这里,操作了.直接++,将位置下移
                        def dateInfo,startDate,endDate,workDays,workDate,workStartDate,workEndDate,remark
                        sheet.addCell(new Label(0, ++index, name, getCellFormat()));
                        sheet.addCell(new Label(1, index, dateInfo=localDate.toString(), getCellFormat()));
                        EmployeeRest employee = employeeItems.find({it.employeeName==name})

                        def restCodeItem = getEmployeeWorkTime(name, localDate)
                        sheet.addCell(new Label(2, index, restCodeItem.startDate, getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));
                        sheet.addCell(new Label(3, index, restCodeItem.endDate, getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));
                        sheet.addCell(new Label(4, index,workDays=restCodeItem.code?:"###", getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));
                        switch (attendanceType){
                            case AttendanceType.LATE:
                                sheet.addCell(new Label(5, index, workDate=localDate.toString(), getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(6, index, workStartDate=result.startTime, getCellFormat(COLOR_LATE, Alignment.LEFT)));
                                sheet.addCell(new Label(7, index, workEndDate=result.endTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(8, index, remark="迟到", getCellFormat(COLOR_LATE)));
                                break;
                            case AttendanceType.LEVEL_EARLY:
                                sheet.addCell(new Label(5, index, workDate=localDate.toString(), getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(6, index, workStartDate=result.startTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(7, index, workEndDate=result.endTime, getCellFormat(COLOR_LEVEL_EARLY, Alignment.LEFT)));
                                sheet.addCell(new Label(8, index, remark="早退", getCellFormat(COLOR_LEVEL_EARLY)));
                                break;
                            case AttendanceType.ABSENTEEISM:
                                sheet.addCell(new Label(5, index, workDate=localDate.toString(), getCellFormat(COLOR_ABSENTEEISM,Alignment.LEFT)));
                                sheet.addCell(new Label(6, index, workStartDate="#", getCellFormat(COLOR_UN_CHECK_IN, Alignment.LEFT)));
                                sheet.addCell(new Label(7, index, workEndDate="#", getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(8, index, remark="未上班", getCellFormat(COLOR_ABSENTEEISM)));
                                break;
                            case AttendanceType.UN_CHECK_IN:
                                sheet.addCell(new Label(5, index, workDate=localDate.toString(), getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(6, index, workStartDate=result.startTime, getCellFormat(COLOR_UN_CHECK_IN, Alignment.LEFT)));
                                sheet.addCell(new Label(7, index, workEndDate=result.endTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(8, index, remark="早上未打卡", getCellFormat(COLOR_UN_CHECK_IN)));
                                break;
                            case AttendanceType.UN_CHECK_OUT:
                                sheet.addCell(new Label(5, index, workDate=localDate.toString(), getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(6, index, workStartDate=result.startTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(7, index, workEndDate=result.endTime, getCellFormat(COLOR_UN_CHECK_OUT, Alignment.LEFT)));
                                sheet.addCell(new Label(8, index, remark="晚上未打卡", getCellFormat(COLOR_UN_CHECK_OUT)));
                                break;
                            case AttendanceType.OVER_TIME:
                                sheet.addCell(new Label(5, index, workDate=localDate.toString(), getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(6, index, workStartDate=result.startTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(7, index, workEndDate=result.endTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(8, index, remark="平时(${overHour}H)", getCellFormat(COLOR_OVER_TIME)));
                                break;
                            case AttendanceType.WEEKEND_OVER_TIME:
                                sheet.addCell(new Label(5, index, workDate=localDate.toString(), getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(6, index, workStartDate=result.startTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(7, index, workEndDate=result.endTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(8, index, remark="周末(${overHour}H)", getCellFormat(COLOR_WEEKEND_OVER_TIME)));
                                break;
                            case AttendanceType.HOLIDAY_OVER_TIME:
                                sheet.addCell(new Label(5, index, workDate=localDate.toString(), getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(6, index, workStartDate=result.startTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(7, index, workEndDate=result.endTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(8, index, remark="假日(${overHour}H)", getCellFormat(COLOR_HOLIDAY_OVER_TIME)));
                                break;
                            case AttendanceType.UN_KNOW_WORK_TIME:
                                sheet.addCell(new Label(5, index, workDate=localDate.toString(), getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(6, index, workStartDate=result.startTime, getCellFormat(Alignment.LEFT)));
                                sheet.addCell(new Label(7, index, workEndDate=result.endTime, getCellFormat(Alignment.LEFT)));
                                def workHour=String.format("%.1f", result.workMinute/60)
                                sheet.addCell(new Label(8, index, remark="事假(${workHour}H)", getCellFormat(COLOR_UN_KNOW_WORK)));
                                break;
                        }
                        int itemIndex=0
                        [name,dateInfo,startDate,endDate,workDays,workDate,workStartDate,workEndDate,remark].each{checkMaxValue(maxValueItems,itemIndex++,it as String)}
                    }
                }
            }
        }
        maxValueItems.each { sheet.setColumnView(it.key as Integer, getBestNum(it.value)); }
    }

    RestCodeItem getEmployeeWorkTime(String name, LocalDate localDate){
        //定制员工信息
        EmployeeRest employee = employeeItems.find{it.name==name}
        //获取排班信息
        def attendances = employeeAttendanceItems[name]
        def employeeAttendance = attendances.find { it.year == localDate.year && it.mouth == localDate.monthValue && it.day == localDate.dayOfMonth }
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
     * 写入汇总表
     * @param wwb
     * @param startDateTime
     * @param endDateTime
     * @param results
     * @param employeeItems
     * @param holidayItems 假日信息
     * @throws WriteException
     */
    private void writeSheet3(WritableWorkbook wwb,LocalDateTime startDateTime,LocalDateTime endDateTime,
                             HashMap<String, HashMap<LocalDate, AttendanceResult>> results,List<EmployeeRest> employeeItems,holidayItems) throws WriteException{
//        序号	姓名	正常出勤/天	事假/天	旷工/天	迟到/次	早退/次	平时加班/小时	周末加班/天数	实际出勤/天
        InformantRegistry.getInstance(). notifyMessage("开始初始化员工考勤汇总信息");
        def maxValueItems=[:]//单列最长字符
        int year=startDateTime.year
        int month=startDateTime.monthValue
        WritableSheet[] sheets = wwb.getSheets();
        WritableSheet sheet = wwb.createSheet(year + "_" + month + "汇总列表", null == sheets ? 1 : sheets.length + 1);
        String[] titles = ["序 号", "姓名", "正常出勤/天","月初上班日期(起)","月未上班日期(终)","上班时间","下班时间","出勤编号", "未上班", "迟到/次", "早退/次","早上未打卡","晚上未打卡", "平时加班/小时", "周末加班/天数", "假日加班/天数", "实际出勤/天","备注"]
        int index=0
        for (int i = 0; i < titles.length; i++) {
            WritableCellFormat wc = getCellFormat();
            sheet.addCell(new Label(i, 0, titles[i], wc));
            maxValueItems<<[(i):titles[i]]
        }
        //获得当前月份实际工作天数
        int workDays=getMonthWorkDay(year,month)
        results.each {
            def employeeName=it.key
            EmployeeRest employee = employeeItems.find({it.employeeName==employeeName})
            sheet.addCell(new Label(0, ++index, index as String, getCellFormat()));//序号
            sheet.addCell(new Label(1, index, it.key, getCellFormat()));//姓名
            sheet.addCell(new Label(2, index, workDays as String, getCellFormat()));//正常出勤/天

            def entryTime,departureTime,startDate,endDate,workDaysValue

            def unKnowItem=unKnowItems.find {it.name==employeeName}
            def localDateTime=employee?employee.entryDateTime():null
            //设定未知的入职时间
            if(!localDateTime&&unKnowItem&&unKnowItem.entryDate){
                localDateTime=LocalDateTime.of(unKnowItem.entryDate,LocalTime.of(0,0))
            }
            sheet.addCell(new Label(3, index, entryTime=localDateTime?localDateTime.toLocalDate().toString():"####-##-##", getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));
            //设定未知的未上班日期
            def departureDate=!unKnowItem?null:unKnowItem.departureDate
            sheet.addCell(new Label(4, index, departureTime=departureDate?departureDate.toString():"####-##-##", getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));

            //出勤信息

            def attendances = employeeAttendanceItems[employeeName]
            if(attendances){
                sheet.addCell(new Label(5, index, startDate="###", getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));
                sheet.addCell(new Label(6, index, endDate="###", getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));
                sheet.addCell(new Label(7, index, workDaysValue="###", getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));
            } else {
                sheet.addCell(new Label(5, index, startDate=employee?employee.startDate:departmentRest.startDate, getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));
                sheet.addCell(new Label(6, index, endDate=employee?employee.endDate:departmentRest.endDate, getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));
                sheet.addCell(new Label(7, index, workDaysValue=employee?employee.workDays.join(","):departmentRest.workDays.join(","), getCellFormat(employee?jxl.format.Colour.LIGHT_GREEN:jxl.format.Colour.WHITE,Alignment.CENTRE)));
            }

            int absenteeismDays=getAttendanceTypeDays(it.value,AttendanceType.ABSENTEEISM)
            sheet.addCell(new Label(8, index, absenteeismDays as String, getCellFormat(0==absenteeismDays?jxl.format.Colour.WHITE:COLOR_ABSENTEEISM)));//旷工/天
            int lateDays=getAttendanceTypeDays(it.value,AttendanceType.LATE)
            sheet.addCell(new Label(9, index,lateDays as String, getCellFormat(0==lateDays?jxl.format.Colour.WHITE:COLOR_LATE)));//迟到/次
            int levelEarlyDays=getAttendanceTypeDays(it.value,AttendanceType.LEVEL_EARLY)
            sheet.addCell(new Label(10, index, levelEarlyDays as String, getCellFormat(0==levelEarlyDays?jxl.format.Colour.WHITE:COLOR_LEVEL_EARLY)));//早退/次

            int unCheckInDays=getAttendanceTypeDays(it.value,AttendanceType.UN_CHECK_IN)
            sheet.addCell(new Label(11, index, unCheckInDays as String, getCellFormat(0==unCheckInDays?jxl.format.Colour.WHITE:COLOR_UN_CHECK_IN)));//早上未打卡
            int unCheckOutDays=getAttendanceTypeDays(it.value,AttendanceType.UN_CHECK_OUT)
            sheet.addCell(new Label(12, index, unCheckOutDays as String, getCellFormat(0==unCheckOutDays?jxl.format.Colour.WHITE:COLOR_UN_CHECK_OUT)));//晚上未打卡

            int workHour,days
            (workHour,days)=getAttendanceOverWorkDays(it.value)
            def overTime=0==days?"#":"${workHour}时/${days}天"
            sheet.addCell(new Label(13, index, overTime, getCellFormat(0==days?jxl.format.Colour.WHITE:COLOR_OVER_TIME)));//平时加班/小时

            int weekendOverTimeDays
            (workHour,weekendOverTimeDays)=getAttendanceWeekOverWorkDays(it.value,AttendanceType.WEEKEND_OVER_TIME)
            def weekendOverTime=0==weekendOverTimeDays?"#":"${weekendOverTimeDays}天"
            sheet.addCell(new Label(14, index, weekendOverTime, getCellFormat(0==weekendOverTimeDays?jxl.format.Colour.WHITE:COLOR_WEEKEND_OVER_TIME)));//周末加班/天数
            int holidayOverTimeDays
            (workHour,holidayOverTimeDays)=getAttendanceWeekOverWorkDays(it.value,AttendanceType.HOLIDAY_OVER_TIME)
            def holidayOverTime=0==holidayOverTimeDays?"#":"${holidayOverTimeDays}天"
            sheet.addCell(new Label(15, index, holidayOverTime, getCellFormat(0==holidayOverTime?jxl.format.Colour.WHITE:COLOR_HOLIDAY_OVER_TIME)));//假日加班/天数

            sheet.addCell(new Label(16, index, getAttendanceWorkDays(it.value) as String, getCellFormat()));//实际出勤/天
            def attendanceRemark
//            sheet.addCell(new Label(16, index, attendanceRemark=getAttendanceRemark(it.value) as String, getCellFormat()));//分析员工信息
            sheet.addCell(new Label(17, index, attendanceRemark="#" as String, getCellFormat()));//分析员工信息
            InformantRegistry.getInstance(). notifyMessage("员工:$it.key 考勤分类信息汇总完毕!");
            //检测最长字符
            int itemIndex=0
            [index,it.key,workDays,entryTime,departureTime,startDate,endDate,workDaysValue,absenteeismDays,lateDays,levelEarlyDays,unCheckInDays,unCheckOutDays,overTime,weekendOverTime,holidayOverTime,it.value.size(),attendanceRemark].each { checkMaxValue(maxValueItems,itemIndex++,it as String) }
        }
        maxValueItems.each { sheet.setColumnView(it.key as Integer, getBestNum(it.value)); }
        InformantRegistry.getInstance(). notifyMessage("初始化员工考勤汇总信息完成!");
    }


    /**
     * 检测最长字符
     * @param index
     * @param content
     */
    void checkMaxValue(maxValueItems,index,content){
        if(content&&maxValueItems[index].length()<=content.length()){
            maxValueItems<<[(index):content]
        }
    }
    /**
     * 获得员工实际出勤天数
     * @param items
     * @return
     */
    int getAttendanceWorkDays(items){
        int days=0
        items.each{ 0!=(it.value.type&AttendanceType.ABSENTEEISM)?:days++ }
        days
    }
    /**
     * 获得员工指定标记天数
     * @param items
     * @return
     */
    int getAttendanceTypeDays(items,type){
        int days=0
        items.each{ 0==(it.value.type&type)?:days++ }
        days
    }

    /**
     * 平时加班
     * @param items
     * @param type
     * @return
     */
    def getAttendanceOverWorkDays(items){
        int workHour=0,days=0
        items.each{
            if(0!=(it.value.type&AttendanceType.OVER_TIME)){
                days++
                workHour+=(it.value.overMinute/60)
            }
        }
        [workHour,days]
    }

    /**
     * 周末加班
     * @param items
     * @param type
     * @return
     */
    def getAttendanceWeekOverWorkDays(items,type){
        int hours=0,times=0
        items.each{
            if(0!=(it.value.type&type)){
                hours+=it.value.overMinute/60
                times++
            }
        }
        [hours,times]
    }


    /**
     * 获得月正常出勤天数
     * @param year
     * @param month
     * @return
     */
    private int getMonthWorkDay(year,month){
        int workDays=0
        def localDate=LocalDate.of(year,month,1)
        def endDate=localDate.plusMonths(1)
        while(localDate!=endDate){
            localDate=localDate.plusDays(1)
            def item=holidayItems.find{ it.month==localDate.monthValue&&it.day==localDate.dayOfMonth}
            if(item&&item.isWork||departmentRest.workDays.contains(localDate.dayOfWeek.value)){
                //受假日影响日期/正常日期
                workDays++
            }
        }
        workDays
    }

    def getAttendanceRemark(items){
        def out=new StringBuilder()
        int startDayCount,endDayCount,weekDayCount
        (startDayCount,endDayCount,weekDayCount)=getAttendanceDay(items)
        if(0!=startDayCount){
            out.append("月初未上班:${startDayCount}天")
        }
        if(0!=endDayCount){
            out.append("月末未上班:${endDayCount}天")
        }
        if(0!=weekDayCount){
            out.append("月中未上班:${weekDayCount}天")
        }
    }

    def getAttendanceDay(items){
        int startDayCount=0,endDayCount=0,weekDayCount=0
        if(items){
            //分析员工月初未上班天数,月末未上班天数,以及中间未上班天数.
            def startItem,endItem
            //正向遍历
            [].collect {

            }
            items.find{
                if(!startItem){
                    if(0==(it.value.type&AttendanceType.ABSENTEEISM)){
                        startDayCount++
                    } else {
                        startItem=it
                    }
                }
            }
            //反向遍历
            items.reverseEach {
                if(!endItem){
                    if(0==(it.value.type&AttendanceType.ABSENTEEISM)){
                        endDayCount++
                    } else {
                        endItem=it
                    }
                }
            }
            //遍历中间未上班天数
            def startWork
            for(def entry:items){
                if(entry==startItem){
                    //开始统计
                    startWork=entry
                } else if(entry==endItem) {
                    break
                }
                if(startWork&&0==(entry.value.type&AttendanceType.ABSENTEEISM)){
                    weekDayCount++
                }
            }
        }
        [startDayCount,endDayCount,weekDayCount]
    }


    def getWorkDayCount(workDays,year,month,startDay){
        int workDayCount=0
        def localDate=LocalDate.of(year,month,startDay)
        final def holidayItem=holidayItems.find {it.month==month&&it.day==localDate.dayOfMonth}
        if(workDays.contains(localDate.dayOfWeek)||holidayItem&&holidayItem.isWork){
            workDayCount++
        }
        localDate=localDate.plusDays(1)
        workDayCount
    }


    private int getDayIndex(int day,int itemLength) {
        return (day - INFO_LENGTH) * itemLength + INFO_LENGTH;
    }
}
