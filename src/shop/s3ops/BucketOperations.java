package shop.s3ops;

import java.nio.file.Path;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class BucketOperations {
	
	public static void main(String[] args) {

	}
	
	public static boolean s3BucketExists(S3Client s3Client, String bucketName) {
		HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
	            .bucket(bucketName)
	            .build();

	    try {
	        s3Client.headBucket(headBucketRequest);
	        return true;
	    } catch (NoSuchBucketException e) {
	        return false;
	    }
	}
	
	public static boolean createS3Bucket(String bucketName, S3Client s3Client, Region region ) {
		try {
			CreateBucketRequest createBucketRequest = CreateBucketRequest
				    .builder()
				    .bucket(bucketName)
				    .createBucketConfiguration(CreateBucketConfiguration.builder()
				        .locationConstraint(region.id())
				        .build())
				    .build();
	
				s3Client.createBucket(createBucketRequest);
				System.out.println("Bucket" + bucketName + "created");
				return true;
		}catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	
	public static boolean uploadFileToBucket(S3Client s3, String bucketName, String bucketKey, Path filePath) {
		try{
			s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(bucketKey)
		        .build(), filePath);
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

}
