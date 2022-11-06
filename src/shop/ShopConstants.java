package shop;

import software.amazon.awssdk.regions.Region;

public class ShopConstants {

	// EC2
	public static Region region = Region.US_EAST_1;
	public static String amiId = "ami-05fa00d4c63e32376";

	// S3
//	public static String bucket_name = "mezbucket2";
	public static String bucket_name = "itsmeitsabucket";

	// SNS
	public static String topicARN = "arn:aws:sns:us-east-1:057004900367:SalesAppTopic";
//	public static String topicARN ="arn:aws:sns:us-east-1:996997097668:SalesAppTopic";

	// SQS
	public static String queueURL = "https://sqs.us-east-1.amazonaws.com/057004900367/salesAppQueue";

//	public static String sales_files_folder = "C:\\Users\\admin\\Downloads\\sales-data\\";
	public static String sales_files_folder = "C:\\University\\UJM\\2nd year\\Cloud\\tp\\data\\";

	public static String STORE_1 = "store1";
	public static String STORE_2 = "store2";
	public static String STORE_3 = "store3";
	public static String STORE_4 = "store4";
	public static String STORE_5 = "store5";
	public static String STORE_6 = "store6";
	public static String STORE_7 = "store7";
	public static String STORE_8 = "store8";
	public static String STORE_9 = "store9";
	public static String STORE_10 = "store10";

}
