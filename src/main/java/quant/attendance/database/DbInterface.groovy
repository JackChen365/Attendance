package quant.attendance.database

import quant.attendance.model.DepartmentRest
import quant.attendance.model.EmployeeRest

/**
 * Created by cz on 2017/2/27.
 */
interface DbInterface {

    boolean insertDepartment(DepartmentRest item)

    void deleteDepartment(int id,String name)

    void updateDepartment(DepartmentRest item)

    List<DepartmentRest> queryDepartment()

    boolean insertEmployeeRest(EmployeeRest item)

    void deleteEmployeeByDepartmentId(int id)

    void deleteEmployeeRest(int id,String name)

    void updateEmployeeRest(EmployeeRest item)

    List<EmployeeRest> queryEmployeeRest()
}
