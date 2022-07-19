package com.github.jochenw.wx.cicd.build.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.github.jochenw.afw.core.util.NotImplementedException;
import com.github.jochenw.afw.di.api.ComponentFactoryBuilder;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.wx.cicd.build.template.Template.Model;
import com.github.jochenw.wx.cicd.build.template.TemplateUtils.PlaintextListener;

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
		final StringBuilder sb = new StringBuilder();
		final TemplateUtils utils = newTemplateUtils();
		String line = "abc#{foo}#{bar}def#{some.property}xyz";
		utils.parse(line, new PlaintextListener() {
			
			@Override
			public void text(String pValue) {
				sb.append(pValue);
			}
			
			@Override
			public void reference(String pKey) {
				sb.append("#{");
				sb.append(pKey);
				sb.append("}");
			}
			
			@Override
			public void error(String pMsg) {
				throw new NotImplementedException();
			}
		}, "#{", "}");
		assertEquals(line, sb.toString());
	}

	private RuntimeException error(String pMsg) {
		return new IllegalStateException(pMsg);
	}
}
