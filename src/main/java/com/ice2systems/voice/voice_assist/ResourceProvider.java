package com.ice2systems.voice.voice_assist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

public class ResourceProvider {

	private final static String descriptorName = "voice.ini";
	private final static String URL = "http://<provide host:port>/web-provider/resource";
	
	private final String workingDir;
	private final String title;
	private final String descriptorPath;
	
	enum ResourceType {
		descriptor, media;
	}
	
	public ResourceProvider(final String workingDir, final String title) {	
		
		if( workingDir == null || workingDir == "") {
			throw new IllegalArgumentException("working directory path not provided");
		}
		
		if( Files.notExists(Paths.get(workingDir)) ) {
			throw new IllegalArgumentException("working directory does not exist");
		}

		if( title == null || title == "") {
			throw new IllegalArgumentException("title not provided");
		}
		
		this.workingDir = workingDir;
		this.title = title;
		descriptorPath = workingDir + "/" + title + "/" + descriptorName;
		
		if( !Files.exists(Paths.get(descriptorPath), LinkOption.NOFOLLOW_LINKS)) {
			try {
				Files.createDirectories(Paths.get(workingDir + "/" + title));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private String buildURL(final ResourceType resourceType, final String name) {
		switch(resourceType) {
			case descriptor:
				return String.format("%s?title=%s&resourceType=descriptor", URL, title);
			case media:
				return String.format("%s?title=%s&resourceType=media&name=%s", URL, title, name);	
		}
		throw new RuntimeException("unsupported resourceType="+resourceType);
	}
	
	public boolean downloadDescriptor() throws ClientProtocolException, IOException {
		return downloadObject(ResourceType.descriptor, descriptorName);
	}
	
	public boolean downloadMedia(final String name) throws ClientProtocolException, IOException {
		return downloadObject(ResourceType.media, name);
	}
	
	private boolean downloadObject(final ResourceType resourceType, final String name) throws ClientProtocolException, IOException {
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("yoksel", "moksel");
		provider.setCredentials(AuthScope.ANY, credentials);
		  
		HttpClient client = HttpClientBuilder.create()
		  .setDefaultCredentialsProvider(provider)
		  .build();
		
		String url = buildURL(resourceType, name);
		
		HttpResponse response = client.execute(new HttpGet(url));
		int statusCode = response.getStatusLine().getStatusCode();		
		
		if(statusCode != HttpStatus.SC_OK) {
			throw new RuntimeException(String.format("failure downloading object=%s statusCode=%d", name, statusCode));
		}
		
		HttpEntity entity = response.getEntity();
		
		if (entity == null) {
			throw new RuntimeException(String.format("failure downloading object=%s : Empty content.", name));
		}
		
		InputStream is = entity.getContent();
		String filePath = workingDir + "/" + title + "/" + name;
		FileOutputStream fos = new FileOutputStream(new File(filePath));
		int inByte;
		while((inByte = is.read()) != -1) {
			fos.write(inByte);
		}
		is.close();
		fos.close();
		
		return true;
	}
	
	public Reader getDescriptor() throws IOException {
		if( !Files.exists(Paths.get(descriptorPath), LinkOption.NOFOLLOW_LINKS) ) {
			downloadDescriptor();
		}
		
		return new FileReader(descriptorPath);
	}
	
	public InputStream getMedia(final String name) throws IOException {
		if( !Files.exists(Paths.get(workingDir + "/" + title + "/" +name), LinkOption.NOFOLLOW_LINKS) ) {			
			downloadMedia(name);
		}
		
		return new FileInputStream(workingDir + "/" + title + "/" +name);
	}	
}
