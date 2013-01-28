package ummisco.gaml.ext.maths.statements;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

import msi.gama.common.interfaces.IKeyword;
import msi.gama.common.util.GuiUtils;
import msi.gama.precompiler.GamlAnnotations.facet;
import msi.gama.precompiler.GamlAnnotations.facets;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.operator;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.*;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.descriptions.IDescription;
import msi.gaml.expressions.*;
import msi.gaml.statements.AbstractStatement;
import msi.gaml.types.IType;

@facets(value = { @facet(name = IKeyword.EQUATION_LEFT, type = IType.NONE_STR, optional = false),
	@facet(name = IKeyword.EQUATION_RIGHT, type = IType.FLOAT_STR, optional = false) }, omissible = IKeyword.EQUATION_RIGHT)
@symbol(name = { IKeyword.EQUATION_OP }, kind = ISymbolKind.SINGLE_STATEMENT, with_sequence = false)
@inside(symbols = IKeyword.EQUATION)
/**
 * 
 * The class SingleEquationStatement. 
 * Implements an Equation in the form function(n, t) = expression;
 * The left function is only here as a placeholder for enabling a simpler syntax and grabbing the variable as its left member.
 * @comment later, the order will be used as it will require a different integrator to solve the equation. For the moment, it is just 
 * here to show how to compute it from the function used
 *
 * @author Alexis Drogoul, Huynh Nguyen Nghi
 * @since 26 janv. 2013
 *
 */
public class SingleEquationStatement extends AbstractStatement  implements
FirstOrderDifferentialEquations {

	final IExpression function, expression;
	IVarExpression var;
	int order;

	public SingleEquationStatement(final IDescription desc) {
		super(desc);
		function = getFacet(IKeyword.EQUATION_LEFT);
		var = (IVarExpression) ((AbstractBinaryOperator) function).left();
		expression = getFacet(IKeyword.EQUATION_RIGHT);
	}

	/**
	 * This method is normally called by the system of equations to which this equation belongs.
	 * It simply computes the expression that represents the equation and returns it. The storage of
	 * the new values is realized in SystemOfEquationsStatement, in order not to generate side
	 * effects (e.g. the value of a shared variable changing between the integrations of two
	 * equations)
	 * 
	 * @see msi.gaml.statements.AbstractStatement#privateExecuteIn(msi.gama.runtime.IScope)
	 */
	@Override
	protected Double privateExecuteIn(final IScope scope) throws GamaRuntimeException {
		Double result = (Double) expression.value(scope);

		GuiUtils.informConsole(""+expression.value(scope));
		return result;
	}

//	public Rk4Solver(double[] c, double omega) {
//		this.c = c;
//		this.omega = omega;
//	}


	@Override
	public Object executeOn(IScope scope) throws GamaRuntimeException {
		// TODO Auto-generated method stub

		return super.executeOn(scope);
	}

	public void computeDerivatives(IScope scope) {
		GuiUtils.informConsole(""+expression.value(scope));
	}

	public int getOrder() {
		if ( function.getName().equals("diff") ) { return 1; }
		if ( function.getName().equals("diff2") ) { return 2; }
		return 0;
	}

	// Placeholders operators that are (normally) never called.

	@operator("diff")
	public static Double diff(final IScope scope, final Double var, final Double time) {
		return Double.NaN;
	}

	@operator("diff2")
	public static Double diff2(final IScope scope, final Double var, final Double time) {
		return Double.NaN;
	}

	@Override
	public int getDimension() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void computeDerivatives(double t, double[] y, double[] yDot)
			throws MaxCountExceededException, DimensionMismatchException {
		// TODO Auto-generated method stub
		
	}

}
