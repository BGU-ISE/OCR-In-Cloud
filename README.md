# OCR In Cloud
In this assignment we will code a real-world application to distributively apply OCR algorithms on images. Then we will display each image with its recognized text on a webpage.
## How to run our project
1. Create AWS role with the following:
   1. EC2FullAccess
   2. S3FullAccess
   3. SQSFullAccess
   4. AdministratorAccess
2. You need to create a file `localApp/src/main/resources/secure_info.properties`
3. The content of the file should be
    ```properties
    ami=<image to run for manager and workers>
    arn=arn:aws:iam::<your AWS account number>:instance-profile/<The name of the profile>
    keyName=<name of the keyPair>
    securityGroupIds=<single security group id>
   ```
4. Create `managerApp.jar` and `workerApp.jar` and `scp` them to the instance to `/home/ubuntu`.
5. Create new Amazon AMI based on the instance.
6. Install JDK 15 or above
7. Install Tesseract 4.00, and make sure that its data is in `/usr/share/tesseract-ocr/4.00/tessdata`
8. After the AMI is available, copy its ID to `localApp/src/main/resources/secure_info.properties`
9. run local app with 4 args:
   1. `args[0]`: input image file path
   2. `args[1]`: output file path
   3. `args[2]`: number of workers needed
   4. `args[3]`: (optional) must be equals to "terminate"
## Considerations
### Did you think about scalability?
Yes, on one hand, the Manager doesn't hold local app inforation on the ram, only the amount of "connected" local apps.
- The personal data such as number of urls remaining and return bucket name are held in temp bucket, its name is the personal return SQS queue of the local app.
- The Manager app reads the links file that the local app uploaded for him line after line **dynamically** such that it won't be resource consuming to read the hole file.
- The local app reads the final <link, OCR output> entries from the bucket entry **dynamically** when creating the file.
- Worker App won't save the image on disk, and reads it to memory **dynamically** (we assume that the image size won't pass 800 MB)
- We assume that image OCR description won't pass 200 KB
### What about persistence?
- We're catching all possible exeptions that might rais from the operation of our code, we do not protect against sudden termination from Amazon itself.
### Threads in our application
Only the Manager uses 2 thread:
- thread `main`: responsible for receiving messages (up to 10 at a time) from local apps and sending tasks to the workers
- thread `WorkerToManager`: responsible for receiving messages (up to 10 at a time) and upload their cotent to the proper S3 bucket of the relevant local app
## Technical stuff
- We used Ubuntu 20.04 based AMI, and instance type of T2-micro
- when used with 10 Workers and 24 links it takes about 3 minutes (including manager and workers startup time)
