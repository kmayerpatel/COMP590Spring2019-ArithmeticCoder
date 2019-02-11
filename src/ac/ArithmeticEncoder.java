package ac;

import java.io.IOException;

import io.BitSink;

public class ArithmeticEncoder<T> {

	private int _range_bit_width;
	private long _low;
	private long _high;
	private long _range_mask;
	private long _one_quarter_mark;
	private long _three_quarter_mark;
	private int _pending_bits;
	
	public ArithmeticEncoder(int rangeBitWidth) {
		assert rangeBitWidth < 63;
		
		_range_bit_width = rangeBitWidth;
	
		_low = 0x0L;
		_high = (0x1L << _range_bit_width) - 1L;
		
		_range_mask = ~(0xffffffffffffffffL << _range_bit_width);
		
		_one_quarter_mark = (0x1L << _range_bit_width) / 4L;
		_three_quarter_mark = _one_quarter_mark * 3L;
		
		_pending_bits = 0;
	}

	public void encode(T symbol, SourceModel<T> model, BitSink bit_sink) throws IOException {
		long range_width = _high - _low + 1;
		
		long new_low = _low + ((long) (range_width * model.cdfLow(symbol)));
		long new_high = _low + ((long) (range_width * model.cdfHigh(symbol))) - 1L;

		assert new_high >= new_low;
		
		_low = new_low;
		_high = new_high;
		
		while (highBit(_low) == highBit(_high)) {
			bit_sink.write(highBit(_low), 1);
			
			while(_pending_bits > 0) {
				bit_sink.write(1-highBit(_low), 1);
				_pending_bits--;
			}
			
			
			_low = (_low << 1) & _range_mask;
			_high = ((_high << 1) & _range_mask) | 0x1L;
		}
		
		while(_high < _three_quarter_mark && _low > _one_quarter_mark) {
			_low = (_low & (_range_mask >> 2)) << 1;
			_high = ((_high & (_range_mask >> 2)) << 1) |   // Shift all but top two over by 1
					0x1L | 									// Make bottom bit 1
					(0x1L << (_range_bit_width -1));		// Restore the 1 on top

			_pending_bits++;
		}		
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
