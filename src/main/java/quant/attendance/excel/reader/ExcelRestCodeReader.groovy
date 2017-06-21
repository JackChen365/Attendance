package quant.attendance.excel.reader

import jxl.Cell
import jxl.Sheet
import jxl.Workbook
import quant.attendance.model.dynamic.RestCode
import quant.attendance.model.dynamic.RestCodeItem

import java.time.LocalTime
import java.util.regex.Pattern

/**
 * Created by Administrator on 2017/6/21.
 */
class ExcelRestCodeReader extends AbsExcelReader{

    @Override
    def readWorkBook(Workbook workbook) {
        def items=new HashMap()
        //获取文件的指定工作表 默认的第一个
        Sheet sheet = workbook.getSheet(0);
        int rows = sheet.getRows();
        Pattern pattern = Pattern.compile("(?<ITEM1>(\\d{1,2})[:|：](\\d{1,2})-(\\d{1,2})[:|：](\\d{1,2}))|(?<ITEM2>(\\d{1,2})[:|：](\\d{1,2}))");//匹配日期
        for (int i = 1; i < rows; i++) {
            Cell[] cells = sheet.getRow(i);
            if (null != cells && 0 < cells.length) {
                def item=new RestCodeItem()
                for (int k = 0; k < cells.length; k++) {
                    def contents = cells[k].contents
                    if(null==contents||0==contents.trim().length())continue
                    switch (k){
                        case 0:
                            item.code=contents
                            break;
                        case 1:
                            def (hour,minute) = getContentTime(pattern, contents)
                            item.startDate="$hour:$minute"
                            item.startTimeMillis=LocalTime.of(hour,minute).toSecondOfDay()*1000
                            break;
                        case 2:
                            def (hour,minute) = getContentTime(pattern, contents)
                            item.endDate="$hour:$minute"
                            item.endTimeMillis=LocalTime.of(hour,minute).toSecondOfDay()*1000
                            break;
                        case 4:
                            item.duration=cells[k].contents as Float
                            break;
                    }
                }
                if("休"==item.code) item.code="WEEK"
                items.put(RestCode.valueOf(item.code),item)
            }
        }
        return items
    }

    def getContentTime(Pattern pattern,content){
        def matcher = pattern.matcher(content)
        def hour,minute
        if(matcher){
             if(matcher.group("ITEM1")){
                hour=matcher.group(2) as Integer
                minute=matcher.group(3) as Integer
             } else if(matcher.group("ITEM2")){
                hour=matcher.group(7) as Integer
                minute=matcher.group(8) as Integer
             }
        }
        return [hour,minute]
    }
}
