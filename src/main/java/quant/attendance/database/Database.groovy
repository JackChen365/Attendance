package quant.attendance.database

import quant.attendance.prefs.FilePrefs

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
/**
 * Created by cz on 2017/2/24.
 */
class Database {
    final static def DB_NAME="attendance.db"
    final static def DEPARTMENT="department"
    final static def EMPLOYEE="employee"
    def static Connection con

    static {
        Class.forName("org.sqlite.JDBC")
        def statement =null
        try {
            statement = connection.createStatement()
            //部门表
            statement.execute("CREATE TABLE IF NOT EXISTS $DEPARTMENT(_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,work_day TEXT,start_date TEXT,start_ms LONG,end_date TEXT,end_ms LONG)")
            //员工表
            statement.execute("CREATE TABLE IF NOT EXISTS $EMPLOYEE(_id INTEGER PRIMARY KEY AUTOINCREMENT,department_id INTEGER,department_name INTEGER,name TEXT,work_day TEXT,start_date TEXT,start_ms LONG,end_date TEXT,end_ms LONG,entry_time LONG)")
        } catch (ex) {
            ex.printStackTrace()
        } finally {
            statement?.closed?:statement?.close()
        }
    }

    static Connection getConnection() {
        try {
            def localFile=new File(FilePrefs.DATABASE_FOLDER,DB_NAME)
            con = DriverManager.getConnection("jdbc:sqlite:"+localFile.absolutePath)
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return con;
    }

}
