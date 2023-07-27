import org.apache.spark.sql.catalyst.expressions.Round;

import java.text.DecimalFormat;
import java.util.Random;

public class TestRandomNumber {
    public static void main(String[] args) {

        Double rangeMin = 0.0;
        Double rangeMax = 1000.0;


        Random r = new Random();
        DecimalFormat f = new DecimalFormat("##.00");
        for (int i = 0; i < 10; i++) {
            double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
            System.out.println(f.format(randomValue));
        }
    }
}
