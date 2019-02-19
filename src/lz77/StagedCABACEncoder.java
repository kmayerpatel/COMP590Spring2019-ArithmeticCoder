package lz77;

import java.io.IOException;

import ac.ArithmeticEncoder;
import app.FreqCountIntegerSymbolModel;
import io.BitSink;

public class StagedCABACEncoder {

	private FreqCountIntegerSymbolModel[] _models;
	
	public StagedCABACEncoder(int max_value) {
		int bitwidth = 32 - Integer.numberOfLeadingZeros(max_value);
		
		Integer[] symbols = new Integer[] {0, 1};
		
		_models = new FreqCountIntegerSymbolModel[bitwidth];
		
		for (int i=0; i<_models.length; i++) {
			_models[i] = new FreqCountIntegerSymbolModel(symbols);
		}
	}

	public void encode(BitSink bit_sink, ArithmeticEncoder<Integer> ac, int value) throws IOException {
		for (int i=0; i<_models.length; i++) {
			int symbol = ((value >> (_models.length - i - 1)) & 0x1);
			ac.encode(symbol, _models[i], bit_sink);
			_models[i].addToCount(symbol);
		
		}
	}

}
