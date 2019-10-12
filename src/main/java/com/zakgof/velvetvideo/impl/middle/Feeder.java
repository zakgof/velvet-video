package com.zakgof.velvetvideo.impl.middle;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Feeder {

	public static <I, O> O next(Supplier<I> source, Function<I, O> processor) {
		for(;;) {
			I input = source.get();
			O result = processor.apply(input);
			if (result != null)
				return result;
			if (input == null) {
				return null;
			}
		}
	}

	public static <I, O> void feed(I input, Function<I, O> processor, Consumer<O> output) {

		for(;;) {
			O result = processor.apply(input);
			if (result != null) {
				output.accept(result);
				if (input == null)
					continue;
			}
			if (result == null && input == null)
				output.accept(null);
			return;
		}
	}


}
