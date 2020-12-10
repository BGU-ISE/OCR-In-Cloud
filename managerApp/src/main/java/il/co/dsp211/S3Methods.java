package il.co.dsp211;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.stream.Stream;

public class S3Methods implements AutoCloseable
{
	private final S3Client s3Client = S3Client.builder()
			.region(Region.US_EAST_1)
			.build();

	public void createBucket(String bucketName)
	{
		System.out.println("Creating bucket...");

		s3Client.createBucket(CreateBucketRequest.builder()
				.bucket(bucketName)
				.createBucketConfiguration(CreateBucketConfiguration.builder()
						.build())
				.build());

		s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
				.bucket(bucketName)
				.build())
				.matched()
				.exception()
				.ifPresent(System.out::println);
		System.out.println("Bucket \"" + bucketName + "\" was created successfully");
	}

	/**
	 * Maybe more efficient on the network...<br>
	 * check about limit of 1000 objects: no need to worry, {@link S3Client#listObjectsV2(ListObjectsV2Request)} returns up to 1000 objects
	 */
	public void deleteBucketBatch(String bucketName)
	{
		System.out.println("Deleting bucket Batch...");

		// To delete a bucket, all the objects in the bucket must be deleted first
		ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
				.bucket(bucketName)
				.build();
		ListObjectsV2Response listObjectsV2Response;

		do
		{
			listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
			if (!listObjectsV2Response.contents().isEmpty())
			{
				deleteObjects(bucketName,
						listObjectsV2Response.contents().stream()
								.map(S3Object::key));

				listObjectsV2Request = ListObjectsV2Request.builder()
						.bucket(bucketName)
						.continuationToken(listObjectsV2Response.nextContinuationToken())
						.build();
			}
		} while (listObjectsV2Response.isTruncated());

		s3Client.deleteBucket(DeleteBucketRequest.builder()
				.bucket(bucketName)
				.build());

		System.out.println("Bucket \"" + bucketName + "\" was deleted successfully");
	}

	public void deleteObjects(String bucketName, String... names)
	{
		if (names.length == 0)
			return;
		if (names.length > 1000)
			throw new IllegalArgumentException("There should be 1000 names or less, got " + names.length);

		deleteObjects(bucketName, Stream.of(names));
	}

	private void deleteObjects(String bucketName, Stream<String> names)
	{
		s3Client.deleteObjects(DeleteObjectsRequest.builder()
				.bucket(bucketName)
				.delete(Delete.builder()
						.quiet(true)
						.objects(names.map(name -> ObjectIdentifier.builder()
								.key(name)
								.build())
								.toArray(ObjectIdentifier[]::new))
						.build())
				.build());
	}

	public void uploadFileToS3Bucket(String bucketName, String pathString)
	{
		System.out.println("Upload file to S3 bucket...");

		Path path = Path.of(pathString);
		s3Client.putObject(PutObjectRequest.builder()
				.bucket(bucketName)
				.key(path.getFileName().toString())
				.build(), path);

		System.out.println("File \"" + path.getFileName() + "\" was uploaded successfully");
	}

	public void uploadStringToS3Bucket(String bucketName, String key, String value)
	{
		System.out.println("Upload file to S3 bucket \"" + bucketName + "\"...");

		s3Client.putObject(PutObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build(), RequestBody.fromString(value));

		System.out.println("text was uploaded successfully");
	}

	public void downloadFileFromS3Bucket(String bucketName, String key, String outputPathString)
	{
		System.out.println("Getting object " + key + " and saving it to " + outputPathString + " ...");

		s3Client.getObject(GetObjectRequest.builder()
						.bucket(bucketName)
						.key(key)
						.build(),
				Path.of(outputPathString));

		System.out.println("Object downloaded and saved");
	}

	public BufferedReader readObjectToBufferedReader(String bucketName, String key)
	{
		System.out.println("Getting object " + key + "...");

		return new BufferedReader(new InputStreamReader(s3Client.getObject(GetObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build())));
	}

	public String readObjectToString(String bucketName, String key)
	{
		System.out.println("Getting object " + key + "...");

		ResponseBytes<GetObjectResponse> responseInputStream = s3Client.getObjectAsBytes(GetObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build());

		System.out.println("Object received");
		return responseInputStream.asUtf8String();
	}

	public void uploadLongToS3Bucket(String bucketName, String key, long value)
	{
		System.out.println("Upload " + value + " to S3 bucket \"" + bucketName + "\"...");

		s3Client.putObject(PutObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build(), RequestBody.fromByteBuffer(ByteBuffer.allocate(Long.BYTES).putLong(value)));

		System.out.println("text was uploaded successfully");
	}

	public long readLongToS3Bucket(String bucketName, String key)
	{
		System.out.println("Reading long from S3 bucket \"" + bucketName + "\" and key " + key + "...");

		return s3Client.getObjectAsBytes(GetObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build())
				.asByteBuffer()
				.getLong();
	}

	@Override
	public void close()
	{
		s3Client.close();
	}
}