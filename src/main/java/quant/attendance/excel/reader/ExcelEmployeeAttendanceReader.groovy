package quant.attendance.excel.reader

import jxl.Cell
import jxl.Sheet
import jxl.Workbook
import quant.attendance.model.dynamic.EmployeeAttendance
import quant.attendance.model.dynamic.RestCode

/**
 * Created by Administrator on 2017/6/21.
 */
class ExcelEmployeeAttendanceReader extends AbsExcelReader{

    @Override
    def readWorkBook(Workbook workbook) {
        def items=new HashMap()
        //获取文件的指定工作表 默认的第一个
        Sheet sheet = workbook.getSheet(1);
        int rows = sheet.getRows();
        for (int i = 2; i < rows; i++) {
            Cell[] cells = sheet.getRow(i);
            if (null != cells && 0 < cells.length) {
                def item=new EmployeeAttendance()
                for (int k = 0; k < cells.length; k++) {
                    def contents = cells[k].contents
                    if(null==contents||0==contents.trim().length())continue
                    switch (k){
                        case 0:
                            item.name=contents
                            break;
                        case 1:
                            item.year=contents as Integer
                            break;
                        case 2:
                            item.mouth=contents as Integer
                            break;
                        default:
                            def newItem = item.clone()
                            newItem.day=k-2
                            newItem.code=contents
                            if("休"==newItem.code) newItem.code=RestCode.WEEK
                            items[item.name]?:(items[item.name]=new ArrayList())
                            items[item.name]<<newItem
                            break;
                    }
                }
            }
        }
        return items
    }
}
