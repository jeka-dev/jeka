package org.jake;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;

import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsString;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes({"org.jake.JakeImport", "org.jake.JakeImportRepo"})
public class JakeImportAnnotationProcessor extends AbstractProcessor {

	private static final String FILE_NAME = "jakeImports.properties";

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.isEmpty()) {
			return true;
		}
		final List<String> imports = new LinkedList<String>();
		final List<String> repos = new LinkedList<String>();
		for (final Element element : roundEnv.getElementsAnnotatedWith(JakeImport.class)) {
			final TypeElement typeElement = (TypeElement) element;
			final JakeImport jakeImport = typeElement.getAnnotation(JakeImport.class);
			imports.addAll(Arrays.asList(jakeImport.value()));
		}
		for (final Element element : roundEnv.getElementsAnnotatedWith(JakeImportRepo.class)) {
			final TypeElement typeElement = (TypeElement) element;
			final JakeImportRepo importRepo = typeElement.getAnnotation(JakeImportRepo.class);
			repos.addAll(Arrays.asList(importRepo.value()));
		}
		try {
			final OutputStream outputStream =  processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
					FILE_NAME).openOutputStream();
			new ImportResult(imports, repos).storeAsPropertyFile(outputStream);
			JakeUtilsIO.closeQuietly(outputStream);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return true;
	}



	public static final class ImportResult {

		static ImportResult from(File propsFile) {
			final Properties properties = JakeUtilsFile.readPropertyFile(propsFile);
			return new ImportResult(getImports(properties), getRepos(properties));
		}

		public final List<String> imports;

		public final List<String> repos;

		private ImportResult(List<String> imports, List<String> repos) {
			super();
			this.imports = Collections.unmodifiableList(imports);
			this.repos = Collections.unmodifiableList(repos);
		}

		void storeAsPropertyFile(OutputStream stream) {
			final Properties properties = new Properties();
			properties.put("imports", stringsToString(imports.toArray(new String[0])));
			properties.put("repos", stringsToString(repos.toArray(new String[0])));
			try {
				properties.store(stream, "");
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}

		private static String stringsToString(String[] imports) {
			return JakeUtilsString.join(imports, "|");
		}

		private static List<String> getImports(Properties props) {
			final String property = props.getProperty("imports");
			if (property == null) {
				return Collections.emptyList();
			}
			return stringToStrings(props.getProperty("imports"));
		}

		private static List<String> getRepos(Properties props) {
			final String property = props.getProperty("repos");
			if (property == null) {
				return Collections.emptyList();
			}
			return stringToStrings(props.getProperty("repos"));
		}

		private static List<String> stringToStrings(String string) {
			return Arrays.asList(string.split("|"));
		}

	}


	public static ImportResult getResult(File outputFolder) {
		final File file = new File(outputFolder, FILE_NAME);
		return ImportResult.from(file);
	}

}
