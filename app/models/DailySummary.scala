package models

import play.api.libs.json.{Json, Writes}

case class DailySummary(cases: Int, deaths: Int, changeInCases: Double, changeInDeaths: Double)

object DailySummary {
  implicit val dailySummaryWrites: Writes[DailySummary] = (dailySummary: DailySummary) => Json.obj(
    "cases"  -> dailySummary.cases,
    "deaths" -> dailySummary.deaths,
    "changeInCases" -> dailySummary.changeInCases,
    "changeInDeaths" -> dailySummary.changeInDeaths
  )
}
