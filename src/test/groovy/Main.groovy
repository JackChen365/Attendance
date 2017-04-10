import groovy.io.FileType

import java.time.LocalDateTime
import java.util.regex.Pattern

/**
 * Created by Administrator on 2017/4/9.
 */
def testPattern(){
    Pattern pattern = Pattern.compile("((\\d{1,2})[/|-](\\d{1,2})[/|-](\\d{1,4})|(\\d{1,4})[/|-](\\d{1,2})[/|-](\\d{1,2}))\\s+(\\d{1,2}):(\\d{1,2})(:\\d{1,2})?");//匹配日期
    def matcher=pattern.matcher("2017/3/1  9:28:59")
    if(matcher.find()){
        for(int i=1;i<=matcher.groupCount();i++){
            println matcher.group(i)
        }
    }
}

def testDate(){
    //测试日期是从哪天开始,周一:dayOfWeek 值为1 周日为:7
    def now = LocalDateTime.now()
    println now.dayOfWeek.value
}

//项目代码行数
def readProjectLine(){
    def file=new File("C:\\Users\\Administrator\\Desktop\\JavaFx\\Attendance\\src\\main\\java\\quant")
    if(file.exists()){
        def count=0
        file.eachFileRecurse(FileType.FILES) {
            if(it.name.endsWith('.groovy')) {
                def lineCount=it.readLines().size()
                println "File:$it.name line:$lineCount"
                count+=lineCount
            }
        }
        println "count$count"
    }
}

def testType(){
    def var="123"
    def newVar=var as Integer
    println newVar.class
}

//testDate()
testType()