package com.islam9lam.namazwidget
import kotlin.math.*
object Qibla {
 fun bearing(lat:Double,lon:Double):Int {
  val kaLat=Math.toRadians(21.4225); val dLon=Math.toRadians(39.8262-lon); val p=Math.toRadians(lat)
  val y=sin(dLon); val x=cos(p)*tan(kaLat)-sin(p)*cos(dLon)
  return ((Math.toDegrees(atan2(y,x))+360)%360).roundToInt()
 }
}
