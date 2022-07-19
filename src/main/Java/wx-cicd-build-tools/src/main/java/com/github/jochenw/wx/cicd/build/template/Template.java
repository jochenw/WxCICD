package com.github.jochenw.wx.cicd.build.template;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Properties;

public interface Template {
	public static interface Model {
		public String getValue(String pKey);

		public static Model of(Properties props) {
			return props::getProperty;
		}
	}
	public void write(Model pModel, PrintWriter pWriter);
	public default void write(Model pModel, Writer pWriter) {
		final PrintWriter pw = new PrintWriter(pWriter);
		write(pModel, pw);
		pw.flush();
	}
}
