package victorolaitan.timothyTwitterBot.trigger;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.response.ResponseDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Initial commit by Victor Olaitan on 07/03/2017.
 */
public class FollowTrigger extends Trigger {
    private List<Long> followers = new ArrayList<>();

    public FollowTrigger() {
        super();
        followers.addAll(Main.twitter.users().getFollowerIDs().stream().map(Number::longValue).collect(Collectors.toList()));
    }

    @Override
    boolean onUpdateCycle() {
        if (Main.twitter.getSelf().getFollowersCount() > followers.size()) {
            List<Long> newList = Main.twitter.users().getFollowerIDs().stream().map(Number::longValue).collect(Collectors.toList());
            newList.stream().filter(id -> !followers.contains(id)).forEach(this::deliver);
            UnFollowTrigger unFollowTrigger = Trigger.locateTrigger(UnFollowTrigger.class);
            if (unFollowTrigger != null) {
                followers.stream().filter(id -> !newList.contains(id)).forEach(unFollowTrigger::deliver);
            }
            followers.clear();
            followers.addAll(newList);
            return true;
        }
        return false;
    }

    @Override
    public ResponseDataType suppliedDataType() {
        return ResponseDataType.USER_ID;
    }

}
