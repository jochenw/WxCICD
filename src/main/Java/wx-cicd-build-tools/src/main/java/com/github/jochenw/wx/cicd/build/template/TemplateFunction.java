package com.github.jochenw.wx.cicd.build.template;

public interface TemplateFunction {
	ParameterFunction IS_TRUE = (params) -> {
		if (params.length == 0) {
			return false;
		} else {
			return TemplateUtils.isTrue(params[0]);
		}
	};

	public boolean test(Template.Model pModel);
}
