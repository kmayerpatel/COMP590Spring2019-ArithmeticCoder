package lz77;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.OutputStreamBitSink;

public class LZ77FixedSizeEncode {

	public static void main(String [] args) throws IOException {
		String input_file_name = "data/uncompressed.txt";
		String output_file_name = "data/lz77-fixed-sized-compressed.dat";

		/* 
		 * search_size and lookahead_size configure the
		 * size of the search buffer and the lookahead buffer
		 * and are communicated to the decoder as metadata  
		 */
		int search_size = (1 << 16);
		int lookahead_size = (1 << 10);

		/*
		 * Number of bit needed for fixed size code for search offset and match length
		 * derived from search_size and lookahead_size. Maximum value for offset and
		 * length is one smaller than actual value in order to make use of 0 which
		 * would otherwise be unused.
		 */
		
		int search_size_bitwidth = 32 - Integer.numberOfLeadingZeros(search_size-1);
		int window_size_bitwidth = 32 - Integer.numberOfLeadingZeros(search_size+lookahead_size-1);

		/*
		 * Size of input file provides number of symbols to be encoded. Also communicated
		 * to decoder in metadata.
		 */
		
		int num_symbols = (int) new File(input_file_name).length();

		
		/*
		 * Set up input file stream, LZ77 circular encode buffer, and output bit sink.
		 */
		
		FileInputStream fis = new FileInputStream(input_file_name);

		LZ77EncodeBuffer buffer = new LZ77EncodeBuffer(search_size, lookahead_size, fis);

		FileOutputStream fos = new FileOutputStream(output_file_name);
		OutputStreamBitSink bit_sink = new OutputStreamBitSink(fos);

		/*
		 * Write out metadata:
		 *  -- Number of symbols
		 *  -- Search buffer size
		 *  -- Lookahead buffer size
		 */

		bit_sink.write(num_symbols, 32);
		bit_sink.write(search_size, 32);
		bit_sink.write(lookahead_size, 32);
		
		/*
		 * Encoding loop
		 */

		// Set up variables for statistics reporting
		
		int num_matches_found = 0;
		int match_length_sum = 0;
		int num_symbols_encoded = 0;		
		int next_progress_report = 1000; // Report statistics every this many symbols.
		
		while (num_symbols_encoded < num_symbols) {
			
			// Report statistics if appropriate.
			if (num_symbols_encoded > next_progress_report) {
				next_progress_report += 1000;
				System.out.println("Encoded " + num_symbols_encoded + " bytes");
				if (num_matches_found == 0) {
					System.out.println("Number of matches found: " + num_matches_found);
				} else {
					System.out.println("Number of matches found: " + num_matches_found);
					System.out.println("Average match length: " + (double) match_length_sum / (double) num_matches_found);
				}
			}

			// Variable to keep track of longest match found
			
			int longest_match_offset = -1;
			int longest_match_length = 0;

			// Get first symbol in lookahead buffer.
			
			int next_symbol = buffer.lookahead(0);

			// Search for match in search buffer.
			
			for (int offset = 1; offset <= buffer.searchSize(); offset++) {
				
				// Variables describing match if found.
				
				int match_offset = -1;
				int match_length = 0;

				if (buffer.search(offset) == next_symbol) {
					// Found a potential match.

					match_offset = offset;
					match_length = 1;

					// Now march back to beginning of search buffer to see how
					// long the match is.

					int search_offset = offset - 1;
					int lookahead_idx = 1;

					while ((search_offset > 0) && 						// Not at beginning of search buffer yet.
							(lookahead_idx < buffer.lookaheadSize()) && // Not at end of lookahead buffer yet.
							(buffer.search(search_offset) == 			// Still matching
							buffer.lookahead(lookahead_idx))) {
						search_offset--;
						lookahead_idx++;
						match_length++;
					}

					if (search_offset == 0) {
						// If search_offset is 0, then search only stopped because 
						// we got to the beginning of the search buffer.

						// We might be able to keep going if beginning of lookahead buffer
						// still matches what comes next. We'll now start using search_offset
						// as our index into the lookahead buffer.

						while ((lookahead_idx < buffer.lookaheadSize()) && // Not at end of lookahead buffer yet.
								(buffer.lookahead(search_offset) ==        // Still matching
								buffer.lookahead(lookahead_idx))) {
							search_offset++;
							lookahead_idx++;
							match_length++;
						}
					}

					// Record this match if it is our longest.

					if (match_length > longest_match_length) {
						longest_match_offset = match_offset;
						longest_match_length = match_length;
					}

				}
			}

			if (longest_match_offset == -1) {
				// Didn't find a match in the search buffer.
				// Output a 0 to indicate no match followed by
				// 8-bit fixed code representation of symbol

				bit_sink.write(0, 1);
				bit_sink.write(next_symbol, 8);

				// Roll the buffer window forward by 1
				buffer.rollForward(1);
				
				// Update stats
				num_symbols_encoded += 1;
				
			} else {
				// Found a match. 
				// Output 1 to indicate match found followed
				// by offset and length. Can subtract one from
				// each of these for encoding purposes since
				// offset 0 and length 0 are otherwise illegal.
				bit_sink.write(1, 1);
				bit_sink.write(longest_match_offset-1, search_size_bitwidth);
				bit_sink.write(longest_match_length-1, window_size_bitwidth);

				// Now roll the window forward by the match length.
				buffer.rollForward(longest_match_length);
				num_symbols_encoded += longest_match_length;

				// Update stats
				num_matches_found++;
				match_length_sum += longest_match_length;
			}
		}

		// Close output.

		bit_sink.padToWord();
		fos.close();

		// Final statistics report
		
		System.out.println("Encoded " + num_symbols_encoded + " bytes");
		if (num_matches_found == 0) {
			System.out.println("Number of matches found: " + num_matches_found);
		} else {
			System.out.println("Number of matches found: " + num_matches_found);
			System.out.println("Average match length: " + (double) match_length_sum / (double) num_matches_found);
		}

		System.out.println("Done");
	}
}
