package shop.s3ops;

import java.io.File;
import java.nio.file.Path;
import java.util.Random;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class BucketOperations {
	static String bucketName = "mezbucket1";
	
	static Random rand = new Random();
	static String bucketKey = "mezkey-values20386.csv";
	
	public static void main(String[] args) {
		Region region = MyEc2.region;
		
		File file = new File("C:\\Users\\admin\\Downloads\\values.csv"); //file to upload

		S3Client s3 = S3Client.builder().region(region).build();
		
		try {
			boolean bucketExists = s3BucketExists(s3,bucketName);
			System.out.println("Bucket Exists:" + bucketExists);
			
			if(!bucketExists) {
				//create bucket
				boolean bucketCreated = createS3Bucket(bucketName, s3, region );
				
				if(bucketCreated) {
					System.out.println("Bucket:"+ bucketName + " created");
					bucketExists = true;
				}else {
					System.out.println("Bucket:"+ bucketName + " could not be created");
				}
			}
			
			if(bucketExists) {
				Path filePath = file.toPath();
				
				//upload file to bucket
				boolean bucketUploaded = uploadFileToBucket(s3, bucketName, bucketKey, filePath);
				
				if(bucketUploaded) {
					System.out.println("file uploaded");
					
				}else {
					System.out.println("file upload failed");
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
			
		}

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
