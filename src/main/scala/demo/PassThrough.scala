package demo

import org.chipsalliance.diplomacy.lazymodule._
import org.chipsalliance.cde.config._
import circt.stage.ChiselStage
import chisel3._
import org.chipsalliance.diplomacy.nodes._
import chisel3.experimental.SourceInfo
import chisel3.util.Cat

class PassThroughModule()(implicit p: Parameters) extends LazyModule {
    val node = new NexusNode(PassThroughNodeImp) (
        {widths => widths.sum},
        { _ => 0}
    )
    lazy val module = new PassThroughModuleImp(this)
}

class PassThroughModuleImp(outer: PassThroughModule) extends LazyModuleImp(outer) {
    val cat = Wire(UInt(outer.node.in.map(_._2).sum.W))
    cat := Cat(outer.node.in.map(_._1))
    outer.node.out.foreach({ case (out, _) =>
        out := cat
    })
}

class PassThroughTopModule()(implicit p: Parameters) extends LazyModule {
    val inputNode = new SourceNode(SourceNodeImp)(Seq(3, 4, 5))
    val outputNode = new SinkNode(SinkNodeImp)(Seq(4, 8, 16))
    val passThroughModule = LazyModule(new PassThroughModule)

    passThroughModule.node :=* inputNode
    outputNode :*= passThroughModule.node

    lazy val module = new LazyModuleImp(this) {
        inputNode.out.zipWithIndex.foreach({ case ((wire, width), i) =>
            val in = IO(Input(UInt(width.W))).suggestName(s"in1_${i}")
            wire := in
        })

        outputNode.in.zipWithIndex.foreach({ case ((wire, width), i) =>
            val out = IO(Output(UInt(width.W))).suggestName(s"out_${i}")
            out := wire
        })
    }
}

object PassThroughNodeImp extends NodeImp[Int, Int, Int, Int, UInt] {
    override def edgeI(
        pd: Int, pu: Int, p: Parameters, sourceInfo: SourceInfo
    ) : Int = pd

    override def edgeO(
        pd: Int, pu: Int, p: Parameters, sourceInfo: SourceInfo
    ) : Int = if(pd < pu) pd else pu

    override def bundleI(ei: Int) : UInt = UInt(ei.W)
    override def bundleO(eo: Int) : UInt = UInt(eo.W)
    override def render(e: Int): RenderedEdge =
    RenderedEdge(colour = "#000000" /* black */, label = s"${e}")
}

object SourceNodeImp extends NodeImp[Int, Int, Int, Int, UInt] {
    override def edgeI(
        pd: Int, pu: Int, p: Parameters, sourceInfo: SourceInfo
    ) : Int = 0

    override def edgeO(
        pd: Int, pu: Int, p: Parameters, sourceInfo: SourceInfo
    ) : Int = pd

    override def bundleI(ei: Int) : UInt = UInt(ei.W)
    override def bundleO(eo: Int) : UInt = UInt(eo.W)
    override def render(e: Int): RenderedEdge =
    RenderedEdge(colour = "#000000" /* black */, label = s"${e}")
}

object SinkNodeImp extends NodeImp[Int, Int, Int, Int, UInt] {
    override def edgeI(
        pd: Int, pu: Int, p: Parameters, sourceInfo: SourceInfo
    ) : Int = if(pd < pu) pd else pu

    override def edgeO(
        pd: Int, pu: Int, p: Parameters, sourceInfo: SourceInfo
    ) : Int = 0

    override def bundleI(ei: Int) : UInt = UInt(ei.W)
    override def bundleO(eo: Int) : UInt = UInt(eo.W)
    override def render(e: Int): RenderedEdge =
    RenderedEdge(colour = "#000000" /* black */, label = s"${e}")
}


object PassThrough extends App {
  val top = LazyModule(new PassThroughTopModule()(Parameters.empty))
  println(
    ChiselStage.emitSystemVerilog(
      top.module,
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
  )
  // os.write.over(os.pwd / "dump.graphml", top.graphML)
}
