package lz77;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;
import io.OutputStreamBitSink;

public class LZ77FixedSizeDecode {

	public static void main(String [] args) throws IOException, InsufficientBitsLeftException {
		String input_file_name = "data/lz77-fixed-sized-compressed.dat";
		String output_file_name = "data/reuncompressed.txt";


		FileInputStream fis = new FileInputStream(input_file_name);
		InputStreamBitSource bit_source = new InputStreamBitSource(fis);

		// Read in number of symbols
		
		int num_symbols = bit_source.next(32);
		
		// Read in window size
		
		int search_size = bit_source.next(32);
		int lookahead_size = bit_source.next(32);

		int search_size_bitwidth = 32 - Integer.numberOfLeadingZeros(search_size-1);
		int window_size_bitwidth = 32 - Integer.numberOfLeadingZeros(search_size+lookahead_size-1);

		FileOutputStream fos = new FileOutputStream(output_file_name);
		
		LZ77DecodeBuffer buffer = new LZ77DecodeBuffer(search_size, lookahead_size, fos);

		// Decoding loop

		int num_symbols_decoded = 0;
		int next_reporting_threshold = 10000;
		
		while (num_symbols_decoded < num_symbols) {
			if (num_symbols_decoded > next_reporting_threshold)  {
				System.out.println("Decoded " + num_symbols_decoded);
				next_reporting_threshold += 10000;
			}
			
			int match_flag = bit_source.next(1);
			
			if (match_flag == 0) {
				// No match, read in next byte and put into decode buffer.
				int next_byte = bit_source.next(8);
				buffer.write(next_byte);
				num_symbols_decoded++;
			} else {
				int match_offset = bit_source.next(search_size_bitwidth)+1;				
				int match_length = bit_source.next(window_size_bitwidth)+1;
				
				buffer.copyForward(match_offset, match_length);
				num_symbols_decoded += match_length;
				
			}
		}
		
		buffer.flush();

		fis.close();
		fos.close();

		System.out.println("Done");
	}
}
