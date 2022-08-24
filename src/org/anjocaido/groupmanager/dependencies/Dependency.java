package org.anjocaido.groupmanager.dependencies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;

import org.anjocaido.groupmanager.utils.InjectorUtils;
import org.bukkit.plugin.Plugin;
import org.xml.sax.SAXException;

/**
 * Single Thread dependency management.
 * 
 * Thanks to https://www.spigotmc.org/resources/dependencymanager.62275/
 * DotRar for the original idea.
 *
 */
public abstract class Dependency {

	private Plugin plugin;

	public Dependency(Plugin plugin) {

		this.plugin = plugin;
	}

	protected abstract URL buildUrl() throws IOException, ParserConfigurationException, SAXException;

	public void load(Consumer<String> onComplete, Consumer<Exception> onError) {

		try {

			File cacheFolder = new File("libs");
			if (!cacheFolder.exists())
				cacheFolder.mkdir();

			Consumer<File> inject = (f) -> {
				try {
					InjectorUtils.INSTANCE.loadJar(plugin, f);
					onComplete.accept(f.getName());
				} catch (Exception ex) {
					onError.accept(ex);
				}
			};

			File cached = new File(cacheFolder, getLocalName());
			if (cached.length() == 0) {
				cached.delete();
			}

			if (!cached.exists()) {
				cached.createNewFile();
				download(plugin, cached, inject, onError);
			} else {
				inject.accept(cached);
			}

		} catch (Exception ex) {
			onError.accept(ex);
		}
	}

	protected abstract String getLocalName();

	private void download(Plugin plugin, File downloadDest, Consumer<File> inject, Consumer<Exception> onError) {

		try {

			URL url = buildUrl();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.44");

			InputStream stream = conn.getInputStream();
			ReadableByteChannel byteChan = Channels.newChannel(stream);

			FileOutputStream fos = new FileOutputStream(downloadDest);
			FileChannel fileChan = fos.getChannel();

			fileChan.transferFrom(byteChan, 0, Long.MAX_VALUE);

			fos.close();
			stream.close();

			inject.accept(downloadDest);

		} catch (IOException | ParserConfigurationException | SAXException ex) {
			onError.accept(ex);
		}
	}
}
