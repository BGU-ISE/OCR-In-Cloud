package il.co.dsp211;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class S3Methods
{

	private final static Region region = Region.US_EAST_1;

	public static void createBucket(S3Client s3Client, String bucketName)
	{
		System.out.println("Creating bucket...");

		s3Client.createBucket(CreateBucketRequest.builder()
				.bucket(bucketName)
				.createBucketConfiguration(CreateBucketConfiguration.builder()
						.locationConstraint(region.id())
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

	public static void deleteBucket(S3Client s3Client, String bucketName)
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
	 * @param s3Client
	 * @param bucketName
	 */
	public static void deleteBucketBatch(S3Client s3Client, String bucketName)
	{
		System.out.println("Deleting bucket...");

		for (ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
				.bucket(bucketName)
				.build());
		     listObjectsV2Response.isTruncated();
		     listObjectsV2Response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
				     .bucket(bucketName)
				     .continuationToken(listObjectsV2Response.nextContinuationToken())
				     .build()))
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

		s3Client.deleteBucket(DeleteBucketRequest.builder()
				.bucket(bucketName)
				.build());
		System.out.println("Bucket \"" + bucketName + "\" was deleted successfully");

	}

	public static void uploadFileToS3Bucket(S3Client s3Client, String bucketName, String pathString)
	{
		System.out.println("Upload file to S3 bucket...");

		Path path = Path.of(pathString);
		s3Client.putObject(PutObjectRequest.builder()
				.bucket(bucketName)
				.key(path.getFileName().toString())
				.build(), path);
		System.out.println("File \"" + path.getFileName() + "\" was uploaded successfully");
	}

	public static void downloadFileFromS3Bucket(S3Client s3Client, String bucketName, String outputFileName)
	{
		// TODO: not finished
		ResponseInputStream<GetObjectResponse> s3ObjectResponse = s3Client.getObject(GetObjectRequest.builder()
				.bucket(bucketName)
				.key(outputFileName)
				.build());

		BufferedReader reader = new BufferedReader(new InputStreamReader(s3ObjectResponse));
	}
}
