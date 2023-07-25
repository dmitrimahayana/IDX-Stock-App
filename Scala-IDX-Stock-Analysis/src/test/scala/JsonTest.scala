import org.json4s._
import org.json4s.native.JsonMethods._

object JsonTest {
  def main(args: Array[String]): Unit = {
    implicit val formats = DefaultFormats

    val dfArray = Array(
      "{\"date\":\"2022-01-11\",\"ticker\":\"ARTO\",\"open\":18950.0,\"volume\":26293400,\"prediction\":18793.733454686575}",
      "{\"date\":\"2022-01-10\",\"ticker\":\"ARTO\",\"open\":18800.0,\"volume\":43033600,\"prediction\":18640.81799435595,}")

    case class stock(date: String, ticker: String, open: Double, volume: Long, prediction: Double)
    var listStock = List[stock]()
    for (row <- dfArray) {
      val result = parse(row).extract[stock]
      println(result)
      listStock = listStock :+ result
    }
    println(listStock)
  }
}