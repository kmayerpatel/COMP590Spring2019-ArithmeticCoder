package ac;

import java.io.IOException;

import io.BitSink;
import io.BitSource;
import io.InsufficientBitsLeftException;

public class BinarySearchArithmeticDecoder<T> {

	private int _range_bit_width;
	private long _low;
	private long _high;
	private long _input_buffer;
	private long _range_mask;
	private long _one_quarter_mark;
	private long _three_quarter_mark;
	private boolean _first_time;
	
	public BinarySearchArithmeticDecoder(int rangeBitWidth) {
		assert rangeBitWidth < 63;

		_range_bit_width = rangeBitWidth;
	
		_low = 0x0L;
		_high = (0x1L << _range_bit_width) - 1L;
		
		_range_mask = ~(0xffffffffffffffffL << _range_bit_width);
		
		_one_quarter_mark = (0x1L << _range_bit_width) / 4L;
		_three_quarter_mark = _one_quarter_mark * 3L;

		_input_buffer = 0x0;
		
		_first_time = true;
	}

	public T decode(SourceModel<T> model, BitSource bit_source) throws IOException, InsufficientBitsLeftException {
		
		if (_first_time) {
			for (int i=0; i<_range_bit_width; i++) {
				_input_buffer = (_input_buffer << 1) | ((long) bit_source.next(1));
			}
			_first_time = false;
		}
		
		long range_width = _high - _low + 1;
		
		T symbol = null;
		long sym_low = 0;
		long sym_high = 0;
		
		int sym_idx_low = 0;
		int sym_idx_high = model.size();
		
		while (true) {
			int sym_idx_mid = (sym_idx_low + sym_idx_high)/2;

			sym_low = _low + ((long) (range_width * model.cdfLow(sym_idx_mid)));
			sym_high = _low + ((long) (range_width * model.cdfHigh(sym_idx_mid))) - 1L;
			
			if (_input_buffer <= sym_high && _input_buffer >= sym_low) {
				symbol = model.get(sym_idx_mid);
				break;
			} else if (_input_buffer > sym_high) {
				sym_idx_low = sym_idx_mid+1;
			} else {
				sym_idx_high = sym_idx_mid-1;
			}
		}
		
		assert symbol != null;
		
		_low = sym_low;
		_high = sym_high;
		
		while (highBit(_low) == highBit(_input_buffer) &&
				highBit(_high) == highBit(_input_buffer)) {

			_input_buffer = ((_input_buffer << 1) & _range_mask) | ((long) bit_source.next(1));
			_low = (_low << 1) & _range_mask;
			_high = ((_high << 1) & _range_mask) | 0x1L;
		}
		
		while(_high < _three_quarter_mark && _low > _one_quarter_mark) {
			_low = (_low & (_range_mask >> 2)) << 1;
			_high = ((_high & (_range_mask >> 2)) << 1) |   // Shift all but top two over by 1
					0x1L | 									// Make bottom bit 1
					(0x1L << (_range_bit_width -1));		// Restore the 1 on top

			long top_input_bit = highBit(_input_buffer);
			
			_input_buffer = ((_input_buffer & (_range_mask >> 2)) << 1) |   // Shift all but top two over by 1
					((long) bit_source.next(1)) | 									// Make bottom bit 1
					(top_input_bit << (_range_bit_width -1));		// Restore the 1 on top
		}
		
		return symbol;
	}
		
	private int highBit(long value) {
		return (int) ((value >> (_range_bit_width-1)) & 0x1L);
	}
	
}
