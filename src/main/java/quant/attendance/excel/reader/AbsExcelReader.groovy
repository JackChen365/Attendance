package quant.attendance.excel.reader
import jxl.Workbook
import quant.attendance.excel.InformantRegistry
import quant.attendance.util.IOUtils
/**
 * Created by cz on 2017/4/14.
 */
abstract class AbsExcelReader {

    abstract def readWorkBook(Workbook workbook)

    def attendanceRead(File file){
        def items
        def rwb=getExcelWorkbook(file)
        if(rwb){
            InformantRegistry.instance.notifyMessage("开始分析文件:" + file.getName()+"!");
            items=readWorkBook(rwb)
            InformantRegistry.instance.notifyMessage("分析出勤信息完毕,共计:${items.size()}条")
        }
        return items
    }

    def getExcelWorkbook(File file){
        Workbook rwb
        InputStream stream
        try {
            stream = new FileInputStream(file);
            rwb = Workbook.getWorkbook(stream);
        } catch (Exception e) {
            InformantRegistry.instance.notifyMessage(file.getAbsolutePath() + " 文件未找到!");
        } finally {
            IOUtils.closeStream(stream);
        }
        rwb
    }
}
