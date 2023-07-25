import java.util.Arrays;
import java.util.List;

import org.apache.spark.ml.feature.OneHotEncoder;
import org.apache.spark.ml.feature.OneHotEncoderModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

public class TestFeatureSelection {

    public static void main(String[] args) {
        String sparkMaster = "spark://192.168.1.4:7077";
        // Create SparkSession
        SparkSession spark = SparkSession.builder()
                .appName("StocksPredictionAppJAVA")
                .master(sparkMaster)
                .config("spark.driver.memory", "2g")
                .config("spark.driver.cores", "4")
                .config("spark.executor.memory", "2g")
                .config("spark.executor.cores", "4")
                .config("spark.cores.max", "12")
                .getOrCreate();

        List<Row> data = Arrays.asList(
                RowFactory.create(2.0, 1.0),
                RowFactory.create(0.0, 1.0),
                RowFactory.create(1.0, 0.0),
                RowFactory.create(0.0, 2.0),
                RowFactory.create(0.0, 1.0),
                RowFactory.create(2.0, 0.0),
                RowFactory.create(1.0, 2.0)
        );

        StructType schema = new StructType(new StructField[]{
                new StructField("categoryIndex1", DataTypes.DoubleType, false, Metadata.empty()),
                new StructField("categoryIndex2", DataTypes.DoubleType, false, Metadata.empty())
        });

        Dataset<Row> df = spark.createDataFrame(data, schema);

        OneHotEncoder encoder = new OneHotEncoder()
                .setInputCols(new String[] {"categoryIndex1", "categoryIndex2"})
                .setOutputCols(new String[] {"categoryVec1", "categoryVec2"});

        OneHotEncoderModel model = encoder.fit(df);
        Dataset<Row> encoded = model.transform(df);
        encoded.show();

        List<Row> data2 = Arrays.asList(
                RowFactory.create(2.0, 1.0),
                RowFactory.create(1.0, 2.0)
        );

        StructType schema2 = new StructType(new StructField[]{
                new StructField("categoryIndex1", DataTypes.DoubleType, false, Metadata.empty()),
                new StructField("categoryIndex2", DataTypes.DoubleType, false, Metadata.empty())
        });

        Dataset<Row> df2 = spark.createDataFrame(data2, schema2);

        OneHotEncoder encoder2 = new OneHotEncoder()
                .setInputCols(new String[] {"categoryIndex1", "categoryIndex2"})
                .setOutputCols(new String[] {"categoryVec1", "categoryVec2"});

        OneHotEncoderModel model2 = encoder2.fit(df2);
        Dataset<Row> encoded2 = model2.transform(df2);
        encoded2.show();
    }
}
