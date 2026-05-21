package demo

import org.chipsalliance.diplomacy.lazymodule.LazyModule
import org.chipsalliance.diplomacy.lazymodule.LazyModuleImp
import org.chipsalliance.cde.config.Parameters
import circt.stage.ChiselStage
import chisel3._

class AdderDiplomacyModule()(implicit p: Parameters) extends LazyModule {
  lazy val module = new AdderDiplomacyModuleImp(this)
}

class AdderDiplomacyModuleImp(outer: AdderDiplomacyModule)
    extends LazyModuleImp(outer) {
  val in1 = IO(Input(UInt(32.W)))
  val in2 = IO(Input(UInt(32.W)))
  val out = IO(Output(UInt(32.W)))
  out := in1 + in2
}

object AdderNaive extends App {
  println(
    ChiselStage.emitSystemVerilog(
      LazyModule(new AdderDiplomacyModule()(Parameters.empty)).module,
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
  )
}
