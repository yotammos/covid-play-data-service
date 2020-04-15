package clients

import java.time.LocalDate

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.ws.WSClient
import JohnsHopkinsHttpClient._
import models.CovidData

import scala.concurrent.ExecutionContext

class JohnsHopkinsHttpClientSpec extends PlaySpec with MockitoSugar {

  private final val mockExecutionContext: ExecutionContext = mock[ExecutionContext]
  private final val mockWsClient: WSClient = mock[WSClient]

  private final val client = new JohnsHopkinsHttpClient(mockWsClient, mockExecutionContext)

  "Johns Hopkins Http Client" should {
    "parse row with ending quote correctly" in {
      val columnMap = Map(
        CASES_COL -> 7,
        DEATHS_COL -> 8,
        COUNTRY_COL -> 3,
        LAST_UPDATE_COL -> 4,
        STATE_PROVINCE_COL -> 2
      )
      val rowWithEndingQuote = "45001,Abbeville,South Carolina,US,2020-04-11 22:45:33,34.22333378,-82.46170658,9,0,0,0,\"Abbeville, South Carolina, US\""

      client.parseDataRow(rowWithEndingQuote, columnMap) mustBe CovidData(
        numCases = 9,
        numDeaths = 0,
        country = "US",
        date = LocalDate.of(2020, 4, 11),
        Some("South Carolina")
      )
    }

    "parse row with initial commas" in {
      val columnMap = Map(
        CASES_COL -> 7,
        DEATHS_COL -> 8,
        COUNTRY_COL -> 3,
        LAST_UPDATE_COL -> 4,
        STATE_PROVINCE_COL -> 2
      )
      val rowWithInitialCommas = ",,\"Bonaire, Sint Eustatius and Saba\",Netherlands,2020-04-11 22:45:13,12.1784,-68.2385,2,0,0,2,\"Bonaire, Sint Eustatius and Saba, Netherlands\""

      client.parseDataRow(rowWithInitialCommas, columnMap) mustBe CovidData(
        numCases = 2,
        numDeaths = 0,
        country = "Netherlands",
        date = LocalDate.of(2020, 4, 11),
        Some("Bonaire, Sint Eustatius and Saba")
      )
    }

    "parse row with initial quotes" in {
      val columnMap = Map(
        CASES_COL -> 5,
        DEATHS_COL -> 6,
        COUNTRY_COL -> 1,
        LAST_UPDATE_COL -> 2,
        STATE_PROVINCE_COL -> 0
      )
      val rowWithInitialQuote = "\"Bonaire, Sint Eustatius and Saba\",Netherlands,2020-04-12 23:17:56,12.1784,-68.2385,3,0,0,3,,,\"Bonaire, Sint Eustatius and Saba, Netherlands\",11.441211242896914,"

      client.parseDataRow(rowWithInitialQuote, columnMap) mustBe CovidData(
        numCases = 3,
        numDeaths = 0,
        country = "Netherlands",
        date = LocalDate.of(2020, 4, 12),
        Some("Bonaire, Sint Eustatius and Saba")
      )
    }
  }
}
