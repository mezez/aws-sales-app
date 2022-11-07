package shop;

import software.amazon.awssdk.regions.Region;

public class ShopConstants {

	// EC2
	public static Region region = Region.US_EAST_1;

	// S3
	public static String bucket_name = "mezbucket2";
//	public static String bucket_name = "itsmeitsabucket";

	// SNS
//	public static String topicARN = "arn:aws:sns:us-east-1:057004900367:SalesAppTopic";
	public static String topicARN ="arn:aws:sns:us-east-1:996997097668:SalesAppTopic";
	

	// SQS
	public static String queueURL = "https://sqs.us-east-1.amazonaws.com/057004900367/salesAppQueue";

	// SALES FILES FOLDER PATH
	public static String sales_files_folder = "C:\\Users\\admin\\Downloads\\sales-data\\";
//	public static String sales_files_folder = "C:\\University\\UJM\\2nd year\\Cloud\\tp\\data\\";

}
