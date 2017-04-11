package quant.attendance;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import quant.attendance.prefs.FilePrefs;
import quant.attendance.prefs.PrefsKey;
import quant.attendance.prefs.SharedPrefs;
import quant.attendance.util.FileUtils;

/**
 * Created by cz on 2017/3/6.
 * 1;消息日志过滤,以及展示
 * 2:计划优先级测试机制
 * 3:apk包copy
 */
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        //检测用户是否可以登录
        StageManager stageManager = StageManager.getInstance();
        boolean init = SharedPrefs.getBoolean(PrefsKey.INIT);
        if(!init){
            //初始化配置
            primaryStage.setTitle("Hello!");
            stageManager.stage(primaryStage,init,getClass().getClassLoader().getResource("fxml/add_department.fxml"), 640, 720);
        } else {
            //主界面
            primaryStage.setTitle("考勤数据分析");
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

