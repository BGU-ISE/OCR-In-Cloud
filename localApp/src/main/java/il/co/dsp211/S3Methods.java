package il.co.dsp211;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import java.io.IOException;
import java.nio.file.Path;

public class S3Methods implements SdkAutoCloseable
{
	//	private final static Region region = Region.US_EAST_1;
	private final S3Client s3Client = S3Client.builder()
			.region(Region.US_EAST_1)
			.build();
	private final String bucketName = "bucky" + System.currentTimeMillis();

	public void createBucket()
	{
		System.out.println("Creating bucket...");

		s3Client.createBucket(CreateBucketRequest.builder()
				.bucket(bucketName)
				.createBucketConfiguration(CreateBucketConfiguration.builder()
//						.locationConstraint(region.id())
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

	public void deleteBucket()
	{
		System.out.println("Deleting bucket...");

		// To delete a bucket, all the objects in the bucket must be deleted first
		ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
				.bucket(bucketName)
				.build();
		ListObjectsV2Response listObjectsV2Response;

		do
		{
			listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
			for (S3Object s3Object : listObjectsV2Response.contents())
			{
				s3Client.deleteObject(DeleteObjectRequest.builder()
						.bucket(bucketName)
						.key(s3Object.key())
						.build());
			}

			listObjectsV2Request = ListObjectsV2Request.builder()
					.bucket(bucketName)
					.continuationToken(listObjectsV2Response.nextContinuationToken())
					.build();

		} while (listObjectsV2Response.isTruncated());

		s3Client.deleteBucket(DeleteBucketRequest.builder()
				.bucket(bucketName)
				.build());
		System.out.println("Bucket \"" + bucketName + "\" was deleted successfully");
	}

	/**
	 * Maybe more efficient on the network...<br>
	 * check about limit of 1000 objects: no need to worry, {@link S3Client#listObjectsV2(ListObjectsV2Request)} returns up to 1000 objects
	 *
	 */
	public void deleteBucketBatch()
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
				s3Client.deleteObjects(DeleteObjectsRequest.builder()
						.bucket(bucketName)
						.delete(Delete.builder()
								.quiet(true)
								.objects(listObjectsV2Response.contents().stream()
										.map(s3Object -> ObjectIdentifier.builder()
												.key(s3Object.key())
												.build())
										.toArray(ObjectIdentifier[]::new))
								.build())
						.build());

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

	public void uploadFileToS3Bucket(String pathString)
	{
		System.out.println("Upload file to S3 bucket...");

		Path path = Path.of(pathString);
		s3Client.putObject(PutObjectRequest.builder()
				.bucket(bucketName)
				.key(path.getFileName().toString())
				.build(), path);

		System.out.println("File \"" + path.getFileName() + "\" was uploaded successfully");
	}

	public void downloadFileFromS3Bucket(String key, String outputPathString)
	{
		System.out.println("Getting object " + key + " and saving it to " + outputPathString + " ...");

		s3Client.getObject(GetObjectRequest.builder()
						.bucket(bucketName)
						.key(key)
						.build(),
				Path.of(outputPathString));

		System.out.println("Object downloaded and saved");
	}

	public String readObjectToString(String key) throws IOException
	{
		System.out.println("Getting object " + key + "...");

		try (ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(GetObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build()))
		{
//			System.out.println(responseInputStream.response().contentEncoding());
			return new String(responseInputStream.readAllBytes());
		}
		finally
		{
			System.out.println("Object received");
		}
	}

	@Override
	public void close()
	{
		s3Client.close();
	}
}
