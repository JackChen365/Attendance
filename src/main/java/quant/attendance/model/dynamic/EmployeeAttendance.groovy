package quant.attendance.model.dynamic

/**
 * Created by Administrator on 2017/6/21.
 */
class EmployeeAttendance implements Cloneable{
    public int year
    public int mouth
    public int day
    public String name
    public String code

    @Override
    public EmployeeAttendance clone(){
        def item = new EmployeeAttendance()
        item.year = year
        item.mouth = mouth
        item.day = day
        item.name = name
        item.code = code
        return item
    }
}
