.PHONY: adder-naive multi-adder concat pass-through cde-example

adder-naive:
	./mill demo.runMain demo.AdderNaive

multi-adder:
	./mill demo.runMain demo.MultiAdder

concat:
	./mill demo.runMain demo.Concat

pass-through:
	./mill demo.runMain demo.PassThrough

cde-example:
	./mill demo.runMain demo.CdeExample
