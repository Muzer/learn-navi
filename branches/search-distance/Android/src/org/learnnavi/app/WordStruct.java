package org.learnnavi.app;

public class WordStruct {
	public final Integer distance;
	public final String definition;
	public final String word;
	public WordStruct(Integer d, String de, String w) {
			this.distance = d;
			this.definition = de;
			this.word = w;
	}
	@Override
	public String toString() {
		return definition;
	}
}
