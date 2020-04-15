package models

abstract class Country(val names: Array[String]) {
  def this(name: String) = this(Array(name))
  val name: String = names.head
  override def toString: String = name
}
case object USA extends Country("US")
case object Italy extends Country("Italy")
case object Spain extends Country("Spain")
case object France extends Country("France")
case object Germany extends Country("Germany")
case object UnitedKingdom extends Country("United Kingdom")
case object China extends Country("China")
case object Iran extends Country("Iran")
case object Turkey extends Country("Turkey")
case object Belgium extends Country("Belgium")
case object Netherlands extends Country("Netherlands")
case object Canada extends Country("Canada")
case object Switzerland extends Country("Switzerland")
case object Brazil extends Country("Brazil")
case object Russia extends Country("Russia")
case object Portugal extends Country("Portugal")
case object Austria extends Country("Austria")
case object Israel extends Country("Israel")
case object India extends Country("India")
case object Ireland extends Country("Ireland")
case object Unknown extends Country("Unknown")

object Country {
  val availableCountries: Array[Country] = Array(
    USA,
    Italy,
    Spain,
    France,
    Germany,
    UnitedKingdom,
    China,
    Iran,
    Turkey,
    Belgium,
    Netherlands,
    Canada,
    Switzerland,
    Brazil,
    Russia,
    Portugal,
    Austria,
    Israel,
    India,
    Ireland
  )

  def nameToCountry(name: String): Country =
    availableCountries.find(_.names.contains(name)) getOrElse Unknown
}
