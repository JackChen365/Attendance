package quant.attendance.excel.reader

import jxl.Cell
import jxl.Sheet
import jxl.Workbook
import quant.attendance.excel.InformantRegistry
import quant.attendance.model.Attendance
import quant.attendance.util.TextUtils

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Created by Administrator on 2017/4/14.
 */
class ExcelReaderB extends AbsExcelReader{

    @Override
    def readWorkBook(Workbook workbook) {
        HashMap<String, HashMap<Integer, ArrayList<Attendance>>> items = new HashMap<>();
        //获取文件的指定工作表 默认的第一个
        Sheet sheet = workbook.getSheet(2);
        int rows = sheet.getRows();
        //行数(表头的目录不需要，从1开始)
        //记录表格日期
        String employeeName = null;
        //2017/03/01 ~ 03/31
        Cell[] cells = sheet.getRow(2);
        def dateCell=cells[2]
        def content=dateCell.getContents()
        int year,month
        (year,month)=getCellDate(content)
        for (int i = 4; i < rows; i++) {
            cells = sheet.getRow(i);
            if (null != cells && 0 < cells.length) {
                if(0==i%2){
                    employeeName=getCellName(cells)
                    InformantRegistry.instance.notifyMessage("分析员工:$employeeName")
                } else {
                    for (int k = 0; k < cells.length; k++){
                        Cell cell = cells[k];
                        String contents = cell.getContents();
                        if (TextUtils.isEmpty(contents.trim())) continue;
                        //记录员工出勤信息
                        def matcher=contents=~/(\w{1,2}):(\w{1,2})/
                        while(matcher.find()){
                            Attendance attendance = new Attendance();
                            attendance.name=employeeName
                            int hour = matcher.group(1) as Integer
                            int minute = matcher.group(2)as Integer
                            attendance.year=year
                            attendance.month=month
                            attendance.day=k+1
                            attendance.hour=hour
                            attendance.minute=minute

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
                }
            }
        }
        return items
    }

    def getCellName(cellItems){
        String employeeName = null;
        for (int k = 0; k < cellItems.length; k++) {
            Cell cell = cellItems[k];
            String contents = cell.getContents();
            if (!TextUtils.isEmpty(contents.trim())){
                //记录姓名,部门信息
                if(10==k){
                    employeeName = contents
                } else if(20==k){
                    //部门信息
                }
            }
        }
        employeeName
    }

    def getCellDate(content){
        int year,month
        if(content){
            def matcher=content=~/(\w{2,4})\/(\w{1,2})\/(\w{1,2})\s+~\s+(\w{1,2})\/(\w{1,2})/
            if(matcher){
                year=matcher.group(1) as Integer
                month=matcher.group(2) as Integer
            }
        }
        [year,month]
    }
}
