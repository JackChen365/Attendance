package quant.attendance.excel.writer

import jxl.CellView
import jxl.format.Alignment
import jxl.write.*
import quant.attendance.excel.InformantRegistry
import quant.attendance.model.AttendanceResult
import quant.attendance.model.AttendanceType
import quant.attendance.model.DepartmentRest
import quant.attendance.model.EmployeeRest

import java.time.LocalDate
import java.time.LocalDateTime



//----------------------------------------------------------
// thank you groovy static import!
//----------------------------------------------------------
import static jxl.format.Colour.AUTOMATIC as COLOR_ABSENTEEISM
import static jxl.format.Colour.BLUE2 as COLOR_LATE
import static jxl.format.Colour.DARK_BLUE2 as COLOR_UN_CHECK_IN
import static jxl.format.Colour.DARK_RED2 as COLOR_UN_CHECK_OUT
import static jxl.format.Colour.LIGHT_TURQUOISE2 as COLOR_LEVEL_EARLY
import static jxl.format.Colour.PINK2 as COLOR_OVER_TIME
import static jxl.format.Colour.PLUM2 as COLOR_WEEKEND_OVER_TIME
import static jxl.format.Colour.TEAL2 as COLOR_HOLIDAY_OVER_TIME
/**
 * Created by Administrator on 2017/4/9.
 */
class ExcelWriter extends AbsExcelWriter{
    private final int INFO_LENGTH = 4;

    ExcelWriter(LocalDateTime startDateTime, LocalDateTime endDateTime, HashMap<String, HashMap<Integer, AttendanceResult>> results, DepartmentRest departmentRest, Object employeeItems,def holidayItems) {
        super(startDateTime, endDateTime, results, departmentRest, employeeItems,holidayItems)
    }

    @Override
    void write(WritableWorkbook wwb, LocalDateTime startDateTime, LocalDateTime endDateTime, HashMap<String, HashMap<Integer, AttendanceResult>> results, List<EmployeeRest> employeeItems,holidayItems) throws WriteException {
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
                        HashMap<String, HashMap<Integer, AttendanceResult>> results,List<EmployeeRest> employeeItems) throws WriteException {
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
        for (int i = startDateTime.dayOfMonth; i <= endDateTime.dayOfMonth; i++) {
            titles[i + INFO_LENGTH - 1] = startDateTime.monthValue + "/" + i;
        }
        String[] attendanceTitles = ["上班", "下班", "加班"]
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
                int start = getDayIndex(i);
                sheet.mergeCells(start, 0, (i - INFO_LENGTH + 2) * 3, 0);
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
        for (Map.Entry<String, HashMap<Integer, AttendanceResult>> entry : results.entrySet()) {
            String name = entry.getKey();
            HashMap<Integer, AttendanceResult> resultItems = entry.getValue();
            sheet.addCell(new Label(0, index, name, wc));//插入名字
            EmployeeRest employee = employeeItems.find({it.employeeName==name});
            CellView cellView = new CellView();
            cellView.setAutosize(true); //设置自动大小
            sheet.setColumnView(1, cellView);//根据内容自动设置列宽
            //插入员工配置信息
            sheet.addCell(new Label(1, index, employee?employee.startDate:departmentRest.startDate, getCellFormat(Alignment.LEFT)));
            sheet.addCell(new Label(2, index, employee?employee.endDate:departmentRest.endDate, wc));
            sheet.addCell(new Label(3, index, employee?employee.workDays.join(","):departmentRest.workDays.join(","), wc));


            final int finalIndex = index;
            resultItems.forEach({ day,result->
                try {
                    int dayIndex = getDayIndex(day + INFO_LENGTH - 1);
                    int type = result.type;
                    if (type == (type | AttendanceType.TO_WORK)) {
                        sheet.addCell(new Label(dayIndex, finalIndex, result.startTime, getCellFormat(Alignment.LEFT)));//正常上班
                    }
                    if (type == (type | AttendanceType.OFF_WORK)) {
                        sheet.addCell(new Label(dayIndex + 1, finalIndex, result.endTime, getCellFormat(Alignment.LEFT)));//正常下班
                    }
                    if (type == (type | AttendanceType.LATE)) {
                        sheet.addCell(new Label(dayIndex, finalIndex, result.startTime, getCellFormat(COLOR_LATE, Alignment.LEFT)));//迟到
                    }
                    if (type == (type | AttendanceType.LEVEL_EARLY)) {
                        sheet.addCell(new Label(dayIndex + 1, finalIndex, result.endTime, getCellFormat(COLOR_LEVEL_EARLY, Alignment.LEFT)));//早退
                    }
                    if (type == (type | AttendanceType.ABSENTEEISM)) {
                        sheet.addCell(new Label(dayIndex + 2, finalIndex, "未上班", getCellFormat(COLOR_ABSENTEEISM, Alignment.LEFT)));//翘班
                    }
                    if (type == (type | AttendanceType.UN_CHECK_IN)) {
                        sheet.addCell(new Label(dayIndex, finalIndex, "早上未打卡", getCellFormat(COLOR_UN_CHECK_IN, Alignment.LEFT)));//早上未打卡
                    }
                    if (type == (type | AttendanceType.UN_CHECK_OUT)) {
                        sheet.addCell(new Label(dayIndex + 1, finalIndex, "晚上未打卡", getCellFormat(COLOR_UN_CHECK_OUT, Alignment.LEFT)));//晚上未打卡
                    }
                    int overHour = result.overMinute / 60;
                    if (type == (type | AttendanceType.OVER_TIME)) {
                        sheet.addCell(new Label(dayIndex + 2, finalIndex, overHour + "H", getCellFormat(COLOR_OVER_TIME)));//平时加班
                    }
                    if (type == (type | AttendanceType.WEEKEND_OVER_TIME)) {
                        sheet.addCell(new Label(dayIndex + 2, finalIndex, overHour + "H", getCellFormat(COLOR_WEEKEND_OVER_TIME)));//周末加班
                    }
                    if (0!= (type & AttendanceType.HOLIDAY_OVER_TIME)) {
                        sheet.addCell(new Label(dayIndex + 2, finalIndex, overHour + "H", getCellFormat(COLOR_HOLIDAY_OVER_TIME)));//周末加班
                    }
                    sheet.setColumnView(dayIndex, getBestNum(result.startTime));
                    sheet.setColumnView(dayIndex + 1, getBestNum(result.endTime));
                } catch (WriteException e) {
                    e.printStackTrace();
                }
            });
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
    private void writeSheet2(WritableWorkbook wwb,LocalDateTime startDateTime,LocalDateTime endDateTime, HashMap<String, HashMap<Integer, AttendanceResult>> results,List<EmployeeRest> employeeItems) throws WriteException {
        //二次分析,将分析出的异常信息,再次生成一个sheet
        int year=startDateTime.year
        int month=startDateTime.monthValue
        WritableSheet[] sheets = wwb.getSheets();
        WritableSheet sheet = wwb.createSheet(year + "_" + month + "考勤列表", null == sheets ? 1 : sheets.length + 1);
        String[] titles = ["姓名", "日期", "上班时间", "下班时间", "休息时间", "上班日期", "早上记录", "晚上记录", "备注"]
        for (int i = 0; i < titles.length; i++) {
            WritableCellFormat wc = getCellFormat();
            sheet.addCell(new Label(i, 0, titles[i], wc));
        }
        int index = 0;
        for (Map.Entry<String, HashMap<Integer, AttendanceResult>> entry : results.entrySet()) {
            String name = entry.getKey();
            HashMap<Integer, AttendanceResult> resultItems = entry.getValue();
            for (Map.Entry<Integer, AttendanceResult> resultEntry : resultItems.entrySet()) {
                Integer today = resultEntry.getKey();
                AttendanceResult result = resultEntry.getValue();
                int type = result.type;
                int overHour = result.overMinute / 60;
                if (0 != (type & AttendanceType.NORMA)){
                    for(int attendanceType=AttendanceType.LATE;attendanceType<=AttendanceType.HOLIDAY_OVER_TIME;attendanceType*=2){
                        if(0!=(type&attendanceType)){
                            //上下班不正常,平时加班,周末加班.都记录下名字,待下方添加异常数据
                            //这里,操作了.直接++,将位置下移
                            sheet.addCell(new Label(0, ++index, name, getCellFormat()));
                            sheet.addCell(new Label(1, index, year + "/" + month + "/" + today, getCellFormat()));
                            EmployeeRest employee = employeeItems.find({it.employeeName==name})

                            sheet.addCell(new Label(2, index, employee?employee.startDate:departmentRest.startDate, getCellFormat(Alignment.LEFT)));
                            sheet.addCell(new Label(3, index, employee?employee.endDate:departmentRest.endDate, getCellFormat(Alignment.LEFT)));
                            sheet.addCell(new Label(4, index, employee?employee.workDays.join(","):departmentRest.workDays.join(","), getCellFormat()));

                            switch (attendanceType){
                                case AttendanceType.LATE:
                                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(COLOR_LATE, Alignment.LEFT)));
                                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(8, index, "迟到", getCellFormat(COLOR_LATE)));
                                    break;
                                case AttendanceType.LEVEL_EARLY:
                                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(COLOR_LEVEL_EARLY, Alignment.LEFT)));
                                    sheet.addCell(new Label(8, index, "早退", getCellFormat(COLOR_LEVEL_EARLY)));
                                    break;
                                case AttendanceType.ABSENTEEISM:
                                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(COLOR_ABSENTEEISM,Alignment.LEFT)));
                                    sheet.addCell(new Label(8, index, "翘班", getCellFormat(COLOR_ABSENTEEISM)));
                                    break;
                                case AttendanceType.UN_CHECK_IN:
                                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(COLOR_UN_CHECK_IN, Alignment.LEFT)));
                                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(8, index, "早上未打卡", getCellFormat(COLOR_UN_CHECK_IN)));
                                    break;
                                case AttendanceType.UN_CHECK_OUT:
                                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(COLOR_UN_CHECK_OUT, Alignment.LEFT)));
                                    sheet.addCell(new Label(8, index, "晚上未打卡", getCellFormat(COLOR_UN_CHECK_OUT)));
                                    break;
                                case AttendanceType.OVER_TIME:
                                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(8, index, "平时(${overHour}H)", getCellFormat(COLOR_OVER_TIME)));
                                    break;
                                case AttendanceType.WEEKEND_OVER_TIME:
                                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(8, index, "周末(${overHour}H)", getCellFormat(COLOR_WEEKEND_OVER_TIME)));
                                    break;
                                case AttendanceType.HOLIDAY_OVER_TIME:
                                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Alignment.LEFT)));
                                    sheet.addCell(new Label(8, index, "假日(${overHour}H)", getCellFormat(COLOR_HOLIDAY_OVER_TIME)));
                                    break;
                            }
                        }
                    }
                }
            }
        }
        InformantRegistry.getInstance(). notifyMessage("-------------------初始化员工考勤其他信息完成!-------------------");
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
                             HashMap<String, HashMap<Integer, AttendanceResult>> results,List<EmployeeRest> employeeItems,holidayItems) throws WriteException{
//        序号	姓名	正常出勤/天	事假/天	旷工/天	迟到/次	早退/次	平时加班/小时	周末加班/天数	实际出勤/天
        InformantRegistry.getInstance(). notifyMessage("-------------------开始初始化员工考勤汇总信息--------------------");
        int year=startDateTime.year
        int month=startDateTime.monthValue
        WritableSheet[] sheets = wwb.getSheets();
        WritableSheet sheet = wwb.createSheet(year + "_" + month + "汇总列表", null == sheets ? 1 : sheets.length + 1);
        String[] titles = ["序号", "姓名", "正常出勤/天", "旷工/天", "迟到/次", "早退/次", "平时加班/小时", "周末加班/天数", "假日加班/天数", "实际出勤/天"]
        int index=0
        for (int i = 0; i < titles.length; i++) {
            WritableCellFormat wc = getCellFormat();
            sheet.addCell(new Label(i, 0, titles[i], wc));
        }
        //获得当前月份实际工作天数
        int workDays=getMonthWorkDay(year,month)
        results.each {
            int workHour,days
            sheet.addCell(new Label(0, ++index, index as String, getCellFormat()));//序号
            sheet.addCell(new Label(1, index, it.key, getCellFormat()));//姓名
            sheet.addCell(new Label(2, index, workDays as String, getCellFormat()));//正常出勤/天
            days=getAttendanceTypeDays(it.value,AttendanceType.ABSENTEEISM)
            sheet.addCell(new Label(3, index, days as String, getCellFormat(0==days?jxl.format.Colour.WHITE:COLOR_ABSENTEEISM)));//旷工/天
            days=getAttendanceTypeDays(it.value,AttendanceType.LATE)
            sheet.addCell(new Label(4, index,getAttendanceTypeDays(it.value,AttendanceType.LATE) as String, getCellFormat(0==days?jxl.format.Colour.WHITE:COLOR_LATE)));//迟到/次
            days=getAttendanceTypeDays(it.value,AttendanceType.LEVEL_EARLY)
            sheet.addCell(new Label(5, index, getAttendanceTypeDays(it.value,AttendanceType.LEVEL_EARLY) as String, getCellFormat(0==days?jxl.format.Colour.WHITE:COLOR_LEVEL_EARLY)));//早退/次

            (workHour,days)=getAttendanceOverWorkDays(it.value)
            sheet.addCell(new Label(6, index, 0==days?"#":"${workHour}时/${days}天", getCellFormat(0==days?jxl.format.Colour.WHITE:COLOR_OVER_TIME)));//平时加班/小时

            workDays=getAttendanceWeekOverWorkDays(it.value,AttendanceType.HOLIDAY_OVER_TIME)
            sheet.addCell(new Label(7, index, workDays as String, getCellFormat(0==workDays?jxl.format.Colour.WHITE:COLOR_WEEKEND_OVER_TIME)));//周末加班/天数
            workDays=getAttendanceWeekOverWorkDays(it.value,AttendanceType.WEEKEND_OVER_TIME)
            sheet.addCell(new Label(8, index, workDays as String, getCellFormat(0==workDays?jxl.format.Colour.WHITE:COLOR_HOLIDAY_OVER_TIME)));//假日加班/天数
            sheet.addCell(new Label(9, index, it.value.size() as String, getCellFormat()));//实际出勤/天
        }
        InformantRegistry.getInstance(). notifyMessage("-------------------初始化员工考勤汇总信息完成!-------------------");
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
    int getAttendanceWeekOverWorkDays(items,type){
        float days=0
        final int HALF_DAY_HOUR=4
        final int DAY_HOUR=8
        items.each{
            if(0!=(it.value.type&type)){
                int hour=it.value.overMinute/60
                if((HALF_DAY_HOUR<=hour&&DAY_HOUR>hour)){
                    days+=0.5f
                } else if(DAY_HOUR<=hour){
                    days+=1f
                }
            }
        }
        days
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

    private int getDayIndex(int day) {
        return (day - INFO_LENGTH) * 3 + INFO_LENGTH;
    }
}
