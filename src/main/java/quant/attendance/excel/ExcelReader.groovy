package quant.attendance.excel

import jxl.Cell
import jxl.Sheet
import jxl.Workbook
import quant.attendance.model.Attendance
import quant.attendance.util.IOUtils
import quant.attendance.util.TextUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by Administrator on 2017/4/9.
 */
class ExcelReader {
    final int START_YEAR=2000
    /**
     * 读取模板的员工出勤信息
     *
     * @return
     */
    public HashMap<String, HashMap<Integer, ArrayList<Attendance>>> attendanceRead(File file) {
        HashMap<String, HashMap<Integer, ArrayList<Attendance>>> items = new HashMap<>();
        Workbook rwb = null;
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            rwb = Workbook.getWorkbook(stream);
        } catch (Exception e) {
            InformantRegistry.instance.notifyMessage(file.getAbsolutePath() + " 文件未找到!");
        } finally {
            IOUtils.closeStream(stream);
        }
        InformantRegistry.instance.notifyMessage("开始分析文件:" + file.getName()+"!");
        //获取文件的指定工作表 默认的第一个
        Sheet sheet = rwb.getSheet(0);
        int rows = sheet.getRows();
        Pattern pattern = Pattern.compile("((\\d{1,2})[/|-](\\d{1,2})[/|-](\\d{1,4})|(\\d{1,4})[/|-](\\d{1,2})[/|-](\\d{1,2}))\\s+(\\d{1,2}):(\\d{1,2})(:\\d{1,2})?");//匹配日期
        //行数(表头的目录不需要，从1开始)
        Calendar calendar = Calendar.getInstance();
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
                            break;
                    }
                }
                HashMap<Integer, ArrayList<Attendance>> attendancesItems = items.get(employeeName);
                if (!attendancesItems) {
                    attendancesItems = [:]
                    items.put(employeeName, attendancesItems);
                }
                calendar.set(attendance.year, attendance.month, attendance.day, attendance.hour, attendance.minute, attendance.second);
                attendance.timeMillis = calendar.getTimeInMillis();
                ArrayList<Attendance> attendances = attendancesItems.get(attendance.day);
                if (!attendances) {
                    attendances = [];
                    attendancesItems.put(attendance.day, attendances);
                }
                attendances.add(attendance);
            }
        }
        InformantRegistry.instance.notifyMessage("分析出勤信息完毕,共计:${items.size()}条")
        return items;
    }
}
