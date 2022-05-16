package com.example.kinscopetus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusExecutor;
import io.tus.java.client.TusURLMemoryStore;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URL;

@RequiredArgsConstructor
@SpringBootApplication
public class KinscopeTusApplication {

	private static final String URL_PREFIX_V2_INIT = "https://uploader.kinescope.io/v2/init";
	private static final String projectId = "5f5b06f8-818d-40a7-85dc-61bf576c1fa4";
	private static final String apiToken = "8a749828-6d11-482c-ae1c-48c7b3520ae1";

	private final RestTemplate restTemplate;

	public static void main(String[] args) {
		SpringApplication.run(KinscopeTusApplication.class, args);
	}

	@Bean
	public CommandLineRunner CommandLineRunnerBean() {
		return (args) -> {

			if (args.length > 0) {
				File file = new File("src/main/resources/zov.mp4");

				UploadInfo init = init((int) file.length());


				// Create a new TusClient instance
				TusClient client = new TusClient();

				client.setUploadCreationURL(new URL(init.getUrl()));

				final TusUpload upload = new TusUpload(file);

//		upload.setMetadata(Map.of("id", init.getId()));

				client.enableResuming(new TusURLMemoryStore());

				System.out.println("Starting upload...");

				TusExecutor executor = new TusExecutor() {
					@SneakyThrows
					@Override
					protected void makeAttempt() throws ProtocolException, IOException {
						// First try to resume an upload. If that's not possible we will create a new
						// upload and get a TusUploader in return. This class is responsible for opening
						// a connection to the remote server and doing the uploading.
						//	TusUploader uploader = client.resumeOrCreateUpload(upload);

						// Alternatively, if your tus server does not support the Creation extension
						// and you obtained an upload URL from another service, you can instruct
						// tus-java-client to upload to a specific URL. Please note that this is usually
						// _not_ necessary and only if the tus server does not support the Creation
						// extension. The Vimeo API would be an example where this method is needed.
						TusUploader uploader = client.beginOrResumeUploadFromURL(upload, new URL(init.getUrl()));

						// Upload the file in chunks of 1KB sizes.
						uploader.setChunkSize(1024);

						// Upload the file as long as data is available. Once the
						// file has been fully uploaded the method will return -1
						do {
							// Calculate the progress using the total size of the uploading file and
							// the current offset.
							long totalBytes = upload.getSize();
							long bytesUploaded = uploader.getOffset();
							double progress = (double) bytesUploaded / totalBytes * 100;

							System.out.printf("Upload at %06.2f%%.\n", progress);
						} while (uploader.uploadChunk() > -1);

						// Allow the HTTP connection to be closed and cleaned up
						uploader.finish();

						System.out.println("Upload finished.");
						System.out.format("Upload available at: %s", uploader.getUploadURL().toString());
					}
				};
				executor.makeAttempts();
			}

		};

	}

	private UploadInfo init(int fileSize) {
		InitRequest requestBody = InitRequest.builder()
				.description("desc")
				.filename("name")
				.title("name")
				.type("video")
				.projectId(projectId)
				.filesize(fileSize)
				.build();
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + apiToken);

		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<InitRequest> request = new HttpEntity<>(
				requestBody,
				headers
		);

		ResponseEntity<InitResponseWrapper> response = restTemplate.exchange(
				URL_PREFIX_V2_INIT,
				HttpMethod.POST,
				request,
				InitResponseWrapper.class
		);

		return response.getBody().getData();
	}

	@lombok.Value
	@Builder
	private static class InitRequest {
		// обязательный параметр, нужен, чтобы показывать прогресс загрузки в дашборде
		Integer filesize;
		String type;
		String title;
		@JsonProperty("parent_id")
		String projectId;
		String filename;
		String description;
	}

	@lombok.Value
	private static class InitResponseWrapper {
		UploadInfo data;

		@JsonCreator
		public InitResponseWrapper(@JsonProperty("data") UploadInfo data) {
			this.data = data;
		}
	}

	@Value
	private static class UploadInfo {
		String token;
		String url;

		@JsonCreator
		public UploadInfo(@JsonProperty("id") String id,
						  @JsonProperty("endpoint") String endpoint) {
			this.token = id;
			this.url = endpoint;
		}
	}

}