package quant.attendance.util
/**
 * Created by cz on 2017/3/6.
 */
class FileUtils {
    static final def TAG="FileUtils"

    static void copyResourcesFileIfNotExists(File file,String path) {
        copyResourcesFile(file,path)
    }

    static void copyResourcesFile(File file,String path,closure=null) {
        if(!file.exists()){
            BufferedWriter writer=new BufferedWriter(new FileWriter(file))
            InputStream inputStream = FileUtils.class.getClassLoader().getResourceAsStream(path);
            inputStream.withReader {
                it.readLines().each { writer.write(it+"\n") }
            }
            writer?.close()
            closure?.call(file)
        }
    }

    static def loadProperty(File file){
        Properties properties
        InputStream inputStream = new FileInputStream(file)
        if(null!=inputStream){
            InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8")
            properties=new Properties()
            try{
                properties.load(reader)
            } catch (e){
                e.printStackTrace()
            } finally{
                inputStream.close()
            }
        }
        properties
    }


}
