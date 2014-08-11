import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;
import org.kurento.commons.ConfigFilePropertyHolder;
import org.kurento.commons.PropertiesManager;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class ConfigFileTest {

	@Test
	public void testSimpleProperty() throws JsonSyntaxException,
			JsonIOException, IOException, URISyntaxException {

		ConfigFilePropertyHolder
				.configurePropertiesFromConfigFile(getPathInClasspath("/test.conf.json"));

		assertThat(PropertiesManager.getProperty("prop1"), is("value1"));
		assertThat(PropertiesManager.getProperty("prop2.prop1"), is("xxx"));
		assertThat(PropertiesManager.getProperty("nonExistingProp3"),
				is(nullValue()));
		assertThat(PropertiesManager.getProperty("nonExistingProp3.prop1"),
				is(nullValue()));

		System.setProperty("nonExistingProp4", "kkkk");

		assertThat(PropertiesManager.getProperty("nonExistingProp4"),
				is("kkkk"));

		System.setProperty("prop1", "kkkk");

		assertThat(PropertiesManager.getProperty("prop1"), is("kkkk"));

	}

	public static Path getPathInClasspath(String resource) throws IOException,
			URISyntaxException {

		URL url = ConfigFileTest.class.getResource(resource);

		if (url == null) {
			return null;
		}

		URI uri = url.toURI();

		String scheme = uri.getScheme();
		if (scheme.equals("file")) {
			return Paths.get(uri);
		}

		String s = uri.toString();
		int separator = s.indexOf("!/");
		String entryName = s.substring(separator + 2);
		URI fileURI = URI.create(s.substring(0, separator));

		FileSystem fs = FileSystems.newFileSystem(fileURI,
				Collections.<String, Object> emptyMap());
		return fs.getPath(entryName);
	}
}
