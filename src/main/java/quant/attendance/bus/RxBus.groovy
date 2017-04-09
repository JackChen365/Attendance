package quant.attendance.bus

import javafx.application.Platform
import rx.Observable
import rx.Subscription
import rx.functions.Action1
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subjects.Subject

/**
 * Created by cz on 2017/2/16.
 */
class RxBus {
    static final Subject bus = new SerializedSubject<>(PublishSubject.create());
    static final HashMap<String,List<Subscription>> subscribeItems=[:]

    /**
     * 发送事件
     * @param o
     */
    static void post(o) {
        if(Platform.fxApplicationThread){
            bus.onNext(o)
        } else {
            Platform.runLater({ bus.onNext(o)})
        }
    }

    static<T> void  subscribe(Class<T> eventType, Action1<T> action1){
        subscribe(eventType,action1,null)
    }

    /**
     * 根据class event type观察事件
     * @param <T>
     * @param eventType
     * @return
     */
    static<T> void  subscribe(final Class<T> eventType, Action1<T> action1, Action1<Throwable> action2) {
        String className = getCallClass();
        List<Subscription> items = subscribeItems.get(className);
        if(null==items){
            subscribeItems.put(className,items=new ArrayList<>());
        }
        Observable observable = bus.filter({eventType.isInstance(it)}).cast(eventType);
        if(null!=action1){
            items.add(observable.subscribe(action1, {e->
                e.printStackTrace();
                if(null!=action2){
                    action2.call(e);
                }
            }));
        }
    }

    /**
     * 取消订阅,自动回调的对象,无须用户调用
     * @param object
     */
    static void unSubscribeItems(Object object) {
        if(null!=object){
            String name = object.getClass().getName();
            List<Subscription> items = subscribeItems.get(name);
            if (null != items) {
                for (int i = 0; i < items.size(); i++) {
                    Subscription subscription = items.get(i);
                    if (null != subscription && !subscription.isUnsubscribed()) {
                        subscription.unsubscribe();
                    }
                }
                items.clear();
            }
        }
    }

    static void unSubscribeAll(){
        for(Map.Entry<String,List<Subscription>> entry:subscribeItems.entrySet()){
            List<Subscription> items = entry.getValue();
            if (null != items) {
                for (int i = 0; i < items.size(); i++) {
                    Subscription subscription = items.get(i);
                    if (null != subscription && !subscription.isUnsubscribed()) {
                        subscription.unsubscribe();
                    }
                }
                items.clear();
            }
        }
    }

    /**
     * 获取当前调用class对象,缓存作tag-event,再利用ActivityLifecycleCallbacks接口自动释放订阅对象
     * @return
     */
    static String getCallClass() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int length = stackTrace.length;
        String name = RxBus.class.getName();
        StackTraceElement stackTraceElement = null;
        boolean isCallStackTrace=false;
        for (int i = 0; i < length; i++) {
            stackTraceElement = stackTrace[i];
            if (name.equals(stackTraceElement.getClassName())) {
                isCallStackTrace=true;
            } else if(isCallStackTrace){
                break;
            }
        }
        String className=stackTraceElement.getClassName();
        int index = className.lastIndexOf("\$");
        if(-1<index){
            className=className.substring(0,index);
        }
        return className;
    }
}
