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

		FileInputStream fis = new FileInputStream(input_file_name);

		int next_byte = fis.read();		
		int[] symbol_counts = new int[256];

		int num_symbols = 0;
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

		Scanner s = new Scanner(System.in);
		boolean stepping = true;
		int step_skip = 0;

		int num_bits_emitted = 0;
		for (int i=0; i<num_symbols; i++) {
			int next_symbol = fis.read();

			if (stepping) {
				if (step_skip > 0) {
					step_skip--;
				}

				if ((step_skip == 0) && s.hasNext()) {
					if (s.hasNextInt()) {
						step_skip = s.nextInt();
					} else {
						String cmd = s.next();
						if (cmd.equals("go")) {
							stepping = false;
						}
					}
				}

			}
		}
		fis.close();

		encoder.emitMiddle(bit_sink);
		bit_sink.padToWord();
		fos.close();
	}
}
