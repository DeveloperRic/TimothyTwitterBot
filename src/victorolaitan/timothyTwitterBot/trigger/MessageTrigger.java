package victorolaitan.timothyTwitterBot.trigger;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.response.ResponseDataType;
import winterwell.jtwitter.Message;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Initial commit by Victor Olaitan on 19/03/2017.
 */
public class MessageTrigger extends Trigger {
    private List<BigInteger> messages = new ArrayList<>();

    public MessageTrigger() {
        messages.addAll(Main.twitter.getDirectMessages().stream().map(Message::getId).collect(Collectors.toList()));
    }

    @Override
    boolean onUpdateCycle() {
        List<Message> newList = Main.twitter.getDirectMessages();
        newList.stream().filter(message -> !messages.contains(message.getId())).forEach(message -> deliver(message.id));
        messages.clear();
        messages.addAll(newList.stream().map(Message::getId).collect(Collectors.toList()));
        return true;
    }

    @Override
    public ResponseDataType suppliedDataType() {
        return ResponseDataType.MESSAGE_ID;
    }
}
