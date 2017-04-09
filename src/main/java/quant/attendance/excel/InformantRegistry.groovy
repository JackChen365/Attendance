package quant.attendance.excel

import javafx.application.Platform
import quant.attendance.util.TextUtils

import java.util.function.Consumer

/**
 * Created by Administrator on 2017/4/9.
 */
class InformantRegistry {

    static final def informantInstance=new InformantRegistry()

    final List listenerItems=[]
    static InformantRegistry getInstance(){ informantInstance }

    private InformantRegistry(){
    }
    /**
     * 通知消息
     *
     * @param message
     */
    public void notifyMessage(String message) {
        if(Platform.fxApplicationThread){
            listenerItems.each {it(message)}
        } else {
            Platform.runLater({ listenerItems.each {it(message)} })
        }
    }

    void addListener(consumer){
        listenerItems<<consumer
    }

    void removeListener(consumer){
        listenerItems.remove(consumer)
    }

}
