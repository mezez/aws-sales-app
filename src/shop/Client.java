package shop;

import java.io.File;
import java.nio.file.Path;

import shop.s3ops.BucketOperations;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public class Client {

	public static void main(String[] args) {
		// TODO
		// get store name and filename from run args
		String storeName = args[0];
		String date = args[1];
		String fileName = date + "-" + storeName + ".csv";

		// use store name and filename to load file to upload
		File file = new File(ShopConstants.sales_files_folder + fileName); // file to upload

		S3Client s3 = S3Client.builder().region(ShopConstants.region).build();

		try {
			boolean bucketExists = BucketOperations.s3BucketExists(s3, ShopConstants.bucket_name);
			System.out.println("Bucket Exists:" + bucketExists);

			if (!bucketExists) {
				// create bucket
				boolean bucketCreated = BucketOperations.createS3Bucket(ShopConstants.bucket_name, s3,
						ShopConstants.region);

				if (bucketCreated) {
					System.out.println("Bucket:" + ShopConstants.bucket_name + " created");
					bucketExists = true;
				} else {
					System.out.println("Bucket:" + ShopConstants.bucket_name + " could not be created");
				}
			}

			if (bucketExists) {
				Path filePath = file.toPath();

				// upload file to bucket
				boolean fileUploaded = BucketOperations.uploadFileToBucket(s3, ShopConstants.bucket_name, fileName,
						filePath);

				if (fileUploaded) {
					System.out.println("file uploaded");

					if (args[2].toString().equals("lambda")) {
						System.out.println("Lambda will be used");
						try {
							SnsClient snsClient = SnsClient.builder().region(ShopConstants.region).build();
							PublishRequest request = PublishRequest.builder()
									.message(storeName + ";" + ShopConstants.bucket_name + ";" + fileName)
									.topicArn(ShopConstants.topicARN).build();
							PublishResponse snsResponse = snsClient.publish(request);
							System.out.println(snsResponse.messageId() + " Notification sent to worker. Status is "
									+ snsResponse.sdkHttpResponse().statusCode());
						} catch (SnsException e) {
							System.err.println(e.awsErrorDetails().errorCode());
							System.exit(1);
						}
					} else if (args[2].toString().equals("ec2")) {
						System.out.println("EC2 will be used");
						SqsClient sqsClient = SqsClient.builder().region(ShopConstants.region).build();
						SendMessageRequest sendRequest = SendMessageRequest.builder().queueUrl(ShopConstants.queueURL)
								.messageBody(ShopConstants.bucket_name + ";" + fileName).build();
						SendMessageResponse sqsResponse = sqsClient.sendMessage(sendRequest);
						System.out.println(sqsResponse.messageId() + " Message sent. Status is "
								+ sqsResponse.sdkHttpResponse().statusCode());
					}
				} else {
					System.out.println("file upload failed");
				}
				// send notification to worker application (Lambda or VM application)
				// Lambda
				// Notification sent
			}

		} catch (Exception e) {
			e.printStackTrace();

		}

	}

}
