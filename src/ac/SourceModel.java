package ac;

public interface SourceModel<T> {
	
	int size();

	T get(int index);

	default int lookup(T symbol) {
		for (int i=0; i<size(); i++) {
			if (get(i).equals(symbol)) {
				return i;
			}
		}		
		throw new RuntimeException("Symbol " + symbol.toString() + " not in model");
	}

	double cdfLow(int index);

	default double cdfHigh(int index) {
		if (index < 0 || index > size()-1) {
			throw new IndexOutOfBoundsException();
		}

		if (index == size()-1) {
			return 1.0;
		} else {
			return cdfLow(index+1);
		}		
	}

	default double cdfLow(T symbol) {
		return cdfLow(lookup(symbol));
	}

	default double cdfHigh(T symbol) {
		return cdfHigh(lookup(symbol));
	}
	
	default String cdfTable() {
		String s = "";
		
		for (int i=0; i<size(); i++) {
			s += "Symbol: " + get(i).toString() + " cdfLevel: " + cdfLow(i) + "\n";
		}
		
		return s;
	}
}
