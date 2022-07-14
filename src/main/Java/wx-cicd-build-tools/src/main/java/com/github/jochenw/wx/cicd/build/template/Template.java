package com.github.jochenw.wx.cicd.build.template;

import java.io.Writer;

public interface Template {
	public static interface Model {
		public String getValue(String pKey);
	}
	public void write(Model pModel, Writer pWriter);
}
