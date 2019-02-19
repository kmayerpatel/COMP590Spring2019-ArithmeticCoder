package lz77;

import java.io.FileInputStream;
import java.io.IOException;

public class LZ77EncodeBuffer {

	private FileInputStream _source;
	private int _search_size;
	private int _lookahead_size;
	private int[] _window;
	private int _head;
	private int _search_tail;
	private boolean _eof;
	
	public LZ77EncodeBuffer(int search_size, int lookahead_size, FileInputStream fis) throws IOException {
		_search_size = search_size;
		_lookahead_size = lookahead_size;
		_window = new int[_search_size + _lookahead_size];
		_source = fis;
		_head = _search_size;
		_eof = false;
		
		for (int i=0; i<_lookahead_size; i++) {
			int next_byte = -1;
			
			if (!_eof) {
				next_byte = _source.read();
				if (next_byte == -1) {
					_eof = true;
				}
			}
			
			_window[_head+i] = next_byte;
		}
		
		if (_window[_window.length-1] == -1) {
			_eof = true;
			_source.close();
		}
	}

	public int lookahead(int i) {
		int idx = (_head + i) % _window.length;
		return _window[idx];
	}

	public int searchSize() {
		return _search_size;
	}

	public int search(int offset) {
		return _window[(_head - offset + _window.length) % _window.length];
	}

	public int lookaheadSize() {
		return _lookahead_size;
	}

	public void rollForward(int distance) throws IOException {
		int idx = (_head + _lookahead_size) % _window.length;
		
		_head = (_head + distance) % _window.length;
		
		for (int i=0; i<distance; i++) {
			if (!_eof) {
				_window[idx] = _source.read();
				if (_window[idx] == -1) {
					_eof = true;
				}
			} else {
				// Not really necessary but might as well.
				_window[idx] = -1;
			}
			
			idx = (idx + 1) % _window.length;
		}
	}
	
	

}
