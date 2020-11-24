package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Paths;

public class S3Methods {

    private final static Region region = Region.US_EAST_1;

    public static void createBucket(S3Client s3Client, String bucketName)
    {
        try {
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
        } catch (Exception e) {
            System.out.println("ERROR! got an exception while creating the S3 bucket.");
            throw e;
        }
    }

    public static void deleteBucket(S3Client s3Client, String bucketName)
    {
        try {
            System.out.println("Deleting bucket...");

            // To delete a bucket, all the objects in the bucket must be deleted first
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName).build();
            ListObjectsV2Response listObjectsV2Response;

            do {
                listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
                for (S3Object s3Object : listObjectsV2Response.contents()) {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Object.key())
                            .build());
                }

                listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName)
                        .continuationToken(listObjectsV2Response.nextContinuationToken())
                        .build();

            } while(listObjectsV2Response.isTruncated());

            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
            s3Client.deleteBucket(deleteBucketRequest);
            System.out.println("Bucket \"" + bucketName + "\" was deleted successfully");

        } catch (Exception e) {
            System.out.println("ERROR! got an exception while deleting the S3 bucket.");
            throw e;
        }
    }

    public static void uploadFileToS3Bucket(S3Client s3Client, String bucketName, File file, String localAppID)
    {
        try {
            System.out.println("Upload file to S3 bucket...");
            String key = localAppID + "_" + file.getName();

            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.putObject(objectRequest, Paths.get(file.getName()));
            System.out.println("File \"" + file.getName() + "\" was uploaded successfully");
        } catch (Exception e) {
            System.out.println("ERROR! got an exception while uploading the file to the S3 bucket");
            throw e;
        }
    }
}
