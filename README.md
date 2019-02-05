# COMP590Spring2019-ArithmeticCoder

Arithmetic coder example from class

The text file encoder produces a file with the following format:

* The first 256 * 32 bits are the frequency counts associated with the integer symbol values 0 to 255. 
  * Each count is a 32 bit value.
* The next 32 bits are the number of symbols that were encoded
* The next 8 bits is the value of range\_bit\_width (i.e., the size of the range registers used by the encoder)
* Following this are the encoded symbols.

One thing to note that was just touched on at the very end of class is that when the encoding app gets to the end (i.e.,
it's encoded the last symbol), it needs to have the encoder emit a bit pattern that is known to be between high and low
so that the decoder doesn't run out of bits as it fills its input buffer when it gets to the end. The method emitMiddle() in
the encoder does this by writing out a 1 followed by range_bit_width-1 0's since that represents the middle of value of the whole range which is guaranteed to be between high and low.

