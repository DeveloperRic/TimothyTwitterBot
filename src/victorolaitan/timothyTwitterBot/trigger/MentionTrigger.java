package victorolaitan.timothyTwitterBot.trigger;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.response.ResponseDataType;
import winterwell.jtwitter.Status;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Initial commit by Victor Olaitan on 18/03/2017.
 */
public class MentionTrigger extends Trigger {
    private List<BigInteger> mentions = new ArrayList<>();

    public MentionTrigger() {
        super();
        mentions.addAll(Main.twitter.getMentions().stream().map(status -> status.id).collect(Collectors.toList()));
    }

    @Override
    boolean onUpdateCycle() {
        List<Status> newList = Main.twitter.getMentions();
        newList.stream().filter(status -> !mentions.contains(status.id)).forEach(status -> deliver(status.id));
        mentions.clear();
        mentions.addAll(newList.stream().map(status -> status.id).collect(Collectors.toList()));
        return true;
    }

    @Override
    ResponseDataType suppliedDataType() {
        return ResponseDataType.STATUS_ID;
    }
}
