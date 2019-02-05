package io;

import java.io.IOException;

public interface BitSource {
	/* next
	 * Retrieves count bits from bit source as lower order bits in
	 * value returned. Count must be less than or equal
	 * to 32.
	 */
	int next(int count) throws InsufficientBitsLeftException, IOException;
}
