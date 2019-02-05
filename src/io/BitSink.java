package io;

import java.io.IOException;

public interface BitSink {
	/* write
	 * Writes bits to bit sink. 
	 * write(int, int) accepts a first argument as bits
	 * to write in lower order bits of value provided. Second
	 * argument is number of bits to be written.
	 * write(String) interprets argument as binary string made
	 * up of "1" and "0" characters. Writes bits out accordingly.
	 */

	int write(int bits, int length) throws IOException;
	int write(String bitstring) throws IOException;
	
	/* padToWord
	 * Writes 0 bits to sink until total number of bits
	 * written is a multiple of 32.
	 */
	int padToWord() throws IOException;
}
