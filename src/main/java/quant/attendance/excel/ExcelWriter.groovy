package quant.attendance.excel

import com.sun.istack.internal.NotNull
import jxl.CellView
import jxl.Workbook
import jxl.format.Alignment
import jxl.format.Colour
import jxl.write.Border
import jxl.write.BorderLineStyle
import jxl.write.Label
import jxl.write.WritableCellFormat
import jxl.write.WritableSheet
import jxl.write.WritableWorkbook
import jxl.write.WriteException
import quant.attendance.model.AttendanceResult
import quant.attendance.model.AttendanceType
import quant.attendance.model.DepartmentRest
import quant.attendance.model.Employee
import quant.attendance.model.EmployeeRest
import quant.attendance.util.IOUtils
import quant.attendance.util.TextUtils

import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.regex.Matcher

/**
 * Created by Administrator on 2017/4/9.
 */
class ExcelWriter {
    private final int INFO_LENGTH = 4;
    private final HashMap<String, HashMap<Integer, AttendanceResult>> results;
    private final List<Employee> employeeItems;
    DepartmentRest departmentRest
    LocalDateTime startDateTime, endDateTime;

    public ExcelWriter(LocalDateTime startDateTime,LocalDateTime endDateTime, HashMap<String, HashMap<Integer, AttendanceResult>> results,DepartmentRest departmentRest,employeeItems) {
        this.startDateTime=startDateTime;
        this.endDateTime=endDateTime;
        this.departmentRest=departmentRest
        this.results = new HashMap<>();
        if (null != results && !results.isEmpty()) {
            this.results.putAll(results);
        } else {
            InformantRegistry.getInstance().notifyMessage("分析结果集为空!");
        }
        this.employeeItems = [];
        if (null != employeeItems && !employeeItems.isEmpty()) {
            this.employeeItems.addAll(employeeItems);
        } else {
            InformantRegistry.getInstance().notifyMessage("员工集为空!");
        }
    }

    public void writeExcel() {
        String path = ExcelWriter.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File parentFile = new File(path).getParentFile();
        File filePath = new File(parentFile, "考勤.xls");
        InformantRegistry.getInstance().notifyMessage("目录:" + filePath + " 生成文件!");
        // 创建Excel工作薄
        WritableWorkbook wwb = null;
        OutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
            wwb = Workbook.createWorkbook(os);
            //记录汇总数据
            write1(startDateTime,endDateTime, wwb, results, employeeItems);
            //记录其他数据
            write(wwb, startDateTime.year,startDateTime.monthValue, results, employeeItems);
            //写入所有数据
            wwb.write();
        } catch (FileNotFoundException e) {
            InformantRegistry.getInstance().notifyMessage("写入目录不存在.请检测,如果是中文文件夹.请更名!");
        } catch (WriteException | IOException e) {
            InformantRegistry.getInstance().notifyMessage("写入异常,文件可能被锁定,如果打开,请关闭再重试");
        } finally {
            if (null != wwb) {
                try {
                    wwb.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            IOUtils.closeStream(os);
        }
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
    private void write1(LocalDateTime startDateTime,LocalDateTime endDateTime, WritableWorkbook wwb,
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
                        sheet.addCell(new Label(dayIndex, finalIndex, result.startTime, getCellFormat(Colour.RED, Alignment.LEFT)));//迟到
                    }
                    if (type == (type | AttendanceType.LEVEL_EARLY)) {
                        sheet.addCell(new Label(dayIndex + 1, finalIndex, result.endTime, getCellFormat(Colour.RED, Alignment.LEFT)));//早退
                    }
                    if (type == (type | AttendanceType.ABSENTEEISM)) {
                        sheet.addCell(new Label(dayIndex + 2, finalIndex, "未上班", getCellFormat(Colour.RED, Alignment.LEFT)));//翘班
                    }
                    if (type == (type | AttendanceType.UN_CHECK_IN)) {
                        sheet.addCell(new Label(dayIndex, finalIndex, "早上未打卡", getCellFormat(Colour.RED, Alignment.LEFT)));//早上未打卡
                    }
                    if (type == (type | AttendanceType.UN_CHECK_OUT)) {
                        sheet.addCell(new Label(dayIndex + 1, finalIndex, "晚上未打卡", getCellFormat(Colour.RED, Alignment.LEFT)));//晚上未打卡
                    }
                    int overHour = result.overMinute / 60;
                    if (type == (type | AttendanceType.OVER_TIME)) {
                        sheet.addCell(new Label(dayIndex + 2, finalIndex, overHour + "H", getCellFormat(Colour.YELLOW)));//平时加班
                    }
                    if (type == (type | AttendanceType.WEEKEND_OVER_TIME)) {
                        sheet.addCell(new Label(dayIndex + 2, finalIndex, overHour + "H", getCellFormat(Colour.YELLOW)));//周末加班
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
    private void write(WritableWorkbook wwb, int year, int month, HashMap<String, HashMap<Integer, AttendanceResult>> results,List<EmployeeRest> employeeItems) throws WriteException {
        //二次分析,将分析出的异常信息,再次生成一个sheet
        WritableSheet[] sheets = wwb.getSheets();
        WritableSheet sheet = wwb.createSheet(year + "_" + month + "考勤列表", null == sheets ? 1 : sheets.length + 1);
        String[] titles = ["姓名", "日期", "上班时间", "下班时间", "休息时间", "上班日期", "早上记录", "晚上记录", "备注"]
        for (int i = 0; i < titles.length; i++) {
            WritableCellFormat wc = getCellFormat();
            sheet.addCell(new Label(i, 0, titles[i], wc));
        }
        int index = 0;
        def remarkItems=[]
        Colour colour=Colour.RED
        for (Map.Entry<String, HashMap<Integer, AttendanceResult>> entry : results.entrySet()) {
            String name = entry.getKey();
            HashMap<Integer, AttendanceResult> resultItems = entry.getValue();
            for (Map.Entry<Integer, AttendanceResult> resultEntry : resultItems.entrySet()) {
                remarkItems.clear()
                Integer today = resultEntry.getKey();
                AttendanceResult result = resultEntry.getValue();
                int type = result.type;
                if (type != (type | AttendanceType.NORMA) ||
                        type == (type | AttendanceType.OVER_TIME) ||
                        type == (type | AttendanceType.WEEKEND_OVER_TIME)) {
                    //上下班不正常,平时加班,周末加班.都记录下名字,待下方添加异常数据
                    //这里,操作了.直接++,将位置下移
                    sheet.addCell(new Label(0, ++index, name, getCellFormat()));
                    sheet.addCell(new Label(1, index, year + "/" + month + "/" + today, getCellFormat()));
                    EmployeeRest employee = employeeItems.find({it.employeeName==name})

                    sheet.addCell(new Label(2, index, employee?employee.startDate:departmentRest.startDate, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(3, index, employee?employee.endDate:departmentRest.endDate, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(4, index, employee?employee.workDays.join(","):departmentRest.workDays.join(","), getCellFormat()));
                }
                if (type == (type | AttendanceType.LATE)) {
                    remarkItems<<"迟到"
                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Colour.RED, Alignment.LEFT)));
                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Alignment.LEFT)));
                }
                if (type == (type | AttendanceType.LEVEL_EARLY)) {
                    remarkItems<<"早退"
                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Colour.RED, Alignment.LEFT)));
                }
                if (type == (type | AttendanceType.ABSENTEEISM)) {
                    remarkItems<<"翘班"
                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                }
                if (type == (type | AttendanceType.UN_CHECK_IN)) {
                    remarkItems<<"早上未打卡"
                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Colour.RED, Alignment.LEFT)));
                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Alignment.LEFT)));
                }
                if (type == (type | AttendanceType.UN_CHECK_OUT)) {
                    remarkItems<<"晚上未打卡"
                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Colour.RED, Alignment.LEFT)));
                }
                int overHour = result.overMinute / 60;
                if (type == (type | AttendanceType.OVER_TIME)) {
                    colour=Colour.YELLOW
                    remarkItems<<"平时(${overHour}H)"
                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Alignment.LEFT)));
                }
                if (type == (type | AttendanceType.WEEKEND_OVER_TIME)) {
                    colour=Colour.YELLOW
                    remarkItems<<"周末(${overHour}H)"
                    sheet.addCell(new Label(5, index, year + "/" + month + "/" + today, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(6, index, result.startTime, getCellFormat(Alignment.LEFT)));
                    sheet.addCell(new Label(7, index, result.endTime, getCellFormat(Alignment.LEFT)));
                }
                if(remarkItems){
                    sheet.addCell(new Label(8, index, remarkItems.join("/"), getCellFormat(colour)));
                }
            }
        }
        InformantRegistry.getInstance(). notifyMessage("初始化员工考勤其他信息完成!");
    }

    @NotNull
    private WritableCellFormat getCellFormat() {
        return getCellFormat(null, jxl.format.Alignment.CENTRE);
    }

    @NotNull
    private WritableCellFormat getCellFormat(jxl.format.Colour colour) {
        return getCellFormat(colour, jxl.format.Alignment.CENTRE);
    }

    @NotNull
    private WritableCellFormat getCellFormat(jxl.format.Alignment gravity) {
        return getCellFormat(null, gravity);
    }


    @NotNull
    private WritableCellFormat getCellFormat(jxl.format.Colour colour, jxl.format.Alignment gravity) {
        WritableCellFormat wc1 = new WritableCellFormat();
        try {
            wc1.setAlignment(gravity);
            if (null != colour) wc1.setBackground(colour);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wc1;
    }

    public int getBestNum(String content) {
        int sum = 0;
        if (!TextUtils.isEmpty(content)) {
            sum = getChineseNum(content);
            sum += content.replaceAll("[\u4e00-\u9fa5]", "").length();
        }
        return sum;
    }

    public int getChineseNum(String context) {
        int lenOfChinese = 0;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(context);
        while (m.find()) {
            lenOfChinese += 2;
        }
        return lenOfChinese;
    }

    private int getDayIndex(int day) {
        return (day - INFO_LENGTH) * 3 + INFO_LENGTH;
    }
}
