package quant.attendance.excel.reader

import jxl.Cell
import jxl.Sheet
import jxl.Workbook
import quant.attendance.excel.InformantRegistry
import quant.attendance.model.Attendance
import quant.attendance.util.TextUtils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by Administrator on 2017/6/13.
 */
class ExcelReaderC extends AbsExcelReader{

    @Override
    def readWorkBook(Workbook workbook) {
        HashMap<String, HashMap<LocalDate, ArrayList<Attendance>>> items = new HashMap<>();
        //获取文件的指定工作表 默认的第一个
        Sheet sheet = workbook.getSheet(2);
        int rows = sheet.getRows();
        Pattern pattern1 = Pattern.compile("(\\d{1,4})[/|-](\\d{1,2})[/|-](\\d{1,2})");//匹配日期
        Pattern pattern2 = Pattern.compile("(\\d{1,2})[:|-](\\d{1,2})[:|-](\\d{1,2})");//匹配时间
        //行数(表头的目录不需要，从1开始)
        String employeeName = null;
        def employeeNameItems=[] as Set
        for (int i = 1; i < rows; i++) {
            Cell[] cells = sheet.getRow(i);
            if (null != cells && 0 < cells.length) {
                def attendanceItems=[]
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
                        case 2:
                            //记录部门
                            attendance.department=contents
                        case 3:
                            //考勤日期
                            Matcher matcher = pattern1.matcher(contents);
                            if (matcher.find()) {
                                if(!TextUtils.isEmpty(matcher.group(1))&&
                                        !TextUtils.isEmpty(matcher.group(2))&&
                                        !TextUtils.isEmpty(matcher.group(3))){
                                    attendance.year =Integer.valueOf(matcher.group(1));
                                    attendance.month = Integer.valueOf(matcher.group(2));
                                    attendance.day = Integer.valueOf(matcher.group(3));
                                }
                            }
                            break;
                        default:
                            Matcher matcher = pattern2.matcher(contents);
                            while (matcher.find()) {
                                def newItem = attendance.clone()
                                if(!TextUtils.isEmpty(matcher.group(1))&&
                                        !TextUtils.isEmpty(matcher.group(2))){
                                    newItem.hour =Integer.valueOf(matcher.group(1));
                                    newItem.minute = Integer.valueOf(matcher.group(2))
                                }
                                def localDateTime=LocalDateTime.of(newItem.year,newItem.month,newItem.day,newItem.hour,newItem.minute)
                                newItem.timeMillis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                attendanceItems<<newItem
                            }
                            break
                    }
                }
                if(0==attendance.day) continue
                HashMap<Integer, ArrayList<Attendance>> attendancesItems = items.get(employeeName);
                if (!attendancesItems) {
                    attendancesItems = [:]
                    items.put(employeeName, attendancesItems);
                }
                def localDate = LocalDate.of(attendance.year, attendance.month, attendance.day)
                ArrayList<Attendance> attendances = attendancesItems.get(localDate);
                if (!attendances) {
                    attendances = [];
                    attendancesItems.put(localDate, attendances);
                }
                attendances.addAll(attendanceItems)
            }
        }
        items
    }
}
