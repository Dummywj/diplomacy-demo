package demo

import org.chipsalliance.diplomacy.lazymodule._
import org.chipsalliance.cde.config.Parameters
import circt.stage.ChiselStage
import chisel3._
import org.chipsalliance.diplomacy.nodes._
import chisel3.experimental.SourceInfo
import chisel3.util.Cat

class ConcatModule()(implicit p: Parameters) extends LazyModule {
  val node = new NexusNode(ConcatNodeImp)(
    { widths => widths.sum },
    { _ => }
  )
  lazy val module = new ConcatModuleImp(this)
}

class ConcatModuleImp(outer: ConcatModule) extends LazyModuleImp(outer) {
  // compute concatenation of all inward edges
  val cat = Wire(UInt(outer.node.in.map(_._2).sum.W))
  cat := Cat(outer.node.in.map(_._1))

  // copy concatenation to all outward edges
  outer.node.out.foreach({ case (out, _) =>
    out := cat
  })
}

class ConcatTopModule()(implicit p: Parameters) extends LazyModule {
  val inputNodes1 = new SourceNode(ConcatNodeImp)(Seq(1, 2, 3, 4, 5))
  val inputNodes2 = new SourceNode(ConcatNodeImp)(Seq(6, 7))
  val outputNodes = new SinkNode(ConcatNodeImp)(Seq.fill(3)(()))
  val concat1 = LazyModule(new ConcatModule)
  val concat2 = LazyModule(new ConcatModule)

  concat1.node :=* inputNodes1
  concat2.node := concat1.node
  concat2.node :=* inputNodes2
  outputNodes :*= concat2.node

  lazy val module = new LazyModuleImp(this) {
    // connect input to IO
    inputNodes1.out.zipWithIndex.foreach({ case ((wire, width), i) =>
      val in = IO(Input(UInt(width.W))).suggestName(s"in1_${i}")
      wire := in
    })
    inputNodes2.out.zipWithIndex.foreach({ case ((wire, width), i) =>
      val in = IO(Input(UInt(width.W))).suggestName(s"in2_${i}")
      wire := in
    })

    // connect output to IO
    outputNodes.in.zipWithIndex.foreach({ case ((wire, width), i) =>
      val out = IO(Output(UInt(width.W))).suggestName(s"out_${i}")
      out := wire
    })
  }
}

object ConcatNodeImp extends SimpleNodeImp[Int, Unit, Int, UInt] {
  override def edge(
      pd: Int,
      pu: Unit,
      p: Parameters,
      sourceInfo: SourceInfo
  ): Int = pd
  override def bundle(ei: Int): UInt = UInt(ei.W)
  override def render(e: Int): RenderedEdge =
    RenderedEdge(colour = "#000000" /* black */, label = s"${e}")
}

object Concat extends App {
  val top = LazyModule(new ConcatTopModule()(Parameters.empty))
  println(
    ChiselStage.emitSystemVerilog(
      top.module,
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
  )
  os.write.over(os.pwd / "dump.graphml", top.graphML)
}
