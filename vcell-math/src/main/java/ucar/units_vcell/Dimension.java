package ucar.units_vcell;

/**
 * Provides support for the concept of "dimension": pairs of base entities 
 * and exponents.
 * @author Steven R. Emmerson
 * @version $Id: Dimension.java,v 1.4 2000/07/18 20:15:20 steve Exp $
 */
public abstract class
Dimension implements java.io.Serializable
{
    /**
     * The individual elements of this dimension.
     * @serial
     */
    protected final Factor[]		_factors;

    /**
     * Constructs a dimensionless dimension from nothing.
     */
    public
    Dimension()
    {
	this(new Factor[0]);
    }


    /**
     * Constructs from an array of Factor-s.  This is a trusted constructor for
     * use by subclasses only.
     * @param factors		The factors that define the dimension.
     */
    protected
    Dimension(Factor[] factors)
    {
	_factors = factors;
    }


    /**
     * Constructs from a single Factor.
     * @param factor		The single Factor that defines the dimension.
     */
    protected
    Dimension(Factor factor)
    {
	this(new Factor[] {factor});
    }


    /**
     * Indicates if this dimension is semantically identical to an object.
     * @param object		The  object.
     * @return			<code>true</code> if and only if this
     *				dimension is semantically identical to
     *				<code>object</code>.
     */
    public final boolean
    equals(Object object)
    {
	boolean	equals;
	if (!(object instanceof Dimension))
	    equals = false;
	else
	{
	    Factor[]	thatFactors = ((Dimension)object)._factors;
	    if (_factors.length != thatFactors.length)
		equals = false;
	    else
	    {
		int	i = _factors.length;
		while (--i >= 0)
		    if (!_factors[i].equals(thatFactors[i]))
			break;
		equals = i < 0;
	    }
	}
	return equals;
    }


    /**
     * Returns the array of Factor-s constituting this dimension.
     * @return			The array of Factor-s constituting this 
     *				dimension.
     */
    public final Factor[]
    getFactors()
    {
	Factor[]	factors = new Factor[_factors.length];
	System.arraycopy(_factors, 0, factors, 0, factors.length);
	return factors;
    }


    /**
     * Returns the rank of this dimension.  The rank is the number of
     * base entity and exponent pairs (i.e. the number of Factor-s
     * constituting this dimension).
     * @return			The rank of this dimension.
     */
    public final int
    getRank()
    {
	return _factors.length;
    }


    /**
     * Indicates if this dimension is dimensionless.  A dimension is
     * dimensionless if it has no Factor-s or if all Factor-s are,
     * themselves, dimensionless.
     * @return			<code>true</code> if and only if this
     *				dimension is dimensionless.
     */
    public final boolean
    isDimensionless()
    {
	for (int i = _factors.length; --i >= 0; )
	    if (!_factors[i].isDimensionless())
		return false;
	return true;
    }


    /**
     * Indicates if this Dimension is the reciprocal of another dimension.
     * @param that		The other dimension.
     * @return			<code>true</code> if and only if this dimension
     *				is the reciprocal of the other dimension.
     */
    public final boolean
    isReciprocalOf(Dimension that)
    {
	Factor[]	theseFactors = _factors;
	Factor[]	thoseFactors = that._factors;
	boolean	isReciprocalOf;
	if (theseFactors.length != thoseFactors.length)
	    isReciprocalOf = false;
	else
	{
	    int	i;
	    for (i = theseFactors.length; --i >= 0; )
		if (!theseFactors[i].isReciprocalOf(thoseFactors[i]))
		    break;
	    isReciprocalOf = i < 0;
	}
	return isReciprocalOf;
    }


    /**
     * Multiplies this dimension by another dimension.
     * @param that		The other dimension.
     * @return			The product of the Factor-s of this dimension
     *				and the Factor-s of the other dimension.
     */
    protected Factor[]
    mult(Dimension that)
    {
	// relys on _factors always sorted
	Factor[]	factors1 = _factors;
	Factor[]	factors2 = that._factors;
	int		i1 = 0;
	int		i2 = 0;
	int		k = 0;
	Factor[]	newFactors =
	    new Factor[factors1.length + factors2.length];
	for (;;)
	{
	    if (i1 == factors1.length)
	    {
		int	n = factors2.length-i2;
		System.arraycopy(factors2, i2, newFactors, k, n);
		k += n;
		break;
	    }
	    if (i2 == factors2.length)
	    {
		int	n = factors1.length-i1;
		System.arraycopy(factors1, i1, newFactors, k, n);
		k += n;
		break;
	    }
	    Factor	f1 = factors1[i1];
	    Factor	f2 = factors2[i2];
	    int		comp = f1.getID().compareTo(f2.getID());
	    if (comp < 0)
	    {
		newFactors[k++] = f1;
		i1++;
	    }
	    else if (comp == 0)
	    {
		RationalNumber exponent = f1.getExponent().add(f2.getExponent());
		if (!exponent.isZero()){
		    newFactors[k++] = new Factor(f1, exponent);
		}
		i1++;
		i2++;
	    }
	    else
	    {
		newFactors[k++] = f2;
		i2++;
	    }
	}
	if (k < newFactors.length)
	{
	    Factor[]	tmp = new Factor[k];
	    System.arraycopy(newFactors, 0, tmp, 0, k);
	    newFactors = tmp;
	}
	return newFactors;
    }


    /**
     * Raises this dimension to a power.
     * @param power		The power to raise this dimension by.
     * @return			The Factor-s of this dimension raised to the
     *				power <code>power</code>.
     */
    protected Factor[]
    pow(RationalNumber power)
    {
	Factor[]	factors;
	if (power.isZero())
	{
	    factors = new Factor[0];
	}
	else
	{
	    factors = getFactors();
	    if (power.doubleValue() != 1)
		for (int i = factors.length; --i >= 0; )
		    factors[i] = factors[i].pow(power);
	}
	return factors;
    }


    /**
     * Returns the string representation of this dimension.
     * @return			The string representation of this dimension.
     */
    public String
    toString()
    {
	StringBuffer	buf = new StringBuffer(40);
	for (int i = 0; i < _factors.length; i++)
	    buf.append(_factors[i]).append('.');
	if (buf.length() != 0)
	    buf.setLength(buf.length()-1);
	return buf.toString();
    }
}