package victorolaitan.timothyTwitterBot.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.response.ResponseDataType;

import javax.imageio.ImageIO;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by RictAcius on 05/03/2017.
 */
public class Util {

    public static void init() {
        File folder = new File("TimothyTwitterBotFiles");
        folder.mkdirs();
    }

    public static ArrayList<String> bufferClassTextFile(String name) {
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(Util.class.getClassLoader().getResourceAsStream("victorolaitan/timothyTwitterBot/res/data/" + name + ".txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            lines.clear();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return lines;
    }

    public static void writeToTextFile(String path, boolean append, String... linesAry) {
        ArrayList<String> lines = new ArrayList<>();
        Collections.addAll(lines, linesAry);
        Path file = Paths.get(path + ".txt");
        try {
            if (!append) {
                Files.write(file, lines, Charset.forName("UTF-8"));
            } else {
                Files.write(file, lines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> T switchScene(Stage stage, String res) {
        try {
            FXMLLoader loader = new FXMLLoader(Util.class.getClassLoader().getResource("victorolaitan/timothyTwitterBot/res/scene/" + res + ".fxml"));
            stage.setScene(new Scene(loader.load()));
            return loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<String> bufferTextFile(String name) {
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(name + ".txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            lines.clear();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return lines;
    }

    public static Object convertDataTypes(Object data, ResponseDataType supplied, ResponseDataType required) {
        if (supplied == required) {
            return data;
        }
        if (supplied == ResponseDataType.STATUS_ID && required == ResponseDataType.USER_ID) {
            return Main.twitter.getStatus((BigInteger) data).getUser().getId();
        } else if (supplied == ResponseDataType.MESSAGE_ID && required == ResponseDataType.USER_ID) {
            return Main.twitter.getDirectMessage((BigInteger) data).getSender().getId();
        } else {
            return null;
        }
    }

    public static boolean checkDataTypesCompatible(ResponseDataType supplied, ResponseDataType required) {
        if (supplied == null) {
            return false;
        } else if (required == null) {
            return true;
        } else if (supplied == required) {
            return true;
        } else if (supplied == ResponseDataType.STATUS_ID && required == ResponseDataType.USER_ID) {
            return true;
        } else if (supplied == ResponseDataType.MESSAGE_ID && required == ResponseDataType.USER_ID) {
            return true;
        }
        return false;
    }

    public static Image downloadImage(URL url) throws IOException {
        return SwingFXUtils.toFXImage(ImageIO.read(url), null);
    }

    public static Image loadClassImage(String name) {
        try {
            return SwingFXUtils.toFXImage(ImageIO.read(ClassLoader.getSystemResource("victorolaitan/timothyTwitterBot/res/image/" + name)), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return downloadErrorImage();
    }

    public static Image downloadErrorImage() {
        try {
            return SwingFXUtils.toFXImage(ImageIO.read(ClassLoader.getSystemResource("victorolaitan/timothyTwitterBot/res/image/image-download-error.png")), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
