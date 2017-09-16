package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.trigger.Trigger;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

import java.math.BigInteger;

/**
 * Initial commit by Victor Olaitan on 26/03/2017.
 */
public class RetweetTweetResponse implements Response {
    Trigger trigger;

    public RetweetTweetResponse(Trigger trigger) {
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
    }

    @Override
    public EasyJSON exportResponse() {
        EasyJSON json = EasyJSON.create();
        json.putGeneric("class", this.getClass().getName());
        return json;
    }

    @Override
    public void run(Object data) {
        Main.twitter.retweet(Main.twitter.getStatus((BigInteger) data));
    }

    @Override
    public Object getSavedData() {
        return null;
    }

    @Override
    public void updateSavedData(Object newData) {
    }
}
