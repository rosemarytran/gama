package ummisco.gaml.extensions.maths.ode.statements;

import msi.gama.common.interfaces.IKeyword;
import msi.gama.precompiler.GamlAnnotations.facet;
import msi.gama.precompiler.GamlAnnotations.facets;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.operator;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.ISymbolKind;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaList;
import msi.gama.util.IList;
import msi.gaml.compilation.IDescriptionValidator;
import msi.gaml.descriptions.IDescription;
import msi.gaml.descriptions.IExpressionDescription;
import msi.gaml.expressions.AbstractNAryOperator;
import msi.gaml.expressions.IExpression;
import msi.gaml.expressions.IVarExpression;
import msi.gaml.statements.AbstractStatement;
import msi.gaml.types.IType;
import msi.gaml.types.Types;

@facets(value = {
		@facet(name = IKeyword.EQUATION_LEFT, type = IType.NONE, optional = false),
		@facet(name = IKeyword.EQUATION_RIGHT, type = IType.FLOAT, optional = false) }, omissible = IKeyword.EQUATION_RIGHT)
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
 * @author Alexis Drogoul, Huynh Quang Nghi
 * @since 26 janv. 2013
 *
 */
public class SingleEquationStatement extends AbstractStatement {
	public static final Class VALIDATOR = SingleEquationValidator.class;

	public static class SingleEquationValidator implements
			IDescriptionValidator {

		/**
		 * Method validate()
		 * 
		 * @see msi.gaml.compilation.IDescriptionValidator#validate(msi.gaml.descriptions.IDescription)
		 */
		@Override
		public void validate(final IDescription d) {
			IExpressionDescription func = d.getFacets().get(
					IKeyword.EQUATION_LEFT);
			if (!(func.getExpression() instanceof AbstractNAryOperator)) {
				d.error("Left-side of equation must be a NAryOperator",
						d.getName());
				return;
			}

			if (!func.getExpression().getType().equals(Types.get(IType.FLOAT))) {
				d.warning("Parameters of equation must be a float type",
						d.getName());
				return;
			}


		}
	}

	private IExpression function, expression;
	private final IList<IExpression> var = new GamaList<IExpression>();
	private IExpression var_t;

	int order;

	public IExpression getFunction() {
		return function;
	}

	public void setFunction(IExpression function) {
		this.function = function;
	}

	public IExpression getExpression() {
		return expression;
	}

	public void setExpression(IExpression expression) {
		this.expression = expression;
	}

	public IList<IExpression> getVars() {
		return var;
	}

	public IExpression getVar(final int index) {
		return var.get(index);
	}

	public void setVar(final int index, IVarExpression v) {
		this.var.set(index, v);
	}

	public IExpression getVar_t() {
		return var_t;
	}

	public void setVar_t(IVarExpression vt) {
		this.var_t = vt;
	}

	public SingleEquationStatement(final IDescription desc) {
		super(desc);
		function = getFacet(IKeyword.EQUATION_LEFT);
		if (function != null && getOrder() > 0) {
			etablishVar();
		}
		expression = getFacet(IKeyword.EQUATION_RIGHT);
	}

	public void etablishVar() {
		for (int i = 0; i < ((AbstractNAryOperator) function).numArg(); i++) {
			IExpression tmp = ((AbstractNAryOperator) function).arg(i);
			if (tmp.getName().equals("t")) {
				var_t = tmp;
			} else {
				var.add(i, tmp);
			}
		}
		// var_t = ((AbstractNAryOperator) function).arg(1);
	}
	/**
	 * This method is normally called by the system of equations to which this
	 * equation belongs. It simply computes the expression that represents the
	 * equation and returns it. The storage of the new values is realized in
	 * SystemOfEquationsStatement, in order not to generate side effects (e.g.
	 * the value of a shared variable changing between the integrations of two
	 * equations)
	 * 
	 * @see msi.gaml.statements.AbstractStatement#privateExecuteIn(msi.gama.runtime.IScope)
	 */
	@Override
	protected Double privateExecuteIn(final IScope scope)
			throws GamaRuntimeException {
		Double result = (Double) expression.value(scope);

		return result;
	}

	@Override
	public Object executeOn(final IScope scope) throws GamaRuntimeException {
		return expression.value(scope);// super.executeOn(scope);
	}

	public int getOrder() {
		if (function.getName().equals("diff")) {
			return 1;
		}
		if (function.getName().equals("diff2")) {
			return 2;
		}
		return 0;
	}

	// Placeholders operators that are (normally) never called.
	// FIXME Can probably be replaced by actions, so that they do not pollute
	// the whole scope of
	// GAMA operators.
	// TODO And maybe they can do something useful, like gathering the order, or
	// the var or var_t,
	// whenever they are called.

	@operator("diff")
	public static Double diff(final IScope scope, final Double var,
			final Double time) {
		return Double.NaN;
	}

	@operator("diff2")
	public static Double diff2(final IScope scope, final Double var,
			final Double time) {
		return Double.NaN;
	}

}
