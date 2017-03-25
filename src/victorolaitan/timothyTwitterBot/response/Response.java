package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.trigger.Trigger;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

/**
 * Initial commit by Victor Olaitan on 16/03/2017.
 */
public interface Response {

    Trigger getTrigger();

    ResponseDataType requiredDataType();

    void create(EasyJSON.JSONElement data);

    EasyJSON exportResponse();

    void run(Object data);

    Object getSavedData();

    void updateSavedData(Object newData);

}
