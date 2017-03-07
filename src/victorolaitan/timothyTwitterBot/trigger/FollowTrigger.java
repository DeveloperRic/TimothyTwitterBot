package victorolaitan.timothyTwitterBot.trigger;

import victorolaitan.timothyTwitterBot.util.JSONUtil;
import winterwell.jtwitter.User;

/**
 * Initial commit by Victor Olaitan on 07/03/2017.
 */
public class FollowTrigger extends Trigger {
    public boolean active;

    public FollowTrigger() {
        super();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public JSONUtil.EasyJSON exportTrigger() {
        JSONUtil.EasyJSON json = JSONUtil.EasyJSON.create();
        json.put("class", "FollowTrigger");
        json.put("active", active);
        return json;
    }
}
