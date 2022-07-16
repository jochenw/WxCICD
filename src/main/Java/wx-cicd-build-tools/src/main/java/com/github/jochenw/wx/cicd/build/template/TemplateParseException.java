package com.github.jochenw.wx.cicd.build.template;

public class TemplateParseException extends RuntimeException {
	private static final long serialVersionUID = 7589436642828645173L;
	private final String uri;
	private final int lineNumber;

	public TemplateParseException(String pUri, int pLineNumber, String pMessage) {
		super(asMessage(pUri, pLineNumber, pMessage));
		uri = pUri;
		lineNumber = pLineNumber;
	}

	private static String asMessage(String pUri, int pLineNumber, String pMessage) {
		final StringBuilder sb = new StringBuilder();
		sb.append("At ");
		if (pUri != null) {
			sb.append(pUri);
		}
		if (pLineNumber != -1) {
			sb.append(", line ");
			sb.append(pLineNumber);
		}
		sb.append(": ");
		sb.append(pMessage);
		return sb.toString();
	}

	public String getUri() { return uri; }
	public int getLineNumber() { return lineNumber; }


}
