package victorolaitan.timothyTwitterBot.trigger;

import victorolaitan.timothyTwitterBot.response.Response;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Initial commit by Victor Olaitan on 07/03/2017.
 */
public abstract class Trigger {
    private static final class TriggerCycle implements Runnable {
        private static final int TWITTER_MAX_QUERIES_PER_MIN = 15;
        private Thread thread;
        private int size;
        private long waitTime;
        private boolean running;
        private static int currentPosition = 0;

        TriggerCycle() {
            thread = new Thread(this);
            thread.run();
        }

        @Override
        public void run() {
            running = true;
            for (int i = currentPosition; i < size; i++, currentPosition++) {
                Trigger trigger = triggers.get(i);
                if (trigger.active) {
                    trigger.onUpdateCycle();
                }
                if (!running) {
                    return;
                }
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (currentPosition > 0 && currentPosition < triggers.size()) {
                for (int i = 0; i <= currentPosition; i++) {
                    Trigger trigger = triggers.get(i);
                    if (trigger.active) {
                        trigger.onUpdateCycle();
                    }
                    if (!running) {
                        return;
                    }
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        void notifySizeUpdate() {
            running = false;
            size = triggers.size();
            if (size > 0) {
                double triggerRatio = size / TWITTER_MAX_QUERIES_PER_MIN;
                waitTime = (long) triggerRatio * (60000 / TWITTER_MAX_QUERIES_PER_MIN);
                run();
            } else {
                currentPosition = 0;
            }
        }
    }

    private static boolean initialised;
    private static ArrayList<Trigger> triggers = new ArrayList<>();
    private static TriggerCycle triggerCycle;

    public static void init() {
        if (!initialised) {
            triggerCycle = new TriggerCycle();
            initialised = true;
        }
    }


    public static void saveTriggers() {
        boolean resume = triggerCycle.running;
        triggerCycle.running = false;
        EasyJSON json = EasyJSON.create();
        json.putArray("triggers");
        for (Trigger trigger : triggers) {
            json.search("triggers").putGeneric(trigger.exportTrigger(), EasyJSON.JSONElementType.STRUCTURE);
        }
        try {
            json.save("triggers.txt");
        } catch (EasyJSON.ParseException e) {
            e.printStackTrace();
        }
        if (resume) {
            triggerCycle.run();
        }
    }

    public static Trigger includeTrigger(Class<? extends Trigger> triggerClass, Object... args) throws InstantiationException {
        try {
            Trigger trigger = (Trigger) triggerClass.getConstructors()[0].newInstance(args);
            triggers.add(trigger);
            triggerCycle.notifySizeUpdate();
            return trigger;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T extends Trigger> T locateTrigger(Class<T> triggerClass) {
        for (Trigger trigger : triggers) {
            if (trigger.getClass().equals(triggerClass)) {
                return (T) trigger;
            }
        }
        return null;
    }


    private ArrayList<Response> responses = new ArrayList<>();
    private boolean active;

    Trigger(Response... responses) {
        triggers.add(this);
        triggerCycle.notifySizeUpdate();
        this.responses.addAll(Arrays.stream(responses).collect(Collectors.toList()));
    }

    public EasyJSON exportTrigger() {
        EasyJSON json = EasyJSON.create();
        json.put("class", "FollowTrigger", EasyJSON.JSONElementType.PRIMITIVE);
        json.put("active", active, EasyJSON.JSONElementType.PRIMITIVE);
        json.putArray("responses");
        for (Response response : responses) {
            json.search("responses").putGeneric(response.exportResponse(), EasyJSON.JSONElementType.STRUCTURE);
        }
        return json;
    }

    public abstract void onUpdateCycle();

    public void deliver(Object... args) {
        responses.forEach(response -> response.run(args));
    }

    public void addResponses(Response... responses) {
        this.responses.addAll(Arrays.stream(responses).collect(Collectors.toList()));
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

}
