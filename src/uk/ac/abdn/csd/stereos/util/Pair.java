package uk.ac.abdn.csd.stereos.util;

/**
 * A generic tuple class.
 * 
 * @author Chris Burnett
 * 
 * @param <A>
 * @param <B>
 */
public class Pair<A, B>
{

	public final A a;
	public final B b;

	public Pair(A a1, B b1)
	{
		a = a1;
		b = b1;
	}
}
