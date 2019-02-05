package app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import ac.ArithmeticDecoder;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;

public class ACDecodeTextFile {

	public static void main(String[] args) throws InsufficientBitsLeftException, IOException {
		String input_file_name = "data/compressed.dat";
		String output_file_name = "data/reuncompressed.txt";

		FileInputStream fis = new FileInputStream(input_file_name);

		InputStreamBitSource bit_source = new InputStreamBitSource(fis);

		// Read in symbol counts and set up model
		
		int[] symbol_counts = new int[256];
		Integer[] symbols = new Integer[256];
		
		for (int i=0; i<256; i++) {
			symbol_counts[i] = bit_source.next(32);
			symbols[i] = i;
		}

		FreqCountIntegerSymbolModel model = new FreqCountIntegerSymbolModel(symbols, symbol_counts);
		System.out.println(model.cdfTable());
		
		// Read in number of symbols encoded

		int num_symbols = bit_source.next(32);

		System.out.println("File has " + num_symbols + " symbols encoded");

		// Read in range bit width and setup the decoder

		int range_bit_width = bit_source.next(8);
		ArithmeticDecoder<Integer> decoder = new ArithmeticDecoder<Integer>(model, range_bit_width);

		// Decode and produce output.
		
		FileOutputStream fos = new FileOutputStream(output_file_name);

		// Scanner s = new Scanner(System.in);
		for (int i=0; i<num_symbols; i++) {
			// s.next();
			int sym = decoder.decode(bit_source);
			fos.write(sym);
			
			if (i%10000 == 0) {
				System.out.println("After " + i + " symbols, consumed " + decoder.getBitsConsumed());
			}
		}

		fos.flush();
		fos.close();
		fis.close();
	}
}
