package quant.attendance.prefs

/**
 * Created by cz on 2017/2/24.
 */
class FilePrefs {
    final static def APP_NAME="Attendance"

    public final static File APP_FOLDER=new File(System.properties["user.home"],APP_NAME)
    public final static File CONFIG_FOLDER=new File(APP_FOLDER,"config")
    public final static File DATABASE_FOLDER=new File(APP_FOLDER,"database")
    public final static File HOLIDAY_FOLDER=new File(APP_FOLDER,"holiday")
    public final static File EXCEPTION_FOLDER=new File(APP_FOLDER,"exception")

    public final static File HOLIDAY_FILE=new File(HOLIDAY_FOLDER,"2017_holiday.properties")

    static {
        ensureFolder(APP_FOLDER,CONFIG_FOLDER, DATABASE_FOLDER,HOLIDAY_FOLDER,EXCEPTION_FOLDER)
    }

    static void ensureAllFolder(){
        ensureFolder(APP_FOLDER,CONFIG_FOLDER, DATABASE_FOLDER,HOLIDAY_FOLDER,EXCEPTION_FOLDER)
    }

    static def ensureFolder(File...folder){
        if(folder){
            folder.each { it.exists() ?: it.mkdirs() }
        }
    }
}
