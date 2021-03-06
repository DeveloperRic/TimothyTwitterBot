package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.trigger.Trigger;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

/**
 * Initial commit by Victor Olaitan on 18/03/2017.
 */
public class FollowResponse implements Response {
    private Trigger trigger;

    public FollowResponse(Trigger trigger) {
        this.trigger = trigger;
    }

    @Override
    public Trigger getTrigger() {
        return trigger;
    }

    @Override
    public ResponseDataType requiredDataType() {
        return ResponseDataType.USER_ID;
    }

    @Override
    public void create(EasyJSON.JSONElement data) {
    }

    @Override
    public EasyJSON exportResponse() {
        EasyJSON json = EasyJSON.create();
        json.putGeneric("class", FollowResponse.class.getName());
        return json;
    }

    @Override
    public void run(Object data) {
        Main.twitter.users().follow(Main.twitter.users().getUser((long) data));
    }

    @Override
    public Object getSavedData() {
        return null;
    }

    @Override
    public void updateSavedData(Object newData) {
    }
}
