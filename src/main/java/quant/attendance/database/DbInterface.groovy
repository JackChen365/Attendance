package quant.attendance.database

import quant.attendance.model.DepartmentRest
import quant.attendance.model.EmployeeRest

/**
 * Created by cz on 2017/2/27.
 */
interface DbInterface {

    boolean insertDepartment(DepartmentRest item)

    void deleteDepartment(DepartmentRest item)

    void updateDepartment(DepartmentRest item)

    List<DepartmentRest> queryDepartment()

    boolean insertEmployeeRest(EmployeeRest item)

    void deleteEmployeeRest(EmployeeRest item)

    void updateEmployeeRest(EmployeeRest item)

    List<EmployeeRest> queryEmployeeRest()
}
