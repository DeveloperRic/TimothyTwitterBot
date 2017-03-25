package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.trigger.Trigger;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

import java.math.BigInteger;

/**
 * Initial commit by Victor Olaitan on 21/03/2017.
 */
public class ReplyMessageResponse implements Response {
    public String message;
    private Trigger trigger;

    public ReplyMessageResponse(Trigger trigger) {
        this.trigger = trigger;
    }

    @Override
    public Trigger getTrigger() {
        return trigger;
    }

    @Override
    public ResponseDataType requiredDataType() {
        return ResponseDataType.MESSAGE_ID;
    }

    @Override
    public void create(EasyJSON.JSONElement data) {
        message = data.valueOf("message");
    }

    @Override
    public EasyJSON exportResponse() {
        EasyJSON json = EasyJSON.create();
        json.putGeneric("class", ReplyMessageResponse.class.getName());
        json.putGeneric("message", message);
        return json;
    }

    @Override
    public void run(Object data) {
        Main.twitter.sendMessage(Main.twitter.getDirectMessage((BigInteger) data).getSender().getScreenName(),message);
    }

    @Override
    public Object getSavedData() {
        return message;
    }

    @Override
    public void updateSavedData(Object newData) {
        message = newData.toString();
    }
}
