package victorolaitan.timothyTwitterBot.trigger;

import victorolaitan.timothyTwitterBot.util.JSONUtil;

import java.util.Date;

/**
 * Initial commit by Victor Olaitan on 07/03/2017.
 */
public abstract class Trigger {
    public Date timestamp;

    public Trigger() {
        timestamp = new Date();
    }

    public static void registerTrigger(Trigger trigger) {

    }

    public abstract JSONUtil.EasyJSON exportTrigger();
}
