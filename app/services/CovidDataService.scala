package services

import clients.JohnsHopkinsHttpClient
import javax.inject.Inject
import models.{Country, CovidData, DailySummary, LatestCovidData}
import play.api.Logging

import scala.concurrent.{ExecutionContext, Future}

class CovidDataService @Inject()(johnsHopkinsHttpClient: JohnsHopkinsHttpClient, implicit val executionContext: ExecutionContext) extends Logging {

  def fetchDailyData: Future[Array[CovidData]] = fetchJohnsHopkinsDailyData

  def fetchDailySummary: Future[DailySummary] = fetchJohnsHopkinsDailySummary

  def fetchLatestCovidData: Future[Array[LatestCovidData]] = fetchLatestJohnsHopkinsData

  def fetchCountryCovidData(country: Country): Future[Array[LatestCovidData]] = {
    logger.info(s"getting country data for $country")
    fetchJohnsHopkinsCountryData(country)
  }

  private def fetchJohnsHopkinsCountryData(country: Country): Future[Array[LatestCovidData]] =
    fetchLatestCoupleOfDaysData
        .map { x =>
          (
            sumByState(x._1.filter(data => country.names.contains(data.country))),
            sumByState(x._2.filter(data => country.names.contains(data.country)))
          )
        }.map { x =>
        val latestStates: Set[String] = x._1.flatMap(_.stateOrProvince).toSet
        val previousStates: Set[String] = x._2.flatMap(_.stateOrProvince).toSet
        val relevantStates = (latestStates intersect previousStates).toArray

        for {
          stateOrProvince <- relevantStates
          latestData <- x._1.find(_.stateOrProvince contains stateOrProvince)
          previousData <- x._2.find(_.stateOrProvince contains stateOrProvince)
        } yield LatestCovidData(
          latestData.numCases,
          latestData.numDeaths,
          latestData.numCases - previousData.numCases,
          latestData.numDeaths - previousData.numDeaths,
          stateOrProvince
        )
      }.map(_.sortBy(_.cases).reverse)

  private def fetchLatestJohnsHopkinsData: Future[Array[LatestCovidData]] =
    fetchLatestCoupleOfDaysData map { x =>
      val latestDataByCountry = sumByCountry(x._1)
      val previousDataByCountry = sumByCountry(x._2)

        val allCountryData = latestDataByCountry map { latestCountryData =>
          val previousCountryData = previousDataByCountry.find(_.country == latestCountryData.country) getOrElse latestCountryData

          LatestCovidData(
            latestCountryData.numCases,
            latestCountryData.numDeaths,
            latestCountryData.numCases - previousCountryData.numCases,
           latestCountryData.numDeaths - previousCountryData.numDeaths,
            latestCountryData.country
          )
        }

      (
        allCountryData.foldLeft(LatestCovidData(0, 0, 0, 0, "World"))((acc, data) =>
          LatestCovidData(acc.cases + data.cases, acc.deaths + data.deaths, acc.dailyCases + data.dailyCases, acc.dailyDeaths + data.dailyDeaths, acc.region)
        ) +: allCountryData
        ).map(data => LatestCovidData(data.cases, data.deaths, data.dailyCases, data.dailyDeaths, data.region))
    }

  private def fetchJohnsHopkinsDailyData: Future[Array[CovidData]] =
    johnsHopkinsHttpClient.getLatestData
      .map(latestData =>
        latestData
          .foldLeft(CovidData(0, 0, "World", johnsHopkinsHttpClient.buildLatestDate))((acc, countryData) =>
            CovidData(acc.numCases + countryData.numCases, acc.numDeaths + countryData.numDeaths, acc.country, acc.date)
          ) +: sumByCountry(latestData)
      )

  private def sumByState(data: Array[CovidData]): Array[CovidData] =
    data
      .groupBy(_.stateOrProvince)
      .values
      .map(stateData =>
        stateData
          .foldLeft(
            CovidData(0, 0, stateData.head.country, johnsHopkinsHttpClient.buildLatestDate, stateData.head.stateOrProvince)
          )((acc, stateData) =>
            CovidData(acc.numCases + stateData.numCases, acc.numDeaths + stateData.numDeaths, acc.country, acc.date, acc.stateOrProvince)
          )
      )
      .toArray
      .sortBy(_.numCases)
      .reverse

  private def sumByCountry(data: Array[CovidData]): Array[CovidData] =
    data
      .groupBy(_.country)
      .values
      .map(countryData =>
        countryData
          .foldLeft(
            CovidData(0, 0, countryData.head.country, johnsHopkinsHttpClient.buildLatestDate)
          )((acc, countryData) =>
            CovidData(acc.numCases + countryData.numCases, acc.numDeaths + countryData.numDeaths, acc.country, acc.date)
          )
      )
      .toArray
      .sortBy(_.numCases)
      .reverse

  private def fetchLatestCoupleOfDaysData: Future[(Array[CovidData], Array[CovidData])] =
    for {
      latestData <- johnsHopkinsHttpClient.getLatestData
      date = latestData.headOption match {
        case Some(dataRow) => dataRow.date
        case _ => throw new Exception("failed getting latest data")
      }
      previousDayData <- johnsHopkinsHttpClient.getDataByDate(date minusDays 1)
    } yield (latestData, previousDayData)

  private def fetchJohnsHopkinsDailySummary: Future[DailySummary] =
    fetchLatestCoupleOfDaysData map { x =>
      val latestCasesAndDeaths = totalCasesAndDeaths(x._1)
      val previousDayCasesAndDeaths = totalCasesAndDeaths(x._2)

      DailySummary(
        cases = latestCasesAndDeaths._1,
        deaths = latestCasesAndDeaths._2,
        changeInCases = latestCasesAndDeaths._1.toDouble - previousDayCasesAndDeaths._1.toDouble,
        changeInDeaths = latestCasesAndDeaths._2.toDouble - previousDayCasesAndDeaths._2.toDouble
      )
    }

  private def totalCasesAndDeaths(data: Array[CovidData]): (Int, Int) =
    data.foldLeft((0, 0))((acc, covidData) => (acc._1 + covidData.numCases, acc._2 + covidData.numDeaths))
}
