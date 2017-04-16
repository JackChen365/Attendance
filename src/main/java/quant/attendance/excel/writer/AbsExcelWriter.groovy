package quant.attendance.excel.writer

import javafx.scene.paint.Color
import jxl.Workbook
import jxl.format.Colour
import jxl.write.WritableCellFormat
import jxl.write.WritableWorkbook
import jxl.write.WriteException
import quant.attendance.excel.InformantRegistry
import quant.attendance.model.AttendanceResult
import quant.attendance.model.DepartmentRest
import quant.attendance.model.Employee
import quant.attendance.model.EmployeeRest
import quant.attendance.prefs.PrefsKey
import quant.attendance.prefs.SharedPrefs
import quant.attendance.util.IOUtils
import quant.attendance.util.TextUtils

import java.time.LocalDateTime
import java.util.regex.Matcher

import static jxl.format.Colour.AUTOMATIC as COLOR_ABSENTEEISM

//----------------------------------------------------------
// thank you groovy static import!
//----------------------------------------------------------
import static jxl.format.Colour.BLUE2 as COLOR_LATE
import static jxl.format.Colour.DARK_BLUE2 as COLOR_UN_CHECK_IN
import static jxl.format.Colour.DARK_RED2 as COLOR_UN_CHECK_OUT
import static jxl.format.Colour.LIGHT_TURQUOISE2 as COLOR_LEVEL_EARLY
import static jxl.format.Colour.PINK2 as COLOR_OVER_TIME
import static jxl.format.Colour.PLUM2 as COLOR_WEEKEND_OVER_TIME
import static jxl.format.Colour.TEAL2 as COLOR_HOLIDAY_OVER_TIME
/**
 * Created by cz on 2017/4/14.
 */
abstract class AbsExcelWriter {
    private final HashMap<String, HashMap<Integer, AttendanceResult>> results;
    private final List<Employee> employeeItems;
    DepartmentRest departmentRest
    LocalDateTime startDateTime, endDateTime;
    final Map<String,Colour> colorItems=[:]
    final def holidayItems

    public AbsExcelWriter(LocalDateTime startDateTime,LocalDateTime endDateTime, HashMap<String, HashMap<Integer, AttendanceResult>> results,DepartmentRest departmentRest,employeeItems,holidayItems) {
        this.startDateTime=startDateTime;
        this.endDateTime=endDateTime;
        this.departmentRest=departmentRest
        this.holidayItems=holidayItems
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

        //初始化配置颜色
        addColorItem(PrefsKey.COLOR_LATE,Color.BISQUE,COLOR_LATE)
        addColorItem(PrefsKey.COLOR_LEVEL_EARLY,Color.AZURE,COLOR_LEVEL_EARLY)
        addColorItem(PrefsKey.COLOR_ABSENTEEISM,Color.RED,COLOR_ABSENTEEISM)
        addColorItem(PrefsKey.COLOR_UN_CHECK_IN,Color.BLUE,COLOR_UN_CHECK_IN)
        addColorItem(PrefsKey.COLOR_UN_CHECK_OUT,Color.DARKBLUE,COLOR_UN_CHECK_OUT)
        addColorItem(PrefsKey.COLOR_OVER_TIME,Color.GREEN,COLOR_OVER_TIME)
        addColorItem(PrefsKey.COLOR_WEEKEND_OVER_TIME,Color.GREENYELLOW,COLOR_WEEKEND_OVER_TIME)
        addColorItem(PrefsKey.COLOR_HOLIDAY_OVER_TIME,Color.YELLOW,COLOR_HOLIDAY_OVER_TIME)
    }

    void addColorItem(key,Color defaultColor,colour){
        Color color=defaultColor
        def colorValue=SharedPrefs.get(key)
        if(colorValue){
            color=Color.valueOf(colorValue)
        }
        colorItems<<[(colour):new Colour(Colour.allColours.length,key,(int)Math.round(color.red * 255.0),(int)Math.round(color.green * 255.0),(int)Math.round(color.blue * 255.0))]
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
            // 更改标准调色板颜色
            // http://stackoverflow.com/questions/1834973/making-new-colors-in-jexcelapi
            colorItems.each { wwb.setColourRGB(it.key,it.value.defaultRed,it.value.defaultGreen,it.value.defaultBlue) }
            //记录其他数据
            write(wwb, startDateTime,endDateTime, results, employeeItems,holidayItems);
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

    WritableCellFormat getCellFormat() {
        return getCellFormat(null, jxl.format.Alignment.CENTRE);
    }

    WritableCellFormat getCellFormat(jxl.format.Colour colour) {
        return getCellFormat(colour, jxl.format.Alignment.CENTRE);
    }

    WritableCellFormat getCellFormat(jxl.format.Alignment gravity) {
        return getCellFormat(null, gravity);
    }


    WritableCellFormat getCellFormat(jxl.format.Colour colour, jxl.format.Alignment gravity) {
        WritableCellFormat wc1 = new WritableCellFormat();
        wc1.setAlignment(gravity);
        if(colour){
            wc1.setBackground(colour)
        }
        wc1
    }

    int getBestNum(String content) {
        int sum = 0;
        if (!TextUtils.isEmpty(content)) {
            sum = getChineseNum(content);
            sum += content.replaceAll("[\u4e00-\u9fa5]", "").length();
        }
        sum
    }

    int getChineseNum(String context) {
        int lenOfChinese = 0;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(context);
        while (m.find()) {
            lenOfChinese += 2;
        }
        return lenOfChinese;
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
    abstract void write(WritableWorkbook wwb,LocalDateTime startDateTime,LocalDateTime endDateTime,
                        HashMap<String, HashMap<Integer, AttendanceResult>> results,List<EmployeeRest> employeeItems,holidayItems) throws WriteException
}
