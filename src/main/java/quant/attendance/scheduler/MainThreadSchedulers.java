package quant.attendance.scheduler;

import javafx.application.Platform;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.internal.schedulers.ScheduledAction;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.TimeUnit;

/**
 * Created by cz on 2017/3/7.
 */
public class MainThreadSchedulers extends Scheduler {

    private static final Scheduler MAIN_THREAD_SCHEDULER = new MainThreadSchedulers();

    public static Scheduler mainThread(){
        return MAIN_THREAD_SCHEDULER;
    }

    @Override
    public Worker createWorker() {
        return new InnerHandlerThreadScheduler();
    }

    private static class InnerHandlerThreadScheduler extends Worker {


        private final CompositeSubscription compositeSubscription = new CompositeSubscription();


        @Override
        public void unsubscribe() {
            compositeSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return compositeSubscription.isUnsubscribed();
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            final ScheduledAction scheduledAction = new ScheduledAction(action);
            scheduledAction.addParent(compositeSubscription);
            compositeSubscription.add(scheduledAction);
            Platform.runLater(scheduledAction);
            return scheduledAction;
        }

        @Override
        public Subscription schedule(final Action0 action) {
            return schedule(action, 0, TimeUnit.MILLISECONDS);
        }

    }
}
