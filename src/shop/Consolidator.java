package shop;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

public class Consolidator {
	public static void main(String[] args) {
		S3Client s3 = S3Client.builder().region(ShopConstants.region).build();
		String date = args[0].toString();
		// Where 0 is Minimum and 1 is Maximum
		Double[] Profit = new Double[2];
		// Where 0 is Name least profitable store and 1 is name of most profitable store
		String[] Store = new String[2];
		ArrayList<Product> products = new ArrayList<Product>();
		if (!BucketExist(s3, ShopConstants.bucket_name)) {
			createBucket(s3, ShopConstants.bucket_name);
		}
		List<String> fileNames = filesOfDay(s3, ShopConstants.bucket_name, date);
		int fileNumber = 0;
		for (String fileName : fileNames) {
			String storeName = fileName.substring(fileName.indexOf(date) + 11, fileName.length() - 4);
			try {
				DownloadFile(ShopConstants.bucket_name, fileName, s3);
			} catch (IOException e) {
				e.printStackTrace();
			}

			BufferedReader reader;
			int count = 0;
			try {
				reader = new BufferedReader(new FileReader(fileName));
				String line = reader.readLine();
				while (line != null) {
					if (count == 0) {
						String[] cols = line.split(";");
						Double totalProfit = Double.parseDouble(cols[1]);
						if (fileNumber == 0) {
							Profit[0] = totalProfit;
							Profit[1] = totalProfit;
							Store[0] = storeName;
							Store[1] = storeName;
						} else {
							if (totalProfit < Profit[0]) {
								Profit[0] = totalProfit;
								Store[0] = storeName;
							}
							if (totalProfit > Profit[0]) {
								Profit[1] = totalProfit;
								Store[1] = storeName;
							}
						}
					} else if (count == 1 || count == 2) {

					} else {
						String[] cols = line.split(";");
						String nameToMatch = cols[0];

						try {
							int index = IntStream.range(0, products.size())
									.filter(i -> nameToMatch.equals(products.get(i).getName())).findFirst().getAsInt();

							// update existing
							Product existingProduct = products.get(index);

							Product newProduct = new Product(cols[0], Double.parseDouble(cols[1]),
									Double.parseDouble(cols[2]), Double.parseDouble(cols[3]));

							existingProduct.incrementAll(newProduct);
							products.set(index, existingProduct);

						} catch (NoSuchElementException e) {
							// create new product
							Product newProduct = new Product(cols[0], Double.parseDouble(cols[1]),
									Double.parseDouble(cols[2]), Double.parseDouble(cols[3]));
							products.add(newProduct);

						}
					}
					count++;
					line = reader.readLine();
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			fileNumber++;
			deleteFile(fileName);
		}
		writeToCSV(s3, Profit, Store, products, date);
	}

	public static void uploadBucket(S3Client s3, String fileName) {
		PutObjectRequest request = PutObjectRequest.builder().bucket(ShopConstants.bucket_name).key(fileName).build();
		s3.putObject(request, RequestBody.fromFile(new File(fileName)));
		System.out.println("FileAdded");
		deleteFile(fileName);
	}

	public static void writeToCSV(S3Client s3, Double[] Profit, String[] Store, ArrayList<Product> products,
			String date) {
		FileWriter csvWriter;
		String fileName = "Consolidated-" + date.replace("-", ".") + ".csv";
		try {
			csvWriter = new FileWriter(fileName);

			csvWriter.append("Minimum earned");
			csvWriter.append(";");
			csvWriter.append(Store[0]);
			csvWriter.append(";");
			csvWriter.append(Double.toString(Profit[0]));
			csvWriter.append("\n");
			csvWriter.append("Maximum earned");
			csvWriter.append(";");
			csvWriter.append(Store[1]);
			csvWriter.append(";");
			csvWriter.append(Double.toString(Profit[1]));
			csvWriter.append("\n");
			csvWriter.append("\n");

			csvWriter.append("Product");
			csvWriter.append(";");
			csvWriter.append("TotalQuantity");
			csvWriter.append(";");
			csvWriter.append("TotalPrice");
			csvWriter.append(";");
			csvWriter.append("TotalProfit");
			csvWriter.append("\n");

			for (Product product : products) {
				csvWriter.append(product.getName());
				csvWriter.append(";");
				csvWriter.append(Double.toString(product.getQuantity()));
				csvWriter.append(";");
				csvWriter.append(Double.toString(product.getPrice()));
				csvWriter.append(";");
				csvWriter.append(Double.toString(product.getProfit()));
				csvWriter.append("\n");
			}

			csvWriter.flush();
			csvWriter.close();

			System.out.println("Writing done for summary file:" + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		uploadBucket(s3, fileName);
		deleteFile(fileName);
	}

	public static void deleteFile(String fileName) {
		File f = new File(fileName);
		f.delete();
		System.out.println("File delete locally");
	}

	public static void DownloadFile(String bucketName, String fileName, S3Client s3) throws FileNotFoundException {
		if (BucketExist(s3, bucketName)) {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(fileName).build();

			ResponseInputStream<GetObjectResponse> response = s3.getObject(getObjectRequest);
			BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileName));
			byte[] buffer = new byte[4096];
			int bytesRead = -1;
			try {
				while ((bytesRead = response.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
				response.close();
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(fileName + " downloaded");
		}
	}

	private static boolean BucketExist(S3Client s3, String bucketName) {
		ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
		ListBucketsResponse listBucketResponse = s3.listBuckets(listBucketsRequest);
		return listBucketResponse.buckets().stream().anyMatch(x -> x.name().equals(bucketName));
	}

	public static void createBucket(S3Client s3Client, String bucketName) {

		try {
			S3Waiter s3Waiter = s3Client.waiter();
			CreateBucketRequest bucketRequest = CreateBucketRequest.builder().bucket(bucketName).build();

			s3Client.createBucket(bucketRequest);
			HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder().bucket(bucketName).build();

			// Wait until the bucket is created and print out the response.
			WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
			waiterResponse.matched().response().ifPresent(System.out::println);
			System.out.println(bucketName + " is ready");

		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
			System.exit(1);
		}
	}

	public static List<String> filesOfDay(S3Client s3, String bucketName, String date) {
		List<String> files = new ArrayList<>();
		try {
			ListObjectsRequest listObjects = ListObjectsRequest.builder().bucket(bucketName).build();

			ListObjectsResponse res = s3.listObjects(listObjects);
			List<S3Object> objects = res.contents();
			for (S3Object obj : objects) {
				if (obj.key().contains(date)) {
					files.add(obj.key());
				}
			}
			return files;
		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
			System.exit(1);
			return null;
		}
	}
}
