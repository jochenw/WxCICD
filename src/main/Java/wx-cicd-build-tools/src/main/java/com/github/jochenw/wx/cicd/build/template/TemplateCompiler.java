package com.github.jochenw.wx.cicd.build.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public interface TemplateCompiler {
	public Template compile(List<String> pLines);
	public default Template compile(Path pPath) {
		try (BufferedReader br = Files.newBufferedReader(pPath)) {
			return compile(br);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	public default Template compile(File pFile) {
		return compile(pFile.toPath());
	}
	public default Template compile(URL pUrl, Charset pCharset) {
		try (InputStream in = pUrl.openStream()) {
			return compile(in, pCharset);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	public default Template compile(InputStream pIn, Charset pCharset) {
		return compile(new InputStreamReader(pIn, pCharset));
	}
	public default Template compile(Reader pReader) {
		final List<String> lines = new ArrayList<String>();
		final BufferedReader br = new java.io.BufferedReader(pReader);
		try {
			for (;;) {
				final String line = br.readLine();
				if (line == null) {
					break;
				} else {
					lines.add(line);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return compile(lines);
	}
}
