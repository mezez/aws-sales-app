package shop.lambda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;


import shop.Product;
import shop.ShopConstants;
import software.amazon.awssdk.services.s3.S3Client;

public class MockWorker {

	public static void main(String[] args) {
		// PROCESS FILE
		S3Client s3 = S3Client.builder().region(ShopConstants.region).build();
		System.out.println("Processing: " + ShopConstants.sales_files_folder + "01-10-2022-store1.csv");

		double totalProfit = 0.0;
		int count = 0;
		String line = "";
		try (BufferedReader reader = new BufferedReader(
				new FileReader(ShopConstants.sales_files_folder + "01-10-2022-store1.csv"));) {

			// TODO UPDATE FILE PROCESSING
			// by store
			

			
			while ((line = reader.readLine()) != null) {
				// skip header line
				if (count > 0) {
					// use semicolon as separator
					String[] cols = line.split(";");
					totalProfit += Double.parseDouble(cols[6]);
				}
				count++;

			}
		} catch (final IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}
		
		try (BufferedReader reader = new BufferedReader(
				new FileReader(ShopConstants.sales_files_folder + "01-10-2022-store1.csv"));) {

			// by product
			count = 0;
			line = "";
			ArrayList<Product> products = new ArrayList<Product>();
//			Product newwProduct = new Product("p21", 2, 33.40, 12.33);
//			products.add(newwProduct);

			while ((line = reader.readLine()) != null) {
				// skip header line
				if (count > 0) {
					// use semicolon as separator
					String[] cols = line.split(";");

					// create new product if it does not already exist in array
					String nameToMatch = cols[2];
					
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
						
						Product newProduct = new Product(cols[2], Double.parseDouble(cols[3]),
								Double.parseDouble(cols[4]), Double.parseDouble(cols[6]));
						products.add(newProduct);

					}

				}
				count++;

			}

			// write to new file
			writeToCSV(totalProfit, products, "C:\\Users\\admin\\Downloads\\mm-summary.csv", s3);

		} catch (final IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}

		System.out.println("Finished... processing file");

		// delete from s3 after processing


	}

	public static boolean writeToCSV(double totalProfit, ArrayList<Product> products, String fileName, S3Client s3) {

		FileWriter csvWriter;
		try {
			csvWriter = new FileWriter(fileName);

			csvWriter.append("Total Profit");
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

			// update to s3 bucket
			File file = new File(fileName);
			Path filePath = file.toPath();
			
			System.out.println("new file"+filePath);


		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
}
