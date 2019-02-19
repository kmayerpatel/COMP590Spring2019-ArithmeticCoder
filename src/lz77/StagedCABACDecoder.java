package lz77;

import java.io.IOException;

import ac.ArithmeticDecoder;
import app.FreqCountIntegerSymbolModel;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;

public class StagedCABACDecoder {

	private FreqCountIntegerSymbolModel[] _models;
	
	public StagedCABACDecoder(int max_value) {
		int bitwidth = 32 - Integer.numberOfLeadingZeros(max_value);
		
		Integer[] symbols = new Integer[] {0, 1};
		
		_models = new FreqCountIntegerSymbolModel[bitwidth];
		
		for (int i=0; i<_models.length; i++) {
			_models[i] = new FreqCountIntegerSymbolModel(symbols);
		}
	}

	public int decode(InputStreamBitSource bit_source, ArithmeticDecoder<Integer> ac) throws IOException, InsufficientBitsLeftException {
		int value = 0;
		
		for (int i=0; i<_models.length; i++) {
			int symbol = ac.decode(_models[i], bit_source);
			_models[i].addToCount(symbol);
			value = value * 2 + symbol;
		}
		return value;
	}

}
