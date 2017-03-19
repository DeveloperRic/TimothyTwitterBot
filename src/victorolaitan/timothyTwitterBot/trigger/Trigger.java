package victorolaitan.timothyTwitterBot.trigger;

import victorolaitan.timothyTwitterBot.response.Response;
import victorolaitan.timothyTwitterBot.response.ResponseDataType;
import victorolaitan.timothyTwitterBot.util.EasyJSON;
import victorolaitan.timothyTwitterBot.util.Util;

import java.io.IOException;
import java.lang.reflect.Constructor;
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
        private int currentPosition;

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
                    boolean complete = trigger.onUpdateCycle();
                    if (complete) {
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (!running) {
                    return;
                }
            }
            currentPosition = 0;
        }

        void notifySizeUpdate() {
            running = false;
            size = triggers.size();
            if (size > 0) {
                waitTime = (size / TWITTER_MAX_QUERIES_PER_MIN) * (60000 / TWITTER_MAX_QUERIES_PER_MIN);
                run();
            } else {
                currentPosition = 0;
            }
        }
    }

    private static boolean initialised;
    public static ArrayList<Trigger> triggers = new ArrayList<>();
    private static TriggerCycle triggerCycle;

    public static void init() {
        if (initialised) return;
        triggerCycle = new TriggerCycle();
        try {
            EasyJSON triggers = EasyJSON.open("triggers.txt");
            for (EasyJSON.JSONElement triggerData : triggers.search("triggers").children) {
                Class<?> triggerClass = Class.forName(triggerData.valueOf("class"));
                Constructor<?> triggerConstructor = triggerClass.getConstructor();
                Trigger triggerInstance = (Trigger) triggerConstructor.newInstance();
                triggerInstance.create(triggerData);
                triggerInstance.active = true;
            }
        } catch (IOException | EasyJSON.ParseException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        initialised = true;
    }


    public static void saveTriggers() {
        boolean resume = triggerCycle.running;
        triggerCycle.running = false;
        EasyJSON json = EasyJSON.create();
        json.putArray("triggers");
        for (Trigger trigger : triggers) {
            json.search("triggers").putGeneric(trigger.exportTrigger());
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


    public ArrayList<Response> responses = new ArrayList<>();
    private boolean active;

    Trigger(Response... responses) {
        triggers.add(this);
        triggerCycle.notifySizeUpdate();
        this.responses.addAll(Arrays.stream(responses).collect(Collectors.toList()));
    }

    private void create(EasyJSON.JSONElement data) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        active = data.valueOf("active");
        for (EasyJSON.JSONElement responseData : data.search("responses").children) {
            Class<?> responseClass = Class.forName(responseData.valueOf("class"));
            Constructor<?> responseConstructor = responseClass.getConstructor();
            Response responseInstance = (Response) responseConstructor.newInstance();
            responseInstance.create(responseData);
            responses.add(responseInstance);
        }
    }

    private EasyJSON exportTrigger() {
        EasyJSON json = EasyJSON.create();
        json.putGeneric("class", this.getClass().getName());
        json.putGeneric("active", active);
        json.putArray("responses");
        for (Response response : responses) {
            json.search("responses").putGeneric(response.exportResponse());
        }
        return json;
    }

    abstract boolean onUpdateCycle();

    abstract ResponseDataType suppliedDataType();

    void deliver(Object data) {
        for (Response response : responses) {
            Object converted = Util.convertDataTypes(data, suppliedDataType(), response.requiredDataType());
            if (converted != null) {
                response.run(response);
            }
        }
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
