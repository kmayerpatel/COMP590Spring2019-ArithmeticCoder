package io;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamBitSource implements BitSource {
	private InputStream _stream;
	private int _buffer;
	private int _available;

	public InputStreamBitSource(InputStream stream) {
		_stream = stream;
		_buffer = 0x0;
		_available = 0;
	}

	@Override
	public int next(int count) throws InsufficientBitsLeftException, IOException {

		if (count > 32) {
			throw new RuntimeException("Can't read more than 32 bits as an int");
		}
		if (count <= 0) {
			return 0;
		}

		if (_available == 0) {
			// Refill buffer.
			_buffer = 0x0;
			while (_available < count) {
				int next_byte_from_stream = _stream.read();
				if (next_byte_from_stream == -1) {
					throw new InsufficientBitsLeftException(_available);
				}
				_buffer = ((_buffer << 8) | next_byte_from_stream);
				_available += 8;
			}
		}
		
		if (_available < count) {
			int part_a_count = _available;
			int part_b_count = (count - _available);
			int part_a = next(part_a_count);
			int part_b = next(part_b_count);
			return ((part_a << part_b_count) | part_b);
		} else {
			int bits_from_buffer = (_buffer >> (_available - count));
			int bits_from_buffer_mask = 0xffffffff;
			if (count < 32) {
				bits_from_buffer_mask = ~(bits_from_buffer_mask << count);
			}
			_available -= count;
			return bits_from_buffer & bits_from_buffer_mask;
		}
	}
}
