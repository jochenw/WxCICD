package com.github.jochenw.wx.cicd.build.template;

public interface ParameterFunction {
	Object evaluate(Object... pParameters);
}
