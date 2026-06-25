A mini demo using Diplomacy.

## build.mill

`build.mill` 是 Mill 构建工具的配置文件，定义了：

- 一个名为 `demo` 的 `ScalaModule`
- Scala 版本 `2.13.16`
- Chisel `6.7.0` 及 `chisel-plugin` 编译器插件
- 额外的源码目录：`deps/cde` 和 `deps/diplomacy`（通过 git submodule 引入）

## 环境准备

1. 安装 Mill 构建工具：

```shell
curl -L https://repo1.maven.org/maven2/com/lihaoyi/mill-dist/1.1.6/mill-dist-1.1.6-mill.sh -o mill
chmod +x mill
./mill version
```

2. 初始化 git submodule（CDE 和 Diplomacy）：

```shell
git submodule update --init --recursive
```

3. 确保已安装 firtool（CIRCT 的 FIRRTL 编译器），用于生成 SystemVerilog。

## 运行示例

```shell
./mill demo.runMain demo.AdderNaive
./mill demo.runMain demo.MultiAdder
./mill demo.runMain demo.Concat
./mill demo.runMain demo.PassThrough
./mill demo.runMain demo.CdeExample
```

或通过 Makefile：

```shell
make adder-naive
make multi-adder
make concat
make pass-through
make cde-example
```
