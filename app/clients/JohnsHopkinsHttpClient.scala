package clients

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import javax.inject.Inject
import models.CovidData
import play.api.Logging
import play.api.http.Status
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class JohnsHopkinsHttpClient @Inject() (ws: WSClient, implicit val context: ExecutionContext) extends Logging{

  import JohnsHopkinsHttpClient._

  private final val BASE_URL: String = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports"
  private final val URL_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy")

  def getLatestData: Future[Array[CovidData]] = getDataByDate(buildLatestDate)

  def getDataByDate(date: LocalDate): Future[Array[CovidData]] = {
    ws
      .url(urlBuilder(date))
      .get
      .map { response =>
        if (response.status == Status.OK) {
          parseCsv(
            response
              .body
              .split("\n")
          )
        } else {
          throw new Exception(s"failed getting covid data, status = ${response.status}, error = ${response.body}")
        }
      }
  }

  private def parseCsv(rows: Array[String]): Array[CovidData] = {
    rows.headOption match {
      case Some(headRow) =>
        val colHeaders = headRow.split(",")
        val indexMap: Map[String, Int] = CSV_RELEVANT_COLS.map(colName => colName -> colHeaders.indexOf(colName)).toMap
        rows.tail.map(row => parseDataRow(row, indexMap))
      case None => throw new Exception("no rows found in body")
    }
  }

  def parseDataRow(row: String, columnMap: Map[String, Int]): CovidData = {
    val handledRow = handleRowInitialCommas(row)
    val processedRow: Array[String] =
      if (handledRow.contains("\"")) handleComplexRow(handledRow)
      else handledRow split ","

    CovidData(
      processedRow(columnMap(CASES_COL)).toInt,
      processedRow(columnMap(DEATHS_COL)).toInt,
      processedRow(columnMap(COUNTRY_COL)),
      LocalDateTime.parse(
        processedRow(columnMap(LAST_UPDATE_COL)),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      ).toLocalDate,
      Option(processedRow(columnMap(STATE_PROVINCE_COL)))
        .flatMap(s => if (s.isEmpty) None else Option(s))
    )
  }

  private def handleComplexRow(row: String): Array[String] = {
    for {
      first <- row.indices.find(i => row(i) == '\"')
      second <- row.indices.find(i => i != first && row(i) == '\"')
    } yield {
        (if (first == 0) Array.empty[String] else row.substring(0, first) split ",") ++
        Array(row.substring(first + 1, second)) ++
        handleComplexRow(
          Try(row.substring(second + 2))
            .getOrElse("")
        )
    }
  }.getOrElse(row split ",")

  def buildLatestDate: LocalDate =
    if (LocalDateTime.now isAfter LocalDate.now.atStartOfDay.plusHours(20)) LocalDate.now
    else LocalDate.now minusDays 1

  private def urlBuilder(date: LocalDate) = s"$BASE_URL/${date.atStartOfDay().format(URL_DATE_FORMATTER)}.csv"

  private def handleRowInitialCommas(s: String): String = {
    val firstNonComma = s.indexWhere(_ != ',')
    " ,".repeat(firstNonComma) + s.substring(firstNonComma)
  }

}

object JohnsHopkinsHttpClient {
  final val STATE_PROVINCE_COL = "Province_State"
  final val COUNTRY_COL = "Country_Region"
  final val LAST_UPDATE_COL = "Last_Update"
  final val CASES_COL = "Confirmed"
  final val DEATHS_COL = "Deaths"
  final val CSV_RELEVANT_COLS = Array(
    CASES_COL,
    DEATHS_COL,
    COUNTRY_COL,
    LAST_UPDATE_COL,
    STATE_PROVINCE_COL
  )
}
