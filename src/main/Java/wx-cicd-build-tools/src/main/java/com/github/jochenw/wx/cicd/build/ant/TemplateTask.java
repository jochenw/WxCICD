package com.github.jochenw.wx.cicd.build.ant;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.github.jochenw.afw.di.api.ComponentFactoryBuilder;
import com.github.jochenw.wx.cicd.build.template.SimpleTemplateCompiler;
import com.github.jochenw.wx.cicd.build.template.Template;
import com.github.jochenw.wx.cicd.build.template.Template.Model;
import com.github.jochenw.wx.cicd.build.template.TemplateCompiler;
import com.github.jochenw.wx.cicd.build.template.TemplateParseException;

public class TemplateTask extends Task {
	public static class TemplateSet extends FileSet {
		private Charset charset;
		private String prefix;

		public Charset getCharset() {
			if (charset == null) {
				return StandardCharsets.UTF_8;
			} else {
				return charset;
			}
		}

		public void setCharset(String pCharset) {
			if (pCharset == null  ||  pCharset.length() == 0) {
				charset = null;
			} else {
				try {
					charset = Charset.forName(pCharset);
				} catch (UnsupportedCharsetException e) {
					throw new BuildException("Unsupported character set: " + pCharset);
				}
				
			}
		}

		public String getPrefix() {
			return prefix;
		}
		public void setPrefix(String pPrefix) {
			prefix = pPrefix;
		}
	}

	private final List<Path> propertyFiles = new ArrayList<>();
	private final List<TemplateSet> templateSets = new ArrayList<>();
	private Path outputDir;
	private boolean forced;

	public List<Path> getPropertyFiles() {
		return propertyFiles;
	}

	public void addPropertyFile(String pPropertyFile) {
		if (pPropertyFile == null  ||  pPropertyFile.length() == 0) {
			throw new BuildException("A property file must not be null, or empty.");
		}
		final Path path = Paths.get(pPropertyFile);
		if (!Files.isRegularFile(path)) {
			throw new BuildException("Invalid property file: Expected existing file, got " + pPropertyFile);
		}
		propertyFiles.add(path);
	}

	public List<TemplateSet> getTemplateSets() {
		return templateSets;
	}

	public void addTemplateSet(TemplateSet pTemplateSet) {
		templateSets.add(pTemplateSet);
	}

	public Path getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(Path pPath) {
		outputDir = pPath;
	}

	public boolean isForced() {
		return forced;
	}

	public void setForced(boolean pForced) {
		forced = pForced;
	}
	public void execute() throws BuildException {
		final Path outDir = getOutputDir();
		if (outDir == null) {
			throw new BuildException("Output directory (attribute outputDir) is not set.");
		}
		final List<TemplateSet> templateSets = getTemplateSets();
		if (templateSets.isEmpty()) {
			throw new BuildException("No templates (nested element templateSet) given.");
		}
		final Properties props = getProperties();
		final Model model = Model.of(props);
		final TemplateCompiler tc = new ComponentFactoryBuilder()
				.module(SimpleTemplateCompiler.MODULE)
				.build()
				.requireInstance(TemplateCompiler.class);
		templateSets.forEach((ts) -> {
			processTemplateSet(tc, ts, outDir, model);
		});
	}

	protected Properties getProperties() {
		final Properties props = new Properties();
		props.putAll(System.getProperties());
		getPropertyFiles().forEach((p) -> {
			try (InputStream in = Files.newInputStream(p)) {
				final Properties pr = new Properties();
				pr.load(in);
				props.putAll(pr);
			} catch (IOException e) {
				throw new BuildException("Failed to read property file " + p + ": " + e.getMessage(), e);
			}
		});
		return props;
	}

	protected void processTemplateSet(TemplateCompiler pTemplateCompiler, TemplateSet pTemplateSet, Path pOutDir, Model pModel) {
		final Path baseDir = pTemplateSet.getDir(getProject()).toPath();
		final Path targetDir;
		final String templatePrefix = pTemplateSet.getPrefix();
		if (templatePrefix == null  ||  templatePrefix.length() == 0) {
			targetDir = pOutDir;
		} else {
			targetDir = pOutDir.resolve(templatePrefix);
		}
		getProject().log("Searching for template files in directory " + baseDir, Project.MSG_VERBOSE);
		final FileScanner fs = pTemplateSet.getDirectoryScanner(getProject());
		fs.scan();
		final String[] templateFiles = fs.getIncludedFiles();
		getProject().log("Found " + templateFiles.length + " template files in directory " + baseDir);
		for (String templateFile : templateFiles) {
			final Path sourceFile = baseDir.resolve(templateFile);
			try {
				final Path targetFile = targetDir.resolve(templateFile);
				final FileTime targetTime;
				if (!Files.isRegularFile(targetFile)) {
					targetTime = null;
				} else {
					targetTime = Files.getLastModifiedTime(targetFile); 
				}
				final FileTime sourceTime = Files.getLastModifiedTime(sourceFile);
				if (targetTime == null  ||  targetTime.compareTo(sourceTime) <= 0) {
					getProject().log("Reading template file: " + sourceFile, Project.MSG_DEBUG);
				} else {
					getProject().log("Ignoring template file, because it appears to be uptodate: " + sourceFile, Project.MSG_VERBOSE);
					try (InputStream in = Files.newInputStream(sourceFile)) {
						final Template template;
						try {
							template = pTemplateCompiler.compile(in, pTemplateSet.getCharset());
						} catch (TemplateParseException tpe) {
							throw new BuildException("Template file " + sourceFile + " was found to be invalid: " + tpe.getMessage(), tpe);
						}
						getProject().log("Applying template, creating output file: " + targetFile);
						final Path targetFileParent = targetFile.getParent();
						if (targetFileParent != null) {
							Files.createDirectories(targetFileParent);
						}
						try (OutputStream out = Files.newOutputStream(targetFile);
								Writer w = new OutputStreamWriter(out, pTemplateSet.getCharset())) {
							template.write(pModel, w);
						}
					}
				}
			} catch (IOException e) {
				throw new BuildException("I/O error while processing template file " + sourceFile + ": " + e.getMessage(), e);
			}
		}
		
	}
}
