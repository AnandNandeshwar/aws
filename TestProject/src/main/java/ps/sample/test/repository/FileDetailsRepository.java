package ps.sample.test.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import ps.sample.test.entity.FileDetails;

@Repository
public interface FileDetailsRepository extends CrudRepository<FileDetails, Integer> {

	public FileDetails findByFileKey(String fileKey);
	
}
