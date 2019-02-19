package lz77;

import java.io.FileOutputStream;
import java.io.IOException;

public class LZ77DecodeBuffer {

	private FileOutputStream _sink;
	private int[] _window;
	private int _head;
	private int _tail;
	
	public LZ77DecodeBuffer(int search_size, int lookahead_size, FileOutputStream fos) throws IOException {
		_window = new int[search_size + lookahead_size];
		_sink = fos;
		_head = search_size;
		_tail = search_size;
	}

	public void write(int next_byte) throws IOException {
		_window[_head] = next_byte;
		_head = (_head + 1) % _window.length;
		if (_head == _tail) {
			// Caught up with tail, write out tail and advance tail.
			_sink.write(_window[_tail]);
			_tail = (_tail + 1) % _window.length;
		}
	}

	public void copyForward(int match_offset, int match_length) throws IOException {
		int copy_idx = (_head - match_offset + _window.length) % _window.length;
		
		for (int i=0; i<match_length; i++) {
			write(_window[copy_idx]);
			copy_idx = (copy_idx + 1) % _window.length;
		}
	}

	public void flush() throws IOException {
		while (_tail != _head) {
			_sink.write(_window[_tail]);
			_tail = (_tail + 1) % _window.length;			
		}
	}
}
