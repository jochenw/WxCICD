package com.github.jochenw.wx.cicd.build.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.github.jochenw.afw.di.api.ComponentFactoryBuilder;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.wx.cicd.build.template.Template.Model;

class TemplateUtilsTest {

	@Test
	void testParseStringFunctionOfStringRuntimeException() {
		final TemplateUtils utils = newTemplateUtils();
		final TemplateFunction tf = utils.parse("isTrue(some.parameter)", this::error);
		assertNotNull(tf);
		final Properties props = new Properties();
		final Model model = Model.of(props);
		assertThrows(NullPointerException.class, () -> tf.test(model), "");
		props.put("some.parameter", "true");
		assertTrue(tf.test(model));
		props.put("some.parameter", "false");
		assertFalse(tf.test(model));
	}

	private TemplateUtils newTemplateUtils() {
		final IComponentFactory cf = new ComponentFactoryBuilder().module(SimpleTemplateCompiler.MODULE).build();
		return cf.requireInstance(TemplateUtils.class);
	}

	@Test
	void testParseStringPlaintextListenerStringString() {
		final TemplateUtils utils = newTemplateUtils();
	}

	private RuntimeException error(String pMsg) {
		return new IllegalStateException(pMsg);
	}
}
