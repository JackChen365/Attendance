package quant.attendance.excel.reader

import jxl.Cell
import jxl.Sheet
import jxl.Workbook
import jxl.read.biff.DateRecord
import quant.attendance.excel.InformantRegistry
import quant.attendance.model.Attendance
import quant.attendance.util.TextUtils

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by Administrator on 2017/4/9.
 */
class ExcelReaderA extends AbsExcelReader {
    final int START_YEAR=2000

    def readWorkBook(Workbook workbook){
        HashMap<String, HashMap<Integer, ArrayList<Attendance>>> items = new HashMap<>();
        //获取文件的指定工作表 默认的第一个
        Sheet sheet = rwb.getSheet(0);
        int rows = sheet.getRows();
        Pattern pattern = Pattern.compile("((\\d{1,2})[/|-](\\d{1,2})[/|-](\\d{1,4})|(\\d{1,4})[/|-](\\d{1,2})[/|-](\\d{1,2}))\\s+(\\d{1,2}):(\\d{1,2})(:\\d{1,2})?");//匹配日期
        //行数(表头的目录不需要，从1开始)
        String employeeName = null;
        def employeeNameItems=[] as Set
        for (int i = 1; i < rows; i++) {
            Cell[] cells = sheet.getRow(i);
            if (null != cells && 0 < cells.length) {
                Attendance attendance = new Attendance();
                for (int k = 1; k < cells.length; k++) {
                    Cell cell = cells[k];
                    String contents = cell.getContents();
                    if (TextUtils.isEmpty(contents.trim())) continue;
                    switch (k) {
                        case 1:
                            //记录姓名
                            employeeName = contents
                            attendance.name = contents
                            if(!employeeNameItems.contains(employeeName)){
                                employeeNameItems<<employeeName
                                InformantRegistry.instance.notifyMessage("分析员工:$attendance.name")
                            }
                            break;
                        case 3:
                            //考勤日期
                            if(DateRecord.class.isInstance(cell)){
                                LocalDateTime localDateTime=LocalDateTime.ofInstant(cell.date.toInstant(),ZoneId.systemDefault())
                                localDateTime=localDateTime.plusHours(-8)
                                def localDate=localDateTime.toLocalDate()
                                def localTime=localDateTime.toLocalTime()
                                attendance.year=localDate.year
                                attendance.month=localDate.monthValue
                                attendance.day=localDate.dayOfMonth
                                attendance.hour=localTime.hour
                                attendance.minute=localTime.minute
                            } else {
                                Matcher matcher = pattern.matcher(contents);
                                if (matcher.find()) {
                                    if(!TextUtils.isEmpty(matcher.group(2))&&
                                            !TextUtils.isEmpty(matcher.group(3))&&
                                            !TextUtils.isEmpty(matcher.group(4))){
                                        attendance.year =Integer.valueOf(matcher.group(4));
                                        if(START_YEAR>attendance.year){
                                            attendance.year+=START_YEAR
                                        }
                                        attendance.month = Integer.valueOf(matcher.group(2));
                                        attendance.day = Integer.valueOf(matcher.group(3));
                                    } else if(!TextUtils.isEmpty(matcher.group(5))&&
                                            !TextUtils.isEmpty(matcher.group(6))&&
                                            !TextUtils.isEmpty(matcher.group(7))){
                                        attendance.year =Integer.valueOf(matcher.group(5));
                                        if(START_YEAR>attendance.year){
                                            attendance.year+=START_YEAR
                                        }
                                        attendance.month = Integer.valueOf(matcher.group(6));
                                        attendance.day = Integer.valueOf(matcher.group(7));
                                    }
                                    attendance.hour=Integer.valueOf(matcher.group(8));
                                    attendance.minute=Integer.valueOf(matcher.group(9));
                                }
                            }
                            break;
                    }
                }
                HashMap<Integer, ArrayList<Attendance>> attendancesItems = items.get(employeeName);
                if (!attendancesItems) {
                    attendancesItems = [:]
                    items.put(employeeName, attendancesItems);
                }
                def localDateTime=LocalDateTime.of(attendance.year,attendance.month,attendance.day,attendance.hour,attendance.minute)
                attendance.timeMillis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                ArrayList<Attendance> attendances = attendancesItems.get(attendance.day);
                if (!attendances) {
                    attendances = [];
                    attendancesItems.put(attendance.day, attendances);
                }
                attendances.add(attendance);
            }
        }
        items
    }
}
