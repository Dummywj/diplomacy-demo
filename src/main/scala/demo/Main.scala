package demo

import chisel3._
import chisel3.experimental.SourceInfo
import circt.stage.ChiselStage

import org.chipsalliance.cde.config.{Config, Field, Parameters}
import org.chipsalliance.diplomacy.ValName
import org.chipsalliance.diplomacy.nodes._
import org.chipsalliance.diplomacy.lazymodule._

// -----------------------------
// 1. CDE 参数
// -----------------------------

case object DriverWidth extends Field[Int](8)
case object MonitorWidth extends Field[Int](4)

// -----------------------------
// 2. Diplomacy 参数类型
// -----------------------------

case class DownwardParam(width: Int)
case class UpwardParam(width: Int)
case class EdgeParam(width: Int)

// -----------------------------
// 3. NodeImp：定义参数如何协商
// -----------------------------

object DemoNodeImp
    extends SimpleNodeImp[DownwardParam, UpwardParam, EdgeParam, UInt] {

  def edge(
      pd: DownwardParam,
      pu: UpwardParam,
      p: Parameters,
      sourceInfo: SourceInfo
  ): EdgeParam = {
    // 简单协商规则：取 source 和 sink 中较小的宽度
    EdgeParam(math.min(pd.width, pu.width))
  }

  def bundle(e: EdgeParam): UInt = UInt(e.width.W)

  def render(e: EdgeParam): RenderedEdge =
    RenderedEdge("blue", s"width = ${e.width}")
}

// -----------------------------
// 4. 三种 Node
// -----------------------------

class DemoSourceNode(widths: Seq[DownwardParam])(implicit valName: ValName)
    extends SourceNode(DemoNodeImp)(widths)

class DemoSinkNode(width: UpwardParam)(implicit valName: ValName)
    extends SinkNode(DemoNodeImp)(Seq(width))

class DemoNexusNode(
    dFn: Seq[DownwardParam] => DownwardParam,
    uFn: Seq[UpwardParam] => UpwardParam
)(implicit valName: ValName)
    extends NexusNode(DemoNodeImp)(dFn, uFn)

// -----------------------------
// 5. Driver：产生常量
// -----------------------------

class ConstDriver(width: Int, value: Int, numOutputs: Int)(implicit p: Parameters)
    extends LazyModule {

  val node = new DemoSourceNode(Seq.fill(numOutputs)(DownwardParam(width)))

  lazy val module: ConstDriverImp = new ConstDriverImp(this, value)

  override lazy val desiredName = "ConstDriver"
}

class ConstDriverImp(outer: ConstDriver, value: Int) extends LazyModuleImp(outer) {
  val finalWidths = outer.node.edges.out.map(_.width)

  require(finalWidths.nonEmpty)

  outer.node.out.zip(finalWidths).foreach { case ((out, _), w) =>
    out := value.U(w.W)
  }
}

// -----------------------------
// 6. Adder：两个输入，一个输出
// -----------------------------

class Adder(implicit p: Parameters) extends LazyModule {
  val node = new DemoNexusNode(
    dFn = { dps =>
      require(dps.nonEmpty)
      DownwardParam(dps.map(_.width).min)
    },
    uFn = { ups =>
      require(ups.nonEmpty)
      UpwardParam(ups.map(_.width).min)
    }
  )

  lazy val module: AdderImp = new AdderImp(this)

  override lazy val desiredName = "Adder"
}

class AdderImp(outer: Adder) extends LazyModuleImp(outer) {
  require(outer.node.in.size >= 2)
  require(outer.node.out.nonEmpty)

  val finalWidth = outer.node.edges.out.head.width

  val sumWide = outer.node.in.map(_._1).reduce(_ +& _)

  outer.node.out.head._1 := sumWide(finalWidth - 1, 0)
}

// -----------------------------
// 7. Monitor：检查 adder 是否正确
// -----------------------------

class AdderMonitor(width: Int, numOperands: Int)(implicit p: Parameters)
    extends LazyModule {

  val operands = Seq.fill(numOperands) {
    new DemoSinkNode(UpwardParam(width))
  }

  val sum = new DemoSinkNode(UpwardParam(width))

  lazy val module: AdderMonitorImp = new AdderMonitorImp(this)

  override lazy val desiredName = "AdderMonitor"
}

class AdderMonitorImp(outer: AdderMonitor) extends LazyModuleImp(outer) {
  val io = IO(new Bundle {
    val error = Output(Bool())
  })

  val xs = outer.operands.map(_.in.head._1)
  val expectedWide = xs.reduce(_ +& _)
  val got = outer.sum.in.head._1

  io.error := got =/= expectedWide(got.getWidth - 1, 0)
}

// -----------------------------
// 8. Top：连接整个 Diplomacy 图
// -----------------------------

class DemoTop(implicit p: Parameters) extends LazyModule {
  val driverWidth = p(DriverWidth)
  val monitorWidth = p(MonitorWidth)

  val driver0 = LazyModule(new ConstDriver(driverWidth, value = 3, numOutputs = 2))
  val driver1 = LazyModule(new ConstDriver(driverWidth, value = 5, numOutputs = 2))
  val adder = LazyModule(new Adder)
  val monitor = LazyModule(new AdderMonitor(monitorWidth, numOperands = 2))

  // 两个 driver 接到 adder
  adder.node := driver0.node
  adder.node := driver1.node

  // 两个 driver 也接到 monitor，用于检查
  monitor.operands(0) := driver0.node
  monitor.operands(1) := driver1.node

  // adder 输出接到 monitor
  monitor.sum := adder.node

  lazy val module: DemoTopImp = new DemoTopImp(this)

  override lazy val desiredName = "DemoTop"
}

class DemoTopImp(outer: DemoTop) extends LazyModuleImp(outer) {
  val io = IO(new Bundle {
    val error = Output(Bool())
  })

  io.error := outer.monitor.module.io.error
}

// -----------------------------
// 9. Main：生成 SystemVerilog
// -----------------------------

object Main extends App {
  implicit val p: Parameters = new Config((site, here, up) => {
    case DriverWidth  => 8
    case MonitorWidth => 4
  })

  ChiselStage.emitSystemVerilogFile(
    gen = LazyModule(new DemoTop).module,
    args = Array("--target-dir", "generated")
  )
}