package ac;

import java.io.IOException;

import io.BitSink;

public class ArithmeticEncoder<T> {
	private SourceModel<T> _model;
	private long _low;
	private long _high;
	private int _pending;
	private int _range_bit_width;
	private long _range_mask;
	private boolean _tracing;

	public ArithmeticEncoder(SourceModel<T> model, int rangeBitWidth) {
		this(model, rangeBitWidth, false);
	}
	
	public ArithmeticEncoder(SourceModel<T> model, int rangeBitWidth, boolean tracing) {
		assert rangeBitWidth < 63;
		assert model != null;
		
		_range_bit_width = rangeBitWidth;
		_model = model;
		_low = 0;
		_high = (0x1L << rangeBitWidth) - 1L;
		_pending = 0;
		_range_mask = ~(0xffffffffffffffffL << _range_bit_width);
		_tracing = tracing;
	}
	
	public int encode(T symbol, BitSink bitSink) throws IOException {
		int num_bits_emitted = 0;
		long range_width =  _high - _low + 1;

		long old_low = _low;
		
		_low = old_low + ((long) (range_width * _model.cdfLow(symbol)));
		_high = old_low + ((long) (range_width * _model.cdfHigh(symbol))) -1L; 
		
		assert _high > _low;
		
		// While top bit matches, emit bits
		
		while (highBit(_low) == highBit(_high)) {
			int high_bit = highBit(_low);
			bitSink.write(high_bit, 1);
			num_bits_emitted++;
			
			// Write out pending bits if we have any.
			
			while (_pending > 0) {
				bitSink.write(1-high_bit, 1);
				num_bits_emitted++;
				_pending--;
			}

			// Shift low and high
			
			_low = (_low << 1) & _range_mask;
			_high =((_high << 1) | 0x1L) & _range_mask;
		}
		
		// Are we in the middle?	
		long one_quarter_mark = (0x1L << _range_bit_width) / 4L;
		long three_quarter_mark = one_quarter_mark * 3L;
		
		while (_low > three_quarter_mark && _high < one_quarter_mark) {
			// Yes, so shift out the second highest bit and accumulate pending bits

			// We know that:
			// _low must be in form  01xxxx...
			// _high must be in form 10xxxx...
			//
			// To shift out the second bit, we'll mask out everything but the top two bits
			// (i.e., the xxxx... part of above) and shift it over by 1 and then fix up the
			// top bit and low order bit as follows:
			// 
			// _low needs its top bit to remain 0 and new low order bit to be 0
			// _high needs its top bit to remain 1 and new low order bit to be 1

			
			_low = ((_low & (_range_mask>>2)) << 1);		  // Mask out all but top two bits and shift left one.
															  // 0 comes in bottom as part of shift left.
															  // Top bit still 0 after the mask as before.
			

			_high = ((_high & (_range_mask>>2)) << 1) |       // Mask out all but top two bits and shift left one
					0x1L | 									  // Shift in 1 at the bottom.
					(0x1L << (_range_bit_width-1));			  // Restore top bit to be a 1
			
			// Accumulate pending bits
			_pending++;
			
		}

		if (_tracing) {
			System.out.println("Encoded: " + symbol.toString());
			System.out.println("   High: " + String.format("%16x", _high));
			System.out.println("    Low: " + String.format("%16x", _low));
			System.out.println("Emitted: " + num_bits_emitted);
		}
		return num_bits_emitted;
	}
	
	public void emitMiddle(BitSink bitSink) throws IOException {
		bitSink.write("1");
		for (int i=1; i<_range_bit_width; i++) {
			bitSink.write("0");
		}
	}
	
	private int highBit(long value) {
		return (int) ((value >> (_range_bit_width-1)) & 0x1L);
	}	
}
