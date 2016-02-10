import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.broadcast.Broadcast

import scala.util.matching.Regex

/**
  * Created by JamieLongmuir
  */
object MainHDFS extends App {

    val scConf = new SparkConf(true).setAppName("experiment")
      .set("spark.eventLog.enabled", "true")

  implicit val sc = new SparkContext( scConf)

    val hdfsPrefix: String = "hdfs://10.0.163.3:9000/user/sparkexp"

    val argz: Map[String, String] = Map("input-path" -> s"$hdfsPrefix/data/crawl",
      "output-path" -> s"$hdfsPrefix/output/inverted-index-sorted-stop-words-removed"
    )

    wordCountHamAndSpam(hdfsPrefix)
    InvertedIndexSortByWordsAndCountsWithStopWordsFiltering(argz)

    //wait
    sc.stop()


  def InvertedIndexSortByWordsAndCountsWithStopWordsFiltering(argz: Map[String, String])(implicit sc : SparkContext)  = {
    val stopWords: Broadcast[Set[String]] = sc.broadcast(StopWords.words)

    try {
      // Load the input "crawl" data, where each line has the format:
      //   (document_id, text)
      // First remove the outer parentheses, split on the first comma,
      // trim whitespace from the name (we'll do it later for the text)
      // and convert the text to lower case.
      // NOTE: The args("input-path") is a directory; Spark finds the correct
      // data files, part-NNNNN.
      val lineRE = """^\s*\(([^,]+),(.*)\)\s*$""".r
      val input = sc.textFile(argz("input-path")) map {
        case lineRE(name, text) => (name.trim, text.toLowerCase)
        case badLine =>
          Console.err.println(s"Unexpected line: $badLine")
          // If any of these were returned, you could filter them out below.
          ("", "")
      }

      val now = Timestamp.now()
      val out = s"${argz("output-path")}-$now"
      //   if (! argz.getOrElse("quiet", "false").toBoolean)
      println(s"Writing output to: $out")

      // New: See filtering below.
      val numbersRE = """^\d+$""".r
      // Split on non-alphanumeric sequences of character as before.
      // Rather than map to "(word, 1)" tuples, we treat the words by values
      // and count the unique occurrences.
      input
        .flatMap {
          case (path, text) =>
            text.trim.split("""[^\w']""") map (word => ((word, path), 1))
        }
        // New: Filter stop words. Also remove pure numbers.
        // Note that the regular expression match is fairly
        // expensive, so maybe it's not worth doing here!
        .filter {
        case ((word, _), _) =>
          stopWords.value.contains(word) == false && numbersRE.findFirstIn(word) == None
      }
        .reduceByKey {
          case (count1, count2) => count1 + count2
        }
        .map {
          case ((word, path), n) => (word, (path, n))
        }
        .groupByKey  // The words are the keys
        .sortByKey(ascending = true)
        .mapValues { iterable =>
          val seq2 = iterable.toVector.sortBy {
            case (path, n) => (-n, path)
          }
          seq2.mkString(", ")
        }
        .saveAsTextFile(out)
    } finally {

    }
  }

  def wordCountHamAndSpam(hdfsPrefix: String)(implicit sc : SparkContext) = {
    val now = Timestamp.now()
    val out = s"$hdfsPrefix/output/wc-local-star-$now"

    val inputHam  = sc.textFile(hdfsPrefix+"/data/enron-spam-ham/ham100")
    val inputSpam = sc.textFile(hdfsPrefix+"/data/enron-spam-ham/spam100")
    val wc = inputHam.union(inputSpam).
      coalesce(20).
      map(_.toLowerCase).
      flatMap(_.split("""\W+""")).
      map((_,1)).
      reduceByKey(_+_)
    wc.saveAsTextFile(out)
  }

}
