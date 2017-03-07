package victorolaitan.timothyTwitterBot.trigger;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.util.JSONUtil;
import winterwell.jtwitter.User;

import java.util.ArrayList;
import java.util.List;


/**
 * Initial commit by Victor Olaitan on 07/03/2017.
 */
public class FollowTrigger extends Trigger {
    public boolean active;
    public List<User> followers = new ArrayList<>();
    public int followesCount;

    FollowTrigger() {
        super();
        followers.addAll(Main.twitter.getFollowers());
        followesCount = followers.size();

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

    @Override
    public void onUpdateCycle() {
        if (Main.twitter.getSelf().getFollowersCount() > followesCount) {
            List<User> newList = Main.twitter.getFollowers();
            for (User follower : newList) {
                boolean found = false;
                long id = follower.getId();
                for (User user : followers) {
                    if (user.getId() == id) {
                        found = true;
                    }
                }
                if (!found) {
                    deliver(follower);
                }
            }
        }
    }

}
