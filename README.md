
# AWS SalesApp Project

Here we have implemented a system divided into four parts
- A client application for uploading sales files to an S3 bucket.
- Two worker classes, one in Lambda and the other running on an EC2 instance. These workers automatically summarize the uploaded files and creates summary files. They also delete the uploaded files after summary.
- A consolidator app that analyses the summary files and write the sale consolidation to the S3 bucket also.


## How to Run

S3, SNS and SQS [Local Application Directory: aws-sales-app]:
- Create an S3 bucket on AWS following the steps at https://ci.mines-stetienne.fr/cps2/course/cloud/lab/01-aws-ec2-s3.html, then set the bucket_name variable in the shops.ShopConstants.java file found in the shop package
- Create an SNS topic on AWS following the steps at https://ci.mines-stetienne.fr/cps2/course/cloud/lab/04-aws-sqs-sns.html, replace the topicARN variable in the shops.ShopConstants.java file with your topic topicARN of the aws-sales-app directory
- Create an SQS queue on AWS following the steps at https://ci.mines-stetienne.fr/cps2/course/cloud/lab/04-aws-sqs-sns.html, then replace the queueURL variable in the shops.ShopConstants.java file with your queueURL 
Lambda:
- Create a lambda function following the steps at https://ci.mines-stetienne.fr/cps2/course/cloud/lab/03-aws-lambda.html. It should subscribe to the SNS topic above as its trigger
- For the lambda function code, package the aws-sales-app by running the [ mvn package ] console command in the root folder of the project
- Upload the aws-sales-app-0.0.1-SNAPSHOT.jar file in the target folder as the code for the lamda. Prefarably via an s3 bucket if the size is large as recommended by AWS.
EC2 [Local Application Directory: ec2-sales-app]:
- Create EC2 by following steps in this URL: https://ci.mines-stetienne.fr/cps2/course/cloud/lab/01-aws-ec2-s3.html
- Connect to EC2 and install java using this command "sudo amazon-linux-extras install java-openjdk11"
- Type "aws configure" in EC2 terminal (For initial configuration you can follow this URL: https://ci.mines-stetienne.fr/cps2/course/cloud/lab/02-aws-cli-sdk-java.html ). this will generate .aws folder in home directory. Replace information in credentials file with information from awsAcademy under AWS CLI:
- On your machine in ec2-sales-app directory run "mvn package" command. this will generate "ec2-sales-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar" file in the target folder
- Upload generated file to EC2 machine's home folder
- In EC2 terminal run command "java -jar ec2-sales-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar". This will start the program
- Worker should start running on the remote EC2 machine. It will periodically (,every 5 minutes from the termination of the last process,) check if there are any new messages in SQS queue and calculate stores' summary which will be saved in the S3 bucket you have configured

Client
- Update you Region in the shop.ShopConstants.java file if applicable
- Update the full path to your sales folder containing the sales files to be analysed
- Prefarably, open code in an IDE
- Load maven packages in pom.xml File
- From an IDE, e.g. Eclipse, right click on the Client.java. Navigate to Run As -> Run Configurations or the equivalent in your choses IDE. The goal here is to pass runtime arguments
- In the Main tab, select shop.Client class. Next, in the Arguments tab, add [storename] [date] [workertype]
- For example, if you want to execute the '01-10-2022-store1.csv file using the lambda worker, the arguments will be store1 01-10-2022 lambda. Replace lambda with ec2 if you are running with EC2 worker
- The file will now be read and uploaded to the s3 bucket you have configured in the ShopConstants.java file
- If you passed lambda as the worker type, a message will be sent to the SNS topic which will in turn trigger the lambda function you have created. You can check the logs in cloudwatch Management Console to monitor execution
- Once the processing is complete, you will notice that the initially uploaded file in the s3 bucket would have been deleted and a new summary file would be created
- If you passed ec2 as the worker type, a message will be sent to the SQS queue. When next the worker reads from the queue, it will process the file, clear the message from queue and upload summary file to s3 bucket as well as delete initially uploaded file

Consolidator [Local Application Directory: aws-sales-app]
- From an IDE, e.g. Eclipse, right click on the shops.Consolidator.java. Navigate to Run As -> Run Configurations or the equivalent in your choses IDE. The goal here is to pass runtime arguments
- In the Main tab, select shop.Consolidator class. Next, in the Arguments tab, add [date] (in format matching the one of summary files)
- Summary file of all stores is generated and uploaded to the configured S3 Bucket
