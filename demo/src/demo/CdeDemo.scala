package demo

import org.chipsalliance.cde.config._

case object XLen extends Field[Int](32)
case object NRegs extends Field[Int](32)
case object DataBits extends Field[Int]

class BaseConfig extends Config((site, here, up) => {
  case XLen => 32
  case NRegs => 32
  case DataBits => site(XLen)
})

class WithRV64 extends Config((site, here, up) => {
  case XLen => 64
})

class WithDoubleRegs extends Config((site, here, up) => {
  case NRegs => up(NRegs) * 2
})

object Param {
    val p: Parameters =
    new WithDoubleRegs ++
    new WithRV64 ++
    new BaseConfig
}

object CdeExample extends App {
  println(s"XLen     = ${Param.p(XLen)}")
  println(s"NRegs    = ${Param.p(NRegs)}")
  println(s"DataBits = ${Param.p(DataBits)}")
}
