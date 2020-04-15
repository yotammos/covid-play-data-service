package models

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.libs.json.{Json, Writes}

case class CovidData(numCases: Int, numDeaths: Int, country: String, date: LocalDate, stateOrProvince: Option[String] = None)

object CovidData {
  implicit val covidDataWrites: Writes[CovidData] = (covidData: CovidData) => Json.obj(
    "cases"  -> covidData.numCases,
    "deaths" -> covidData.numDeaths,
    "country" -> covidData.country,
    "date" -> covidData.date.format(DateTimeFormatter ofPattern "MM/dd/yyyy")
  )
}
