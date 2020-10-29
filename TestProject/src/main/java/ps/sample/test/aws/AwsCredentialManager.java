package ps.sample.test.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Component
public class AwsCredentialManager {
	
	@Value("${aws.access.key}")
	private String awsAccessKey;
	
	@Value("${aws.secret.key}")
	private String awsSecretKey;
	
	public AWSCredentials getAwsCredentials() {
		return new BasicAWSCredentials(awsAccessKey,awsSecretKey);
	}
	
	public AmazonS3 getAmazonS3Client() {
		return AmazonS3ClientBuilder
				  .standard()
				  .withCredentials(new AWSStaticCredentialsProvider(getAwsCredentials()))
				  .withRegion(Regions.AP_SOUTH_1)
				  .build();
	}
}
