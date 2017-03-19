package victorolaitan.timothyTwitterBot.trigger;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.response.ResponseDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Initial commit by Victor Olaitan on 15/03/2017.
 */
public class UnFollowTrigger extends Trigger {
    private List<Long> followers = new ArrayList<>();

    public UnFollowTrigger() {
        super();
        followers.addAll(Main.twitter.users().getFollowerIDs().stream().map(Number::longValue).collect(Collectors.toList()));
    }

    @Override
    boolean onUpdateCycle() {
        FollowTrigger followTrigger = Trigger.locateTrigger(FollowTrigger.class);
        if (followTrigger == null) {
            if (Main.twitter.getSelf().getFollowersCount() > followers.size()) {
                List<Long> newList = Main.twitter.users().getFollowerIDs().stream().map(Number::longValue).collect(Collectors.toList());
                followers.stream().filter(id -> !newList.contains(id)).forEach(this::deliver);
                followers.clear();
                followers.addAll(newList);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    ResponseDataType suppliedDataType() {
        return ResponseDataType.USER_ID;
    }
}
