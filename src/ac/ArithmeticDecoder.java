package ac;

import java.io.IOException;

import io.BitSource;
import io.InsufficientBitsLeftException;

public class ArithmeticDecoder<T> {
	private SourceModel<T> _model;
	private long _low;
	private long _high;
	private int _range_bit_width;
	private long _range_mask;
	private long _input_buffer;
	private boolean _first_fill;
	private int _bits_consumed;
	private boolean _tracing;

	public ArithmeticDecoder(SourceModel<T> model, int rangeBitWidth) {
		this(model, rangeBitWidth, false);
	}
	
	public ArithmeticDecoder(SourceModel<T> model, int rangeBitWidth, boolean tracing) {
		assert rangeBitWidth < 63;
		assert model != null;
		
		_range_bit_width = rangeBitWidth;
		_model = model;
		_low = 0;
		_high = (0x1L << rangeBitWidth) - 1L;
		_range_mask = ~(0xffffffffffffffffL << _range_bit_width);
		_first_fill = true;
		_input_buffer = 0;
		_bits_consumed = 0;
		_tracing = tracing;
	}

	public T decode(BitSource bit_source) throws InsufficientBitsLeftException, IOException {
		if (_first_fill) {
			for (int i=0; i<_range_bit_width; i++) {
				_input_buffer = ((_input_buffer << 1) & _range_mask) | ((long) bit_source.next(1));
			}
			_first_fill = false;
		}

		long range_width =  _high - _low + 1;
		int sym_idx = -1;
		long sym_low = 0;
		long sym_high = 0;

		// Find the next symbol according to the symbol model.
		
		for (int i=0; i<_model.size(); i++) {
			sym_low = _low + ((long) (range_width * _model.cdfLow(i)));
			sym_high =_low + ((long) (range_width * _model.cdfHigh(i))) -1L;

			if (_input_buffer >= sym_low && _input_buffer <= sym_high) {
				// Found it
				sym_idx = i;
				break;
			}
		}
		assert sym_idx != -1;

		_low = sym_low;
		_high = sym_high;

		int high_bit = highBit(_input_buffer);
		
		while((highBit(_low) == high_bit) &&
			  (highBit(_high) == high_bit)) {

			_input_buffer = ((_input_buffer << 1) & _range_mask) | ((long) bit_source.next(1));
			_low = (_low << 1) & _range_mask;
			_high =((_high << 1) & _range_mask) | 0x1L;

			high_bit = highBit(_input_buffer);
			_bits_consumed++;
		}

		// Are we in the middle?	
		long one_quarter_mark = (0x1L << _range_bit_width) / 4L;
		long three_quarter_mark = one_quarter_mark * 3L;

		
		while (_low > three_quarter_mark && _high < one_quarter_mark) {
			// Yes, so shift out the second highest bits until we are not.
			
			_input_buffer = ((_input_buffer & (_range_mask>>2)) << 1) | // Mask out top two bits and shift left
					((long) bit_source.next(1)) |                       // Bring in next bit from source
					(((long) high_bit) << (_range_bit_width-1));        // Restore top bit to old value

			_low = ((_low & (_range_mask>>2)) << 1);                    // Mask out top two bits and shift left
			                                                            // Low order bit already 0 from shift
			                                                            // Top bit still 0 after mask
			
			_high = ((_high & (_range_mask>>2)) << 1) |                 // Mask out top two bits and shift left
					0x1L |                                              // Set low order bit to 1
					(0x1L << (_range_bit_width-1));                     // Restore top bit as 1.
			
			_bits_consumed++;
		}

		T symbol = _model.get(sym_idx);
		
		if (_tracing) {
			System.out.println("Decoded: " + symbol.toString());
			System.out.println("   High: " + String.format("%16x", _high));
			System.out.println("    Low: " + String.format("%16x", _low));
		}
		return symbol;
	}

	public int getBitsConsumed() {
		return _bits_consumed;
	}
	
	private int highBit(long value) {
		return (int) ((value >> (_range_bit_width-1)) & 0x1L);
	}
}
