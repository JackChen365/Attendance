package quant.attendance.observable;

import java.util.Observable;

/**
 * Created by cz on 2017/2/17.
 */
public class DataObservable extends Observable {
    @Override
    public void notifyObservers(Object arg) {
        setChanged();
        super.notifyObservers(arg);
    }
}
