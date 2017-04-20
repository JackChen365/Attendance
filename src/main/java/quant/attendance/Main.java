package quant.attendance;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import quant.attendance.exception.ExceptionHandler;
import quant.attendance.prefs.FilePrefs;
import quant.attendance.prefs.PrefsKey;
import quant.attendance.prefs.SharedPrefs;
import quant.attendance.util.FileUtils;
import quant.attendance.util.TextUtils;

import java.nio.file.Files;

/**
 * Created by cz on 2017/3/6.
 * 1;消息日志过滤,以及展示
 * 2:计划优先级测试机制
 * 3:apk包copy
 */
public class Main extends Application {
    final static String VERSION="2.0";
    @Override
    public void start(Stage primaryStage) throws Exception{
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        //检测用户是否可以登录
        StageManager stageManager = StageManager.getInstance();
        boolean init = SharedPrefs.getBoolean(PrefsKey.INIT);
        String version = SharedPrefs.get(PrefsKey.VERSION);
        if(TextUtils.isEmpty(version)){
            //初次安装,删除所有用户目录
            FileUtils.deleteDir(FilePrefs.APP_FOLDER);
            FilePrefs.ensureAllFolder();
            SharedPrefs.save(PrefsKey.VERSION,VERSION);
        } else if(!VERSION.equals(version)){
            //TODO 版本不同
            SharedPrefs.save(PrefsKey.VERSION,VERSION);
        }
        primaryStage.setTitle("Hello!");
        if(!init){
            //初始化配置
            stageManager.stage(primaryStage,init,getClass().getClassLoader().getResource("fxml/add_department.fxml"), 640, 720);
        } else {
            //主界面
            stageManager.stage(primaryStage, getClass().getClassLoader().getResource("fxml/main_layout.fxml"), 960, 720);
        }
        //拷贝节日资料
        FileUtils.copyResourcesFileIfNotExists(FilePrefs.HOLIDAY_FILE,"assets/2017_holiday.properties");
        //结束监听
        PlatformImpl.addListener(new PlatformImpl.FinishListener() {
            @Override
            public void idle(boolean implicitExit) {
            }

            @Override
            public void exitCalled() {
                //exit
                System.exit(0);
            }
        });
        primaryStage.setOnCloseRequest(it -> Platform.exit());
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}

