import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Random;

public class LanguageModel {

    // The map of this model.
    // Maps windows to lists of charachter data objects.
    HashMap<String, List> CharDataMap;
    
    // The window length used in this model.
    int windowLength;
    
    // The random number generator used by this model. 
	private Random randomGenerator;

    /** Constructs a language model with the given window length and a given
     *  seed value. Generating texts from this model multiple times with the 
     *  same seed value will produce the same random texts. Good for debugging. */
    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    /** Constructs a language model with the given window length.
     * Generating texts from this model multiple times will produce
     * different random texts. Good for production. */
    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
    public void train(String fileName) {
    String window = "";
    char c;

    In in = new In(fileName);

    for (int i = 0; i < windowLength; ) {
        if (in.isEmpty()) return;
        char ch = in.readChar();
        if (ch == '\r') continue;    
        window = window + ch;
        i++;
    }

    while (!in.isEmpty()) {
        c = in.readChar();
        if (c == '\r') continue;  

        List probs = CharDataMap.get(window);
        if (probs == null) {
            probs = new List();
            CharDataMap.put(window, probs);
        }

        probs.update(c);
        window = window.substring(1) + c;
    }

    java.util.Iterator<List> it = CharDataMap.values().iterator();
    while (it.hasNext()) {
        calculateProbabilities(it.next());
    }
}


    // Computes and sets the probabilities (p and cp fields) of all the
	// characters in the given list. */
    void calculateProbabilities(List probs) {
    int sum = 0;
    for (int i = 0; i < probs.getSize(); i++) {
        sum += probs.get(i).count;
    }

    if (sum == 0) {
        for (int i = 0; i < probs.getSize(); i++) {
            probs.get(i).p = 0.0;
            probs.get(i).cp = 0.0;
        }
        return;
    }

    double cumulative = 0.0;
    for (int i = 0; i < probs.getSize(); i++) {
        CharData pr = probs.get(i);
        pr.p = (double) pr.count / sum;
        cumulative += pr.p;
        pr.cp = cumulative;
    }

    if (probs.getSize() > 0) {
        probs.get(probs.getSize() - 1).cp = 1.0;
    }
}


    // Returns a random character from the given probabilities list.
	char getRandomChar(List probs) {
    double r = randomGenerator.nextDouble();
    int size = probs.getSize();

    for (int i = 0; i < size; i++) {
        CharData cd = probs.get(i);
        if (cd.cp > r) return cd.chr;
    }
    return probs.get(size - 1).chr;
}



	

    /**
	 * Generates a random text, based on the probabilities that were learned during training. 
	 * @param initialText - text to start with. If initialText's last substring of size numberOfLetters
	 * doesn't appear as a key in Map, we generate no text and return only the initial text. 
	 * @param numberOfLetters - the size of text to generate
	 * @return the generated text
	 */
	public String generate(String initialText, int textLength) {
    if (initialText == null || initialText.length() < windowLength) {
        return initialText;
    }

    StringBuilder generated = new StringBuilder(initialText);

    int targetLength = initialText.length() + textLength;

    while (generated.length() < targetLength) {
        String window = generated.substring(generated.length() - windowLength);
        List probs = CharDataMap.get(window);

        if (probs == null) {
            return generated.toString();
        }

        char nextChar = getRandomChar(probs);
        generated.append(nextChar);
    }

    return generated.toString();
}

    

    /** Returns a string representing the map of this language model. */
	public String toString() {
		StringBuilder str = new StringBuilder();
		for (String key : CharDataMap.keySet()) {
			List keyProbs = CharDataMap.get(key);
			str.append(key + " : " + keyProbs + "\n");
		}
		return str.toString();
	}

    public static void main(String[] args) {
        int windowLength = Integer.parseInt(args[0]);
        String initialText = args[1];
        int generatedTextLength = Integer.parseInt(args[2]);
        Boolean randomGeneration = args[3].equals("random");
        String fileName = args[4];
        if (args.length != 5) {
            System.out.println("Usage: java LanguageModel windowLength initialText textLength fixed/random fileName");
            System.out.println("args.length=" + args.length);
            return;
        }
        LanguageModel lm;
        if (randomGeneration)
            lm = new LanguageModel(windowLength);
        else
            lm = new LanguageModel(windowLength, 20);
// Trains the model, creating the map.
lm.train(fileName);
// Generates text, and prints it.
System.out.println(lm.generate(initialText, generatedTextLength));
    }
}
