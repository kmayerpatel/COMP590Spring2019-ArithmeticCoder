package io;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamBitSink implements BitSink {
	OutputStream _stream;
	int _buffer;
	int _available;
	
	public OutputStreamBitSink(OutputStream stream) {
		_stream = stream;
		_buffer = 0x0;
		_available = 32;
	}

	@Override
	public int write(int bits, int length) throws IOException {
		if (length > 32) {
			throw new RuntimeException("Can't write more than 32 bits from an int");
		}
		
		if (length <= _available) {
			_buffer <<= length;
			_buffer |= (bits & (~((0xffffffffL) << length)));
			_available -= length;
			
			if (_available == 0) {
				_stream.write(((_buffer >> 24) & 0xff));
				_stream.write(((_buffer >> 16) & 0xff));
				_stream.write(((_buffer >> 8) & 0xff));
				_stream.write(((_buffer >> 0) & 0xff));
				_buffer = 0x0;
				_available = 32;
			}
			return length;
		}

		int left_to_write = length;
		while (left_to_write > _available) {
			left_to_write -= write((bits >> (length - _available)), _available);
		}
		if (left_to_write > 0) {
			write(bits, left_to_write);
		}
		return length;
	}

	@Override
	public int write(String bitstring) throws IOException {
		for (int i=0; i<bitstring.length(); i++) {
			if (bitstring.charAt(i) == '0') {
				write(0x0, 1);
			} else if (bitstring.charAt(i) == '1') {
				write(0x1, 1);
			} else {
				throw new RuntimeException(
					"Can only write characters '0' or '1'; '" +
					bitstring.charAt(i) + "' encountered."
				);
			}
		}
		return bitstring.length();
	}

	@Override
	public int padToWord() throws IOException {
		return write(0x0, _available);
	}
}
