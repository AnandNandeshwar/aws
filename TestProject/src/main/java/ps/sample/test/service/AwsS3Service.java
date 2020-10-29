package ps.sample.test.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

import ps.sample.test.utils.ExceptionPrinter;

@Service
public class AwsS3Service {
	
	/**
	 * Check if S3 bucket already exist or not
	 * @param s3Client
	 * @param bucketName
	 * @return
	 */
	public boolean isBucketExist(AmazonS3 s3Client,String bucketName) {
		if(s3Client.doesBucketExistV2(bucketName)) {
		    return true;
		}
		return false;
	}
	
	/**
	 * Creating S3 Bucket
	 * @param s3Client
	 * @param bucketName
	 * @return
	 */
	public boolean createBucket(AmazonS3 s3Client,String bucketName) {
		try {
			s3Client.createBucket(bucketName);
			return true;
		}catch (Exception e) {
			ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
		}
		return false;
	}
	
	/**
	 * Check if object exist in bucket or not. you can check directory exist or not
	 * @param bucketName
	 * @param objectName
	 * @param s3Client
	 * @return
	 */
	public boolean isObjectExist(String bucketName,String objectName, AmazonS3 s3Client) {
		if(s3Client.doesObjectExist(bucketName, objectName+"/")) {
			return true;
		}
		return false;
	}
	
	/**
	 * Creating directory or folder in the s3 bucket
	 * @param bucketName
	 * @param folderName
	 * @param client
	 * @return
	 */
	public boolean createFolder(String bucketName, String folderName, AmazonS3 client) {
		try {
			// create meta-data for your folder and set content-length to 0
		    ObjectMetadata metadata = new ObjectMetadata();
		    metadata.setContentLength(0);
		    // create empty content
		    InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
		    // create a PutObjectRequest passing the folder name suffixed by /
		    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,folderName + "/", emptyContent, metadata);
		    // send request to S3 to create folder
		    client.putObject(putObjectRequest);
		    return true;
		}catch (Exception e) {
			ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
		}
		return false;
	}
	
	/**
	 * Generate presigned download url for file with expiration date
	 * @param bucketName
	 * @param objectKey
	 * @param s3Client
	 * @return
	 */
	public URL generatePresignedUrl(String bucketName, String objectKey, AmazonS3 s3Client) {
		try {
			// Set the presigned URL to expire after one hour.
			java.util.Date expiration = new java.util.Date();
			long expTimeMillis = expiration.getTime();
			expTimeMillis += 1000 * 60 * 60;
			expiration.setTime(expTimeMillis);

			// Generate the presigned URL.
			System.out.println("Generating pre-signed URL.");
			GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName,
					objectKey).withMethod(HttpMethod.GET).withExpiration(expiration);
			URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

			System.out.println("Pre-Signed URL: " + url.toString());
			return url;
		} catch (AmazonServiceException e) {
			// The call was transmitted successfully, but Amazon S3 couldn't process
			// it, so it returned an error response.
			ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
		} catch (SdkClientException e) {
			// Amazon S3 couldn't be contacted for a response, or the client
			// couldn't parse the response from Amazon S3.
			ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
		}
		return null;
	}

	/**
	 * Generate publicly download url
	 * @param s3BucketRegion
	 * @param bucketName
	 * @param directory
	 * @param keyName
	 * @return
	 */
	public String generateFileDownloadUrl(String s3BucketRegion,String bucketName,String directory, String keyName) {
		return "https://"+bucketName+"."+s3BucketRegion+"/"+directory+"/"+keyName;
	}
	
	/**
	 * Getting / Read file from the S3 bucket
	 * @param bucketName
	 * @param fileName
	 * @param s3Client
	 * @return
	 */
	public S3Object getS3File(String bucketName,String fileName,AmazonS3 s3Client) {
		return s3Client.getObject(new GetObjectRequest(bucketName, fileName));
	}
	
	/**
	 * Upload objects in a single operation—With a single PUT operation you can upload objects up to 5 GB in size.
	 * @param bucketName
	 * @param directoryName
	 * @param file
	 * @param s3Client
	 * @return
	 */
	public PutObjectResult uploadFile(String bucketName,String directoryName,File file,AmazonS3 s3Client) {
		return  s3Client.putObject(new PutObjectRequest(bucketName+"/"+directoryName, file.getName(), file));
	}
	
	/**
	 * Upload objects in parts—Using the Multipart upload API you can upload large objects, up to 5 TB.
	 * @param s3Client
	 * @param file
	 * @param folderPath
	 * @param bucketName
	 * @return
	 */
	public boolean uploadFile(AmazonS3 s3Client,File file,String folderPath,String bucketName) {
		try {
			if(file != null) {
				long     contentLength = file.length();
				long     partSize = 5 * 1024 * 1024; // Set part size to 5 MB.
				/** 
				 * Create a list of ETag objects. You retrieve ETags for each object part uploaded,
				 * then, after each individual part has been uploaded, pass the list of ETags to
				 * the request to complete the upload. 
				 * 
				 **/
				List<PartETag> partETags = new ArrayList<PartETag>();

				// Initiate the multipart upload.
				InitiateMultipartUploadRequest initRequest  = new InitiateMultipartUploadRequest(bucketName+"/"+folderPath, file.getName()).withCannedACL(CannedAccessControlList.PublicRead);
				InitiateMultipartUploadResult  initResponse = s3Client.initiateMultipartUpload(initRequest);

				// Upload the file parts.
				long filePosition = 0;
				for (int i = 1; filePosition < contentLength; i++) {
					// Because the last part could be less than 5 MB, adjust the part size as needed.
					partSize = Math.min(partSize, (contentLength - filePosition));

					// Create the request to upload a part.
					UploadPartRequest uploadRequest = new UploadPartRequest().withBucketName(bucketName+"/"+folderPath).withKey(file.getName())
							.withUploadId(initResponse.getUploadId()).withPartNumber(i).withFileOffset(filePosition)
							.withFile(file).withPartSize(partSize);

					// Upload the part and add the response's ETag to our list.
					UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
					partETags.add(uploadResult.getPartETag());

					filePosition += partSize;
				}
				// Complete the multipart upload.
				CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName+"/"+folderPath, file.getName(),initResponse.getUploadId(), partETags);
				s3Client.completeMultipartUpload(compRequest);
			}
			return true;
		} catch (AmazonServiceException e) {
			// The call was transmitted successfully, but Amazon S3 couldn't process it, so it returned an error response.
			ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
		} catch (SdkClientException e) {
			// Amazon S3 couldn't be contacted for a response, or the client  couldn't parse the response from Amazon S3.
			ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
		}catch (Exception e) {
			ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
		}
		return false;
	}
}
