package org.jmathml;

/**
 * Class representing a MathML &lt;cn&gt; element of type "e-notation"
 * 
 * @author radams
 *
 */
public class ENotationNumber extends ASTNumber {
	private double mantissa, exp;

	public double getValue() {
		return mantissa * Math.pow(10.0, exp);
	}

	public double getExponent() {
		return exp;
	}

	public double getMantissa() {
		return mantissa;
	}

	ENotationNumber(double mantissa, double exponent) {
		super(ASTNumberType.NUMBER);
		this.mantissa = mantissa;
		this.exp = exponent;

	}

	public boolean isENotation() {
		return true;
	}

	@Override
	public String getString() {
		return new StringBuffer("(").append(mantissa).append(" * pow(10, ")
				.append(exp).append("))").toString();
	}

}
