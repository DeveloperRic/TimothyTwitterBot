package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

/**
 * Initial commit by Victor Olaitan on 18/03/2017.
 */
public class UnFollowResponse implements Response {

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
        json.putGeneric("class", UnFollowResponse.class.getName());
        return json;
    }

    @Override
    public void run(Object data) {
        Main.twitter.users().stopFollowing(Main.twitter.users().getUser((long) data));
    }
}
