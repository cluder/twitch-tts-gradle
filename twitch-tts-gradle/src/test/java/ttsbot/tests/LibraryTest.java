package ttsbot.tests;
/*
 * This Java source file was generated by the Gradle 'init' task.
 */
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ttsbot.util.Utils;

public class LibraryTest {
	@Test
	public void testConstrainToRange() {
		assertEquals(5, Utils.constrainToRange(10, 1, 5));
	}
}