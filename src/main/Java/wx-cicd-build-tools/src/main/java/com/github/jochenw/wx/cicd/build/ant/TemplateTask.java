package com.github.jochenw.wx.cicd.build.ant;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.afw.di.api.ComponentFactoryBuilder;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.wx.cicd.build.template.SimpleTemplateCompiler;
import com.github.jochenw.wx.cicd.build.template.Template;
import com.github.jochenw.wx.cicd.build.template.Template.Model;
import com.github.jochenw.wx.cicd.build.template.TemplateCompiler;
import com.github.jochenw.wx.cicd.build.template.TemplateParseException;
import com.github.jochenw.wx.cicd.build.template.TemplateUtils;

public class TemplateTask extends Task {
	public static class PropertyFile {
		private Path file;

		public void addText(String pText) {
			setFile(pText);
		}

		public Path getFile() {
			return file;
		}
		public void setFile(String pFile) {
			if (pFile == null) {
				throw new BuildException("The file attribute of a propertyFile element must not be null.");
			}
			if (pFile.length() == 0) {
				throw new BuildException("The file attribute of a propertyFile element must not be empty.");
			}
			final Path p = Paths.get(pFile);
			if (!Files.isRegularFile(p)) {
				throw new BuildException("The file attribute of a propertyFile element must refer to an existing file:" + p);
			}
			file = p;
		}
	}
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


	private final List<PropertyFile> propertyFiles = new ArrayList<>();
	private final List<TemplateSet> templateSets = new ArrayList<>();
	private Path outputDir;
	private String startToken, endToken;
	private boolean forced;

	public String getStartToken() { return Objects.notNull(startToken, "#{"); }
	public void setStartToken(String pStartToken) { startToken = pStartToken; }
	public String getEndToken() { return Objects.notNull(endToken, "}"); }

	public void setEndToken(String endToken) {
		this.endToken = endToken;
	}

	public List<PropertyFile> getPropertyFiles() {
		return propertyFiles;
	}

	public PropertyFile createPropertyFile() {
		final PropertyFile pf = new PropertyFile();
		propertyFiles.add(pf);
		return pf;
	}

	public List<TemplateSet> getTemplateSets() {
		return templateSets;
	}

	public TemplateSet createTemplateSet() {
		final TemplateSet ts = new TemplateSet();
		templateSets.add(ts);
		return ts;
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
		final List<FileTime> propertyFileTimes = new ArrayList<FileTime>();
		final Properties props = getProperties(propertyFileTimes::add);
		final Model model = Model.of(props);
		final IComponentFactory cf = new ComponentFactoryBuilder()
				.module(SimpleTemplateCompiler.MODULE)
				.build();
		templateSets.forEach((ts) -> {
			processTemplateSet(cf, propertyFileTimes, ts, outDir, model);
		});
	}

	protected Properties getProperties(Consumer<FileTime> pTimeConsumer) {
		final Properties props = new Properties();
		props.putAll(System.getProperties());
		getPropertyFiles().forEach((pf) -> {
			final Path p = pf.getFile();
			if (p == null) {
				throw new BuildException("A nested propertyFile element must have its file attribute set.");
			}
			try (InputStream in = Files.newInputStream(p)) {
				final Properties pr = new Properties();
				pr.load(in);
				props.putAll(pr);
				pTimeConsumer.accept(Files.getLastModifiedTime(p));
			} catch (IOException e) {
				throw new BuildException("Failed to read property file " + p + ": " + e.getMessage(), e);
			}
		});
		return props;
	}

	protected void processTemplateSet(IComponentFactory pComponentFactory, List<FileTime> pPropertyFileTimes,
			                          TemplateSet pTemplateSet, Path pOutDir, Model pModel) {
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
		final TemplateUtils templateUtils = pComponentFactory.requireInstance(TemplateUtils.class);
		for (String templateFile : templateFiles) {
			final Path sourceFile = baseDir.resolve(templateFile);
			final SimpleTemplateCompiler stc = new SimpleTemplateCompiler(sourceFile.toString(), templateUtils, getStartToken(), getEndToken());
			try {
				final Path targetFile = targetDir.resolve(templateFile);
				final boolean uptodate = isUptodate(pPropertyFileTimes, sourceFile, targetFile);
				if (uptodate) {
					getProject().log("Ignoring template file, because it appears to be uptodate: " + sourceFile, Project.MSG_VERBOSE);
				} else {
					getProject().log("Reading template file: " + sourceFile, Project.MSG_DEBUG);
					try (InputStream in = Files.newInputStream(sourceFile)) {
						final Template template;
						try {
							template = stc.compile(in, pTemplateSet.getCharset());
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

	protected boolean isUptodate(List<FileTime> pPropertyFileTimes, Path pSourceFile, Path pTargetFile) {
		final Function<Path,FileTime> timeProvider = (p) -> {
			try {
				return Files.getLastModifiedTime(p);
			} catch (FileNotFoundException|NoSuchFileException e) {
				return null;
			} catch (IOException e) {
				throw new BuildException("Unable to get file time for " + p + ": " + e.getMessage(), e);
			}
		};
		final FileTime sourceTime = timeProvider.apply(pSourceFile);
		final FileTime targetTime = timeProvider.apply(pTargetFile);
		final boolean uptodate = isUptodate(sourceTime, targetTime);
		if (!uptodate) {
			return false;
		}
		for (FileTime ft : pPropertyFileTimes) {
			if (!isUptodate(ft, targetTime)) {
				return false;
			}
		}
		return true;
	}

	protected boolean isUptodate(FileTime pSourceTime, FileTime pTargetTime) {
		if (pTargetTime == null) {
			return false;
		}
		return pSourceTime.compareTo(pTargetTime) <= 0;
	}
}
