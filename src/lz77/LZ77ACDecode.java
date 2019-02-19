package lz77;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ac.ArithmeticDecoder;
import ac.BinarySearchArithmeticDecoder;
import app.FreqCountIntegerSymbolModel;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;

public class LZ77ACDecode {

	public static void main(String [] args) throws IOException, InsufficientBitsLeftException {
		String input_file_name = "data/lz77-ac-compressed.dat";
		String output_file_name = "data/reuncompressed.txt";

		FileInputStream fis = new FileInputStream(input_file_name);
		InputStreamBitSource bit_source = new InputStreamBitSource(fis);

		int num_symbols = bit_source.next(32);

		int search_size = bit_source.next(32);

		int lookahead_size = bit_source.next(32);

		int ac_range_bitwidth = bit_source.next(32);

		FileOutputStream fos = new FileOutputStream(output_file_name);		
		LZ77DecodeBuffer buffer = new LZ77DecodeBuffer(search_size, lookahead_size, fos);

		// Decoding loop

		FreqCountIntegerSymbolModel flag_model = new FreqCountIntegerSymbolModel(new Integer[] {0, 1});

		Integer[] unmatched_bytes = new Integer[256];
		for (int i=0; i<256; i++) {
			unmatched_bytes[i] = i;
		}
		FreqCountIntegerSymbolModel unmatched_model = new FreqCountIntegerSymbolModel(unmatched_bytes);

		Integer[] match_offset_values = new Integer[search_size];
		for (int i=0; i<search_size; i++) {
			match_offset_values[i] = i;
		}
		FreqCountIntegerSymbolModel match_offset_model = new FreqCountIntegerSymbolModel(match_offset_values);

		Integer[] match_length_values = new Integer[search_size+lookahead_size];
		for (int i=0; i<search_size+lookahead_size; i++) {
			match_length_values[i] = i;
		}
		FreqCountIntegerSymbolModel match_length_model = new FreqCountIntegerSymbolModel(match_length_values);

		ArithmeticDecoder<Integer> ac = new ArithmeticDecoder<Integer>(ac_range_bitwidth);

		int num_symbols_decoded = 0;
		int next_reporting_threshold = 10000;

		while (num_symbols_decoded < num_symbols) {
			if (num_symbols_decoded > next_reporting_threshold)  {
				System.out.println("Decoded " + num_symbols_decoded);
				next_reporting_threshold += 10000;
			}

			int match_flag = ac.decode(flag_model, bit_source);
			flag_model.addToCount(match_flag);

			if (match_flag == 0) {
				// No match, read in next byte and put into decode buffer.
				int next_byte = ac.decode(unmatched_model, bit_source);
				unmatched_model.addToCount(next_byte);
				buffer.write(next_byte);
				num_symbols_decoded++;
			} else {
				int match_offset = ac.decode(match_offset_model, bit_source);
				match_offset_model.addToCount(match_offset);
				match_offset += 1;
				
				int match_length = ac.decode(match_length_model, bit_source);
				match_length_model.addToCount(match_length);
				match_length += 1;
				
//				int match_offset = match_offset_decoder.decode(bit_source, ac)+1;
//				int match_length = match_length_decoder.decode(bit_source, ac)+1;

				buffer.copyForward(match_offset, match_length);
				num_symbols_decoded += match_length;				
			}
		}
		buffer.flush();
		fos.close();

		System.out.println("Done");
	}
}
