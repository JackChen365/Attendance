package quant.attendance

import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import quant.attendance.callback.InitializableArgs

/**
 * Created by cz on 2017/2/16.
 */
class StageManager {
    static final StageManager instance=new StageManager();
    private static final Map<Object,Stage> stageItems=new HashMap<>();

    static final StageManager getInstance(){
        return instance;
    }

    private StageManager(){
    }

    Stage stage(item,url,int width,int height){
        stage(item,null,url,width,height)
    }

    Stage stage(stage,args,url,int width,int height){
        stage?:(stage=new Stage())
        FXMLLoader loader=new FXMLLoader(url)
        def parent = loader.load()
        Scene scene=new Scene(parent,width,height)
        def controller = loader.getController()
        if(controller instanceof InitializableArgs){
            controller.initializeWithArgs(args)
        }
        !controller?:stageItems.put(controller,stage)

        scene.getStylesheets().add(getClass().getResource("/resources/css/jfoenix-fonts.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/resources/css/jfoenix-design.css").toExternalForm());
        scene.getStylesheets().add(getClass().getClassLoader().getResource("css/jfoenix-main-demo.css").toExternalForm());
        stage.setScene(scene)
        stage
    }

    Stage newStage(url,int width,int height){
        stage(null,null,url,width,height);
    }

    Stage newStage(url,args,int width,int height){
        stage(null,args,url,width,height);
    }

    Stage getStage(controller){
        stageItems.get(controller)
    }

}
