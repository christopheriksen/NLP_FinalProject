class Pair implements Comparable<Pair> {
	  private final String word;
	  private final double similarity;

	  /**
	   * @param word Word to enter in priority queue
	   * @param similarity Similarity to query
	   */
	  public Pair(String word, double similarity) {
		  
	    this.word = word;
	    this.similarity = similarity;
	    
	  }

	  @Override
	  public int compareTo(Pair other) {
	    return Double.valueOf(similarity).compareTo(other.similarity);
	  }
	  
	  public String getWord() {
		  return word;
	  }
	  
	  public void print() {
		  System.out.println(word + "\t" + similarity);
	  }

	}