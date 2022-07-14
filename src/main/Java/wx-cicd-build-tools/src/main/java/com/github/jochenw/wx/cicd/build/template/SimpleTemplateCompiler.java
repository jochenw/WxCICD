package com.github.jochenw.wx.cicd.build.template;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SimpleTemplateCompiler implements TemplateCompiler {
	@Override
	public Template compile(List<String> pLines) {
		final List<Template> templates = new ArrayList<>();
		compile(pLines, 0, templates::add);
		return new Template() {
			@Override
			public void write(Model pModel, Writer pWriter) {
				templates.forEach((t) -> {
					t.write(pModel, pWriter);
				});
			}
		};
	}

	protected void compile(List<String> pLines, int pIndex, Consumer<Template> pConsumer) {
		for (int i = pIndex;  i < pLines.size();  ) {
			final String line = pLines.get(i++);
			final String l = line.trim();
			if (l.startsWith("#if")) {
				i = compileIf(pLines, i, pConsumer);
			} else {
				compilePlainText(line, pConsumer);
			}
		}
	}
}
