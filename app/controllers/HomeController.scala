package controllers

import java.time.format.DateTimeFormatter

import javax.inject._
import models.DailySummary._
import models.{Country, DatedFormattedCovidData, FormattedCovidData, FormattedLatestCovidData, LatestCovidData}
import play.api.libs.json.Json
import play.api.mvc._
import services.CovidDataService

import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(override val controllerComponents: ControllerComponents, covidDataService: CovidDataService, implicit val context: ExecutionContext) extends AbstractController(controllerComponents) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def dailySummary: Action[AnyContent] = Action.async(
    covidDataService
      .fetchDailySummary
      .map(dailySummary => Ok(Json.toJson(dailySummary)))
  )

  def latest: Action[AnyContent] =
    Action.async(
      covidDataService.fetchDailyData.map { data =>
        val formatter = java.text.NumberFormat.getIntegerInstance

        Ok(views.html.latest(
          DatedFormattedCovidData(
            date = data.head.date.format(DateTimeFormatter ofPattern "MM/dd/yyyy"),
            data.map(covidData => FormattedCovidData(
              formatter.format(covidData.numCases),
              formatter.format(covidData.numDeaths),
              covidData.country
            ))
          )
        ))
      }
    )

  def latestWithDifference: Action[AnyContent] =
    Action.async(
      covidDataService.fetchLatestCovidData.map(
        data => buildLatestWithDifference(data, "Country")
      )
    )

  def latestWithDifferenceByCountry(countryName: String): Action[AnyContent] =
    Action.async(
      covidDataService.fetchCountryCovidData(Country nameToCountry countryName).map(
        data => buildLatestWithDifference(data, "State/Province")
      )
    )

  private def buildLatestWithDifference(data: Array[LatestCovidData], region: String): Result = {
    val formatter = java.text.NumberFormat.getIntegerInstance

    Ok(views.html.latestWithDifference(data
      .map(x => FormattedLatestCovidData(
        formatter format x.cases,
        formatter format x.deaths,
        formatter format x.dailyCases,
        formatter format x.dailyDeaths,
        x.region
      )),
      region = region
    ))
  }
}
