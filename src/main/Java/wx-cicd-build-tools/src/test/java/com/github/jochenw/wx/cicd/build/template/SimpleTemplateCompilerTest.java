package com.github.jochenw.wx.cicd.build.template;

import static org.junit.jupiter.api.Assertions.*;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.jochenw.afw.di.api.ComponentFactoryBuilder;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.wx.cicd.build.template.Template.Model;

class SimpleTemplateCompilerTest {
	private static final String TEMPLATE0 =
			"#/ This is a comment line.It will be ignored.\n"
			+ "Hello, #{world}!\n"
			+ "#if isTrue(text.included)\n"
			+ "  The variable text.included has the value #{text.included}.\n"
			+ "  So, this paragraph becomes visible.\n"
			+ "  #/ Except for this comment line.\n"
			+ "#end";
			
	@Test
	void testRunTemplate() {
		final IComponentFactory cf = new ComponentFactoryBuilder().module(SimpleTemplateCompiler.MODULE).build();
		final SimpleTemplateCompiler stc = new SimpleTemplateCompiler("TEMPLATE0", cf.requireInstance(TemplateUtils.class), "#{", "}");
		final Template t = stc.compile(new StringReader(TEMPLATE0));
		final Properties props = new Properties();
		final Model model = Model.of(props);
		final Supplier<String> runner = () -> {
			final StringWriter sw = new StringWriter();
			t.write(model, sw);
			return sw.toString();
		};
		props.put("world", "world");
		assertThrows(NullPointerException.class, () -> runner.get(),
				     "Parameter evaluates to null: text.included");
		props.put("text.included", "true");
		final String result1 = runner.get();
		assertEquals("Hello, world!" + System.lineSeparator()
				     + "  The variable text.included has the value true." + System.lineSeparator()
				     + "  So, this paragraph becomes visible." + System.lineSeparator(), result1);
		props.put("text.included", "false");
		final String result2 = runner.get();
		assertEquals("Hello, world!" + System.lineSeparator(), result2);
	}

}
