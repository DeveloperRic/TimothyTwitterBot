package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.trigger.Trigger;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

import java.math.BigInteger;

/**
 * Initial commit by Victor Olaitan on 19/03/2017.
 */
public class ReplyTweetResponse implements Response {
    String message;
    Trigger trigger;

    public ReplyTweetResponse(Trigger trigger) {
        this.trigger = trigger;
    }

    @Override
    public Trigger getTrigger() {
        return trigger;
    }

    @Override
    public ResponseDataType requiredDataType() {
        return ResponseDataType.STATUS_ID;
    }

    @Override
    public void create(EasyJSON.JSONElement data) {
        message = data.valueOf("message");
    }

    @Override
    public EasyJSON exportResponse() {
        EasyJSON json = EasyJSON.create();
        json.putGeneric("class", ReplyTweetResponse.class.getName());
        json.putGeneric("message", message);
        return json;
    }

    @Override
    public void run(Object data) {
        Main.twitter.updateStatus(message, (BigInteger) data);
    }

    @Override
    public Object getSavedData() {
        return  message;
    }

    @Override
    public void updateSavedData(Object newData) {
        message = newData.toString();
    }
}
