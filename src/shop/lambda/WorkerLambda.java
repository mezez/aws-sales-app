package shop.lambda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;


public class WorkerLambda implements RequestHandler<SNSEvent, Object>{

	public Object handleRequest(SNSEvent request, Context context) {
		
		
		String timeStamp = new SimpleDateFormat("yyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
		context.getLogger().log("Invocation started: " + timeStamp);
		String message  = request.getRecords().get(0).getSNS().getMessage();
		context.getLogger().log(message);
		
		if(message != null) {
			
			//PROCESS FILE
		    String messageContent[] = message.split(";");
		    
		    String bucketName = messageContent[0];
		    String fileKey = messageContent[1].trim(); //THROWS FILE NOT FOUND EXCEPTION IN AWS. WHYYYYY. FILE EXISTS SO WHYYY

		    AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
		    try (final S3Object s3Object = s3Client.getObject(bucketName, fileKey);
		        final InputStreamReader streamReader = new InputStreamReader(s3Object.getObjectContent(),
		            StandardCharsets.UTF_8);
		        final BufferedReader reader = new BufferedReader(streamReader)) {

		    	//TODO UPDATE FILE PROCESSING
		      Integer[] values = new Integer[4];
		      values[0] = 0; // total
		      values[1] = 0; // count
		      values[2] = Integer.MAX_VALUE; // min
		      values[3] = Integer.MIN_VALUE; // max

		      reader.lines().forEach(line -> {
		        int value = Integer.parseInt(line);
		        values[0] += value;
		        values[1] += 1;
		        if (values[2] > value) {
		          values[2] = value;
		        }
		        if (values[3] < value) {
		          values[3] = value;
		        }
		      });

		      System.out.println("Count: " + values[1] + " Sum: " + values[0] + " Avg: " + values[0] / (double) values[1]
		          + " Min: " + values[2] + " Max: " + values[3]);
		    } catch (final IOException e) {
		      System.out.println("IOException: " + e.getMessage());
		    }

		    System.out.println("Finished... processing file");
		    
		    //TODO DELETE FILE AFTER USE
			
		}else {
			System.out.println("No message found in the latest request");
			context.getLogger().log("No message found to process ");
		}
		
		timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
		context.getLogger().log("Invocation completed: " + timeStamp);
		
		return null;
	}


}
