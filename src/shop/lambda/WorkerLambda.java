package shop.lambda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import shop.Product;
import shop.ShopConstants;
import shop.s3ops.BucketOperations;

public class WorkerLambda implements RequestHandler<SNSEvent, Object> {

	public Object handleRequest(SNSEvent request, Context context) {

		String timeStamp = new SimpleDateFormat("yyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
		context.getLogger().log("Invocation started: " + timeStamp);
		String message = request.getRecords().get(0).getSNS().getMessage();
		context.getLogger().log(message);

		if (message != null) {
			S3Client s3 = S3Client.builder().region(ShopConstants.region).build();

			// PROCESS FILE
			String messageContent[] = message.split(";");

			String storeName = messageContent[0];
			String bucketName = messageContent[1];
			String fileKey = messageContent[2].trim(); // THROWS FILE NOT FOUND EXCEPTION IN AWS. WHYYYYY. FILE EXISTS
														// SO WHYYY

			AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
			try (final S3Object s3Object = s3Client.getObject(bucketName, fileKey);
					final InputStreamReader streamReader = new InputStreamReader(s3Object.getObjectContent(),
							StandardCharsets.UTF_8);
					final BufferedReader reader = new BufferedReader(streamReader)) {

				// TODO UPDATE FILE PROCESSING
				// by store
				double totalProfit = 0.0;
				int count = 0;

				String line = "";
				while ((line = reader.readLine()) != null) {
					// skip header line
					if (count > 0) {
						// use semicolon as separator
						String[] cols = line.split(";");
						totalProfit += Integer.parseInt(cols[6]);
					}
					count++;

				}

				// by product
				count = 0;
				ArrayList<Product> products = new ArrayList<Product>();

				while ((line = reader.readLine()) != null) {
					// skip header line
					if (count > 0) {
						// use semicolon as separator
						String[] cols = line.split(";");

						// create new product if it does not already exist in array
						String nameToMatch = cols[2];
//					    Optional<String> productExistsBasedOnProp = products.stream().map(Product::getName).findFirst();

//					    Optional<Object> existingProduct = Optional.of(products.stream()
//					    	    .filter(product -> product.getName().equals(nameToMatch)) // Returns a stream consisting of the elements of this stream that match the given predicate.
//					    	    .findFirst().get()) ;// Returns an Optional describing the first element of this stream, or an empty Optional if the stream is empty. If the stream has no encounter order, then any element may be returned.
//					    	    .map(product -> product.incrementAll(product)); // If a value is present, apply the provided mapping function to it, and if the result is non-null, return an Optional describing the result. Otherwise return an empty Optional.
						try {
							int index = IntStream.range(0, products.size())
									.filter(i -> nameToMatch.equals(products.get(i).getName())).findFirst().getAsInt();

							// update existing
							Product existingProduct = products.get(index);

							Product newProduct = new Product(cols[2], Double.parseDouble(cols[3]),
									Double.parseDouble(cols[4]), Double.parseDouble(cols[6]));

							existingProduct.incrementAll(newProduct);
							products.set(index, existingProduct);

						} catch (NoSuchElementException e) {
							// create new product
							Product newProduct = new Product(cols[2], Double.parseDouble(cols[3]),
									Double.parseDouble(cols[4]), Double.parseDouble(cols[6]));
							products.add(newProduct);

						}

					}
					count++;

				}

				// write to new file
				this.writeToCSV(totalProfit, products, fileKey+"-summary.csv", s3);
				
			} catch (final IOException e) {
				System.out.println("IOException: " + e.getMessage());
			}

			System.out.println("Finished... processing file");


			//delete from s3 after processing
			
			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
 
			s3.deleteObject(deleteRequest);
			System.out.println(fileKey +" deleted from bucket:" + bucketName );

		} else {
			System.out.println("No message found in the latest request");
			context.getLogger().log("No message found to process ");
		}

		timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
		context.getLogger().log("Invocation completed: " + timeStamp);

		return null;
	}

	public boolean writeToCSV(double totalProfit, ArrayList<Product> products, String fileName, S3Client s3) {


		FileWriter csvWriter;
		try {
			csvWriter = new FileWriter(fileName);

			csvWriter.append("Total Profit:");
			csvWriter.append(";");
			csvWriter.append(Double.toString(totalProfit));
			csvWriter.append("\n");
			csvWriter.append("\n");

			csvWriter.append("Product");
			csvWriter.append(";");
			csvWriter.append("Quantity");
			csvWriter.append(";");
			csvWriter.append("Total Price");
			csvWriter.append(";");
			csvWriter.append("Total Profit");
			csvWriter.append("\n");

			for (Product product : products) {
				csvWriter.append(product.getName());
				csvWriter.append(Double.toString(product.getQuantity()));
				csvWriter.append(";");
				csvWriter.append(Double.toString(product.getPrice()));
				csvWriter.append(";");
				csvWriter.append(Double.toString(product.getProfit()));
				csvWriter.append("\n");
			}

			csvWriter.flush();
			csvWriter.close();
			
			//update to s3 bucket
			File file = new File(fileName);
			Path filePath = file.toPath();
			
			//upload file to bucket
			boolean bucketUploaded = BucketOperations.uploadFileToBucket(s3, ShopConstants.bucket_name, fileName, filePath);
			
			if(bucketUploaded) {
				System.out.println("file uploaded");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

}
