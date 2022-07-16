package com.github.jochenw.wx.cicd.build.template;

import java.util.function.Function;

import javax.inject.Inject;

import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.wx.cicd.build.template.Template.Model;

public class TemplateUtils {
	private @Inject IComponentFactory componentFactory;

	public interface PlaintextListener {
		public void text(String line);
		public void reference(String line);
		public void error(String pMsg);

	}

	public TemplateFunction parse(String pExpression, Function<String,RuntimeException> pErrorHandler) {
		String expression = pExpression.trim();
		int offset = 0;
		final String functionName;
		{
			StringBuilder functionNameSb = new StringBuilder();
			while(offset < expression.length()) {
				final char c = expression.charAt(offset++);
				if (c == '(') {
					break;
				} else {
					functionNameSb.append(c);
				}
			}
			functionName = functionNameSb.toString().trim();
		}
		if (functionName.isEmpty()) {
			throw pErrorHandler.apply("Expected parameter list (character '(') not found in expression: " + pExpression);
		}
		final String parameterName;
		boolean terminated = false;
		{
			StringBuilder parameterNameSb = new StringBuilder();
			while(offset < expression.length()) {
				final char c = expression.charAt(offset++);
				if (c == ')') {
					terminated = true;
					break;
				} else {
					parameterNameSb.append(c);
				}
			}
			parameterName = parameterNameSb.toString().trim();
		}
		if (!terminated) {
			throw pErrorHandler.apply("Expected end of parameter list (character ')') not found in expression: " + pExpression);
		}
		if (parameterName.isEmpty()) {
			throw pErrorHandler.apply("Parameter name is empty in expression: " + pExpression);
		}
		final ParameterFunction pf = componentFactory.getInstance(ParameterFunction.class, functionName);
		if (pf == null) {
			throw pErrorHandler.apply("Unknown function: " + functionName);
		}
		return new TemplateFunction() {
			@Override
			public boolean test(Model pModel) {
				final Object parameterValue = pModel.getValue(parameterName);
				if (parameterValue == null) {
					throw new NullPointerException("Parameter evaluates to null: " + parameterName);
				}
				final Object v = pf.evaluate(parameterValue);
				return isTrue(v);
			}
		};
	}

	public void parse(String pLine, PlaintextListener pListener, String pStartToken, String pEndToken) {
		String line = pLine;
		for(;;) {
			int offset = line.indexOf(pStartToken);
			if (offset == -1) {
				pListener.text(line);
				break;
			} else {
				int endOffset = line.indexOf(pEndToken);
				if (endOffset == -1) {
					pListener.error("End token " + pEndToken + " not found in variable reference: " + pLine);
				}
				final String key = line.substring(offset + pStartToken.length(), endOffset);
				pListener.reference(key);
				line = line.substring(endOffset + pEndToken.length());
			}
		}
	}

	public static boolean isTrue(final Object v) {
		if (v == null) {
			return false;
		} else if (v instanceof Boolean) {
			return ((Boolean) v).booleanValue();
		} else if (v instanceof String) {
			return Boolean.valueOf((String) v).booleanValue();
		} else if (v instanceof Number) {
			return ((Number) v).intValue() != 0;
		} else {
			return false;
		}
	}
}
