package victorolaitan.timothyTwitterBot.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

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
            reader = new BufferedReader(new InputStreamReader(Util.class.getResourceAsStream("data/" + name + ".txt")));
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
        ArrayList<String> lines = new ArrayList<String>();
        for (String line : linesAry) {
            lines.add(line);
        }
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

    public static ArrayList<String> bufferFromTextFile(String name) {
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

}
