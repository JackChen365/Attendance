package quant.attendance.prefs

/**
 * Created by cz on 2017/2/15.
 */
class SharedPrefs {
    final static def CONFIG_NAME="config.properties"
    /**
     * 存储key value
     * @param adbPath
     */
    static def save(key,value){
        Properties properties=new Properties()
        def file=new File(FilePrefs.CONFIG_FOLDER,CONFIG_NAME)
        if(file.exists()){
            def inputStream=new FileInputStream(file)
            try{
                properties.load(inputStream)
            } catch (e){
                e.printStackTrace()
            } finally{
                inputStream.close()
            }
        }
        properties.put(key,value as String)
        properties.store(new FileOutputStream(file),"save key:$key value:$value")
    }

    /**
     * 获取properties值
     * @return
     */
    static String get(key) {
        def value
        def configFolder=FilePrefs.CONFIG_FOLDER
        if(configFolder.exists()) {
            Properties properties = new Properties()
            def file = new File(configFolder, CONFIG_NAME)
            if (file.exists()) {
                def inputStream = new FileInputStream(file)
                properties.load(inputStream)
                value = properties.getProperty(key)
            }
        }
        value
    }

    static boolean getBoolean(key){
        def value=get(key)
        return Boolean.valueOf(value)
    }
}
