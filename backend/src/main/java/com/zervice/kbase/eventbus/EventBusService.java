package com.zervice.kbase.eventbus;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * We use guava EventBus as internal *non-persistent* message channel
 */
@UtilityClass
@Log4j2
public class EventBusService {
    private static AsyncEventBus _ebus = null;
//    private DeadEventHandler _handler = new DeadEventHandler();

    public static void init(int numThreads) {
        Preconditions.checkArgument(numThreads > 0, "numThread must be > 0");
        _ebus = new AsyncEventBus(Executors.newFixedThreadPool(numThreads, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "eventbus");
            }
        }));
    }

    public static void post(SystemEvent event) {
        _ebus.post(event);
    }

    public static void register(Object object) {
        _ebus.register(object);
    }

    public static void unregister(Object object) {
        _ebus.unregister(object);
    }

    /**
     * A dead event handler. Just print the dead event ...
     */
    class DeadEventHandler {
        @Subscribe
        public void handleDeadEvent(DeadEvent deadEvent) {
            LOG.warn("Unhandled dead event - " + deadEvent.toString());
        }
    }
}
