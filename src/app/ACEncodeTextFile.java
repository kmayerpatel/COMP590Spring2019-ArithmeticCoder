package app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import ac.ArithmeticEncoder;
import io.OutputStreamBitSink;

public class ACEncodeTextFile {

	public static void main(String[] args) throws IOException {
		String input_file_name = "data/uncompressed.txt";
		String output_file_name = "data/compressed.dat";

		int range_bit_width = 40;

		System.out.println("Encoding text file: " + input_file_name);
		System.out.println("Output file: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);
		
		FileInputStream fis = new FileInputStream(input_file_name);

		int[] symbol_counts = new int[256];

		int num_symbols = 0;
		int next_byte = fis.read();		

		while (next_byte != -1) {
			symbol_counts[next_byte]++;
			num_symbols++;

			next_byte = fis.read();
		}
		fis.close();

		Integer[] symbols = new Integer[256];
		for (int i=0; i<256; i++) {
			symbols[i] = i;
		}

		FreqCountIntegerSymbolModel model = new FreqCountIntegerSymbolModel(symbols, symbol_counts);

		ArithmeticEncoder<Integer> encoder = new ArithmeticEncoder<Integer>(model, range_bit_width);

		FileOutputStream fos = new FileOutputStream(output_file_name);
		OutputStreamBitSink bit_sink = new OutputStreamBitSink(fos);

		for (int i=0; i<256; i++) {
			bit_sink.write(symbol_counts[i], 32);
		}
		bit_sink.write(num_symbols, 32);
		bit_sink.write(range_bit_width, 8);

		fis = new FileInputStream(input_file_name);
		for (int i=0; i<num_symbols; i++) {
			int next_symbol = fis.read();
			encoder.encode(next_symbol, bit_sink);
		}
		fis.close();

		encoder.emitMiddle(bit_sink);
		bit_sink.padToWord();
		fos.close();
		
		System.out.println("Done");
	}
}
