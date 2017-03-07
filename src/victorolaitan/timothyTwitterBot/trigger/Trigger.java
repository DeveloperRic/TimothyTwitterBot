package victorolaitan.timothyTwitterBot.trigger;

import victorolaitan.timothyTwitterBot.util.JSONUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Initial commit by Victor Olaitan on 07/03/2017.
 */
public abstract class Trigger {
    private static ArrayList<Trigger> triggers = new ArrayList<>();

    Trigger() {
        triggers.add(this);
    }

    public abstract JSONUtil.EasyJSON exportTrigger();

    public abstract void onUpdateCycle();

    public static void deliver(Object... args) {

    }

    public static void saveTriggers() {
        JSONUtil.EasyJSON json = JSONUtil.EasyJSON.create();
        for (int i = 0; i < triggers.size(); i++) {
            json.put(String.valueOf(i), triggers.get(i).exportTrigger());
        }

    }

    public static Trigger includeTrigger(Class<? extends Trigger> triggerClass, Object... args) throws InstantiationException {
        boolean found = false;
        for (Trigger t : triggers) {
            if (t.getClass().equals(triggerClass)) {
                found = true;
                break;
            }
        }
        if (!found) {
            Class<?>[] classes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                classes[i] = args[i].getClass();
            }
            try {
                Trigger trigger = (Trigger) triggerClass.getConstructors()[0].newInstance();
                triggers.add(trigger);
                return trigger;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
