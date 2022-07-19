package com.github.jochenw.wx.cicd.build.template;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.github.jochenw.afw.di.api.Module;
import com.github.jochenw.afw.di.api.Scopes;


public class SimpleTemplateCompiler implements TemplateCompiler {
	private final String startToken, endToken;
	private final String uri;
	private final TemplateUtils templateUtils;

	public SimpleTemplateCompiler(String pUri, TemplateUtils pTemplateUtils, String pStartToken, String pEndToken) {
		uri = pUri;
		templateUtils = pTemplateUtils;
		startToken = pStartToken;
		endToken= pEndToken;
	}

	@Override
	public Template compile(List<String> pLines) {
		final List<Template> templates = new ArrayList<>();
		parse(pLines, 0, templates::add, () ->{});
		return new Template() {
			@Override
			public void write(Model pModel, PrintWriter pWriter) {
				templates.forEach((t) -> {
					t.write(pModel, pWriter);
				});
			}
		};
	}

	protected int parse(List<String> pLines, int pIndex, Consumer<Template> pConsumer, Runnable pEofHandler) {
		for (int i = pIndex;  i < pLines.size();  ) {
			final String line = pLines.get(i);
			final String l = line.trim();
			if (l.length() > 4  &&  l.startsWith("#if")  &&  Character.isWhitespace(l.charAt(3))) {
				final String expression = l.substring(4);
				i = parseIf(expression, pLines, i, pConsumer);
			} else if (l.startsWith("#/")) {
				// Ignore comment line.
				++i;
			} else if (l.startsWith("#end")) {
				return i;
			} else {
				parsePlainText(i++, line, pConsumer);
			}
		}
		pEofHandler.run();
		return -1;
	}

	protected int parseIf(String pExpression, List<String> pLines, int pIndex, Consumer<Template> pConsumer)
	 		throws TemplateParseException {
		final List<Template> templates = new ArrayList<>();
		final TemplateFunction tf = templateUtils.parse(pExpression, (s) -> error(pIndex, s)); 
		final int endIndex = parse(pLines, pIndex+1, templates::add, () -> {
			throw error(pIndex, "No #end seen for #if at line" + pIndex);
		});
		pConsumer.accept(new Template() {
			@Override
			public void write(Model pModel, PrintWriter pWriter) {
				if (tf.test(pModel)) {
					templates.forEach((t) -> t.write(pModel, pWriter));
				}
			}
		});
		return endIndex+1;
	}

	protected TemplateParseException error(int pLineNumber, String pMessage) {
		return new TemplateParseException(uri, pLineNumber, pMessage);
	}

	protected void parsePlainText(int pIndex, String pLine, Consumer<Template> pTemplateConsumer) {
		final List<BiConsumer<Template.Model,StringBuilder>> consumers = new ArrayList<>();
		final TemplateUtils.PlaintextListener listener = new TemplateUtils.PlaintextListener() {
			@Override
			public void text(String pText) {
				consumers.add((model,sb) -> sb.append(pText));
			}

			@Override
			public void reference(String pKey) {
				consumers.add((model,sb) -> {
					final String value = model.getValue(pKey);
					if (value == null) {
						throw new NullPointerException("Undeclared property: " + pKey);
					}
					sb.append(value);
				});
			}

			@Override
			public void error(String pMsg) {
				throw SimpleTemplateCompiler.this.error(pIndex, pMsg);
			}
		};
		templateUtils.parse(pLine, listener, startToken, endToken);
		pTemplateConsumer.accept((model,lw) -> {
			final StringBuilder sb = new StringBuilder();
			consumers.forEach((c) -> c.accept(model,sb));
			lw.println(sb.toString());
		});
	}

	public static final Module MODULE = (b) -> {
		b.bind(TemplateUtils.class).in(Scopes.SINGLETON);
		b.bind(ParameterFunction.class, "isTrue").toInstance(TemplateFunction.IS_TRUE);
	};
}
