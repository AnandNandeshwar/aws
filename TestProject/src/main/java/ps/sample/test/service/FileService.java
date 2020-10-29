package ps.sample.test.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.util.IOUtils;

import ps.sample.test.aws.AwsCredentialManager;
import ps.sample.test.entity.FileDetails;
import ps.sample.test.repository.FileDetailsRepository;
import ps.sample.test.utils.ExceptionPrinter;
import ps.sample.test.utils.Feature;

@Service
public class FileService {

	@Autowired
	private FileDetailsRepository fileDetailsRepository;
	
	@Autowired
	private AwsCredentialManager awsCredentialManager;
	
	@Autowired
	private AwsS3Service awsS3Service;;
	
	@Value("${aws.s3.bucket.name}")
	private String bucketName;
	
	@Value("${aws.s3.bucket.region}")
	private String s3BucketRegion;
	
	/**
	 * Upload objects in parts—Using the Multipart upload API you can upload large objects, up to 5 TB.
	 * @param multipartFile
	 * @param feature - Enum object which tells upload request for which feature e.g investment,payroll,employeemaster etc.
	 * @return
	 */
	public boolean uploadFile(MultipartFile[] multipartFile,Feature feature) {
		try {
			AmazonS3 s3Client   = this.awsCredentialManager.getAmazonS3Client();
			String   folderPath = getFileDirectory(feature);
			
			if(!this.awsS3Service.isBucketExist(s3Client, this.bucketName)) {
				if(!this.awsS3Service.createBucket(s3Client, this.bucketName)) {
					return false;
				}
			}
			if(!this.awsS3Service.isObjectExist(this.bucketName, folderPath, s3Client)) {
				if(!this.awsS3Service.createFolder(this.bucketName, folderPath, s3Client)) {
					return false;
				}
			}
			for(int fileIndex = 0; fileIndex < multipartFile.length; fileIndex++) {
				File file = convertMultiPartToFile(multipartFile[fileIndex]);
				
				//awsS3Service.uploadFile(this.bucketName, folderPath, file, s3Client);
				
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
					saveFileDetails(file.getName(),initResponse.getUploadId(),file.getName(),
							this.awsS3Service.generateFileDownloadUrl(this.s3BucketRegion,this.bucketName,folderPath, file.getName()));
				}
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
	
	private File convertMultiPartToFile(MultipartFile file) throws IOException {
	    File convFile = new File(file.getOriginalFilename());
	    FileOutputStream fos = new FileOutputStream(convFile);
	    fos.write(file.getBytes());
	    fos.close();
	    return convFile;
	}
	
	private String getFileDirectory(Feature feature) {
		switch (feature) {
		case INVESTMENT:
			return "paysquare/investment";
		case EMPLOYEE_MASTER:
			return "paysquare/employee";
		case PAYROLL:
			return "paysquare/payroll";
		default:
			break;
		}
		return "paysquare";
	}
	
	private void saveFileDetails(String keyName, String uploadId, String name,String fileUrl) {
		FileDetails fileDetails = new FileDetails();
		fileDetails.setCreatedBy("Anand Nandeshwar");
		fileDetails.setCreatedDateTime(new Date());
		fileDetails.setFileKey(keyName);
		fileDetails.setUploadId(uploadId);
		fileDetails.setFileName(name);
		fileDetails.setFileUrl(fileUrl);
		this.fileDetailsRepository.save(fileDetails);
	}

	public String getFileDownloadUrl(String key) {
		FileDetails fileDetails = this.fileDetailsRepository.findByFileKey(key);
		return fileDetails == null ?  null :  fileDetails.getFileUrl();
	}
	
	/**
	 * Getting s3Object i.e. download file from aws
	 * @param key
	 * @param feature
	 * @return
	 */
	public byte[] getFile(String key,Feature feature) {
        S3Object s3Object = null;
        try {
            AmazonS3 s3Client = this.awsCredentialManager.getAmazonS3Client();
            // Get an object and print its contents.
            System.out.println("***Downloading an file***");
            s3Object = s3Client.getObject(new GetObjectRequest(this.bucketName+"/"+this.getFileDirectory(feature),key));
            return IOUtils.toByteArray(s3Object.getObjectContent());
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process 
            // it, so it returned an error response.
        	ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
        	ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
        }catch (Exception e) {
			ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
		} finally {
			// To ensure that the network connection doesn't remain open, close any open
			// input streams.
			try {
				if (s3Object != null) {
					s3Object.close();
				}
			} catch (Exception e) {
				ExceptionPrinter.printWarningLogs(e, this.getClass().getName());
			}
		}
       return null;
    }

    private  StringBuilder displayTextInputStream(InputStream input) throws IOException {
        // Read the text input stream one line at a time and display each line.
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder builder = new StringBuilder();
        while ((builder.append(reader.readLine())) != null) {
            System.out.println(builder);
        }
        return builder;
    }
}
