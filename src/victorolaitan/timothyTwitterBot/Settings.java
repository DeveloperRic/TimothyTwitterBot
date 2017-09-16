package victorolaitan.timothyTwitterBot;

import victorolaitan.timothyTwitterBot.util.EasyJSON;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Initial commit by Victor Olaitan on 25/03/2017.
 */
public class Settings {
    public static boolean NO_WARNING_WHEN_DELETING_AR;

    public static void init() {
        try {
            EasyJSON json = EasyJSON.open("settings.txt");
            for (EasyJSON.JSONElement setting : json.root.children) {
                try {
                    Field field = Settings.class.getDeclaredField(setting.key);
                    field.set(null, setting.value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException | EasyJSON.ParseException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        EasyJSON json = EasyJSON.create();
        for (Field field : Settings.class.getFields()) {
            try {
                json.putGeneric(field.getName(), field.get(null));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            json.save("settings.txt");
        } catch (EasyJSON.ParseException e) {
            e.printStackTrace();
        }
    }
}
