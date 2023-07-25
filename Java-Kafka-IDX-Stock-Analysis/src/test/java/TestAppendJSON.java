import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;


public class TestAppendJSON {

    public static void main(String[] args) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "John Doe");
        jsonObject.put("age", 30);

        JSONArray jsonArray = new JSONArray();
        jsonArray.put("Java");
        jsonArray.put("Python");
        jsonArray.put("C++");

        jsonObject.put("languages", jsonArray);

        String filePath = "data.json";

        try (FileWriter fileWriter = new FileWriter(filePath, true)) {
            fileWriter.write(jsonObject.toString());
            fileWriter.write(System.lineSeparator());
            System.out.println("JSON data appended to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
