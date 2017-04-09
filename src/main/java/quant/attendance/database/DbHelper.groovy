package quant.attendance.database

import quant.attendance.model.DepartmentRest
import quant.attendance.model.EmployeeRest

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

/**
 * Created by cz on 2017/2/27.
 */
class DbHelper implements DbInterface{

    static final def helper=new DbHelper()

    public static DbHelper getHelper(){
        helper
    }

    @Override
    boolean insertDepartment(DepartmentRest item) {
        def connection=Database.connection
        PreparedStatement statement = connection.prepareStatement("INSERT INTO $Database.DEPARTMENT(" +
                "name," +
                "work_day," +
                "start_date," +
                "start_ms," +
                "end_date," +
                "end_ms) VALUES(?,?,?,?,?,?)")
        statement.setString(1,item.departmentName)
        statement.setString(2,item.workDayToString())
        statement.setString(3,item.startDate)
        statement.setLong(4,item.startTimeMillis)
        statement.setString(5,item.endDate)
        statement.setLong(6,item.endTimeMillis)
        boolean result = statement.execute()
        statement.closed?:statement.close()
        result
    }

    @Override
    void deleteDepartment(DepartmentRest item) {

    }

    @Override
    void updateDepartment(DepartmentRest item) {

    }

    @Override
    List<DepartmentRest> queryDepartment() {
        def connection=Database.connection
        Statement statement = connection.createStatement()
        ResultSet resultSet=statement.executeQuery("SELECT " +
                "_id," +
                "name," +
                "work_day," +
                "start_date," +
                "start_ms," +
                "end_date," +
                "end_ms FROM "+
                "$Database.DEPARTMENT ORDER BY _id DESC ")
        def items=[]
        while(resultSet.next()){
            def departmentRest=new DepartmentRest()
            departmentRest.id=resultSet.getInt(1)
            departmentRest.departmentName=resultSet.getString(2)
            def workDayValue=resultSet.getString(3)
            if(workDayValue){
                workDayValue.split(",").each { departmentRest.workDays<<(it as Integer)}
            }
            departmentRest.startDate=resultSet.getString(4)
            departmentRest.startTimeMillis=resultSet.getLong(5)
            departmentRest.endDate=resultSet.getString(6)
            departmentRest.endTimeMillis=resultSet.getLong(7)
            items<<departmentRest
        }
        statement.closed?:statement.close()
        return items
    }

    @Override
    boolean insertEmployeeRest(EmployeeRest item) {
        def connection=Database.connection
//        department_id INTEGER,name TEXT,start_date TEXT,start_ms LONG,end_date TEXT,end_ms LONG
        PreparedStatement statement = connection.prepareStatement("INSERT INTO $Database.EMPLOYEE(" +
                "name," +
                "work_day," +
                "department_id," +
                "department_name," +
                "start_date," +
                "start_ms," +
                "end_date," +
                "end_ms) VALUES(?,?,?,?,?,?,?,?)")
        statement.setString(1,item.employeeName)
        statement.setString(2,item.workDayToString())
        statement.setInt(3,item.departmentId)
        statement.setString(4,item.departmentName)
        statement.setString(5,item.startDate)
        statement.setLong(6,item.startTimeMillis)
        statement.setString(7,item.endDate)
        statement.setLong(8,item.endTimeMillis)
        boolean result = statement.execute()
        statement.closed?:statement.close()
        result
    }

    @Override
    void deleteEmployeeRest(EmployeeRest item) {

    }

    @Override
    void updateEmployeeRest(EmployeeRest item) {

    }

    @Override
    List<EmployeeRest> queryEmployeeRest() {
        def connection=Database.connection
        Statement statement = connection.createStatement()
        ResultSet resultSet=statement.executeQuery("SELECT " +
                "_id," +
                "department_id," +
                "department_name," +
                "name," +
                "work_day," +
                "start_date," +
                "start_ms," +
                "end_date," +
                "end_ms FROM "+
                "$Database.EMPLOYEE ORDER BY _id DESC ")
        def items=[]
        while(resultSet.next()){
            def employeeRest=new EmployeeRest()
            employeeRest.id=resultSet.getInt(1)
            employeeRest.departmentId=resultSet.getInt(2)
            employeeRest.departmentName=resultSet.getString(3)
            employeeRest.employeeName=resultSet.getString(4)
            def workDayValue=resultSet.getString(5)
            !workDayValue?:(workDayValue.split(",").each {employeeRest.workDays<<(it as Integer)})
            employeeRest.startDate=resultSet.getString(6)
            employeeRest.startTimeMillis=resultSet.getLong(7)
            employeeRest.endDate=resultSet.getString(8)
            employeeRest.endTimeMillis=resultSet.getLong(9)
            items<<employeeRest
        }
        statement.closed?:statement.close()
        return items
    }
}
