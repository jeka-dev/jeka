package org.jake;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsString;

class ImportAnnotationProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (final Element element : roundEnv.getElementsAnnotatedWith(JakeImport.class)) {
			final TypeElement typeElement = (TypeElement) element;
			final JakeImport jakeImport = typeElement.getAnnotation(JakeImport.class);
			try {
				final FileObject fileObject =  processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
						typeElement.getQualifiedName().toString() + ".properties");
				if (jakeImport.value().length > 0) {
					fileObject.openWriter().append("imports="+stringsToString(jakeImport.value())).append("\n");
					final JakeImportRepo importRepo = typeElement.getAnnotation(JakeImportRepo.class);
					if (importRepo != null && importRepo.value().length > 0) {
						fileObject.openWriter().append("repos="+stringsToString(importRepo.value()));
					}
				}


			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}

		return false;
	}

	private static String stringsToString(String[] imports) {
		return JakeUtilsString.join(imports, "|");
	}

	private static List<String> stringToStrings(String string) {
		return Arrays.asList(string.split("|"));
	}

	public static List<String> getImports(Properties props) {
		final String property = props.getProperty("imports");
		if (property == null) {
			return Collections.emptyList();
		}
		return stringToStrings(props.getProperty("imports"));
	}

	public static List<String> getRepos(Properties props) {
		final String property = props.getProperty("repos");
		if (property == null) {
			return Collections.emptyList();
		}
		return stringToStrings(props.getProperty("repos"));
	}

	public static final class Result {

		public final List<String> imports;

		public final List<String> repos;

		private Result(List<String> imports, List<String> repos) {
			super();
			this.imports = imports;
			this.repos = repos;
		}

	}

	@SuppressWarnings("unchecked")
	public static Result get(Class<?> clazz, File folder) {
		final File propsFile = new File(folder, clazz.getName()+".properties");
		if (!propsFile.exists()) {
			return new Result(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
		}
		final Properties properties = JakeUtilsFile.readPropertyFile(propsFile);
		return new Result(getImports(properties), getRepos(properties));
	}





}
