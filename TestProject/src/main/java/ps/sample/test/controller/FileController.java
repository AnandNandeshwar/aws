package ps.sample.test.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ps.sample.test.service.FileService;
import ps.sample.test.utils.Feature;

@RestController
@RequestMapping("/file")
public class FileController {

	@Autowired
	private FileService fileService;
	
	@PostMapping(path = "/upload")
	public ResponseEntity<?> uploadFile(@RequestPart(value = "file") MultipartFile[] multipartFile){
		if(multipartFile == null || multipartFile.length == 0) {
			return new ResponseEntity<>("Invalid file upload request.",HttpStatus.BAD_REQUEST);
		}
		boolean isFileUplod = this.fileService.uploadFile(multipartFile,Feature.INVESTMENT);
		if(isFileUplod) {
			return new ResponseEntity<>("Document uploaded successfully.",HttpStatus.OK);
		}else {
			return new ResponseEntity<>("Something went wrong! Please try again after 30 seconds.",HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GetMapping(path = "/download")
	public ResponseEntity<?> getFile(@Param("key") String key){
		final byte[] data = this.fileService.getFile(key,Feature.INVESTMENT);
		
		if(data == null) {
			return new ResponseEntity<>("Unable to download file.",HttpStatus.INTERNAL_SERVER_ERROR);
		}
	    final ByteArrayResource resource = new ByteArrayResource(data);
		
	    return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
	            .contentLength(data.length)
	            .contentType(MediaType.APPLICATION_OCTET_STREAM)
	            .body(resource);
	}
	
	@GetMapping(path = "/download-url")
	public ResponseEntity<?> getDownloadUrl(@Param("key") String key){
		
		String downloadUrl = this.fileService.getFileDownloadUrl(key);
		
		if(downloadUrl != null) {
			return new ResponseEntity<>(downloadUrl,HttpStatus.OK);
		}
		return new ResponseEntity<>("Invalid file upload request.",HttpStatus.BAD_REQUEST);
		
	}
}
